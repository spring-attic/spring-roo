package org.springframework.roo.addon.gwt;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.entity.EntityMetadata;
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
 */
@Component
@Service
public class GwtFileListener implements FileEventListener {

	@Reference private FileManager fileManager;
	@Reference private MetadataService metadataService;

	public void onFileEvent(FileEvent fileEvent) {
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
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
                updateListActivitiesMapper(fileManager, projectMetadata);
                updatePlaceProcessor(fileManager, projectMetadata);
                updatePlaceFilter(fileManager, projectMetadata);
                // TODO: (cromwellian) don't know if I'm supposed to be doing
                // this here instead of GwtMetaData, but I'm low on time
                updateListActivity(fileManager, projectMetadata);
                updateDetailsActivity(fileManager, projectMetadata);
                updateEditActivity(fileManager, projectMetadata);
                updateScaffoldActivities(fileManager, projectMetadata);
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

  private void updateListActivity(FileManager fileManager, ProjectMetadata projectMetadata) {

    MirrorType locate = MirrorType.RECORD;
    String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**"
        + locate.getSuffix() + ".java";
    for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
      String fullPath = fd.getFile().getName()
          .substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
      String clazz = fullPath.substring(0, fullPath.length() - locate.getSuffix().length());
      JavaType javaType = new JavaType(locate.getPath().packageName(projectMetadata) + "." + fullPath);

      MirrorType dType = MirrorType.LIST_ACTIVITY;
      String destFile = dType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + clazz
          + dType.getSuffix() + ".java";
      InvocableMemberBodyBuilder bb = new InvocableMemberBodyBuilder();
      bb.reset();
      bb.appendFormalLine("package " + dType.getPath().packageName(projectMetadata) + ";");
      bb.appendFormalLine("import com.google.gwt.app.place.PlaceController;");
      bb.appendFormalLine("import com.google.gwt.bikeshed.list.shared.Range;");
      bb.appendFormalLine("import com.google.gwt.requestfactory.shared.Receiver;");
      bb.appendFormalLine("import com.google.gwt.requestfactory.shared.RecordListRequest;");
      bb.appendFormalLine("import com.google.gwt.valuestore.ui.AbstractRecordListActivity;");
      bb.appendFormalLine("import com.google.gwt.valuestore.ui.RecordListView;");
      bb.appendFormalLine("import " + getQualifiedType(MirrorType.SCAFFOLD_PLACE, projectMetadata, clazz) + ";");
      bb.appendFormalLine("import " + getQualifiedType(MirrorType.RECORD, projectMetadata, clazz) + ";");
      bb.appendFormalLine("import " + getQualifiedType(SharedType.APP_REQUEST_FACTORY, projectMetadata) + ";");
      bb.appendFormalLine("import " + getQualifiedType(SharedType.APP_PLACE, projectMetadata) + ";");
      bb.appendFormalLine("import " + getQualifiedType(SharedType.APP_RECORD_PLACE, projectMetadata) + ".Operation;");

      String recordType = getQualifiedType(MirrorType.RECORD, projectMetadata, clazz);
      bb.appendFormalLine(
          "public class " + clazz + dType.getSuffix() + " extends AbstractRecordListActivity<" + recordType + "> {");
      bb.indent();
      bb.appendFormalLine("private static RecordListView<" + recordType + "> defaultView;");
      bb.appendFormalLine("private static RecordListView<" + recordType + "> getDefaultView() {");
      bb.indent();
      bb.appendFormalLine("if (defaultView == null) {");
      bb.indent();
      bb.appendFormalLine(
          " defaultView = new " + getQualifiedType(MirrorType.LIST_VIEW, projectMetadata, clazz) + "();");
      bb.indentRemove();
      bb.appendFormalLine("}");
      bb.appendFormalLine("return defaultView;");
      bb.indentRemove();
      bb.appendFormalLine("}");
      String appFactory = getQualifiedType(SharedType.APP_REQUEST_FACTORY, projectMetadata);
      bb.appendFormalLine("private final " + appFactory + " requests;");
      bb.appendFormalLine("private final PlaceController<ApplicationPlace> placeController;");
      bb.appendFormalLine("public "+clazz + dType.getSuffix()+"("+appFactory+" requests, PlaceController<ApplicationPlace> placeController) {");
      bb.appendFormalLine("  this(requests, getDefaultView(), placeController);");
      bb.appendFormalLine("}");
      bb.appendFormalLine("public "+clazz + dType.getSuffix()+"("+appFactory+" requests, RecordListView<"+recordType+"> view, PlaceController<ApplicationPlace> placeController) {");
      bb.appendFormalLine("  super(view);");
      bb.appendFormalLine("  this.requests = requests;");
      bb.appendFormalLine("  this.placeController = placeController;");
      bb.appendFormalLine("}");
      
      bb.appendFormalLine("public void edit("+recordType+" record) {");
      bb.appendFormalLine("  placeController.goTo(new "+getQualifiedType(MirrorType.SCAFFOLD_PLACE, projectMetadata, clazz)+"(record, Operation.EDIT));");
      bb.appendFormalLine("}");
      
      bb.appendFormalLine("public void showDetails("+recordType+" record) {");
      bb.appendFormalLine("  placeController.goTo(new "+getQualifiedType(MirrorType.SCAFFOLD_PLACE, projectMetadata, clazz)+"(record, Operation.DETAILS));");
      bb.appendFormalLine("}");
      
      
      bb.appendFormalLine("protected RecordListRequest<"+recordType+"> createRangeRequest(Range range) {");
      bb.appendFormalLine("  return requests."+StringUtils.uncapitalize(clazz)+"Request().find"+clazz+"Entries(range.getStart(), range.getLength());");
      bb.appendFormalLine("}");
      
      bb.appendFormalLine("protected void fireCountRequest(Receiver<Long> callback) {");
      bb.appendFormalLine("  requests."+StringUtils.uncapitalize(clazz)+"Request().count"+clazz+"s().to(callback).fire();");
      bb.appendFormalLine("}");
      bb.indentRemove();
      bb.appendFormalLine("}");
      write(destFile, bb.getOutput(), fileManager);
    }
  }

