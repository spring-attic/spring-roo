package org.springframework.roo.addon.web.mvc.controller.json;

import static org.springframework.roo.addon.web.mvc.controller.json.RooWebJson.CREATE_FROM_JSON;
import static org.springframework.roo.addon.web.mvc.controller.json.RooWebJson.CREATE_FROM_JSON_ARRAY;
import static org.springframework.roo.addon.web.mvc.controller.json.RooWebJson.DELETE_FROM_JSON_ARRAY;
import static org.springframework.roo.addon.web.mvc.controller.json.RooWebJson.EXPOSE_FINDERS;
import static org.springframework.roo.addon.web.mvc.controller.json.RooWebJson.LIST_JSON;
import static org.springframework.roo.addon.web.mvc.controller.json.RooWebJson.SHOW_JSON;
import static org.springframework.roo.addon.web.mvc.controller.json.RooWebJson.UPDATE_FROM_JSON;
import static org.springframework.roo.addon.web.mvc.controller.json.RooWebJson.UPDATE_FROM_JSON_ARRAY;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;

/**
 * Represents a parsed {@link RooWebJson} annotation.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
public class WebJsonAnnotationValues extends AbstractAnnotationValues {

    @AutoPopulate String createFromJsonArrayMethod = CREATE_FROM_JSON_ARRAY;
    @AutoPopulate String createFromJsonMethod = CREATE_FROM_JSON;
    @AutoPopulate String deleteFromJsonMethod = DELETE_FROM_JSON_ARRAY;
    @AutoPopulate boolean exposeFinders = EXPOSE_FINDERS;
    @AutoPopulate JavaType jsonObject;
    @AutoPopulate String listJsonMethod = LIST_JSON;
    @AutoPopulate String showJsonMethod = SHOW_JSON;
    @AutoPopulate String updateFromJsonArrayMethod = UPDATE_FROM_JSON_ARRAY;
    @AutoPopulate String updateFromJsonMethod = UPDATE_FROM_JSON;

    /**
     * Constructor
     * 
     * @param governorPhysicalTypeMetadata
     */
    public WebJsonAnnotationValues(
            final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
        super(governorPhysicalTypeMetadata, RooJavaType.ROO_WEB_JSON);
        AutoPopulationUtils.populate(this, annotationMetadata);
    }

    public String getCreateFromJsonArrayMethod() {
        return createFromJsonArrayMethod;
    }

    public String getCreateFromJsonMethod() {
        return createFromJsonMethod;
    }

    public String getDeleteFromJsonMethod() {
        return deleteFromJsonMethod;
    }

    public JavaType getJsonObject() {
        return jsonObject;
    }

    public String getListJsonMethod() {
        return listJsonMethod;
    }

    public String getShowJsonMethod() {
        return showJsonMethod;
    }

    public String getUpdateFromJsonArrayMethod() {
        return updateFromJsonArrayMethod;
    }

    public String getUpdateFromJsonMethod() {
        return updateFromJsonMethod;
    }

    public boolean isExposeFinders() {
        return exposeFinders;
    }
}
