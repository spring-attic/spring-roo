package org.springframework.roo.addon.jms;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Interface to {@link JmsOperationsImpl}.
 * 
 * @author Ben Alex
 */
public interface JmsOperations {

    void addJmsListener(JavaType targetType, String name,
            JmsDestinationType destinationType);

    void injectJmsTemplate(JavaType targetType, JavaSymbolName fieldName,
            boolean async);

    void installJms(JmsProvider jmsProvider, String name,
            JmsDestinationType destinationType);

    boolean isJmsInstallationPossible();

    boolean isManageJmsAvailable();
}