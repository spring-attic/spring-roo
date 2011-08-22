package org.springframework.roo.addon.web.mvc.json;

import static org.springframework.roo.addon.web.mvc.json.RooWebJson.CREATE_FROM_JSON;
import static org.springframework.roo.addon.web.mvc.json.RooWebJson.CREATE_FROM_JSON_ARRAY;
import static org.springframework.roo.addon.web.mvc.json.RooWebJson.DELETE_FROM_JSON_ARRAY;
import static org.springframework.roo.addon.web.mvc.json.RooWebJson.EXPOSE_FINDERS;
import static org.springframework.roo.addon.web.mvc.json.RooWebJson.LIST_JSON;
import static org.springframework.roo.addon.web.mvc.json.RooWebJson.SHOW_JSON;
import static org.springframework.roo.addon.web.mvc.json.RooWebJson.UPDATE_FROM_JSON;
import static org.springframework.roo.addon.web.mvc.json.RooWebJson.UPDATE_FROM_JSON_ARRAY;

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
	
	// From annotation
	@AutoPopulate JavaType jsonObject;
	@AutoPopulate String showJsonMethod = SHOW_JSON;
	@AutoPopulate String listJsonMethod = LIST_JSON;
	@AutoPopulate String createFromJsonMethod = CREATE_FROM_JSON;
	@AutoPopulate String createFromJsonArrayMethod = CREATE_FROM_JSON_ARRAY;
	@AutoPopulate String updateFromJsonMethod = UPDATE_FROM_JSON;
	@AutoPopulate String updateFromJsonArrayMethod = UPDATE_FROM_JSON_ARRAY;
	@AutoPopulate String deleteFromJsonMethod = DELETE_FROM_JSON_ARRAY;
	@AutoPopulate boolean exposeFinders = EXPOSE_FINDERS;

	/**
	 * Constructor
	 *
	 * @param governorPhysicalTypeMetadata
	 */
	public WebJsonAnnotationValues(PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(governorPhysicalTypeMetadata, RooJavaType.ROO_WEB_JSON);
		AutoPopulationUtils.populate(this, annotationMetadata);
	}

	public JavaType getJsonObject() {
		return jsonObject;
	}

	public String getShowJsonMethod() {
		return showJsonMethod;
	}

	public String getListJsonMethod() {
		return listJsonMethod;
	}

	public String getCreateFromJsonMethod() {
		return createFromJsonMethod;
	}

	public String getCreateFromJsonArrayMethod() {
		return createFromJsonArrayMethod;
	}

	public String getUpdateFromJsonMethod() {
		return updateFromJsonMethod;
	}

	public String getUpdateFromJsonArrayMethod() {
		return updateFromJsonArrayMethod;
	}

	public String getDeleteFromJsonMethod() {
		return deleteFromJsonMethod;
	}

	public boolean isExposeFinders() {
		return exposeFinders;
	}
}
