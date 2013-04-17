package org.springframework.roo.addon.layers.service;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;

/**
 * The values of a given {@link RooService} annotation.
 * 
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2.0
 */
public class ServiceAnnotationValues extends AbstractAnnotationValues {

    @AutoPopulate private String countAllMethod = RooService.COUNT_ALL_METHOD;
    @AutoPopulate private String deleteMethod = RooService.DELETE_METHOD;
    @AutoPopulate private JavaType[] domainTypes;
    @AutoPopulate private String findAllMethod = RooService.FIND_ALL_METHOD;
    @AutoPopulate private String findEntriesMethod = RooService.FIND_ENTRIES_METHOD;
    @AutoPopulate private String findMethod = RooService.FIND_METHOD;
    @AutoPopulate private String saveMethod = RooService.SAVE_METHOD;
    @AutoPopulate private boolean transactional = true;
    @AutoPopulate private String updateMethod = RooService.UPDATE_METHOD;
    @AutoPopulate private boolean requireAuthentication = false;
    @AutoPopulate private boolean usePermissionEvaluator = false;
    @AutoPopulate private String[] authorizedCreateOrUpdateRoles = new String[0];
    @AutoPopulate private String[] authorizedReadRoles = new String[0];
    @AutoPopulate private String[] authorizedDeleteRoles = new String[0];
    @AutoPopulate private boolean useXmlConfiguration = false;

    /**
     * Constructor
     * 
     * @param governorPhysicalTypeMetadata to parse (required)
     */
    public ServiceAnnotationValues(
            final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
        super(governorPhysicalTypeMetadata, RooJavaType.ROO_SERVICE);
        AutoPopulationUtils.populate(this, annotationMetadata);
    }

    public String getCountAllMethod() {
        return countAllMethod;
    }

    public String getDeleteMethod() {
        return deleteMethod;
    }

    public JavaType[] getDomainTypes() {
        return domainTypes;
    }

    public String getFindAllMethod() {
        return findAllMethod;
    }

    public String getFindEntriesMethod() {
        return findEntriesMethod;
    }

    public String getFindMethod() {
        return findMethod;
    }

    public String getSaveMethod() {
        return saveMethod;
    }

    public String getUpdateMethod() {
        return updateMethod;
    }

    public boolean isTransactional() {
        return transactional;
    }

    public boolean requireAuthentication() {
        return requireAuthentication;
    }

    public boolean usePermissionEvaluator() {
        return usePermissionEvaluator;
    }

    public String[] getAuthorizedCreateOrUpdateRoles() {
        return authorizedCreateOrUpdateRoles;
    }

    public String[] getAuthorizedReadRoles() {
        return authorizedReadRoles;
    }

    public String[] getAuthorizedDeleteRoles() {
        return authorizedDeleteRoles;
    }

    public boolean useXmlConfiguration() {
        return useXmlConfiguration;
    }

}
