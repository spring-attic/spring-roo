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
	APP_LIST_PLACE(GwtPath.GWT_PLACE, "ApplicationListPlace"),
	APP_PLACES(GwtPath.GWT_PLACE, "ApplicationPlaces"),
	APP_ENTITY_TYPES_PROCESSOR(GwtPath.GWT_REQUEST, "ApplicationEntityTypesProcessor"),
	APP_REQUEST_FACTORY(GwtPath.GWT_REQUEST, "ApplicationRequestFactory"),
	APP_REQUEST_SERVER_SIDE_OPERATIONS(GwtPath.GWT_REQUEST, "ApplicationRequestServerSideOperations"),
	SCAFFOLD_DETAILS_VIEW_BUILDER(GwtPath.GWT_SCAFFOLD_GENERATED, "ScaffoldDetailsViewBuilder"),
	SCAFFOLD_LIST_VIEW_BUILDER(GwtPath.GWT_SCAFFOLD_GENERATED, "ScaffoldListViewBuilder"),
	LIST_PLACE_RENDERER(GwtPath.GWT_UI, "ListPlaceRenderer");

	private GwtPath path;
	private String fullName;
	
	private SharedType(GwtPath path, String fullName) {
		this.path = path;
		this.fullName = fullName;
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
