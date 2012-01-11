package org.springframework.roo.addon.web.mvc.controller.scaffold;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;

/**
 * Represents a parsed {@link RooWebScaffold} annotation.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @since 1.0
 */
public class WebScaffoldAnnotationValues extends AbstractAnnotationValues {

    @AutoPopulate boolean create = true;
    @AutoPopulate boolean delete = true;
    @AutoPopulate boolean exposeFinders = true;
    @AutoPopulate JavaType formBackingObject;
    @AutoPopulate String path;
    @AutoPopulate boolean populateMethods = true;
    @AutoPopulate boolean registerConverters = true;
    @AutoPopulate boolean update = true;

    public WebScaffoldAnnotationValues(
            final ClassOrInterfaceTypeDetails governorPhysicalTypeDetails) {
        super(governorPhysicalTypeDetails, RooJavaType.ROO_WEB_SCAFFOLD);
        AutoPopulationUtils.populate(this, annotationMetadata);
    }

    /**
     * Constructor
     * 
     * @param governorPhysicalTypeMetadata
     */
    public WebScaffoldAnnotationValues(
            final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
        super(governorPhysicalTypeMetadata, RooJavaType.ROO_WEB_SCAFFOLD);
        AutoPopulationUtils.populate(this, annotationMetadata);
    }

    public JavaType getFormBackingObject() {
        return formBackingObject;
    }

    public String getPath() {
        return path;
    }

    public boolean isCreate() {
        return create;
    }

    public boolean isDelete() {
        return delete;
    }

    public boolean isExposeFinders() {
        return exposeFinders;
    }

    public boolean isPopulateMethods() {
        return populateMethods;
    }

    public boolean isRegisterConverters() {
        return registerConverters;
    }

    public boolean isUpdate() {
        return update;
    }

    @Override
    public String toString() {
        // For debugging
        return "WebScaffoldAnnotationValues [" + "create=" + create
                + ", delete=" + delete + ", exposeFinders=" + exposeFinders
                + ", populateMethods=" + populateMethods
                + ", registerConverters=" + registerConverters + ", update="
                + update + ", formBackingObject=" + formBackingObject
                + ", path=" + path + "]";
    }
}
