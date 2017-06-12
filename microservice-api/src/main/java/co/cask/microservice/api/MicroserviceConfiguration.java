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

import co.cask.microservice.annotation.PublicEvolving;
import com.google.common.base.Objects;

import java.util.Map;

/**
 * Class description here.
 *
 * @see Endpoints
 */
@PublicEvolving
public final class MicroserviceConfiguration {
  private int instances;
  private int ethreshold;
  private Map<String, String> properties;
  private int vcores;
  private int memory;
  private Endpoints endpoints;

  public int getInstances() {
    return instances;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public Endpoints getEndpoints() {
    return endpoints;
  }

  public int getVCores() {
    return vcores;
  }

  public int getMemory() {
    return memory;
  }

  public int getEthreshold() {
    return ethreshold;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("instances", instances)
      .add("vcores", vcores)
      .add("ethreshold", ethreshold)
      .add("memoryMB", memory)
      .add("properties", properties)
      .add("endpoints", endpoints)
      .toString();
  }
}
