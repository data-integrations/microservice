package co.cask.microservice.api;

import avro.shaded.com.google.common.base.Objects;
import co.cask.microservice.annotation.PublicEvolving;

/**
 * Class description here.
 */
@PublicEvolving
public final class Plugin {
  private String name;
  private Artifact artifact;

  public String getName() {
    return name;
  }

  public Artifact getArtifact() {
    return artifact;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("name", name)
      .add("artifact", artifact)
      .toString();
  }
}
