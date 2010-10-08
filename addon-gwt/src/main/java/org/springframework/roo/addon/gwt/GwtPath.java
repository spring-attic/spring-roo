package org.springframework.roo.addon.gwt;

import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;

public enum GwtPath {
	CLIENT("/client", "module/client/" + GwtPath.templateSelector),
	GWT_ROOT("/", "module/" + GwtPath.templateSelector),
	GWT_REQUEST("/client/request", "module/client/request/" + GwtPath.templateSelector),
	GWT_SCAFFOLD("/client/scaffold", "module/client/scaffold/" + GwtPath.templateSelector),
	GWT_SCAFFOLD_GENERATED("/client/scaffold/generated", "module/client/scaffold/generated/" + GwtPath.templateSelector),
	GWT_SCAFFOLD_UI("/client/scaffold/ui", "module/client/scaffold/ui/" + GwtPath.templateSelector),
	GWT_UI("/client/ui", "module/client/ui/" + GwtPath.templateSelector),
	SERVER("/server", "module/server/" + GwtPath.templateSelector),
	STYLE("/client/style", "module/client/style/" + GwtPath.templateSelector),
	SHARED("/shared", "module/shared/" + GwtPath.templateSelector),
	IOC("/client/scaffold/ioc", "module/client/scaffold/ioc/" + GwtPath.templateSelector),
	PLACE("/client/scaffold/place", "module/client/scaffold/place/" + GwtPath.templateSelector),
	PUBLIC("/public", "module/public/" + GwtPath.wildCardSelector),
	IMAGES("/public/images", "module/public/images/" + GwtPath.wildCardSelector),
	WEB("", "webapp/" + GwtPath.wildCardSelector);
        
    private static final String wildCardSelector = "*";
    private static final String templateSelector = "*-template.*";
    private final String segmentName;
    private final String sourceAntPath;
    
    GwtPath(String segmentName, String sourceAntPath) {    	
    	this.segmentName = segmentName;
    	this.sourceAntPath = sourceAntPath;    	
    }
	
	private String segmentName() {
		return segmentName;
	}

	public String sourceAntPath() {
		return sourceAntPath;
	}

	public String canonicalFileSystemPath(ProjectMetadata projectMetadata) {
		String packagePath = projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName().replace('.', '/') + segmentName();
		if (WEB.equals(this)) {
			return projectMetadata.getPathResolver().getRoot(Path.SRC_MAIN_WEBAPP);
		}	
		return projectMetadata.getPathResolver().getIdentifier(Path.SRC_MAIN_JAVA, packagePath);		
	}
	
	public String segmentPackage() {	
		if (WEB.equals(this)) {
			return "";
		} 
		return segmentName().substring(1).replace('/', '.');		
	}

	public String canonicalFileSystemPath(ProjectMetadata projectMetadata, String filename) {
		return canonicalFileSystemPath(projectMetadata) + "/" + filename;
	}

	public String packageName(ProjectMetadata projectMetadata) {
		if (WEB.equals(this)) {
			return "";
		} 
		return projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName() + segmentName().replace('/', '.');
	}
}
