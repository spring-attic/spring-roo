package org.springframework.roo.addon.gwt;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
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
		boolean isMaintainedByRoo = eventPath.startsWith(GwtPath.GWT_REQUEST.canonicalFileSystemPath(projectMetadata))
        || eventPath.startsWith(GwtPath.GWT_SCAFFOLD_GENERATED.canonicalFileSystemPath(projectMetadata));
		if (!isMaintainedByRoo
        && (processedApplicationFiles || !eventPath.startsWith(GwtPath.GWT_SCAFFOLD.canonicalFileSystemPath(projectMetadata)))) {
      return;
    }

		// Something happened with a GWT auto-generated *.java file (or we're starting monitoring)
    if (isMaintainedByRoo) {
      // First thing is for us to figure out the record file (or what it used to
      // be called, if it has gone away)
      String recordFile = null;
      if (eventPath.endsWith("Record.java")) {
        recordFile = eventPath;
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
            // Drop the part of the filename with the suffix, as well as the
            // extension
            String entityName = name.substring(0,
                name.lastIndexOf(t.getSuffix()));
            recordFile = GwtPath.GWT_REQUEST.canonicalFileSystemPath(
                projectMetadata, entityName + "Record.java");
            break;
          }
        }
      }
      Assert.hasText(recordFile, "Record file not computed for input "
          + eventPath);

      // Calculate the name without the "Record.java" portion (simplifies
      // working with it later)
      String simpleName = new File(recordFile).getName();
      simpleName = simpleName.substring(0, simpleName.length() - 11); // Drop Record.java

      Assert.hasText(simpleName, "Simple name not computed for input "
          + eventPath);

      // Remove all the related files should the key no longer exist
      if (!fileManager.exists(recordFile)) {
        for (MirrorType t : MirrorType.values()) {
          String filename = simpleName + t.getSuffix() + ".java";
          String canonicalPath = t.getPath().canonicalFileSystemPath(
              projectMetadata, filename);
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
		
		// TODO: (cromwellian) don't know if I'm supposed to be doing this here instead of GwtMetaData, but I'm low on time
		updateFactory();
		updateMasterActivities();
		updateDetailsActivities();
		updateMobileActivities();
	}

	private void updateApplicationEntityTypesProcessor(FileManager fileManager, ProjectMetadata projectMetadata) {
    SharedType type = SharedType.APP_ENTITY_TYPES_PROCESSOR;
    VelocityContext ctx = buildContext(type);

    MirrorType locate = MirrorType.RECORD;
    String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
    ArrayList<Map<String, String>> entities = new ArrayList<Map<String, String>>();
    ctx.put("entities", entities);
    for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
      String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
      String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Record" suffix from filename
      Map<String, String> ent = new HashMap<String, String>();
      ent.put("record", fullPath);
      ent.put("name", simpleName);
      ent.put("nameUncapitalized", StringUtils.uncapitalize(simpleName));
      entities.add(ent);
    }
  
    try {
      writeWithTemplate(type, ctx, TemplateResourceLoader.TEMPLATE_DIR + type.getVelocityTemplate());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

	private void updateApplicationRequestFactory(FileManager fileManager, ProjectMetadata projectMetadata) {
		SharedType destType = SharedType.APP_REQUEST_FACTORY;
		String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata, destType.getFullName() + ".java");
		InvocableMemberBodyBuilder bb = new InvocableMemberBodyBuilder();
		bb.reset();
		bb.appendFormalLine("package " + destType.getPath().packageName(projectMetadata) + ";");
		bb.appendFormalLine("import com.google.gwt.requestfactory.shared.RequestFactory;");
		bb.appendFormalLine("import com.google.gwt.requestfactory.shared.UserInformationRequest;");
		bb.appendFormalLine("public interface " + destType.getFullName() + " extends RequestFactory {");
		bb.indent();

		MirrorType locate = MirrorType.REQUEST;
		String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
		for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
			String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
			JavaType javaType = new JavaType(locate.getPath().packageName(projectMetadata) + "." + fullPath);
			bb.appendFormalLine(javaType.getSimpleTypeName() + " " + StringUtils.uncapitalize(javaType.getSimpleTypeName()) + "();");
		}
	  bb.appendFormalLine("UserInformationRequest userInformationRequest();");

		bb.indentRemove();
		bb.appendFormalLine("}");
		write(destFile, bb.getOutput(), fileManager);
	}

	public static String getQualifiedType(MirrorType type, ProjectMetadata projectMetadata, String clazz) {
		return type.getPath().packageName(projectMetadata) + "." + clazz + type.getSuffix();
	}

	public static String getQualifiedType(SharedType type, ProjectMetadata projectMetadata) {
		return type.getFullyQualifiedTypeName(projectMetadata);
	}

	private void updateListPlaceRenderer(FileManager fileManager, ProjectMetadata projectMetadata) {
    SharedType type = SharedType.LIST_PLACE_RENDERER;
    VelocityContext ctx = buildContext(type);
    addReference(ctx, SharedType.APP_ENTITY_TYPES_PROCESSOR);
    MirrorType locate = MirrorType.RECORD;
    String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
    ArrayList<Map<String, String>> entities = new ArrayList<Map<String, String>>();
    ctx.put("entities", entities);
    List<String> imports = asList(ctx.get("imports"));
    for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
      String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
      String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Record" suffix from filename
      Map<String, String> ent = new HashMap<String, String>();
      ent.put("record", fullPath);
      ent.put("name", simpleName);
      imports.add(MirrorType.RECORD.getPath().packageName(projectMetadata) + "." + simpleName + MirrorType.RECORD.getSuffix());
      entities.add(ent);
    }

    try {
      writeWithTemplate(type, ctx, TemplateResourceLoader.TEMPLATE_DIR + type.getVelocityTemplate());
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

	public void updateMobileActivities() {
		SharedType type = SharedType.MOBILE_ACTIVITIES;
		VelocityContext ctx = buildContext(type);
		addReference(ctx, SharedType.APP_REQUEST_FACTORY);
		MirrorType locate = MirrorType.RECORD;
		String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
		List<Map<String, String>> entities = new ArrayList<Map<String, String>>();
		ctx.put("entities", entities);
		List<String> imports = asList(ctx.get("imports"));
		for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
			String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
			String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Record" suffix from filename
			Map<String, String> ent = new HashMap<String, String>();
			ent.put("name", simpleName);
			ent.put("nameUncapitalized", StringUtils.uncapitalize(simpleName));
			ent.put("activitiesMapper", simpleName + MirrorType.ACTIVITIES_MAPPER.getSuffix());
			imports.add(MirrorType.ACTIVITIES_MAPPER.getPath().packageName(projectMetadata) + "." + simpleName + MirrorType.ACTIVITIES_MAPPER.getSuffix());
			entities.add(ent);
		}

    try {
			writeWithTemplate(type, ctx, TemplateResourceLoader.TEMPLATE_DIR + type.getVelocityTemplate());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public void updateFactory() {
    SharedType type = SharedType.FACTORY;
    VelocityContext ctx = buildContext(type);
    addReference(ctx, SharedType.APP_REQUEST_FACTORY);
    addReference(ctx, SharedType.PLACE_HISTORY_HANDLER);

    try {
      writeWithTemplate(type, ctx, TemplateResourceLoader.TEMPLATE_DIR + type.getVelocityTemplate());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

	public void updateDetailsActivities() {
		SharedType type = SharedType.DETAILS_ACTIVITIES;
		VelocityContext ctx = buildContext(type);
    addReference(ctx, SharedType.APP_REQUEST_FACTORY);
    addReference(ctx, SharedType.APP_ENTITY_TYPES_PROCESSOR);
		MirrorType locate = MirrorType.RECORD;
		String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
		ArrayList<Map<String, String>> entities = new ArrayList<Map<String, String>>();
		ctx.put("entities", entities);
		List<String> imports = asList(ctx.get("imports"));
		for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
			String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
			String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Record" suffix from filename
			Map<String, String> ent = new HashMap<String, String>();
      ent.put("record", fullPath);
			ent.put("name", simpleName);
			ent.put("nameUncapitalized", StringUtils.uncapitalize(simpleName));
			ent.put("activitiesMapper", simpleName + MirrorType.ACTIVITIES_MAPPER.getSuffix());
      imports.add(MirrorType.RECORD.getPath().packageName(projectMetadata) + "." + simpleName + MirrorType.RECORD.getSuffix());
      imports.add(MirrorType.ACTIVITIES_MAPPER.getPath().packageName(projectMetadata) + "." + simpleName + MirrorType.ACTIVITIES_MAPPER.getSuffix());
			entities.add(ent);
		}

    try {
			writeWithTemplate(type, ctx, TemplateResourceLoader.TEMPLATE_DIR + type.getVelocityTemplate());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public void updateMasterActivities() {
		SharedType type = SharedType.MASTER_ACTIVITIES;
		VelocityContext ctx = buildContext(type);
    addReference(ctx, SharedType.APP_REQUEST_FACTORY);
    addReference(ctx, SharedType.APP_ENTITY_TYPES_PROCESSOR);

    MirrorType locate = MirrorType.RECORD;
    String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
    ArrayList<Map<String, String>> entities = new ArrayList<Map<String, String>>();
    ctx.put("entities", entities);
    List<String> imports = asList(ctx.get("imports"));
    for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
      String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
      String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Record" suffix from filename
      Map<String, String> ent = new HashMap<String, String>();
      ent.put("record", fullPath);
      ent.put("name", simpleName);
      ent.put("nameUncapitalized", StringUtils.uncapitalize(simpleName));
      ent.put("listActivity", simpleName + MirrorType.LIST_ACTIVITY.getSuffix());
      imports.add(MirrorType.LIST_ACTIVITY.getPath().packageName(projectMetadata) + "." + simpleName + MirrorType.LIST_ACTIVITY.getSuffix());
      imports.add(MirrorType.RECORD.getPath().packageName(projectMetadata) + "." + simpleName + MirrorType.RECORD.getSuffix());
      entities.add(ent);
    }  
    
    try {
			writeWithTemplate(type, ctx, TemplateResourceLoader.TEMPLATE_DIR + type.getVelocityTemplate());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

  @SuppressWarnings("unchecked")
  private <T> List<T> asList(Object object) {
    return (List<T>) object;
  }
  
  @SuppressWarnings("unchecked")
  private <K, V> Map<K, V> asMap(Object object) {
    return (Map<K, V>) object;
  }
  
	private VelocityContext buildContext(SharedType destType) {
		JavaType javaType = new JavaType(destType.getFullyQualifiedTypeName(projectMetadata));
		String clazz = javaType.getSimpleTypeName();

		VelocityContext context = new VelocityContext();
		context.put("shared", new HashMap<String, String>());
		context.put("className", clazz);
		context.put("packageName", javaType.getPackage().getFullyQualifiedPackageName());
		ArrayList<String> imports = new ArrayList<String>();
		context.put("imports", imports);
		return context;
	}

	private JavaType getDestinationJavaType(SharedType destType) {
		return new JavaType(destType.getFullyQualifiedTypeName(projectMetadata));
	}

	private void writeWithTemplate(SharedType destType, VelocityContext context, String templateFile) throws Exception {
		String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + getDestinationJavaType(destType).getSimpleTypeName() + ".java";
		writeWithTemplate(destFile, context, templateFile);
	}

	private void writeWithTemplate(String destFile, VelocityContext context, String templateFile) throws Exception {
		VelocityEngine engine = new VelocityEngine();
		engine.setProperty("resource.loader", "mine");
		engine.setProperty("mine.resource.loader.instance", new TemplateResourceLoader());

		StringWriter sw = new StringWriter();
		engine.getTemplate(templateFile).merge(context, sw);
		write(destFile, sw.toString(), fileManager);
	}

	private void addReference(VelocityContext ctx, SharedType type) {
		List<String> imports = asList(ctx.get("imports"));
    addImport(imports, type);
		Map<String, String> shared = asMap(ctx.get("shared"));
		shared.put(type.getVelocityName(), getDestinationJavaType(type).getSimpleTypeName());
	}

	private void addImport(List<String> imports, SharedType type) {
		imports.add(getDestinationJavaType(type).getFullyQualifiedTypeName());
	}
}
