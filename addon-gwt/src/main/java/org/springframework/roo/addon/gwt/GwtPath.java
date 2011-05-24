package org.springframework.roo.addon.gwt;

import java.io.File;

import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;

public enum GwtPath {
	CLIENT("/client", "module/client/" + GwtPath.templateSelector),
	GWT_ROOT("/", "module/" + GwtPath.templateSelector),
	MANAGED_REQUEST("/client/managed/request", "module/client/request/" + GwtPath.templateSelector), // GWT_REQUEST
	SCAFFOLD_REQUEST("/client/scaffold/request", "module/client/scaffold/request/" + GwtPath.templateSelector),
	SCAFFOLD("/client/scaffold", "module/client/scaffold/" + GwtPath.templateSelector), // GWT_SCAFFOLD
	MANAGED("/client/managed", "module/client/managed/" + GwtPath.templateSelector), // GWT_SCAFFOLD_GENERATED
	SCAFFOLD_UI("/client/scaffold/ui", "module/client/scaffold/ui/" + GwtPath.templateSelector), // GWT_SCAFFOLD_UI
	MANAGED_UI("/client/managed/ui", "module/client/managed/ui/" + GwtPath.templateSelector),
	SERVER("/server", "module/server/" + GwtPath.templateSelector),
	SERVER_GAE("/server/gae", "module/server/gae/" + GwtPath.templateSelector),
	STYLE("/client/style", "module/client/style/" + GwtPath.templateSelector),
	SHARED("/shared", "module/shared/" + GwtPath.templateSelector),
	SHARED_SCAFFOLD("/shared/scaffold", "module/shared/scaffold/" + GwtPath.templateSelector),
	SHARED_GAE("/shared/gae", "module/shared/gae/" + GwtPath.templateSelector),
	SCAFFOLD_IOC("/client/scaffold/ioc", "module/client/scaffold/ioc/" + GwtPath.templateSelector), // IOC
	SCAFFOLD_PLACE("/client/scaffold/place", "module/client/scaffold/place/" + GwtPath.templateSelector), // PLACE
	SCAFFOLD_GAE("/client/scaffold/gae", "module/client/scaffold/gae/" + GwtPath.templateSelector),
	IMAGES("/client/style/images", "module/client/style/images/" + GwtPath.wildCardSelector),
	WEB("", "webapp/" + GwtPath.wildCardSelector),
	MANAGED_ACTIVITY("/client/managed/activity", "module/client/managed/activity/" + GwtPath.templateSelector),
	SCAFFOLD_ACTIVITY("/client/scaffold/activity", "module/client/scaffold/activity/" + GwtPath.templateSelector);

	private static final String wildCardSelector = "*";
	private static final String templateSelector = "*-template.*";
	private final String segmentName;
	private final String sourceAntPath;

	GwtPath(String segmentName, String sourceAntPath) {
		this.segmentName = segmentName;
		this.sourceAntPath = sourceAntPath;
	}

	private String getSegmentName() {
		return segmentName;
	}

	public String getSourceAntPath() {
		return sourceAntPath;
	}

	public String canonicalFileSystemPath(ProjectMetadata projectMetadata) {
		String packagePath = projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName().replace('.', File.separatorChar) + getSegmentName().replace('/', File.separatorChar);
		if (WEB.equals(this)) {
			return projectMetadata.getPathResolver().getRoot(Path.SRC_MAIN_WEBAPP);
		}
		return projectMetadata.getPathResolver().getIdentifier(Path.SRC_MAIN_JAVA, packagePath);
	}

	public String segmentPackage() {
		if (WEB.equals(this)) {
			return "";
		}
		return getSegmentName().substring(1).replace('/', '.');
	}

	public String canonicalFileSystemPath(ProjectMetadata projectMetadata, String filename) {
		return canonicalFileSystemPath(projectMetadata) + File.separatorChar + filename;
	}

	public String packageName(ProjectMetadata projectMetadata) {
		if (WEB.equals(this)) {
			return "";
		}
		return projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName() + getSegmentName().replace('/', '.');
	}
}
