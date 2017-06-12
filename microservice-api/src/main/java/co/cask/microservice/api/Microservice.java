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

package co.cask.microservice.api;

import co.cask.cdap.api.data.format.StructuredRecord;
import co.cask.microservice.annotation.PublicEvolving;

import java.util.List;

/**
 * This class defines the microservice interface that users would implement.
 *
 * Following is an example on how this class can be used.
 *
 * <code>
 *   @Plugin(type = Microservice.TYPE)
 *   @Name("MyMicroservice")
 *   @Description("This is a sample microservice")
 *   public final class MyMicroservice implements Microservice {
 *     ...
 *     ...
 *   }
 * </code>
 *
 * @see MicroserviceDefinition
 * @see MicroserviceContext
 * @see MicroserviceConfiguration
 * @since 1.0
 */
@PublicEvolving
public interface Microservice {
  String TYPE = "microservice";

  /**
   * {@link Microservice#start(MicroserviceContext)} is invoked just before starting the microservice.
   * This is invoked at runtime of the microservice. This method is added to support resource initialization
   * or creation.
   *
   * @param context Runtime context includes the configuration.
   * @throws MicroserviceException thrown if there is issue with initialization of microservice.
   * @see Microservice#stop()
   * @see MicroserviceContext
   */
  void start(MicroserviceContext context) throws MicroserviceException;

  /**
   * Processes the records that are defined using {@link StructuredRecord}.
   *
   * @param input {@link StructuredRecord} of the event being received.
   * @return List of {@link StructuredRecord}
   * @throws MicroserviceException  thrown if there is issue with processing of microservice.
   * @see Microservice#stop()
   * @see EventContext
   */
  void consume(byte[] input, EventContext context) throws MicroserviceException;

  /**
   * Processes the records that are defined using {@link StructuredRecord}.
   *
   * @param input {@link StructuredRecord} of the event being received.
   * @return List of {@link StructuredRecord}
   * @throws MicroserviceException  thrown if there is issue with processing of microservice.
   * @see Microservice#stop()
   * @see EventContext
   */
  List<byte[]> process(byte[] input, EventContext context) throws MicroserviceException;

  /**
   * Processes the records that are defined using {@link StructuredRecord}.
   *
   * @return List of {@link StructuredRecord}
   * @throws MicroserviceException  thrown if there is issue with processing of microservice.
   * @see Microservice#stop()
   * @see EventContext
   */
  List<byte[]> produce() throws MicroserviceException;

  /**
   * {@link Microservice#stop()} invoked when the microservice is being shutdown.
   *
   * @throws MicroserviceException  thrown if there is issue with stopping of microservice.
   */
  void stop() throws MicroserviceException;
}
