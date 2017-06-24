/*
 * Copyright © 2017 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.microservice;

import co.cask.cdap.api.Resources;
import co.cask.cdap.api.TxRunnable;
import co.cask.cdap.api.common.Bytes;
import co.cask.cdap.api.data.DatasetContext;
import co.cask.cdap.api.dataset.lib.CloseableIterator;
import co.cask.cdap.api.dataset.lib.KeyValueTable;
import co.cask.cdap.api.messaging.Message;
import co.cask.cdap.api.messaging.MessageFetcher;
import co.cask.cdap.api.messaging.MessagePublisher;
import co.cask.cdap.api.messaging.TopicAlreadyExistsException;
import co.cask.cdap.api.messaging.TopicNotFoundException;
import co.cask.cdap.api.metrics.Metrics;
import co.cask.cdap.api.worker.AbstractWorker;
import co.cask.cdap.api.worker.WorkerContext;
import co.cask.microservice.api.Endpoints;
import co.cask.microservice.api.EventContext;
import co.cask.microservice.api.Microservice;
import co.cask.microservice.api.MicroserviceConfiguration;
import co.cask.microservice.api.MicroserviceContext;
import co.cask.microservice.api.MicroserviceDefinition;
import co.cask.microservice.api.MicroserviceException;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.gson.Gson;
import org.apache.tephra.TransactionFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of a Reactive Microservice within a container provided for runtime by {@link AbstractWorker}
 * and the Persistent queues provided by TMS.
 */
public class MicroserviceWorker extends AbstractWorker {
  private static final Logger LOG = LoggerFactory.getLogger(MicroserviceWorker.class);
  private static final Gson GSON = new Gson();
  private Microservice service;
  private MicroserviceDefinition definition;
  private MicroserviceContext microserviceContext;
  private MessageFetcher fetcher = null;
  private MessagePublisher publisher = null;
  private Metrics metrics;
  private String pluginId;
  private int fetchSize;
  private AtomicReference<String> messageId;
  private AtomicBoolean stop;

  public MicroserviceWorker(String pluginId, MicroserviceDefinition definition) {
    this.pluginId = pluginId;
    this.definition = definition;
  }

  @Override
  protected void configure() {
    super.configure();

    // Extract configuration and set the right properties.
    MicroserviceConfiguration configuration = definition.getConfiguration();
    int instances = configuration.getInstances() == 0 ? 1 : configuration.getInstances();
    int vcores = configuration.getVCores() == 0 ? 1 : configuration.getVCores();
    int memory = configuration.getMemory() == 0 ? 512 : configuration.getMemory();

    Endpoints endpoints = configuration.getEndpoints();
    if (endpoints.getInbound().size() > 1) {
      throw new IllegalArgumentException(
        String.format("Microservice currently supports only one inbound event queue. There are %d defined as per" +
                        "your configuration.", endpoints.getInbound().size())
      );
    }

    if (endpoints.getOutbound().size() > 1) {
      throw new IllegalArgumentException(
        String.format("Microservice currently supports only zero or one outbound event queue. " +
                        "There are %d defined as per your configuration.", endpoints.getOutbound().size())
      );
    }

    Map<String, String> properties = new HashMap<>();
    properties.put(Constants.ID, pluginId);
    properties.put(Constants.CONFIG, GSON.toJson(definition));

    // Sets all the configurations and properties for the application.
    setName(Microservice.TYPE);
    setDescription(definition.getDescription());
    setResources(new Resources(memory, vcores));
    setInstances(instances);
    setProperties(properties);

    LOG.info("Microservice '{}' configured to use {} instances, with {} vcores and {}MB memory.", definition.getId(),
             instances, vcores, memory);
  }

  @Override
  public void initialize(WorkerContext context) throws Exception {
    super.initialize(context);

    // Extract configuration from the runtime.
    Map<String, String> arguments = getContext().getSpecification().getProperties();
    pluginId = arguments.get(Constants.ID);
    definition = GSON.fromJson(arguments.get(Constants.CONFIG), MicroserviceDefinition.class);

    // Create an instance of microservice based on the plugin id.
    service = context.newPluginInstance(pluginId);

    // Create the instance of microservice context to be passed at runtime
    // to the plugin.
    microserviceContext = new MicroserviceContext(context, metrics, definition);

    // Store the properties with each queue that is being created by this microservice.
    Map<String, String> properties = new HashMap<>();
    properties.put(Constants.ID, definition.getId());
    properties.put(Constants.CREATED, Long.toString(System.currentTimeMillis()/1000));

    LOG.info("Starting microservice '{}'", definition.getId());
    Endpoints endpoints = definition.getConfiguration().getEndpoints();
    fetchSize = endpoints.getFetch() == 0 ? 100 : endpoints.getFetch();

    // Creating inbound queues if they don't exist.
    if (endpoints.getInbound().size() > 0) {
      fetcher = getContext().getMessageFetcher();
      List<String> inbound = endpoints.getInbound();
      for (String endpoint : inbound) {
        try {
          getContext().getAdmin().getTopicProperties(endpoint);
        } catch (TopicNotFoundException e) {
          // Topic not found, so creating it.
          try {
            getContext().getAdmin().createTopic(endpoint, properties);
          } catch (TopicAlreadyExistsException e1) {
            // No-Op
          } catch (IOException e2) {
            LOG.error("Failure to create inbound endpoint '{}' for microservice '{}'.", endpoint, definition.getId());
            throw e2;
          }
        }
      }
      LOG.info("Attaching to {} inbound event queues [{}]", inbound.size(), Joiner.on(",").join(inbound));
    }

    // Creating outbound queues if they don't exist.
    if (endpoints.getOutbound().size() > 0) {
      publisher = getContext().getMessagePublisher();
      List<String> outbound = endpoints.getOutbound();
      for (String endpoint : outbound) {
        try {
          getContext().getAdmin().getTopicProperties(endpoint);
        } catch (TopicNotFoundException e) {
          // Topic not found, so creating it.
          try {
            getContext().getAdmin().createTopic(endpoint, properties);
          } catch (TopicAlreadyExistsException e1) {
            // No-Op
          } catch (IOException e2) {
            LOG.error("Failure to create outbound endpoint '{}' for microservice '{}'.", endpoint, definition.getId());
            throw e2;
          }
        }
      }
      LOG.info("Dispatching to {} outbound event queues [{}]", outbound.size(), Joiner.on(",").join(outbound));
    }

    messageId = new AtomicReference<>(null);
    stop = new AtomicBoolean(false);
  }

