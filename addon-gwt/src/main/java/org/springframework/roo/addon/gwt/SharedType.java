package org.springframework.roo.addon.gwt;

import org.springframework.roo.project.ProjectMetadata;

/**
 * Represents shared types. There is one such type per application using Roo's GWT support.
 * 
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.1
 */
public enum SharedType {
	APP_ENTITY_TYPES_PROCESSOR(GwtPath.GWT_REQUEST, "ApplicationEntityTypesProcessor", "entityTypes", "ApplicationEntityTypesProcessor"), 
	APP_REQUEST_FACTORY(GwtPath.GWT_REQUEST, "ApplicationRequestFactory", "requestFactory", "ApplicationRequestFactory"),
	LIST_PLACE_RENDERER(GwtPath.GWT_SCAFFOLD, "ApplicationListPlaceRenderer", "listPlaceRenderer", "ApplicationListPlaceRenderer"), 
	SCAFFOLD_APP(GwtPath.GWT_SCAFFOLD, "ScaffoldApp", "scaffoldApp", "ScaffoldApp"),
	SCAFFOLD_MOBILE_APP(GwtPath.GWT_SCAFFOLD, "ScaffoldMobileApp", "scaffoldMobileApp", "ScaffoldMobileApp"),
	IS_SCAFFOLD_MOBILE_ACTIVITY(GwtPath.GWT_SCAFFOLD, "IsScaffoldMobileActivity", "isScaffoldMobileActivity", "IsScaffoldMobileActivity"),
	MOBILE_PROXY_LIST_VIEW(GwtPath.GWT_SCAFFOLD, "MobileProxyListView", "mobileProxyListView", "MobileProxyListView"),
	MASTER_ACTIVITIES(GwtPath.GWT_SCAFFOLD, "ApplicationMasterActivities", "masterActivities", "ApplicationMasterActivities"), 
	DETAILS_ACTIVITIES(GwtPath.GWT_SCAFFOLD, "ApplicationDetailsActivities", "detailsActivities", "ApplicationDetailsActivities"), 
	MOBILE_ACTIVITIES(GwtPath.GWT_SCAFFOLD, "ScaffoldMobileActivities", "mobileActivities", "ScaffoldMobileActivities");    

	private GwtPath path;
	private String fullName;
	private String name;
	private String template;

	private SharedType(GwtPath path, String fullName, String name, String template) {
		this.path = path;
		this.fullName = fullName;
		this.name = name;
		this.template = template;
	}

	public GwtPath getPath() {
		return path;
	}

	public String getFullName() {
		return fullName;
	}

	public String getName() {
		return name;
	}

	public String getTemplate() {
		return template;
	}

	public String getFullyQualifiedTypeName(ProjectMetadata projectMetadata) {
		return path.packageName(projectMetadata) + "." + getFullName();
	}
}
