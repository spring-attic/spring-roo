package org.springframework.roo.addon.email;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Provides email configuration operations.
 * 
 * @author Ben Alex
 */
public interface MailOperations {

	boolean isInstallEmailAvailable();

	boolean isManageEmailAvailable();

	void installEmail(String hostServer, MailProtocol protocol, String port, String encoding, String username, String password);

	void configureTemplateMessage(String from, String subject);

	void injectEmailTemplate(JavaType targetType, JavaSymbolName fieldName, boolean async);
}