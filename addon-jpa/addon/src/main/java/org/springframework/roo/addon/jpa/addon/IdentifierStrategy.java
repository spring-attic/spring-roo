package org.springframework.roo.addon.jpa.addon;

public enum IdentifierStrategy {

  SEQUENCE("SEQUENCE"), TABLE("TABLE"), IDENTITY("IDENTITY"), AUTO("AUTO");

  private final String type;

  private IdentifierStrategy(final String type) {
    this.type = type;
  }

  public String getType() {
    return this.type;
  }
}
