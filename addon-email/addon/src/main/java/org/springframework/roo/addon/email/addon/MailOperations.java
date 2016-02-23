package org.springframework.roo.addon.email.addon;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Feature;

/**
 * Provides email configuration operations.
 * 
 * @author Ben Alex
 */
public interface MailOperations extends Feature {

  static final String FEATURE_NAME = "email";

  void configureTemplateMessage(String from, String subject);

  void injectEmailTemplate(JavaType targetType, JavaSymbolName fieldName, boolean async);

  void installEmail(String hostServer, MailProtocol protocol, String port, String encoding,
      String username, String password);

  /**
   * Indicates whether the command for adding a JavaMailSender to the user's
   * project is available.
   * 
   * @return see above
   */
  boolean isEmailInstallationPossible();

  /**
   * Indicates whether the commands relating to mail templates are available
   * 
   * @return see above
   */
  boolean isManageEmailAvailable();
}
