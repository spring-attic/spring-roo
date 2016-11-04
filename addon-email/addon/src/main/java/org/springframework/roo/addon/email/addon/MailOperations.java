package org.springframework.roo.addon.email.addon;

import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Feature;
import org.springframework.roo.project.maven.Pom;

import java.util.List;

/**
 * Provides email configuration operations.
 *
 * @author Ben Alex
 * @author Manuel Iborra
 * @since 1.0
 */
public interface MailOperations extends Feature {

  static final String FEATURE_NAME = "email";

  /**
   * Add support to send emails from the configured account
   *
   * @param host Server host
   * @param port Server port
   * @param protocol Protocol used to send emails
   * @param username Account username
   * @param password Account password
   * @param starttls Enable or disable the use of the STARTTLS command
   * @param jndiName Enable and set JNDI to send emails
   * @param profile Indicate the profile where the properties will be set
   * @param module Indicate the module where the properties will be set
   * @param service Service implementantion where create an instance of JavaMailSender
   * @param force Indicate if the properties will be overwritten
   */
  void installSendEmailSupport(String host, String port, String protocol, String username,
      String password, Boolean starttls, String jndiName, String profile, Pom module,
      JavaType service, boolean force);

  /**
   * Add support to obtain emails received into the configured account
   *
  	 * @param host Server host
  	 * @param port Server port
  	 * @param protocol Protocol used to receive emails
  	 * @param username Account username
  	 * @param password Account password
  	 * @param starttls Enable or disable the use of the STARTTLS command
  	 * @param jndiName Enable and set JNDI to receive emails
  	 * @param profile Indicate the profile where the properties will be set
  	 * @param module Indicate the module where the properties will be set
  	 * @param service Service implementantion where create an instance of MailReceiverService
  	 * @param force Indicate if the properties will be overwritten
   */
  void installReceiveEmailSupport(String host, String port, String protocol, String username,
      String password, Boolean starttls, String jndiName, String profile, Pom module,
      JavaType service, boolean force);

  /**
   * Indicates whether the command for adding mail support to the
   * user's project is available.
   *
   * @return see above
   */
  boolean isEmailInstallationPossible();

  /**
   * Get all the services implementations of the current project
   *
   * @param currentService String that represents a service selected
   * @return list with the package and name of all services implementations
   */
  List<String> getAllServiceImpl(String currentService);

}
