package org.springframework.roo.addon.email;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Interface to operations available via {@link MailOperationsImpl}.
 * 
 * @author Ben Alex
 *
 */
public interface MailOperations {

	public abstract boolean isInstallEmailAvailable();

	public abstract boolean isManageEmailAvailable();

	public abstract void installEmail(String hostServer, MailProtocol protocol, String port, String encoding, String username, String password);

	public abstract void configureTemplateMessage(String from, String subject);

	public abstract void injectEmailTemplate(JavaType targetType, JavaSymbolName fieldName);

}