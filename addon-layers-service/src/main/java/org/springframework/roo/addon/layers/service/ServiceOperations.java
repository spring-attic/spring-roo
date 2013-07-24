package org.springframework.roo.addon.layers.service;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;

/**
 * @author Stefan Schmidt
 * @since 1.2.0
 */
public interface ServiceOperations {

    boolean isServiceInstallationPossible();

    boolean isSecureServiceInstallationPossible();

    void setupService(JavaType interfaceType, JavaType classType,
            JavaType domainType, boolean requireAuthentication,
            String authorizedRole, boolean usePermissionEvalutor,
            boolean useXmlConfiguration);

    void setupAllServices(JavaPackage interfacePackage,
            JavaPackage classPackage, boolean requireAuthentication,
            String authorizedRole, boolean usePermissionEvalutor,
            boolean useXmlConfiguration);

}