  private void updateListView(FileManager fileManager, ProjectMetadata projectMetadata) {

      MirrorType locate = MirrorType.RECORD;
      String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**"
          + locate.getSuffix() + ".java";
      for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
        String fullPath = fd.getFile().getName()
            .substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
        String clazz = fullPath.substring(0, fullPath.length() - locate.getSuffix().length());
        JavaType javaType = new JavaType(locate.getPath().packageName(projectMetadata) + "." + fullPath);

        MirrorType dType = MirrorType.LIST_VIEW;
        String destFile = dType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + clazz
            + dType.getSuffix() + ".java";
        InvocableMemberBodyBuilder bb = new InvocableMemberBodyBuilder();
        bb.reset();
        bb.appendFormalLine("package " + dType.getPath().packageName(projectMetadata) + ";");
        bb.appendFormalLine("import com.google.gwt.bikeshed.list.client.CellTable;");
        bb.appendFormalLine("import com.google.gwt.uibinder.client.UiField;");
        bb.appendFormalLine("import com.google.gwt.uibinder.client.UiBinder;");
                                
        bb.appendFormalLine("import com.google.gwt.user.client.ui.HTMLPanel;");
        bb.appendFormalLine("import com.google.gwt.valuestore.ui.AbstractRecordListActivity;");
        bb.appendFormalLine("import com.google.gwt.valuestore.ui.PropertyColumn;");
        bb.appendFormalLine("import " + getQualifiedType(MirrorType.RECORD, projectMetadata, clazz) + ";");

        String recordType = getQualifiedType(MirrorType.RECORD, projectMetadata, clazz);
        bb.appendFormalLine(
            "public class " + clazz + dType.getSuffix() + " extends AbstractRecordListView<" + recordType + "> {");
        bb.indent();
        bb.appendFormalLine("interface Binder extends UiBinder<HTMLPanel, "+clazz+dType.getSuffix()+"> {}");
        
        bb.appendFormalLine("private static final Binder BINDER = GWT.create(Binder.class);");
        bb.appendFormalLine("@UiField CellTable<"+recordType+"> table;");
        
        bb.appendFormalLine("public "+clazz+dType.getSuffix()+"() {");
        bb.indent();
        bb.appendFormalLine("init(BINDER.createAndBindUi(this), table, getColumns());");
        bb.indentRemove();
        bb.appendFormalLine("}");
        bb.indent();
        
       
     
      
        bb.appendFormalLine("protected List<PropertyColumn<"+recordType+", ?>> getColumns() {");
        bb.indent();
        bb.appendFormalLine("List<PropertyColumn<"+recordType+", ?>> columns = new ArrayList<PropertyColumn<"+recordType+", ?>>();");
        bb.appendFormalLine("}");
        bb.indentRemove();
        bb.appendFormalLine("}");
        write(destFile, bb.getOutput(), fileManager);
      }
    }

  
  private void updateDetailsActivity(FileManager fileManager, ProjectMetadata projectMetadata) {

    MirrorType locate = MirrorType.RECORD;
    String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**"
        + locate.getSuffix() + ".java";
    for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
      String fullPath = fd.getFile().getName()
          .substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
      String clazz = fullPath.substring(0, fullPath.length() - locate.getSuffix().length());
      JavaType javaType = new JavaType(locate.getPath().packageName(projectMetadata) + "." + fullPath);

      MirrorType dType = MirrorType.DETAIL_ACTIVITY;
      String destFile = dType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + clazz
          + dType.getSuffix() + ".java";
      InvocableMemberBodyBuilder bb = new InvocableMemberBodyBuilder();
      bb.reset();
      bb.appendFormalLine("package " + dType.getPath().packageName(projectMetadata) + ";");
      bb.appendFormalLine("import com.google.gwt.app.place.AbstractActivity;");
      bb.appendFormalLine("import com.google.gwt.app.util.IsWidget;");
      bb.appendFormalLine("import com.google.gwt.requestfactory.shared.Receiver;");
      bb.appendFormalLine("import com.google.gwt.valuestore.shared.Value;");
      bb.appendFormalLine("import com.google.gwt.user.client.ui.TakesValue;");
      bb.appendFormalLine("import com.google.gwt.valuestore.ui.RecordListView;");
      
      bb.appendFormalLine("import " + getQualifiedType(MirrorType.RECORD, projectMetadata, clazz) + ";");
      bb.appendFormalLine("import " + getQualifiedType(SharedType.APP_REQUEST_FACTORY, projectMetadata) + ";");

      String recordType = getQualifiedType(MirrorType.RECORD, projectMetadata, clazz);
      bb.appendFormalLine(
          "public class " + clazz + dType.getSuffix() + " extends AbstractActivity {");
      bb.indent();
      bb.appendFormalLine("public interface View extends TakesValue<"+recordType+">, IsWidget {}");
      bb.appendFormalLine("private static View defaultView;");
      bb.appendFormalLine("private static View getDefaultView() {");
      bb.indent();
      bb.appendFormalLine("if (defaultView == null) {");
      bb.indent();
      bb.appendFormalLine(
          " defaultView = new " + getQualifiedType(MirrorType.DETAILS_VIEW, projectMetadata, clazz) + "();");
      bb.indentRemove();
      bb.appendFormalLine("}");
      bb.appendFormalLine("return defaultView;");
      bb.indentRemove();
      bb.appendFormalLine("}");
      String appFactory = getQualifiedType(SharedType.APP_REQUEST_FACTORY, projectMetadata);
      bb.appendFormalLine("private final " + appFactory + " requests;");
      bb.appendFormalLine("private final View view;");
      bb.appendFormalLine("private String id;");
      
      bb.appendFormalLine("public "+clazz + dType.getSuffix()+"(String id, "+appFactory+" requests) {");
      bb.appendFormalLine("  this(id, requests, getDefaultView());");
      bb.appendFormalLine("}");
      bb.appendFormalLine("public "+clazz + dType.getSuffix()+"(String id, "+appFactory+" requests, View view) {");
      bb.appendFormalLine("  this.id = id;");
      bb.appendFormalLine("  this.requests = requests;");
      bb.appendFormalLine("  this.view = view;");
      bb.appendFormalLine("}");
      
      bb.appendFormalLine("public void start(final Display display) {");
      bb.appendFormalLine("  Receiver<"+recordType+"> callback = new Receiver<"+recordType+">() {");
      bb.indent();
      bb.appendFormalLine("public void onSuccess("+recordType+" record) {");
      bb.indent();
      bb.appendFormalLine("view.setValue(record);");
      bb.appendFormalLine("display.showActivityWidget(view);");
      bb.indentRemove();
      bb.appendFormalLine("}");
      bb.indentRemove();
      bb.appendFormalLine("};");
      bb.appendFormalLine("requests."+StringUtils.uncapitalize(clazz)+"Request().find"+clazz+"(Value.of(id)).to(callback).fire();");
      bb.appendFormalLine("}");
      
    
      bb.indentRemove();
      bb.appendFormalLine("}");
      write(destFile, bb.getOutput(), fileManager);
    }
  }
  
  private void updateEditActivity(FileManager fileManager, ProjectMetadata projectMetadata) {

    MirrorType locate = MirrorType.RECORD;
    String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**"
        + locate.getSuffix() + ".java";
    for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
      String fullPath = fd.getFile().getName()
          .substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
      String clazz = fullPath.substring(0, fullPath.length() - locate.getSuffix().length());
      JavaType javaType = new JavaType(locate.getPath().packageName(projectMetadata) + "." + fullPath);

      MirrorType dType = MirrorType.EDIT_ACTIVITY;
      String destFile = dType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + clazz
          + dType.getSuffix() + ".java";
      InvocableMemberBodyBuilder bb = new InvocableMemberBodyBuilder();
      bb.reset();
      bb.appendFormalLine("package " + dType.getPath().packageName(projectMetadata) + ";");
      bb.appendFormalLine("import com.google.gwt.app.place.AbstractActivity;");
      bb.appendFormalLine("import com.google.gwt.app.place.PlaceController;");
      bb.appendFormalLine("import com.google.gwt.requestfactory.shared.Receiver;");
      bb.appendFormalLine("import com.google.gwt.user.client.Window;");
      bb.appendFormalLine("import com.google.gwt.valuestore.shared.Value;");
      bb.appendFormalLine("import com.google.gwt.valuestore.shared.DeltaValueStore;");
      bb.appendFormalLine("import com.google.gwt.valuestore.ui.RecordEditView;");
      bb.appendFormalLine("import com.google.gwt.requestfactory.shared.SyncResult;");
      bb.appendFormalLine("import java.util.Set;");
      bb.appendFormalLine("import " + getQualifiedType(MirrorType.RECORD, projectMetadata, clazz) + ";");
      bb.appendFormalLine("import " + getQualifiedType(SharedType.APP_REQUEST_FACTORY, projectMetadata) + ";");
      bb.appendFormalLine("import " + getQualifiedType(SharedType.APP_PLACE, projectMetadata) + ";");
      bb.appendFormalLine("import " + getQualifiedType(SharedType.APP_LIST_PLACE, projectMetadata) + ";");
      
      String recordType = getQualifiedType(MirrorType.RECORD, projectMetadata, clazz);
      bb.appendFormalLine(
          "public class " + clazz + dType.getSuffix() + " extends AbstractActivity implements RecordEditView.Delegate {");
      bb.indent();
      bb.appendFormalLine("private static RecordEditView<" + recordType + "> defaultView;");
      bb.appendFormalLine("private static RecordEditView<" + recordType + "> getDefaultView() {");
      bb.indent();
      bb.appendFormalLine("if (defaultView == null) {");
      bb.indent();
      bb.appendFormalLine(
          " defaultView = new " + getQualifiedType(MirrorType.EDIT_VIEW, projectMetadata, clazz) + "();");
      bb.indentRemove();
      bb.appendFormalLine("}");
      bb.appendFormalLine("return defaultView;");
      bb.indentRemove();
      bb.appendFormalLine("}");
      String appFactory = getQualifiedType(SharedType.APP_REQUEST_FACTORY, projectMetadata);
      bb.appendFormalLine("private final " + appFactory + " requests;");
      bb.appendFormalLine("private final RecordEditView<"+recordType+"> view;");
      bb.appendFormalLine("private final String id;");
      bb.appendFormalLine("private final PlaceController<"+getQualifiedType(SharedType.APP_PLACE, projectMetadata)+"> placeController;");
      bb.appendFormalLine("private DeltaValueStore deltas;");
      
      bb.appendFormalLine("public "+clazz + dType.getSuffix()+"(String id, "+appFactory+" requests, PlaceController<"+getQualifiedType(SharedType.APP_PLACE, projectMetadata)+"> placeController) {");
      bb.appendFormalLine("  this(id, getDefaultView(), requests, placeController);");
      bb.appendFormalLine("}");
      
      bb.appendFormalLine("public "+clazz + dType.getSuffix()+"(String id, RecordEditView<"+recordType+"> view, "+appFactory+" requests, PlaceController<"+getQualifiedType(SharedType.APP_PLACE, projectMetadata)+"> placeController) {");
      bb.appendFormalLine("  this.requests = requests;");
      bb.appendFormalLine("  this.id = id;");
      bb.appendFormalLine("  this.view = view;");
      bb.appendFormalLine("  this.deltas = requests.getValueStore().spawnDeltaView();");
      bb.appendFormalLine("  this.placeController = placeController;");
      bb.appendFormalLine("  view.setDelegate(this);");
      bb.appendFormalLine("  view.setDeltaValueStore(deltas);");
      bb.appendFormalLine("}");
      
      bb.appendFormalLine("public void saveClicked() {");
      bb.appendFormalLine("  if (deltas.isChanged()) {");
      bb.indent();
      bb.appendFormalLine("view.setEnabled(false);");
      bb.appendFormalLine("final DeltaValueStore toCommit = deltas;");
      bb.appendFormalLine("deltas = null;");
      bb.appendFormalLine("Receiver<Set<SyncResult>> receiver = new Receiver<Set<SyncResult>>() {");
      bb.indent();
      bb.appendFormalLine("public void onSuccess(Set<SyncResult> response) {");
      bb.indent();
      bb.appendFormalLine("boolean hasViolations = false;");
      bb.appendFormalLine("for (SyncResult syncResult : response) {");
      bb.indent();
      bb.appendFormalLine("if (syncResult.getRecord().getId().equals(id)) {");
      bb.indent();
      bb.appendFormalLine("if (syncResult.hasViolations()) {");
      bb.indent();
      bb.appendFormalLine("hasViolations = true;");
      bb.appendFormalLine("view.showErrors(syncResult.getViolations());");
      bb.indentRemove();
      bb.appendFormalLine("}");
      bb.indentRemove();
      bb.appendFormalLine("}");
      
      bb.indentRemove();
      bb.appendFormalLine("}");
      bb.appendFormalLine("if (!hasViolations) { placeController.goTo(new "+getQualifiedType(SharedType.APP_LIST_PLACE, projectMetadata)+"("+clazz+"Record.class)); }");
      bb.appendFormalLine("else {");
      bb.indent();
      bb.appendFormalLine("view.setEnabled(true);");
      bb.appendFormalLine("deltas = toCommit;");
      bb.appendFormalLine("deltas.clearUsed();");
      bb.indentRemove();
      bb.appendFormalLine("}");
      bb.indentRemove();
      bb.appendFormalLine("}");
      bb.indentRemove();
      bb.appendFormalLine("};");
      
      bb.appendFormalLine("requests.syncRequest(toCommit).to(receiver).fire();");
      bb.indentRemove();
      bb.appendFormalLine("}");
      bb.indentRemove();
      bb.appendFormalLine("}");
      
      bb.appendFormalLine("public void start(final Display display) {");
      bb.appendFormalLine("  Receiver<"+recordType+"> callback = new Receiver<"+recordType+">() {");
      bb.indent();
      bb.appendFormalLine("public void onSuccess("+recordType+" record) {");
      bb.indent();
      bb.appendFormalLine("view.setEnabled(true);");
      bb.appendFormalLine("view.setValue(record);");
      bb.appendFormalLine("view.showErrors(null);");
      bb.appendFormalLine("display.showActivityWidget(view);");
      bb.indentRemove();
      bb.appendFormalLine("}");
      bb.indentRemove();
      bb.appendFormalLine("};");
      bb.appendFormalLine("requests."+StringUtils.uncapitalize(clazz)+"Request().find"+clazz+"(Value.of(id)).to(callback).fire();");
      bb.appendFormalLine("}");
      
      bb.appendFormalLine("public boolean willStop() {");
      bb.indent();
      bb.appendFormalLine("return deltas == null || !deltas.isChanged() || Window.confirm(\"Dude! Really drop your edits?\");");
      bb.indentRemove();
      bb.appendFormalLine("}");
      bb.appendFormalLine("}");
      write(destFile, bb.getOutput(), fileManager);
    }
  }
  
  public static String getQualifiedType(MirrorType type, ProjectMetadata projectMetadata,
      String clazz) {
    return type.getPath().packageName(projectMetadata)
        + "." + clazz + type.getSuffix();
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

	private void updateScaffoldListViewBuilder(FileManager fileManager, ProjectMetadata projectMetadata) {
		SharedType destType = SharedType.SCAFFOLD_LIST_VIEW_BUILDER;
		String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata, destType.getFullName() + ".java");
		InvocableMemberBodyBuilder bb = new InvocableMemberBodyBuilder();
		bb.reset();
		bb.appendFormalLine("package " + destType.getPath().packageName(projectMetadata) + ";");
		bb.appendFormalLine("import java.util.HashMap;");
		bb.appendFormalLine("import java.util.Map;");
		bb.appendFormalLine("import com.google.gwt.app.util.Renderer;");
		bb.appendFormalLine("import com.google.gwt.valuestore.ui.RecordListView;");
		bb.appendFormalLine("import " + SharedType.APP_LIST_PLACE.getFullyQualifiedTypeName(projectMetadata) + ";");
		bb.appendFormalLine("import " + SharedType.APP_PLACES.getFullyQualifiedTypeName(projectMetadata) + ";");
		bb.appendFormalLine("import " + SharedType.APP_REQUEST_FACTORY.getFullyQualifiedTypeName(projectMetadata) + ";");

		bb.appendFormalLine("public class " + destType.getFullName() + " {");
		bb.indent();
		bb.appendFormalLine("private final " + SharedType.APP_REQUEST_FACTORY.getFullName() + " requests;");
		bb.appendFormalLine("private final Renderer<" + SharedType.APP_LIST_PLACE.getFullName() + "> placeRenderer;");
		bb.appendFormalLine("private final " + SharedType.APP_PLACES.getFullName() + " places;");
		bb.appendFormalLine("private final Map<" + SharedType.APP_LIST_PLACE.getFullName() + ", RecordListView<?>> viewMap = new HashMap<" + SharedType.APP_LIST_PLACE.getFullName() + ", RecordListView<?>>();");

		bb.appendFormalLine("public ScaffoldListViewBuilder(" + SharedType.APP_PLACES.getFullName() + " places, " + SharedType.APP_REQUEST_FACTORY.getFullName() + " requests, Renderer<" + SharedType.APP_LIST_PLACE.getFullName() + "> placeRenderer) {");
		bb.indent();
		bb.appendFormalLine("this.places = places;");
		bb.appendFormalLine("this.requests = requests;");
		bb.appendFormalLine("this.placeRenderer = placeRenderer;");
		bb.indentRemove();
		bb.appendFormalLine("}");

		bb.appendFormalLine("public RecordListView<?> getListView(final " + SharedType.APP_LIST_PLACE.getFullName() + " newPlace) {");
		bb.indent();

		bb.appendFormalLine("if (!viewMap.containsKey(newPlace)) {");
		bb.indent();
		MirrorType locate = MirrorType.RECORD;
		String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
		for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
			String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
			JavaType javaType = new JavaType(locate.getPath().packageName(projectMetadata) + "." + fullPath);
			String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Record" suffix from filename
			JavaType listViewType = new JavaType(MirrorType.LIST_VIEW.getPath().packageName(projectMetadata) + "." + simpleName + MirrorType.LIST_VIEW.getSuffix());
			JavaType findAllRequester = new JavaType(MirrorType.FIND_ALL_REQUESTER.getPath().packageName(projectMetadata) + "." + simpleName + MirrorType.FIND_ALL_REQUESTER.getSuffix());
			bb.appendFormalLine("if (newPlace.getRecord().equals(" + javaType.getFullyQualifiedTypeName() + ".class)) {");
			bb.indent();
			bb.appendFormalLine(listViewType.getSimpleTypeName() + " newView = new " + listViewType.getSimpleTypeName() + "(placeRenderer.render(newPlace), places, requests);");
			bb.appendFormalLine("newView.setDelegate(new " + findAllRequester.getSimpleTypeName() + "(requests, newView));");
			bb.appendFormalLine("viewMap.put(newPlace, newView);");
			bb.indentRemove();
			bb.appendFormalLine("}");
		}
		bb.indentRemove();
		bb.appendFormalLine("}");

		bb.appendFormalLine("return viewMap.get(newPlace);");
		bb.indentRemove();
		bb.appendFormalLine("}");

		bb.indentRemove();
		bb.appendFormalLine("}");
		write(destFile, bb.getOutput(), fileManager);
	}

	private void updateScaffoldDetailsViewBuilder(FileManager fileManager, ProjectMetadata projectMetadata) {
		SharedType destType = SharedType.SCAFFOLD_DETAILS_VIEW_BUILDER;
		String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata, destType.getFullName() + ".java");
		InvocableMemberBodyBuilder bb = new InvocableMemberBodyBuilder();
		bb.reset();
		bb.appendFormalLine("package " + destType.getPath().packageName(projectMetadata) + ";");
		bb.appendFormalLine("import com.google.gwt.valuestore.shared.Record;");

		bb.appendFormalLine("public class " + destType.getFullName() + " {");
		bb.indent();

		bb.appendFormalLine("public static void appendHtmlDescription(final StringBuilder list, final Record entity) {");
		bb.indent();
		MirrorType locate = MirrorType.RECORD;
		String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
		for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
			String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
			JavaType keyType = new JavaType(locate.getPath().packageName(projectMetadata) + "." + fullPath);
			String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Record" suffix from filename
			JavaType listViewType = new JavaType(MirrorType.DETAILS_BUILDER.getPath().packageName(projectMetadata) + "." + simpleName + MirrorType.DETAILS_BUILDER.getSuffix());
			bb.appendFormalLine("if (entity instanceof " + keyType.getFullyQualifiedTypeName() + ") {");
			bb.indent();
			bb.appendFormalLine(listViewType.getSimpleTypeName() + ".append(list, (" + keyType.getFullyQualifiedTypeName() + ") entity);");
			bb.indentRemove();
			bb.appendFormalLine("}");
		}

		bb.indentRemove();
		bb.appendFormalLine("}");

		bb.indentRemove();
		bb.appendFormalLine("}");
		write(destFile, bb.getOutput(), fileManager);
	}

        private void updateListActivitiesMapper(FileManager fileManager, ProjectMetadata projectMetadata) {
		SharedType destType = SharedType.LIST_ACTIVITIES_MAPPER;
		String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata, destType.getFullName() + ".java");
		InvocableMemberBodyBuilder bb = new InvocableMemberBodyBuilder();
		bb.reset();
		bb.appendFormalLine("package " + destType.getPath().packageName(projectMetadata) + ";");
		bb.appendFormalLine("import com.google.gwt.app.place.Activity;");
                bb.appendFormalLine("import com.google.gwt.app.place.ActivityMapper;");
                bb.appendFormalLine("import com.google.gwt.app.place.PlaceController;");
                           
		bb.appendFormalLine("import " + SharedType.APP_LIST_PLACE.getFullyQualifiedTypeName(projectMetadata) + ";");

		bb.appendFormalLine("public class " + destType.getFullName() + " implements ActivityMapper<" + SharedType.APP_LIST_PLACE.getFullName() + "> {");
		bb.indent();
                bb.appendFormalLine("private final "+SharedType.APP_REQUEST_FACTORY.getFullyQualifiedTypeName(projectMetadata)+" requests;");
                bb.appendFormalLine("private final PlaceController<"+SharedType.APP_PLACE.getFullyQualifiedTypeName(projectMetadata)+"> placeController;");
                bb.appendFormalLine("public "+SharedType.LIST_ACTIVITIES_MAPPER.getFullName()+"("+SharedType.APP_REQUEST_FACTORY.getFullyQualifiedTypeName(projectMetadata)+" requests, PlaceController<"+SharedType.APP_PLACE.getFullyQualifiedTypeName(projectMetadata)+"> placeController) {") ;
                bb.indent();
                bb.appendFormalLine("this.requests = requests;");
                bb.appendFormalLine("this.placeController = placeController;");
                bb.indentRemove();
                bb.appendFormalLine("}");
          
		bb.appendFormalLine("public Activity getActivity("+SharedType.APP_LIST_PLACE.getFullyQualifiedTypeName(projectMetadata)+" place) {");
                bb.indent();
		MirrorType locate = MirrorType.RECORD;
		String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
		for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
			String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
			JavaType keyType = new JavaType(locate.getPath().packageName(projectMetadata) + "." + fullPath);
			String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Record" suffix from filename
			bb.appendFormalLine("if (place.getType().equals(" + keyType.getFullyQualifiedTypeName() + ".class)) {");
			bb.indent();
			bb.appendFormalLine("return new " + MirrorType.LIST_ACTIVITY.getPath().packageName(projectMetadata) + "." + simpleName + MirrorType.LIST_ACTIVITY.getSuffix()+ "(requests, placeController);");
			bb.indentRemove();
			bb.appendFormalLine("}");
		}
		bb.appendFormalLine("throw new RuntimeException(\"Unable to locate an activity for \" + place);");

		bb.indentRemove();
		bb.appendFormalLine("}");

		bb.indentRemove();
		bb.appendFormalLine("}");
		write(destFile, bb.getOutput(), fileManager);
	}
  
        private void updateScaffoldActivities(FileManager fileManager, ProjectMetadata projectMetadata) {
		SharedType destType = SharedType.SCAFFOLD_ACTIVITIES;
		String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata, destType.getFullName() + ".java");
		InvocableMemberBodyBuilder bb = new InvocableMemberBodyBuilder();
		bb.reset();
		bb.appendFormalLine("package " + destType.getPath().packageName(projectMetadata) + ";");
		bb.appendFormalLine("import com.google.gwt.app.place.Activity;");
                bb.appendFormalLine("import com.google.gwt.app.place.ActivityMapper;");
                bb.appendFormalLine("import com.google.gwt.app.place.PlaceController;");
                           
                bb.appendFormalLine("import " + SharedType.APP_PLACE.getFullyQualifiedTypeName(projectMetadata) + ";");
		bb.appendFormalLine("import " + SharedType.APP_LIST_PLACE.getFullyQualifiedTypeName(projectMetadata) + ";");
                bb.appendFormalLine("import " + SharedType.APP_PLACE_FILTER.getFullyQualifiedTypeName(projectMetadata) + ";");
                bb.appendFormalLine("import " + SharedType.APP_REQUEST_FACTORY.getFullyQualifiedTypeName(projectMetadata) + ";");
                bb.appendFormalLine("import " + SharedType.LIST_ACTIVITIES_MAPPER.getFullyQualifiedTypeName(projectMetadata) + ";");
                
                MirrorType locate = MirrorType.RECORD;
		String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
                List<String> entityNames = new ArrayList<String>();
                for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
			String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
			JavaType keyType = new JavaType(locate.getPath().packageName(projectMetadata) + "." + fullPath);
			String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Record" suffix from filename
                        bb.appendFormalLine("import "+MirrorType.ACTIVITIES_MAPPER.getPath().packageName(projectMetadata)+"."+simpleName+MirrorType.ACTIVITIES_MAPPER.getSuffix()+";");
                        bb.appendFormalLine("import "+MirrorType.SCAFFOLD_PLACE.getPath().packageName(projectMetadata)+"."+simpleName+MirrorType.SCAFFOLD_PLACE.getSuffix()+";");
                        entityNames.add(simpleName);
                }
          
		bb.appendFormalLine("public class " + destType.getFullName() + " implements ActivityMapper<" + SharedType.APP_PLACE.getFullName() + "> {");
		bb.indent();
                bb.appendFormalLine("private final ActivityMapper<"+SharedType.APP_LIST_PLACE.getFullyQualifiedTypeName(projectMetadata)+"> listActivitiesBuilder;");
                for(String entityName : entityNames) {
                  bb.appendFormalLine("private final ActivityMapper<"+entityName+MirrorType.SCAFFOLD_PLACE.getSuffix()+"> "+StringUtils.uncapitalize(entityName)+"ActivitiesBuilder;");
                }
          
                bb.appendFormalLine("public "+SharedType.SCAFFOLD_ACTIVITIES.getFullName()+"("+SharedType.APP_REQUEST_FACTORY.getFullyQualifiedTypeName(projectMetadata)+" requests, PlaceController<"+SharedType.APP_PLACE.getFullyQualifiedTypeName(projectMetadata)+"> placeController) {") ;
                bb.indent();
                bb.appendFormalLine("this.listActivitiesBuilder = new "+SharedType.LIST_ACTIVITIES_MAPPER.getFullName()+"(requests, placeController);");
                for(String entityName : entityNames) {
                  bb.appendFormalLine("this."+StringUtils.uncapitalize(entityName)+"ActivitiesBuilder = new "+entityName+MirrorType.ACTIVITIES_MAPPER.getSuffix()+"(requests, placeController);");
                }
                bb.indentRemove();
                bb.appendFormalLine("}");
          
		bb.appendFormalLine("public Activity getActivity("+SharedType.APP_PLACE.getFullyQualifiedTypeName(projectMetadata)+" place) {");
                bb.indent();
		
                bb.appendFormalLine("return place.acceptFilter(new "+SharedType.APP_PLACE_FILTER.getFullName()+"<Activity>() {");
                bb.indent();
                bb.appendFormalLine("public Activity filter("+SharedType.APP_LIST_PLACE.getFullName()+" place) {");
                bb.indent();
                bb.appendFormalLine("return listActivitiesBuilder.getActivity(place);");
                bb.appendFormalLine("}");
                bb.indentRemove();
		for (String entityName : entityNames) {
		    bb.appendFormalLine("public Activity filter("+entityName+MirrorType.SCAFFOLD_PLACE.getSuffix()+" place) {");
                    bb.indent();
                    bb.appendFormalLine("return "+StringUtils.uncapitalize(entityName)+"ActivitiesBuilder.getActivity(place);");
                    bb.indentRemove();	
                    bb.appendFormalLine("}");
		}
                bb.indentRemove();
                bb.appendFormalLine("});");
		bb.indentRemove();
		bb.appendFormalLine("}");

		bb.indentRemove();
		bb.appendFormalLine("}");
		write(destFile, bb.getOutput(), fileManager);
	}
  
        private void updatePlaceFilter(FileManager fileManager, ProjectMetadata projectMetadata) {
		SharedType destType = SharedType.APP_PLACE_FILTER;
		String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata, destType.getFullName() + ".java");
		InvocableMemberBodyBuilder bb = new InvocableMemberBodyBuilder();
		bb.reset();
		bb.appendFormalLine("package " + destType.getPath().packageName(projectMetadata) + ";");

		bb.appendFormalLine("public interface " + destType.getFullName() + "<T> {");
		bb.indent();

		bb.appendFormalLine("com.google.gwt.app.place.Activity filter(ApplicationListPlace object);");
           
		MirrorType locate = MirrorType.RECORD;
		String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
		for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
			String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
			JavaType keyType = new JavaType(locate.getPath().packageName(projectMetadata) + "." + fullPath);
			String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Record" suffix from filename
                        bb.appendFormalLine("com.google.gwt.app.place.Activity filter("+simpleName+MirrorType.SCAFFOLD_PLACE.getSuffix()+" place);");
		}
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
			JavaType keyType = new JavaType(locate.getPath().packageName(projectMetadata) + "." + fullPath);
			String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Record" suffix from filename
                        bb.appendFormalLine("void process("+simpleName+MirrorType.SCAFFOLD_PLACE.getSuffix()+" place);");
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
		bb.appendFormalLine("import com.google.gwt.app.util.Renderer;");
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
}
