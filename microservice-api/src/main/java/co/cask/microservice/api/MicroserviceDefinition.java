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

import co.cask.cdap.api.Config;
import co.cask.microservice.annotation.PublicEvolving;
import com.google.common.base.Objects;

/**
 * This class define a single microservice.
 *
 * @see MicroserviceConfiguration
 * @since 1.0
 */
@PublicEvolving
public final class MicroserviceDefinition extends Config {
  private int version;
  private String id;
  private String description;
  private Plugin plugin;
  private MicroserviceConfiguration configuration;

  public int getVersion() {
    return version;
  }

  public Plugin getPlugin() {
    return plugin;
  }

  /**
   * @return Id of the microservice.
   */
  public String getId() {
    return id;
  }

  /**
   * @return Description for the microservice.
   */
  public String getDescription() {
    return description;
  }

  /**
   * @return Configuration for the microservice.
   */
  public MicroserviceConfiguration getConfiguration() {
    return configuration;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("id", id)
      .add("version", version)
      .add("description", description)
      .add("plugin", plugin)
      .add("configuration", configuration)
      .toString();
  }
}
