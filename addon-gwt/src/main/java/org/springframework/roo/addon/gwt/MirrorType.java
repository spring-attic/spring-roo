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
	RECORD(GwtPath.GWT_REQUEST, "Record"),
	RECORD_CHANGED(GwtPath.GWT_REQUEST, "RecordChanged"),
	CHANGED_HANDLER(GwtPath.GWT_REQUEST, "ChangedHandler"),
	REQUEST(GwtPath.GWT_REQUEST, "Request"),
	REQUEST_SERVER_SIDE_OPERATIONS(GwtPath.GWT_REQUEST, "RequestServerSideOperations"),
	FIND_ALL_REQUESTER(GwtPath.GWT_SCAFFOLD_GENERATED, "FindAllRequester"),
	DETAILS_BUILDER(GwtPath.GWT_SCAFFOLD_GENERATED, "DetailsBuilder"),
	LIST_VIEW(GwtPath.GWT_SCAFFOLD_GENERATED, "ListView");

	private GwtPath path;
	private String suffix;
	
	private MirrorType(GwtPath path, String suffix) {
		this.path = path;
		this.suffix = suffix;
	}
	
	public GwtPath getPath() {
		return path;
	}

	public String getSuffix() {
		return suffix;
	}
}
