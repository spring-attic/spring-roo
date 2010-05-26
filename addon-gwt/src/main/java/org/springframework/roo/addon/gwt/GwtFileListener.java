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
 */
@Component
@Service
public class GwtFileListener implements FileEventListener {
	@Reference private FileManager fileManager;
	@Reference private MetadataService metadataService;

	private ProjectMetadata projectMetadata;

	public void onFileEvent(FileEvent fileEvent) {
		projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		if (projectMetadata == null) {
			return;
		}

		String requestGwt = GwtPath.GWT_REQUEST.canonicalFileSystemPath(projectMetadata);
		String generatedGwt = GwtPath.GWT_SCAFFOLD_GENERATED.canonicalFileSystemPath(projectMetadata);
		String eventPath = fileEvent.getFileDetails().getCanonicalPath();

		if (!eventPath.endsWith(".java")) {
			return;
		}
		if (!eventPath.startsWith(requestGwt) && !eventPath.startsWith(generatedGwt)) {
			return;
		}

		// Something happened with a GWT auto-generated *.java file (or we're starting monitoring)

		// First thing is for us to figure out the record file (or what it used to be called, if it has gone away)
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
					// Drop the part of the filename with the suffix, as well as the extension
					String entityName = name.substring(0, name.lastIndexOf(t.getSuffix()));
					recordFile = GwtPath.GWT_REQUEST.canonicalFileSystemPath(projectMetadata, entityName + "Record.java");
					break;
				}
			}
		}
		Assert.hasText(recordFile, "Record file not computed for input " + eventPath);

		// Calculate the name without the "Record.java" portion (simplifies working with it later)
		String simpleName = new File(recordFile).getName();
		simpleName = simpleName.substring(0, simpleName.length() - 11); // Drop Record.java

		Assert.hasText(simpleName, "Simple name not computed for input " + eventPath);

		// Remove all the related files should the key no longer exist
		if (!fileManager.exists(recordFile)) {
			for (MirrorType t : MirrorType.values()) {
				String filename = simpleName + t.getSuffix() + ".java";
				String canonicalPath = t.getPath().canonicalFileSystemPath(projectMetadata, filename);
				deleteIfExists(canonicalPath);
			}
		}

		// By this point the directory structure should correspond to files that should exist

		// Now we need to refresh all the application-wide files

		// We're going to do this crudely (no JavaParser) just to get it done in a sensible period of time...
		updateApplicationEntityTypesProcessor(fileManager, projectMetadata);
		updateApplicationRequestFactory(fileManager, projectMetadata);
		updateApplicationServerSideOperations(fileManager, projectMetadata);
		updateListPlaceRendered(fileManager, projectMetadata);
		updateListActivitiesMapper();

		updatePlaceProcessor(fileManager, projectMetadata);
		// TODO: (cromwellian) don't know if I'm supposed to be doing this here instead of GwtMetaData, but I'm low on time updateScaffoldActivities(fileManager, projectMetadata);
		updateMasterActivities();
		updateDetailsActivities();
		updateMobileActivities();
		updatePlaceFilter();
		updateBasePlaceFilter();
		updatePlaceTypeFilter();
	}

	private void updateApplicationEntityTypesProcessor(FileManager fileManager, ProjectMetadata projectMetadata) {
		SharedType destType = SharedType.APP_ENTITY_TYPES_PROCESSOR;
		String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata, destType.getFullName() + ".java");
		final String param = "Class<? extends Record>";
		InvocableMemberBodyBuilder bb = new InvocableMemberBodyBuilder();
		bb.reset();
		bb.appendFormalLine("package " + destType.getPath().packageName(projectMetadata) + ";");
		bb.appendFormalLine("import java.util.HashSet;");
		bb.appendFormalLine("import java.util.Set;");
		bb.appendFormalLine("import com.google.gwt.valuestore.shared.Record;");
		bb.appendFormalLine("public class " + destType.getFullName() + " {");
		bb.indent();
		bb.appendFormalLine("private static Set<" + param + "> instance;");
		bb.appendFormalLine("private static Set<" + param + "> get() {");
		bb.indent();
		bb.appendFormalLine("if (instance == null) {");
		bb.indent();
		bb.appendFormalLine("instance = new HashSet<" + param + ">();");

		MirrorType locate = MirrorType.RECORD;
		String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
		for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
			String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
			JavaType javaType = new JavaType(locate.getPath().packageName(projectMetadata) + "." + fullPath);
			if (!fullPath.equals(param)) {
				bb.appendFormalLine("instance.add(" + javaType.getSimpleTypeName() + ".class);");
			}
		}

		bb.indentRemove();
		bb.appendFormalLine("}");
		bb.appendFormalLine("return instance;");
		bb.indentRemove();
		bb.appendFormalLine("}");

		bb.appendFormalLine("public static void processAll(EntityTypesProcessor processor) {");
		bb.indent();
		bb.appendFormalLine("for (" + param + " record : get()) {");
		bb.indent();
		bb.appendFormalLine("processor.processType(record);");
		bb.indentRemove();
		bb.appendFormalLine("}");
		bb.indentRemove();
		bb.appendFormalLine("}");

		bb.appendFormalLine("public interface EntityTypesProcessor {");
		bb.indent();
		bb.appendFormalLine("void processType(" + param + " record);");
		bb.indentRemove();
		bb.appendFormalLine("}");

		bb.indentRemove();
		bb.appendFormalLine("}");
		
		write(destFile, bb.getOutput(), fileManager);
	}

	private void updateApplicationRequestFactory(FileManager fileManager, ProjectMetadata projectMetadata) {
		SharedType destType = SharedType.APP_REQUEST_FACTORY;
		String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata, destType.getFullName() + ".java");
		InvocableMemberBodyBuilder bb = new InvocableMemberBodyBuilder();
		bb.reset();
		bb.appendFormalLine("package " + destType.getPath().packageName(projectMetadata) + ";");
		bb.appendFormalLine("import com.google.gwt.requestfactory.shared.RequestFactory;");
		bb.appendFormalLine("public interface " + destType.getFullName() + " extends RequestFactory {");
		bb.indent();

		MirrorType locate = MirrorType.REQUEST;
		String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
		for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
			String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
			JavaType javaType = new JavaType(locate.getPath().packageName(projectMetadata) + "." + fullPath);
			bb.appendFormalLine(javaType.getSimpleTypeName() + " " + StringUtils.uncapitalize(javaType.getSimpleTypeName()) + "();");
		}

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

	private void updateApplicationServerSideOperations(FileManager fileManager, ProjectMetadata projectMetadata) {
		SharedType destType = SharedType.APP_REQUEST_SERVER_SIDE_OPERATIONS;
		String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata, destType.getFullName() + ".java");
		InvocableMemberBodyBuilder bb = new InvocableMemberBodyBuilder();
		bb.reset();
		bb.appendFormalLine("package " + destType.getPath().packageName(projectMetadata) + ";");
		bb.appendFormalLine("import java.util.Collections;");
		bb.appendFormalLine("import java.util.HashMap;");
		bb.appendFormalLine("import java.util.HashSet;");
		bb.appendFormalLine("import java.util.Map;");
		bb.appendFormalLine("import java.util.Set;");
		bb.appendFormalLine("import com.google.gwt.requestfactory.shared.RequestFactory.Config;");
		bb.appendFormalLine("import com.google.gwt.requestfactory.shared.RequestFactory.RequestDefinition;");
		bb.appendFormalLine("import com.google.gwt.valuestore.shared.Record;");
		bb.appendFormalLine("public class " + destType.getFullName() + " implements Config {");
		bb.indent();

		bb.appendFormalLine("private final Map<String, RequestDefinition> map;");

		bb.appendFormalLine("public Map<String, RequestDefinition> requestDefinitions() {");
		bb.indent();
		bb.appendFormalLine("return map;");
		bb.indentRemove();
		bb.appendFormalLine("}");

		bb.appendFormalLine("private static void putAll(RequestDefinition[] values, Map<String, RequestDefinition> newMap) {");
		bb.indent();
		bb.appendFormalLine("for (RequestDefinition def : values) {");
		bb.indent();
		bb.appendFormalLine("newMap.put(def.name(), def);");
		bb.indentRemove();
		bb.appendFormalLine("}");
		bb.indentRemove();
		bb.appendFormalLine("}");

		bb.appendFormalLine("public ApplicationRequestServerSideOperations() {");
		bb.indent();
		bb.appendFormalLine("Map<String, RequestDefinition> newMap = new HashMap<String, RequestDefinition>();");
		MirrorType locate = MirrorType.REQUEST_SERVER_SIDE_OPERATIONS;
		String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
		for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
			String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
			JavaType javaType = new JavaType(locate.getPath().packageName(projectMetadata) + "." + fullPath);
			if (!fullPath.equals(SharedType.APP_REQUEST_SERVER_SIDE_OPERATIONS.getFullName())) {
				bb.appendFormalLine("putAll(" + javaType.getSimpleTypeName() + ".values(), newMap);");
			}
		}
		bb.appendFormalLine("map = Collections.unmodifiableMap(newMap);");
		bb.indentRemove();
		bb.appendFormalLine("}");

		bb.appendFormalLine("public Set<Class<? extends Record>> recordTypes() {");
		bb.indent();
		bb.appendFormalLine("Set<Class<? extends Record>> records = new HashSet<Class<? extends Record>>();");
		locate = MirrorType.RECORD;
		antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
		for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
			String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
			bb.appendFormalLine("records.add(" + fullPath + ".class);");
		}
		bb.appendFormalLine("return records;");
		bb.indentRemove();
		bb.appendFormalLine("}");

		bb.indentRemove();
		bb.appendFormalLine("}");
		
		write(destFile, bb.getOutput(), fileManager);
	}

	private void updatePlaceProcessor(FileManager fileManager, ProjectMetadata projectMetadata) {
		SharedType destType = SharedType.APP_PLACE_PROCESSOR;
		String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata, destType.getFullName() + ".java");
		InvocableMemberBodyBuilder bb = new InvocableMemberBodyBuilder();
		bb.reset();
		bb.appendFormalLine("package " + destType.getPath().packageName(projectMetadata) + ";");

		bb.appendFormalLine("public interface " + destType.getFullName() + " {");
		bb.indent();

		bb.appendFormalLine("void process(ApplicationListPlace object);");

		MirrorType locate = MirrorType.RECORD;
		String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
		for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
			String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
			String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Record" suffix from filename
			bb.appendFormalLine("void process(" + simpleName + MirrorType.SCAFFOLD_PLACE.getSuffix() + " place);");
		}
		bb.indentRemove();
		bb.appendFormalLine("}");
		
		write(destFile, bb.getOutput(), fileManager);
	}

	private void updateListPlaceRendered(FileManager fileManager, ProjectMetadata projectMetadata) {
		SharedType destType = SharedType.LIST_PLACE_RENDERER;
		String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata, destType.getFullName() + ".java");
		InvocableMemberBodyBuilder bb = new InvocableMemberBodyBuilder();
		bb.reset();
		bb.appendFormalLine("package " + destType.getPath().packageName(projectMetadata) + ";");
		bb.appendFormalLine("import com.google.gwt.input.shared.Renderer;");
		bb.appendFormalLine("import com.google.gwt.valuestore.shared.Record;");
		bb.appendFormalLine("import " + SharedType.APP_LIST_PLACE.getFullyQualifiedTypeName(projectMetadata) + ";");

		bb.appendFormalLine("public class " + destType.getFullName() + " implements Renderer<" + SharedType.APP_LIST_PLACE.getFullName() + "> {");
		bb.indent();

		bb.appendFormalLine("public String render(ApplicationListPlace object) {");
		bb.indent();
		bb.appendFormalLine("Class<? extends Record> type = object.getType();");
		MirrorType locate = MirrorType.RECORD;
		String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
		for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
			String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
			JavaType keyType = new JavaType(locate.getPath().packageName(projectMetadata) + "." + fullPath);
			String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Record" suffix from filename
			bb.appendFormalLine("if (type.equals(" + keyType.getFullyQualifiedTypeName() + ".class)) {");
			bb.indent();
			bb.appendFormalLine("return \"" + simpleName + "s\";");
			bb.indentRemove();
			bb.appendFormalLine("}");
		}
		bb.appendFormalLine("throw new IllegalArgumentException(\"Cannot render unknown type \" + object);");

		bb.indentRemove();
		bb.appendFormalLine("}");

		bb.indentRemove();
		bb.appendFormalLine("}");
		
		write(destFile, bb.getOutput(), fileManager);
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

	@SuppressWarnings("unchecked")
	public void updatePlaceFilter() {
		try {
			SharedType type = SharedType.APP_PLACE_FILTER;
			VelocityContext ctx = buildContext(type);
			addReference(ctx, SharedType.APP_PLACE);
			addReference(ctx, SharedType.APP_LIST_PLACE);

			MirrorType locate = MirrorType.RECORD;
			String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
			ArrayList<Map<String, String>> entities = new ArrayList<Map<String, String>>();
			ctx.put("entities", entities);
			ArrayList<String> imports = (ArrayList<String>) ctx.get("imports");
			for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
				String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
				String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Record" suffix from filename
				Map<String, String> ent = new HashMap<String, String>();
				ent.put("detailsPlace", simpleName + MirrorType.SCAFFOLD_PLACE.getSuffix());
				imports.add(MirrorType.SCAFFOLD_PLACE.getPath().packageName(projectMetadata) + "." + simpleName + MirrorType.SCAFFOLD_PLACE.getSuffix());
				entities.add(ent);
			}

			writeWithTemplate(type, ctx, TemplateResourceLoader.TEMPLATE_DIR + type.getVelocityTemplate());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public void updateBasePlaceFilter() {
		try {
			SharedType type = SharedType.BASE_PLACE_FILTER;
			VelocityContext ctx = buildContext(type);
			addReference(ctx, SharedType.APP_PLACE_FILTER);
			addReference(ctx, SharedType.APP_LIST_PLACE);

			MirrorType locate = MirrorType.RECORD;
			String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
			ArrayList<Map<String, String>> entities = new ArrayList<Map<String, String>>();
			ctx.put("entities", entities);
			ArrayList<String> imports = (ArrayList<String>) ctx.get("imports");
			for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
				String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
				String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Record" suffix from filename
				Map<String, String> ent = new HashMap<String, String>();
				ent.put("detailsPlace", simpleName + MirrorType.SCAFFOLD_PLACE.getSuffix());
				imports.add(MirrorType.SCAFFOLD_PLACE.getPath().packageName(projectMetadata) + "." + simpleName + MirrorType.SCAFFOLD_PLACE.getSuffix());
				entities.add(ent);
			}

			writeWithTemplate(type, ctx, TemplateResourceLoader.TEMPLATE_DIR + type.getVelocityTemplate());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public void updatePlaceTypeFilter() {
		try {
			SharedType type = SharedType.APP_PLACE_TO_RECORD_TYPE;
			VelocityContext ctx = buildContext(type);
			addReference(ctx, SharedType.APP_PLACE_FILTER);
			addReference(ctx, SharedType.APP_LIST_PLACE);

			MirrorType locate = MirrorType.RECORD;
			String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
			ArrayList<Map<String, String>> entities = new ArrayList<Map<String, String>>();
			ctx.put("entities", entities);
			ArrayList<String> imports = (ArrayList<String>) ctx.get("imports");
			for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
				String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
				String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Record" suffix from filename
				Map<String, String> ent = new HashMap<String, String>();
				ent.put("detailsPlace", simpleName + MirrorType.SCAFFOLD_PLACE.getSuffix());
				imports.add(MirrorType.SCAFFOLD_PLACE.getPath().packageName(projectMetadata) + "." + simpleName + MirrorType.SCAFFOLD_PLACE.getSuffix());
				imports.add(MirrorType.RECORD.getPath().packageName(projectMetadata) + "." + simpleName + MirrorType.RECORD.getSuffix());
				ent.put("record", simpleName + MirrorType.RECORD.getSuffix());
				entities.add(ent);
			}

			writeWithTemplate(type, ctx, TemplateResourceLoader.TEMPLATE_DIR + type.getVelocityTemplate());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public void updateMobileActivities() {
		try {
			SharedType type = SharedType.MOBILE_ACTIVITIES;
			VelocityContext ctx = buildContext(type);
			addReference(ctx, SharedType.APP_PLACE);
			addReference(ctx, SharedType.APP_LIST_PLACE);
			addReference(ctx, SharedType.APP_RECORD_PLACE);
			addReference(ctx, SharedType.APP_REQUEST_FACTORY);
			addReference(ctx, SharedType.APP_PLACE_FILTER);
			MirrorType locate = MirrorType.RECORD;
			String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
			ArrayList<Map<String, String>> entities = new ArrayList<Map<String, String>>();
			ctx.put("entities", entities);
			ArrayList<String> imports = (ArrayList<String>) ctx.get("imports");
			for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
				String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
				String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Record" suffix from filename
				Map<String, String> ent = new HashMap<String, String>();
				ent.put("name", simpleName);
				ent.put("nameUncapitalized", StringUtils.uncapitalize(simpleName));
				ent.put("activitiesMapper", simpleName + MirrorType.ACTIVITIES_MAPPER.getSuffix());
				imports.add(MirrorType.ACTIVITIES_MAPPER.getPath().packageName(projectMetadata) + "." + simpleName + MirrorType.ACTIVITIES_MAPPER.getSuffix());
				ent.put("detailsPlace", simpleName + MirrorType.SCAFFOLD_PLACE.getSuffix());
				imports.add(MirrorType.SCAFFOLD_PLACE.getPath().packageName(projectMetadata) + "." + simpleName + MirrorType.SCAFFOLD_PLACE.getSuffix());
				entities.add(ent);
			}

			writeWithTemplate(type, ctx, TemplateResourceLoader.TEMPLATE_DIR + type.getVelocityTemplate());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public void updateDetailsActivities() {
		try {
			SharedType type = SharedType.DETAILS_ACTIVITIES;
			VelocityContext ctx = buildContext(type);
			addReference(ctx, SharedType.APP_PLACE);
			addReference(ctx, SharedType.APP_RECORD_PLACE);
			addReference(ctx, SharedType.APP_REQUEST_FACTORY);
			addReference(ctx, SharedType.BASE_PLACE_FILTER);
			MirrorType locate = MirrorType.RECORD;
			String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
			ArrayList<Map<String, String>> entities = new ArrayList<Map<String, String>>();
			ctx.put("entities", entities);
			ArrayList<String> imports = (ArrayList<String>) ctx.get("imports");
			for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
				String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
				String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Record" suffix from filename
				Map<String, String> ent = new HashMap<String, String>();
				ent.put("name", simpleName);
				ent.put("nameUncapitalized", StringUtils.uncapitalize(simpleName));
				ent.put("activitiesMapper", simpleName + MirrorType.ACTIVITIES_MAPPER.getSuffix());
				imports.add(MirrorType.ACTIVITIES_MAPPER.getPath().packageName(projectMetadata) + "." + simpleName + MirrorType.ACTIVITIES_MAPPER.getSuffix());
				ent.put("detailsPlace", simpleName + MirrorType.SCAFFOLD_PLACE.getSuffix());
				imports.add(MirrorType.SCAFFOLD_PLACE.getPath().packageName(projectMetadata) + "." + simpleName + MirrorType.SCAFFOLD_PLACE.getSuffix());
				entities.add(ent);
			}

			writeWithTemplate(type, ctx, TemplateResourceLoader.TEMPLATE_DIR + type.getVelocityTemplate());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private void updateListActivitiesMapper() {
		try {
			SharedType type = SharedType.LIST_ACTIVITIES_MAPPER;
			VelocityContext ctx = buildContext(type);
			addReference(ctx, SharedType.APP_PLACE);
			addReference(ctx, SharedType.APP_LIST_PLACE);
			addReference(ctx, SharedType.APP_REQUEST_FACTORY);
			MirrorType locate = MirrorType.RECORD;
			String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
			ArrayList<Map<String, String>> entities = new ArrayList<Map<String, String>>();
			ctx.put("entities", entities);
			ArrayList<String> imports = (ArrayList<String>) ctx.get("imports");
			for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
				String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
				String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Record" suffix from filename
				Map<String, String> ent = new HashMap<String, String>();
				ent.put("name", simpleName);
				ent.put("listActivity", simpleName + MirrorType.LIST_ACTIVITY.getSuffix());
				imports.add(MirrorType.LIST_ACTIVITY.getPath().packageName(projectMetadata) + "." + simpleName + MirrorType.LIST_ACTIVITY.getSuffix());
				ent.put("record", simpleName + MirrorType.RECORD.getSuffix());
				imports.add(MirrorType.RECORD.getPath().packageName(projectMetadata) + "." + simpleName + MirrorType.RECORD.getSuffix());
				entities.add(ent);
			}

			writeWithTemplate(type, ctx, TemplateResourceLoader.TEMPLATE_DIR + type.getVelocityTemplate());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public void updateMasterActivities() {
		try {
			SharedType type = SharedType.MASTER_ACTIVITIES;
			VelocityContext ctx = buildContext(type);
			addReference(ctx, SharedType.APP_PLACE);
			addReference(ctx, SharedType.APP_LIST_PLACE);
			addReference(ctx, SharedType.APP_PLACE_TO_RECORD_TYPE);
			addReference(ctx, SharedType.APP_RECORD_PLACE);
			addReference(ctx, SharedType.LIST_ACTIVITIES_MAPPER);
			writeWithTemplate(type, ctx, TemplateResourceLoader.TEMPLATE_DIR + type.getVelocityTemplate());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private VelocityContext buildContext(SharedType destType) {
		JavaType javaType = new JavaType(destType.getFullyQualifiedTypeName(projectMetadata));
		String clazz = javaType.getSimpleTypeName();

		VelocityContext context = new VelocityContext();
		context.put("shared", new HashMap());
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

	@SuppressWarnings("unchecked")
	private void addReference(VelocityContext ctx, SharedType type) {
		addImport((List<String>) ctx.get("imports"), type);
		Map<String, String> sMap = (Map<String, String>) ctx.get("shared");
		sMap.put(type.getVelocityName(), getDestinationJavaType(type).getSimpleTypeName());
	}

	private void addImport(List<String> imports, SharedType type) {
		imports.add(getDestinationJavaType(type).getFullyQualifiedTypeName());
	}
}
