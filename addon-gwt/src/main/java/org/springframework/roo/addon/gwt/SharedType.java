package org.springframework.roo.addon.gwt;

import org.springframework.roo.project.ProjectMetadata;

/**
 * Represents shared types. There is one such type per application using Roo's GWT support.
 * 
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.1
 *
 */
public enum SharedType {
	APP_ENTITY_TYPES_PROCESSOR(GwtPath.GWT_REQUEST, "ApplicationEntityTypesProcessor", "entityTypes", "ApplicationEntityTypesProcessor.vm"), 
	APP_REQUEST_FACTORY(GwtPath.GWT_REQUEST, "ApplicationRequestFactory", "requestFactory", "ApplicationRequestFactory.vm"), 
  FACTORY(GwtPath.GWT_SCAFFOLD, "ScaffoldFactory", "factory", "ScaffoldFactory.vm"), 
  PLACE_HISTORY_HANDLER(GwtPath.GWT_SCAFFOLD, "ScaffoldPlaceHistoryHandler", "placeHistoryHandler", "ScaffoldPlaceHistoryHandler.vm"), 
	LIST_PLACE_RENDERER(GwtPath.GWT_SCAFFOLD, "ApplicationListPlaceRenderer", "listPlaceRenderer", "ApplicationListPlaceRenderer.vm"), 
	MASTER_ACTIVITIES(GwtPath.GWT_SCAFFOLD, "ApplicationMasterActivities", "masterActivities", "ApplicationMasterActivities.vm"), 
	DETAILS_ACTIVITIES(GwtPath.GWT_SCAFFOLD, "ApplicationDetailsActivities", "detailsActivities", "ApplicationDetailsActivities.vm"), 
	MOBILE_ACTIVITIES(GwtPath.GWT_SCAFFOLD, "ScaffoldMobileActivities", "mobileActivities", "ScaffoldMobileActivities.vm"),
	;    

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
