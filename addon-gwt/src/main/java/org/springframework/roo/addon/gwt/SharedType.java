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
	APP_ENTITY_TYPES_PROCESSOR(GwtPath.MANAGED_REQUEST, "ApplicationEntityTypesProcessor", "entityTypes", "ApplicationEntityTypesProcessor"), 
	APP_REQUEST_FACTORY(GwtPath.MANAGED_REQUEST, "ApplicationRequestFactory", "requestFactory", "ApplicationRequestFactory"),
	LIST_PLACE_RENDERER(GwtPath.MANAGED_UI, "ApplicationListPlaceRenderer", "listPlaceRenderer", "ApplicationListPlaceRenderer"), 
	SCAFFOLD_APP(GwtPath.SCAFFOLD, "ScaffoldApp", "scaffoldApp", "ScaffoldApp"),
	SCAFFOLD_MOBILE_APP(GwtPath.SCAFFOLD, "ScaffoldMobileApp", "scaffoldMobileApp", "ScaffoldMobileApp"),
	IS_SCAFFOLD_MOBILE_ACTIVITY(GwtPath.SCAFFOLD_ACTIVITY, "IsScaffoldMobileActivity", "isScaffoldMobileActivity", "IsScaffoldMobileActivity"),
	MOBILE_PROXY_LIST_VIEW(GwtPath.SCAFFOLD_UI, "MobileProxyListView", "mobileProxyListView", "MobileProxyListView"),
	MASTER_ACTIVITIES(GwtPath.MANAGED_ACTIVITY, "ApplicationMasterActivities", "masterActivities", "ApplicationMasterActivities"), 
	DETAILS_ACTIVITIES(GwtPath.MANAGED_ACTIVITY, "ApplicationDetailsActivities", "detailsActivities", "ApplicationDetailsActivities"), 
	MOBILE_ACTIVITIES(GwtPath.MANAGED_ACTIVITY, "ScaffoldMobileActivities", "mobileActivities", "ScaffoldMobileActivities");    

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
