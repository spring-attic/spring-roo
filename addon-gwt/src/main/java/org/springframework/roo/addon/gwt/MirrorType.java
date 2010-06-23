package org.springframework.roo.addon.gwt;

/**
 * Represents mirror types classes. There are one of these for each entity mirrored by Roo.
 * 
 * <p>
 * A mirror type has its .java source code produced by the {@link GwtMetadataProvider}. 
 * 
 * <p>
 * This enum provides a convenient way to ensure filenames are composed correctly for each mirror type
 * to be generated, and also ensures the mirror type is placed into the correct package. The correct
 * package is resolved via the {@link GwtPath} enum.
 * 
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.1
 *
 */
public enum MirrorType {
	RECORD(GwtPath.GWT_REQUEST, "Record", "record", "Record.vm"), 
	RECORD_CHANGED(GwtPath.GWT_REQUEST, "RecordChanged", "recordChanged", "RecordChanged.vm"), 
	CHANGED_HANDLER(GwtPath.GWT_REQUEST, "ChangedHandler", "changeHandler", "ChangeHandler.vm"), 
	REQUEST(GwtPath.GWT_REQUEST, "Request", "request", "Request.vm"), 
	ACTIVITIES_MAPPER(GwtPath.GWT_UI, "ActivitiesMapper", "activitiesMapper", "ActivitiesMapper.vm"), 
	SCAFFOLD_PLACE(GwtPath.GWT_PLACE, "ScaffoldPlace", "detailPlace", "ScaffoldPlace.vm"), 
	DETAIL_ACTIVITY(GwtPath.GWT_UI, "DetailsActivity", "detailsActivity", "DetailsActivity.vm"), 
	EDIT_ACTIVITY(GwtPath.GWT_UI, "EditActivity", "editActivity", "EditActivity.vm"), 
	LIST_ACTIVITY(GwtPath.GWT_UI, "ListActivity", "listActivity", "ListActivity.vm"), 
	LIST_VIEW(GwtPath.GWT_SCAFFOLD_GENERATED, "ListView", "listView", "ListView.vm"), 
	LIST_VIEW_BINDER(GwtPath.GWT_SCAFFOLD_GENERATED, "ListViewBinder", "listViewBinder", "ListViewBinder.vm"), 
	DETAILS_VIEW_BINDER(GwtPath.GWT_SCAFFOLD_GENERATED, "DetailsViewBinder", "detailsViewBinder", "DetailsViewBinder.vm"), 
	DETAILS_VIEW(GwtPath.GWT_SCAFFOLD_GENERATED, "DetailsView", "detailsView", "DetailsView.vm"),
	EDIT_VIEW_BINDER(GwtPath.GWT_SCAFFOLD_GENERATED, "EditViewBinder", "editViewBinder", "EditViewBinder.vm"), 
	EDIT_VIEW(GwtPath.GWT_SCAFFOLD_GENERATED, "EditView", "editView", "EditView.vm");  

	private GwtPath path;
	private String suffix;
	private String velocityName;
	private String velocityTemplate;

	private MirrorType(GwtPath path, String suffix, String velocityName, String velocityTemplate) {
		this.path = path;
		this.suffix = suffix;
		this.velocityName = velocityName;
		this.velocityTemplate = velocityTemplate;
	}

	public GwtPath getPath() {
		return path;
	}

	public String getSuffix() {
		return suffix;
	}

	public String getVelocityName() {
		return velocityName;
	}

	public String getVelocityTemplate() {
		return velocityTemplate;
	}
}
