package org.springframework.roo.addon.jpa.addon;

public enum GenerationType {

  SEQUENCE("SEQUENCE"), TABLE("TABLE"), IDENTITY("IDENTITY"), AUTO("AUTO");

  private final String type;

  private GenerationType(final String type) {
    this.type = type;
  }

  public String getType() {
    return this.type;
  }
}
