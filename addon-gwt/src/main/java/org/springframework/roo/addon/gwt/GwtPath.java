package org.springframework.roo.addon.gwt;

import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;

public enum GwtPath {
	// TODO: Make this cleaner; should be passing into a GwtPath(String segmentName, String sourceAntPath) constructor instead of the glorified conditional statements
	GWT_ROOT, 
	GWT_REQUEST, 
	GWT_SCAFFOLD, 
	GWT_SCAFFOLD_GENERATED, 
	GWT_UI, 
	SERVER, 
	WEB, 
	STYLE, 
	STYLE_CLIENT,
        SHARED,
        IOC;
	
	private String segmentName() {
		if (GWT_ROOT.equals(this)) {
			return "/gwt";
		} else if (GWT_REQUEST.equals(this)) {
			return "/gwt/request";
		} else if (GWT_SCAFFOLD.equals(this)) {
			return "/gwt/scaffold";
		} else if (GWT_SCAFFOLD_GENERATED.equals(this)) {
			return "/gwt/scaffold/generated";
		} else if (GWT_UI.equals(this)) {
			return "/gwt/ui";
		} else if (SERVER.equals(this)) {
			return "/server";
		} else if (STYLE.equals(this)) {
			return "/gwt/style";
		} else if (STYLE_CLIENT.equals(this)) {
			return "/gwt/style/client";
		} else if (SHARED.equals(this)) {
			return "/gwt/shared";
		} else if (IOC.equals(this)) {
			return "/gwt/scaffold/ioc";
		} else {
			return "/";
		}
	}

	public String sourceAntPath() {
		// Drop the "/gwt" prefix, and then add "*-template.*" as the suffix
		String segmentName = segmentName();
		if (segmentName.length() == 4) {
			segmentName = "";
		} else if (segmentName.equals("/server")) {
			segmentName = "server/";
		} else if (segmentName.equals("/")) {
			segmentName = "web/";
		} else if (STYLE.equals(this)) {
			return "style-template/*";
		} else if (STYLE_CLIENT.equals(this)) {
			return "style-template/client/*";
		} else {
			segmentName = segmentName.substring(5) + "/";
		}
		return segmentName + "*-template.*";
	}

	public String canonicalFileSystemPath(ProjectMetadata projectMetadata) {
		String packagePath = projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName().replace('.', '/') + segmentName();
		if (WEB.equals(this)) {
			return projectMetadata.getPathResolver().getRoot(Path.SRC_MAIN_WEBAPP);
		} else {
			return projectMetadata.getPathResolver().getIdentifier(Path.SRC_MAIN_JAVA, packagePath);
		}
	}

	public String canonicalFileSystemPath(ProjectMetadata projectMetadata, String filename) {
		return canonicalFileSystemPath(projectMetadata) + "/" + filename;
	}

	public String packageName(ProjectMetadata projectMetadata) {
		return projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName() + segmentName().replace('/', '.');
	}
}
