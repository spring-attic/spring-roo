package org.springframework.roo.obr.addon.search.model;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Version;

public class ObrBundle {

  private String symbolicName;
  private String presentationName;
  private Long size;
  private String uri;
  private Version version;
  private List<String> commands;

  public ObrBundle(final String symbolicName) {
    super();
    this.symbolicName = symbolicName;
    this.commands = new ArrayList<String>();
  }

  public ObrBundle(String symbolicName, String presentationName, Long size, Version version) {
    super();
    this.symbolicName = symbolicName;
    this.setPresentationName(presentationName);
    this.setVersion(version);
    this.setSize(size);
    this.commands = new ArrayList<String>();
  }

  public ObrBundle(String symbolicName, String presentationName, Long size, Version version,
      String uri) {
    super();
    this.symbolicName = symbolicName;
    this.setPresentationName(presentationName);
    this.setVersion(version);
    this.setSize(size);
    this.setUri(uri);
    this.commands = new ArrayList<String>();
  }


  public ObrBundle(String symbolicName, String presentationName, Long size, Version version,
      String uri, List<String> commands) {
    super();
    this.symbolicName = symbolicName;
    this.setPresentationName(presentationName);
    this.setVersion(version);
    this.setSize(size);
    this.setUri(uri);
    this.commands = commands;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final ObrBundle other = (ObrBundle) obj;
    if (symbolicName == null) {
      if (other.symbolicName != null) {
        return false;
      }
    } else if (!symbolicName.equals(other.symbolicName)) {
      return false;
    }
    return true;
  }



  public String getSymbolicName() {
    return symbolicName;
  }

  /**
   * @return the presentationName
   */
  public String getPresentationName() {
    return presentationName;
  }

  /**
   * @param presentationName the presentationName to set
   */
  public void setPresentationName(String presentationName) {
    this.presentationName = presentationName;
  }

  /**
   * @return the version
   */
  public Version getVersion() {
    return version;
  }

  /**
   * @param version the version to set
   */
  public void setVersion(Version version) {
    this.version = version;
  }

  /**
   * Method to add available commands on Bundle
   * 
   * @param command
   */
  public void addCommand(String command) {
    this.commands.add(command);
  }

  /**
   * Method to remove commands from Bundle
   * 
   * @param command
   */
  public void removeCommand(String command) {
    this.commands.remove(command);
  }

  /**
   * Method that returns Bundle Commands
   * 
   * @return
   */
  public List<String> getCommands() {
    return this.commands;
  }

  /**
   * Method to set commands
   * 
   * @param commands
   */
  public void setCommands(List<String> commands) {
    this.commands = commands;
  }

  /**
   * @return the size
   */
  public Long getSize() {
    return this.size;
  }

  /**
   * @param size the size to set
   */
  public void setSize(Long size) {
    this.size = size;
  }

  /**
   * @return the uri
   */
  public String getUri() {
    return uri;
  }

  /**
   * @param uri the uri to set
   */
  public void setUri(String uri) {
    this.uri = uri;
  }

}
