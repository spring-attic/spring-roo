package org.springframework.roo.addon.gwt;

import org.springframework.roo.project.ProjectMetadata;

/**
 * Represents shared types. There is one such type for application using Roo's GWT support.
 * 
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.1
 *
 */
public enum SharedType {
	APP_LIST_PLACE(GwtPath.GWT_PLACE, "ApplicationListPlace", "listPlace", "ListPlace.vm"), 
	APP_PLACE(GwtPath.GWT_PLACE, "ApplicationPlace", "applicationPlace", "ApplicationPlace.vm"), 
	APP_RECORD_PLACE(GwtPath.GWT_PLACE, "ApplicationRecordPlace", "applicationRecordPlace", "ApplicationRecordPlace.vm"), 
	APP_PLACE_PROCESSOR(GwtPath.GWT_PLACE, "ApplicationPlaceProcessor", "placeProcessor", "ApplicationPlaceProcessor.vm"), 
	APP_PLACE_FILTER(GwtPath.GWT_PLACE, "ApplicationPlaceFilter", "placeFilter", "ApplicationPlaceFilter.vm"), 
	APP_ENTITY_TYPES_PROCESSOR(GwtPath.GWT_REQUEST, "ApplicationEntityTypesProcessor", "entityTypes", "ApplicationEntityTypesProcessor.vm"), 
	APP_PLACE_TO_RECORD_TYPE(GwtPath.GWT_PLACE, "ApplicationPlaceToRecordType", "placeToRecordType", "ApplicationPlaceToRecordType.vm"), 
	APP_REQUEST_FACTORY(GwtPath.GWT_REQUEST, "ApplicationRequestFactory", "requestFactory", "ApplicationRequestFactory.vm"), 
	SCAFFOLD_ACTIVITIES(GwtPath.GWT_SCAFFOLD, "ScaffoldActivities", "scaffoldActivities", "ScaffoldActivities.vm"), 
	LIST_PLACE_RENDERER(GwtPath.GWT_UI, "ListPlaceRenderer", "listPlaceRenderer", "ListPlaceRenderer.vm"), 
	LIST_ACTIVITIES_MAPPER(GwtPath.GWT_UI, "ListActivitiesMapper", "listActivitiesMapper", "ListActivitiesMapper.vm"), 
	MASTER_ACTIVITIES(GwtPath.GWT_SCAFFOLD, "ScaffoldMasterActivities", "masterActivities", "ScaffoldMasterActivities.vm"), 
	DETAILS_ACTIVITIES(GwtPath.GWT_SCAFFOLD, "ScaffoldDetailsActivities", "detailsActivities", "ScaffoldDetailsActivities.vm"), 
	MOBILE_ACTIVITIES(GwtPath.GWT_SCAFFOLD, "ScaffoldMobileActivities", "mobileActivities", "ScaffoldMobileActivities.vm"),    
	BASE_PLACE_FILTER(GwtPath.GWT_PLACE, "BasePlaceFilter", "basePlaceFilter", "BasePlaceFilter.vm");

	private GwtPath path;
	private String fullName;
	private String velocityName;
	private String velocityTemplate;

	public String getVelocityName() {
		return velocityName;
	}

	public String getVelocityTemplate() {
		return velocityTemplate;
	}

	private SharedType(GwtPath path, String fullName, String velocityName, String velocityTemplate) {
		this.path = path;
		this.fullName = fullName;
		this.velocityName = velocityName;
		this.velocityTemplate = velocityTemplate;
	}

	public GwtPath getPath() {
		return path;
	}

	public String getFullName() {
		return fullName;
	}

	public String getFullyQualifiedTypeName(ProjectMetadata projectMetadata) {
		return path.packageName(projectMetadata) + "." + getFullName();
	}
}
