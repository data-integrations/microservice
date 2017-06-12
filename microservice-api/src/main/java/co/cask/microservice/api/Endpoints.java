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

import java.util.ArrayList;
import java.util.List;

/**
 * This class defines the endpoints for the worker.
 * <p>
 *   Inbound queues are required, but not necessary to have outbound queues.
 * </p>
 */
@PublicEvolving
public final class Endpoints {
  // Defines the fetch size across all the queues.
  private int fetch;

  // Defines the in-bound queues.
  private List<String> in;

  // Defines the out-bound queues.
  private List<String> out;

  /**
   * @return List of inbound queues.
   */
  public List<String> getInbound() {
    return in == null ? new ArrayList<String>() : in;
  }

  /**
   * @return List of outbound queues.
   */
  public List<String> getOutbound() {
    return out == null ? new ArrayList<String>() : out;
  }

  /**
   * @return Returns fetch size that is currently same across all event queues.
   */
  public int getFetch() {
    return fetch;
  }

  /**
   * @return String representation of the object.
   */
  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("in", in)
      .add("out", out)
      .toString();
  }
}
