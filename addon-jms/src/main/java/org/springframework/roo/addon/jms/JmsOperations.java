package org.springframework.roo.addon.jms;

import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.ShellContext;

/**
 * Interface to {@link JmsOperationsImpl}.
 *
 * @author Ben Alex
 * @author Manuel Iborra
 */
public interface JmsOperations {

  boolean isJmsInstallationPossible();

  /**
   * Creates a service that permits reception of JMS messages.
   * Adds the necessary configuration to the project.
   *
   * @param destinationName Name of the queue or topic
   * @param service Service that has a method to get JMS messages of the destination
   * @param jndiConnectionFactory Name of the JNDI where is configured JMS connection
   * @param profile Indicate the profile where the properties will be set
   * @param force Indicate if the properties or service will be overwritten
   */
  void addJmsReceiver(String destinationName, JavaType service, String jndiConnectionFactory,
      String profile, boolean force);

  /**
   * TODO
   *
   * @param name
   * @param module
   * @param service
   * @param shellContext
   */
  void addJmsSender(String name, Pom module, JavaType service, ShellContext shellContext);
}
