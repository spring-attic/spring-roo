package org.springframework.roo.addon.gwt;

import hapax.Template;
import hapax.TemplateDataDictionary;
import hapax.TemplateDictionary;
import hapax.TemplateException;
import hapax.TemplateLoader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.file.monitor.event.FileEvent;
import org.springframework.roo.file.monitor.event.FileEventListener;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.StringUtils;

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
	@Reference private FileManager fileManager;
	@Reference private MetadataService metadataService;
	private ProjectMetadata projectMetadata;
	private boolean processedApplicationFiles = false;

	public void onFileEvent(FileEvent fileEvent) {
		projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		if (projectMetadata == null) {
			return;
		}

		String eventPath = fileEvent.getFileDetails().getCanonicalPath();

		if (!eventPath.endsWith(".java")) {
			return;
		}
		boolean isMaintainedByRoo = eventPath.startsWith(GwtPath.GWT_REQUEST.canonicalFileSystemPath(projectMetadata)) || eventPath.startsWith(GwtPath.GWT_SCAFFOLD_GENERATED.canonicalFileSystemPath(projectMetadata));
		if (!isMaintainedByRoo && (processedApplicationFiles || !eventPath.startsWith(GwtPath.GWT_SCAFFOLD.canonicalFileSystemPath(projectMetadata)))) {
			return;
		}

		// Something happened with a GWT auto-generated *.java file (or we're starting monitoring)
		if (isMaintainedByRoo) {
			// First thing is for us to figure out the proxy file (or what it used to be called, if it has gone away)
			String proxyFile = null;
			if (eventPath.endsWith("Proxy.java")) {
				proxyFile = eventPath;
			} else {
				String name = fileEvent.getFileDetails().getFile().getName();
				name = name.substring(0, name.length() - 5); // Drop .java
				for (SharedType t : SharedType.values()) {
					if (name.endsWith(t.getFullName())) {
						// This is just a shared type; we don't care about changes to them
						return;
					}
				}
				for (MirrorType t : MirrorType.values()) {
					if (name.endsWith(t.getSuffix())) {
						// Drop the part of the filename with the suffix, as well as the extension
						String entityName = name.substring(0, name.lastIndexOf(t.getSuffix()));
						proxyFile = GwtPath.GWT_REQUEST.canonicalFileSystemPath(projectMetadata, entityName + "Proxy.java");
						break;
					}
				}
			}
			Assert.hasText(proxyFile, "Proxy file not computed for input " + eventPath);

			// Calculate the name without the "Proxy.java" portion (simplifies working with it later)
			String simpleName = new File(proxyFile).getName();
			simpleName = simpleName.substring(0, simpleName.length() - 10); // Drop Proxy.java

			Assert.hasText(simpleName, "Simple name not computed for input " + eventPath);

			// Remove all the related files should the key no longer exist
			if (!fileManager.exists(proxyFile)) {
				for (MirrorType t : MirrorType.values()) {
					String filename = simpleName + t.getSuffix() + ".java";
					String canonicalPath = t.getPath().canonicalFileSystemPath(projectMetadata, filename);
					deleteIfExists(canonicalPath);
				}
			}
		}

		// By this point the directory structure should correspond to files that should exist

		// Now we need to refresh all the application-wide files
		processedApplicationFiles = true;
		updateApplicationEntityTypesProcessor(fileManager, projectMetadata);
		updateApplicationRequestFactory(fileManager, projectMetadata);
		updateListPlaceRenderer(fileManager, projectMetadata);
		updateModule();
		updateInjector();
		updateMasterActivities();
		updateDetailsActivities();
		updateMobileActivities();
	}

	private void updateApplicationEntityTypesProcessor(FileManager fileManager, ProjectMetadata projectMetadata) {
		SharedType type = SharedType.APP_ENTITY_TYPES_PROCESSOR;
		TemplateDataDictionary dataDictionary = buildDataDictionary(type);

		MirrorType locate = MirrorType.PROXY;
		String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
		for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
			String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
			String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Proxy" suffix from filename

			dataDictionary.addSection("proxys").setVariable("proxy", fullPath);

			String entity1 = new StringBuilder("\t\tif (").append(fullPath).append(".class.equals(clazz)) {\n\t\t\tprocessor.handle").append(simpleName).append("((").append(fullPath).append(") null);\n\t\t\treturn;\n\t\t}").toString();
			dataDictionary.addSection("entities1").setVariable("entity", entity1);

			String entity2 = new StringBuilder("\t\tif (proxy instanceof ").append(fullPath).append(") {\n\t\t\tprocessor.handle").append(simpleName).append("((").append(fullPath).append(") proxy);\n\t\t\treturn;\n\t\t}").toString();
			dataDictionary.addSection("entities2").setVariable("entity", entity2);

			String entity3 = new StringBuilder("\tpublic abstract void handle").append(simpleName).append("(").append(fullPath).append(" proxy);").toString();
			dataDictionary.addSection("entities3").setVariable("entity", entity3);
		}

		try {
			writeWithTemplate(type, dataDictionary);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void updateApplicationRequestFactory(FileManager fileManager, ProjectMetadata projectMetadata) {
		SharedType type = SharedType.APP_REQUEST_FACTORY;
		TemplateDataDictionary dataDictionary = buildDataDictionary(type);

		MirrorType locate = MirrorType.PROXY;
		String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
		for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
			String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
			String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Proxy" suffix from filename
			String entity = new StringBuilder("\t").append(simpleName).append("Request ").append(StringUtils.uncapitalize(simpleName)).append("Request();").toString();
			dataDictionary.addSection("entities").setVariable("entity", entity);
		}

		try {
			writeWithTemplate(type, dataDictionary, type.getTemplate());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public static String getQualifiedType(MirrorType type, ProjectMetadata projectMetadata, String clazz) {
		return type.getPath().packageName(projectMetadata) + "." + clazz + type.getSuffix();
	}

	public static String getQualifiedType(SharedType type, ProjectMetadata projectMetadata) {
		return type.getFullyQualifiedTypeName(projectMetadata);
	}

	private void updateListPlaceRenderer(FileManager fileManager, ProjectMetadata projectMetadata) {
		SharedType type = SharedType.LIST_PLACE_RENDERER;
		TemplateDataDictionary dataDictionary = buildDataDictionary(type);
		addReference(dataDictionary, SharedType.APP_ENTITY_TYPES_PROCESSOR);

		MirrorType locate = MirrorType.PROXY;
		String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
		for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
			String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
			String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Proxy" suffix from filename
      TemplateDataDictionary section = dataDictionary.addSection("entities");
      section.setVariable("entitySimpleName", simpleName);
      section.setVariable("entityFullPath", fullPath);
			addImport(dataDictionary, MirrorType.PROXY.getPath().packageName(projectMetadata) + "." + simpleName + MirrorType.PROXY.getSuffix());
		}

		try {
			writeWithTemplate(type, dataDictionary);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void write(String destFile, String newContents, FileManager fileManager) {
		// Write to disk, or update a file if it is already present
		MutableFile mutableFile = null;
		if (fileManager.exists(destFile)) {
			// First verify if the file has even changed
			File f = new File(destFile);
			String existing = null;
			try {
				existing = FileCopyUtils.copyToString(new FileReader(f));
			} catch (IOException ignoreAndJustOverwriteIt) {
			}

			if (!newContents.equals(existing)) {
				mutableFile = fileManager.updateFile(destFile);
			}
		} else {
			mutableFile = fileManager.createFile(destFile);
			Assert.notNull(mutableFile, "Could not create output file '" + destFile + "'");
		}

		try {
			if (mutableFile != null) {
				// If mutableFile was null, that means the source == destination content
				FileCopyUtils.copy(newContents.getBytes(), mutableFile.getOutputStream());
			}
		} catch (IOException ioe) {
			throw new IllegalStateException("Could not output '" + mutableFile.getCanonicalPath() + "'", ioe);
		}
	}

	private void deleteIfExists(String canonicalPath) {
		if (fileManager.exists(canonicalPath)) {
			fileManager.delete(canonicalPath);
		}
	}
	
	public void updateInjector() {
		SharedType type = SharedType.IOC_INJECTOR;
		TemplateDataDictionary dataDictionary = buildDataDictionary(type);
		addReference(dataDictionary, SharedType.IOC_MODULE);
		addReference(dataDictionary, SharedType.APP_REQUEST_FACTORY);
		
		try {
			writeWithTemplate(type, dataDictionary);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}	
	}
	
	public void updateModule() {
		SharedType type = SharedType.IOC_MODULE;
		TemplateDataDictionary dataDictionary = buildDataDictionary(type);
		addReference(dataDictionary, SharedType.APP_REQUEST_FACTORY);
		
		try {
			writeWithTemplate(type, dataDictionary);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}	
	}

	public void updateMasterActivities() {
		SharedType type = SharedType.MASTER_ACTIVITIES;
		TemplateDataDictionary dataDictionary = buildDataDictionary(type);
		addReference(dataDictionary, SharedType.APP_REQUEST_FACTORY);
		addReference(dataDictionary, SharedType.APP_ENTITY_TYPES_PROCESSOR);

		MirrorType locate = MirrorType.PROXY;
		String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
		for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
			String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
			String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Proxy" suffix from filename
      TemplateDataDictionary section = dataDictionary.addSection("entities");
      section.setVariable("entitySimpleName", simpleName);
      section.setVariable("entityFullPath", fullPath);
      addImport(dataDictionary, simpleName, MirrorType.LIST_ACTIVITY);
      addImport(dataDictionary, simpleName, MirrorType.PROXY);
      addImport(dataDictionary, simpleName, MirrorType.LIST_VIEW);
		}

		try {
			writeWithTemplate(type, dataDictionary);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

  private void addImport(TemplateDataDictionary dataDictionary,
      String simpleName, MirrorType mirrorType) {
    addImport(dataDictionary, mirrorType.getPath().packageName(projectMetadata) + "." + simpleName + mirrorType.getSuffix());
  }

	public void updateDetailsActivities() {
		SharedType type = SharedType.DETAILS_ACTIVITIES;
		TemplateDataDictionary dataDictionary = buildDataDictionary(type);
		addReference(dataDictionary, SharedType.APP_REQUEST_FACTORY);
		addReference(dataDictionary, SharedType.APP_ENTITY_TYPES_PROCESSOR);

		MirrorType locate = MirrorType.PROXY;
		String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
		for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
			String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
			String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Proxy" suffix from filename
			String entity = new StringBuilder("\t\t\tpublic void handle").append(simpleName).append("(").append(fullPath).append(" proxy) {\n").append("\t\t\t\tsetResult(new ").append(simpleName).append("ActivitiesMapper(requests, placeController).getActivity(proxyPlace));\n\t\t\t}").toString();
			dataDictionary.addSection("entities").setVariable("entity", entity);
			addImport(dataDictionary, MirrorType.PROXY.getPath().packageName(projectMetadata) + "." + simpleName + MirrorType.PROXY.getSuffix());
			addImport(dataDictionary, MirrorType.ACTIVITIES_MAPPER.getPath().packageName(projectMetadata) + "." + simpleName + MirrorType.ACTIVITIES_MAPPER.getSuffix());
		}

		try {
			writeWithTemplate(type, dataDictionary);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public void updateMobileActivities() {
		SharedType type = SharedType.MOBILE_ACTIVITIES;
		TemplateDataDictionary dataDictionary = buildDataDictionary(type);

		try {
			writeWithTemplate(type, dataDictionary);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private TemplateDataDictionary buildDataDictionary(SharedType destType) {
		JavaType javaType = getDestinationJavaType(destType);
		TemplateDataDictionary dataDictionary = TemplateDictionary.create();
		dataDictionary.setVariable("className", javaType.getSimpleTypeName());
		dataDictionary.setVariable("packageName", javaType.getPackage().getFullyQualifiedPackageName());
		return dataDictionary;
	}

	private JavaType getDestinationJavaType(SharedType destType) {
		return new JavaType(destType.getFullyQualifiedTypeName(projectMetadata));
	}

	private void writeWithTemplate(SharedType destType, TemplateDataDictionary dataDictionary, String templateFile) throws TemplateException {
		String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + getDestinationJavaType(destType).getSimpleTypeName() + ".java";
		writeWithTemplate(destFile, dataDictionary, templateFile);
	}

	private void writeWithTemplate(SharedType destType, TemplateDataDictionary dataDictionary) throws TemplateException {
		String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + getDestinationJavaType(destType).getSimpleTypeName() + ".java";
		writeWithTemplate(destFile, dataDictionary, destType.getTemplate());
	}

	private void writeWithTemplate(String destFile, TemplateDataDictionary dataDictionary, String templateFile) throws TemplateException {
		TemplateLoader templateLoader = TemplateResourceLoader.create();
		Template template = templateLoader.getTemplate(templateFile);
		write(destFile, template.renderToString(dataDictionary), fileManager);
	}

	private void addReference(TemplateDataDictionary dataDictionary, SharedType type) {
		addImport(dataDictionary, getDestinationJavaType(type).getFullyQualifiedTypeName());
		dataDictionary.setVariable(type.getName(), getDestinationJavaType(type).getSimpleTypeName());
	}
	
	private void addImport(TemplateDataDictionary dataDictionary, String importDeclaration) {
		dataDictionary.addSection("imports").setVariable("import", importDeclaration);		
	}
}
