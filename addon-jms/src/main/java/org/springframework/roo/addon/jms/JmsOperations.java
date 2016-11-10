package org.springframework.roo.addon.jms;

import org.springframework.roo.model.JavaType;

/**
 * Interface to {@link JmsOperationsImpl}.
 *
 * @author Ben Alex
 * @author Manuel Iborra
 */
public interface JmsOperations {

  boolean isJmsInstallationPossible();

  /**
   * Creates a service to receive JMS messages.
   * Adds the necessary configuration to the project.
   *
   * @param destinationName Name of the queue or topic
   * @param endpointService Service that has a method to get JMS messages of the destination
   * @param jndiConnectionFactory Name of the JNDI where is configured JMS connection
   * @param profile Indicate the profile where the properties will be set
   * @param force Indicate if the properties or service will be overwritten
   */
  void addJmsReceiver(String destinationName, JavaType endpointService,
      String jndiConnectionFactory, String profile, boolean force);

  /**
   * Creates a service to send JMS messages.
   * Adds the necessary configuration to the project.
   *
   * @param destinationName Name of the queue or topic
   * @param classSelected Class where put the service that can send JMS messages
   * @param jndiConnectionFactory Name of the JNDI where is configured JMS connection
   * @param profile Indicate the profile where the properties will be set
   * @param force Indicate if the properties will be overwritten
   */
  void addJmsSender(String destinationName, JavaType classSelected, String jndiConnectionFactory,
      String profile, boolean force);
}
