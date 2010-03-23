package org.springframework.roo.addon.jms;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Interface to {@link JmsOperationsImpl}.
 * 
 * @author Ben Alex
 *
 */
public interface JmsOperations {

	public abstract boolean isInstallJmsAvailable();

	public abstract boolean isManageJmsAvailable();

	public abstract void installJms(JmsProvider jmsProvider, String name, JmsDestinationType destinationType);

	public abstract void injectJmsTemplate(JavaType targetType, JavaSymbolName fieldName);

	public abstract void addJmsListener(JavaType targetType, String name, JmsDestinationType destinationType);

}