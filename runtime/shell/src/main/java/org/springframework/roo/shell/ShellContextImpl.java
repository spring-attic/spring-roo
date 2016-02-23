package org.springframework.roo.shell;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Provides an implementation for {@link ShellContext}
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class ShellContextImpl implements ShellContext {

  private boolean force;
  private String profile;
  private String executedCommand;
  private Map<String, String> parameters;

  public ShellContextImpl() {
    this.force = false;
    this.profile = "";
    this.executedCommand = "";
    this.parameters = new HashMap<String, String>();
  }

  @Override
  public boolean isForce() {
    return this.force;
  }

  @Override
  public String getProfile() {
    return this.profile;
  }

  @Override
  public String getExecutedCommand() {
    return this.executedCommand;
  }

  @Override
  public Map<String, String> getParameters() {
    return this.parameters;
  }

  /**
   * Set value of current executed command
   * 
   * @param command
   */
  public void setExecutedCommand(String command) {
    this.executedCommand = command;
  }

  /**
   * Set a list of parameters like defined parameters on Spring Roo Shell
   * 
   * @param parameters
   */
  public void setParameters(Map<String, String> parameters) {
    for (Entry<String, String> param : parameters.entrySet()) {
      setParameter(param.getKey(), param.getValue());
    }
  }

  /**
   * Add new parameter on current parameters map.
   * 
   * @param key
   * @param value
   */
  public void setParameter(String key, String value) {
    // Check --force global parameter
    if ("force".equals(key)) {
      if (("".equals(value) || "true".equals(value))) {
        this.force = true;
        this.parameters.put(key, "true");
        return;
      } else {
        this.force = false;
        this.parameters.put(key, "false");
        return;
      }
    }

    // Check --profile global parameter
    if ("profile".equals(key)) {
      this.profile = value;
    }

    this.parameters.put(key, value);


  }
}
