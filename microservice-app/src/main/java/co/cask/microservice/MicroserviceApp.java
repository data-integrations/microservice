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

import co.cask.cdap.api.app.AbstractApplication;
import co.cask.cdap.api.dataset.lib.KeyValueTable;
import co.cask.cdap.api.plugin.PluginProperties;
import co.cask.microservice.api.Microservice;
import co.cask.microservice.api.MicroserviceDefinition;
import co.cask.microservice.api.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * This class {@link MicroserviceApp} is CDAP Application that configures a
 * microservice as a CDAP Worker. This application is specialized or configured
 * using a plugin of type 'microservice'.
 *
 * A microservice is constructed using a CDAP worker with the inbound events
 * consumed from a TMS (Transactional Message Service) topic, processed and
 * then optionally passed to the outbound queue.
 *
 * TMS provides a reliable queue for passing messages between microservices.
 *
 * Following is the configuration that configures this application.
 *
 * <code>
 *   {
 *      "artifact" : {
 *        "name" : "microservice-app",
 *        "version" : "1.0-SNAPSHOT",
 *        "scope" : "user"
 *      },
 *      "config" : {
 *        "version" : 1,
 *        "id" : "GeoFenceNotifier",
 *        "description" : "Description for the microservice.",
 *        "plugin" : {
 *          "name" : "abc",
 *          "version" : "1.1-SNAPSHOT",
 *          "scope" : "user"
 *        }
 *      "configuration" : {
 *        "instances" : 1,
 *        "vcores" : 1,
 *        "memory" : 512,
 *        "endpoints" : {
 *          "in" : [
 *            "ms://ericsson.com/geofence"
 *          ],
 *          "out" : [
 *            "ms://ericsson.com/notify"
 *          ]
 *        },
 *        "properties" : {
 *          "a" : "b",
 *          "c" : "d"
 *        }
 *      }
 *    }
 *  }
 * </code>
 *
 * @see MicroserviceWorker
 * @since 1.0
 */
public final class MicroserviceApp extends AbstractApplication<MicroserviceDefinition> {
  private static final Logger LOG = LoggerFactory.getLogger(MicroserviceApp.class);

  /**
   * Configures this application based on the configuration passed to this application.
   * The configuration JSON is as specified above.
   *
   * The configuration passed to the application are then passed onto the plugin as
   * properties.
   */
  @Override
  public void configure() {
    MicroserviceDefinition definition = getConfig();

    // Set the top level information for the microservice.
    setDescription(definition.getDescription());

    // Pass the configuration to the plugin
    PluginProperties.Builder builder = PluginProperties.builder().addAll(definition.getConfiguration().getProperties());
    String uuid = UUID.randomUUID().toString();
    Plugin plugin = definition.getPlugin();
    if (plugin != null) {
      usePlugin(Microservice.TYPE, plugin.getName(), uuid, builder.build());
    } else {
      throw new IllegalArgumentException(
        String.format("Microservice plugin is not specified. Please specify the plugin - name, version and scope.")
      );
    }

    createDataset(Microservice.TYPE, KeyValueTable.class);

    // Create a unique id for the instance of this plugin.
    addWorker(new MicroserviceWorker(uuid, definition));
  }
}
