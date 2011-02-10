package org.springframework.roo.addon.web.mvc.controller.scaffold;

import org.springframework.roo.addon.web.mvc.controller.RooWebScaffold;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;

/**
 * Represents a parsed {@link RooWebScaffold} annotation.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @since 1.0
 */
public class WebScaffoldAnnotationValues extends AbstractAnnotationValues {
	// From annotation
	@AutoPopulate String path;
	@AutoPopulate JavaType formBackingObject = null;
	@AutoPopulate boolean delete = true;
	@AutoPopulate boolean create = true;
	@AutoPopulate boolean update = true;
	@AutoPopulate boolean exposeFinders = true;
	@AutoPopulate boolean registerConverters = true;
	@AutoPopulate boolean exposeJson = true;

	public WebScaffoldAnnotationValues(PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(governorPhysicalTypeMetadata, new JavaType(RooWebScaffold.class.getName()));
		AutoPopulationUtils.populate(this, annotationMetadata);
	}

	public String getPath() {
		return path;
	}

	public JavaType getFormBackingObject() {
		return formBackingObject;
	}

	public boolean isDelete() {
		return delete;
	}

	public boolean isCreate() {
		return create;
	}

	public boolean isUpdate() {
		return update;
	}

	public boolean isExposeFinders() {
		return exposeFinders;
	}

	public boolean isRegisterConverters() {
		return registerConverters;
	}

	public boolean isExposeJson() {
		return exposeJson;
	}
}
