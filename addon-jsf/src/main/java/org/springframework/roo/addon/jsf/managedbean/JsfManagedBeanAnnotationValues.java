package org.springframework.roo.addon.jsf.managedbean;

import static org.springframework.roo.model.RooJavaType.ROO_JSF_MANAGED_BEAN;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;

/**
 * Represents a parsed {@link RooJsfManagedBean} annotation.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public class JsfManagedBeanAnnotationValues extends AbstractAnnotationValues {

    @AutoPopulate private String beanName;
    @AutoPopulate private JavaType entity;
    @AutoPopulate private boolean includeOnMenu = true;

    public JsfManagedBeanAnnotationValues(
            final ClassOrInterfaceTypeDetails governorPhysicalTypeDetails) {
        super(governorPhysicalTypeDetails, RooJavaType.ROO_JSF_MANAGED_BEAN);
        AutoPopulationUtils.populate(this, annotationMetadata);
    }

    public JsfManagedBeanAnnotationValues(
            final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
        super(governorPhysicalTypeMetadata, ROO_JSF_MANAGED_BEAN);
        AutoPopulationUtils.populate(this, annotationMetadata);
    }

    public String getBeanName() {
        return beanName;
    }

    public JavaType getEntity() {
        return entity;
    }

    public boolean isIncludeOnMenu() {
        return includeOnMenu;
    }
}
