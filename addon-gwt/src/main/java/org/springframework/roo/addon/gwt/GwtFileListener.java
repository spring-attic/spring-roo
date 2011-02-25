package org.springframework.roo.addon.gwt;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.file.monitor.event.FileEvent;
import org.springframework.roo.file.monitor.event.FileEventListener;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ProjectMetadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Listens for the creation and deletion of files by {@link GwtMetadata}.
 *
 * @author Ben Alex
 * @author Alan Stewart
 * @author Ray Cromwell
 * @author Amit Manjhi
 * @author Ray Ryan
 */
@Component
@Service
public class GwtFileListener implements FileEventListener {
	private static final String PROXY_FILE_SUFFIX = "Proxy.java";
	@Reference private GwtFileManager gwtFileManager;
	@Reference private MetadataService metadataService;
	@Reference private GwtTemplatingService gwtTemplatingService;
	@Reference private GwtTypeService gwtTypeService;
	private boolean processedApplicationFiles = false;

	public void onFileEvent(FileEvent fileEvent) {
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		if (projectMetadata == null) {
			return;
		}

		String eventPath = fileEvent.getFileDetails().getCanonicalPath();

		if (!eventPath.endsWith(PROXY_FILE_SUFFIX)) {
			return;
		}
		boolean isMaintainedByRoo = eventPath.startsWith(GwtPath.MANAGED_REQUEST.canonicalFileSystemPath(projectMetadata));
		if (!isMaintainedByRoo && (processedApplicationFiles || !eventPath.startsWith(GwtPath.SCAFFOLD.canonicalFileSystemPath(projectMetadata)))) {
			return;
		}

		//TODO: What does this even do? Is it still needed? - JT
		//TODO: We need to decide how the add-on should deal with files after the mirrored type is no more
		// Something happened with a GWT auto-generated *.java file (or we're starting monitoring)
		/* if (isMaintainedByRoo) {
					// First thing is for us to figure out the proxy file (or what it used to be called, if it has gone away)
					String proxyFile = null;
					if (eventPath.endsWith("Proxy.java")) {
						proxyFile = eventPath;
					} else {
						String name = fileEvent.getFileDetails().getFile().getName();
						name = name.substring(0, name.length() - 5); // Drop .java
						for (SharedType t : SharedType.values()) {
							if (name.endsWith(t.getFullName()) || name.endsWith("_Roo_Gwt")) {
								// This is just a shared type; we don't care about changes to them
								return;
							}
						}

						// A suffix could be inclusive of another suffix, so we need to find the best (longest) match,
						// not necessarily the first match.
						String bestMatch = "";
						for (GwtType t : GwtType.values()) {
							String suffix = t.getSuffix();
							if (name.endsWith(suffix) && suffix.length() > bestMatch.length()) {
								// Drop the part of the filename with the suffix, as well as the extension
								bestMatch = suffix;
								String entityName = name.substring(0, name.lastIndexOf(suffix));
								proxyFile = GwtPath.MANAGED_REQUEST.canonicalFileSystemPath(projectMetadata, entityName + "Proxy.java");
							}
						}
					}
					Assert.hasText(proxyFile, "Proxy file not computed for input " + eventPath);

					// Calculate the name without the "Proxy.java" portion (simplifies working with it later)
					String simpleName = new File(proxyFile).getName();
					simpleName = simpleName.substring(0, simpleName.length() - 10); // Drop Proxy.java

					Assert.hasText(simpleName, "Simple name not computed for input " + eventPath);
				}*/

		// By this point the directory structure should correspond to files that should exist

		// Now we need to refresh all the application-wide files
		processedApplicationFiles = true;
		buildType(GwtType.APP_ENTITY_TYPES_PROCESSOR);
		buildType(GwtType.APP_REQUEST_FACTORY);
		buildType(GwtType.LIST_PLACE_RENDERER);
		buildType(GwtType.MASTER_ACTIVITIES);
		buildType(GwtType.LIST_PLACE_RENDERER);
		buildType(GwtType.DETAILS_ACTIVITIES);
		buildType(GwtType.MOBILE_ACTIVITIES);

	}

	private void buildType(GwtType type) {
		if (GwtType.LIST_PLACE_RENDERER.equals(type)) {
			ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
			HashMap<JavaSymbolName, List<JavaType>> watchedMethods = new HashMap<JavaSymbolName, List<JavaType>>();
			watchedMethods.put(new JavaSymbolName("render"), Collections.singletonList(new JavaType(projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName() + ".client.scaffold.place.ProxyListPlace")));
			type.setWatchedMethods(watchedMethods);
		} else {
			type.resolveMethodsToWatch(type);
		}

		type.resolveWatchedFieldNames(type);
		List<ClassOrInterfaceTypeDetails> templateTypeDetails = gwtTemplatingService.getStaticTemplateTypeDetails(type);
		List<ClassOrInterfaceTypeDetails> typesToBeWritten = new ArrayList<ClassOrInterfaceTypeDetails>();
		for (ClassOrInterfaceTypeDetails templateTypeDetail : templateTypeDetails) {
			typesToBeWritten.addAll(GwtUtils.buildType(type, templateTypeDetail, gwtTypeService.getExtendsTypes(templateTypeDetail)));
		}
		gwtFileManager.write(typesToBeWritten, type.isOverwriteConcrete());
	}
}