  /**
   * When an object implementing interface <code>Runnable</code> is used
   * to create a thread, starting the thread causes the object's
   * <code>run</code> method to be called in that separately executing
   * thread.
   * <p>
   * The general contract of the method <code>run</code> is that it may
   * take any action whatsoever.
   *
   * @see Thread#run()
   */
  @Override
  public void run() {
    try {

      service.start(microserviceContext);
      Endpoints endpoints = definition.getConfiguration().getEndpoints();
      if (fetcher != null && endpoints.getInbound().size() > 0) {
        consumeLoop(endpoints.getInbound(), endpoints.getOutbound());
      } else if (publisher != null && endpoints.getOutbound().size() > 0){
        produceLoop();
      }
      service.stop();
    } catch (MicroserviceException e) {
      LOG.error("Microservice '{}' unexpectedly terminated. {}", definition.getId(), e);
    }
  }

  /**
   * Stopping microservice.
   */
  @Override
  public void stop() {
    super.stop();
    stop.set(true);
  }

  private void consumeLoop(List<String> inbounds, List<String> outbounds) {
    String namespace = getContext().getNamespace();
    final String inBoundTopic = inbounds.get(0);
    String outBoundTopic = null;
    if (outbounds.size() > 0) {
      outBoundTopic = outbounds.get(0);
    }

    int errorCount = 0;
    while(!stop.get()) {
      try {
        try (CloseableIterator<Message> messages = fetcher.fetch(namespace, inBoundTopic, fetchSize, messageId.get())) {
          while(messages.hasNext()) {
            Message message = messages.next();
            try {
              List<byte[]> events;
              if (publisher != null) {
                events = service.process(message.getPayload(),
                                         new EventContext(namespace, inBoundTopic, message.getId()));
                for (byte[] event : events) {
                  publisher.publish(namespace, outBoundTopic, event);
                }
              } else {
                service.consume(message.getPayload(),
                                         new EventContext(namespace, inBoundTopic, message.getId()));
              }
              messageId.set(message.getId());
            } catch (MicroserviceException e) {
              LOG.warn(e.getMessage(), e);
              if (errorCount > 10) {
                stop.set(true);
                metrics.count(getMetricName("error"), 1);
                break;
              }
              errorCount++;
            }
          }
        }
      } catch (TopicNotFoundException e) {
        metrics.count(getMetricName("topic.error"), 1);
        LOG.error(e.getMessage());
      } catch (IOException e) {
        metrics.count(getMetricName("io.error"), 1);
        LOG.error(e.getMessage());
      } finally {
        saveMessageId();
      }
    }
  }

  private void produceLoop() {
    String namespace = getContext().getNamespace();
    List<String> outbounds = definition.getConfiguration().getEndpoints().getOutbound();
    String outBoundTopic = null;
    if (outbounds.size() > 0) {
      outBoundTopic = outbounds.get(0);
    }

    while(!stop.get()) {
      try {
        List<byte[]> events = service.produce();
        metrics.count(getMetricName("in"), events.size());
        if (publisher != null) {
          for(byte[] event : events) {
            try {
              publisher.publish(namespace, outBoundTopic, event);
            } catch (TopicNotFoundException e) {
              metrics.count(getMetricName("topic.error"), 1);
              LOG.warn(e.getMessage());
            } catch (IOException e) {
              metrics.count(getMetricName("io.error"), 1);
              LOG.warn(e.getMessage());
            }
          }
          metrics.count(getMetricName("out"), events.size());
        }
      } catch (MicroserviceException e) {
        metrics.count(getMetricName("microservice.error"), 1);
        LOG.error(e.getMessage());
      } finally {
        saveMessageId();
      }
    }
  }

  private void saveMessageId() {
  }

  private String getMetricName(String name) {
    return String.format("%s.%s", definition.getId(), name);
  }

  private byte[] getKey(String id, String topic) {
    return Bytes.toBytes(String.format("%s:%s", id, topic));
  }

  private String getOffset(final String id, final String topic) {
    final String[] offset = new String[1];
    try {
      getContext().execute(new TxRunnable() {
        @Override
        public void run(DatasetContext context) throws Exception {
          KeyValueTable dataset = context.getDataset(Microservice.TYPE);
          byte[] bytes = dataset.read(getKey(id, topic));
          if (bytes == null) {
            offset[0] = null;
          }
          offset[0] = Bytes.toString(bytes);
        }
      });
    } catch (TransactionFailureException e) {
      throw Throwables.propagate(e);
    }
    return offset[0];
  }

  private void setOffset(final String id, final String topic, final String value) {
    final String[] offset = new String[1];
    try {
      getContext().execute(new TxRunnable() {
        @Override
        public void run(DatasetContext context) throws Exception {
          KeyValueTable dataset = context.getDataset(Microservice.TYPE);
          dataset.write(getKey(id, topic), Bytes.toBytes(value));
        }
      });
    } catch (TransactionFailureException e) {
      throw Throwables.propagate(e);
    }
  }

}
