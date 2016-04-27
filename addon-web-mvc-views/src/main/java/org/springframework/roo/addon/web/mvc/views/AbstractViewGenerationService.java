package org.springframework.roo.addon.web.mvc.views;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.felix.scr.annotations.Component;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * 
 * This abstract class implements MVCViewGenerationService interface
 * that provides all necessary elements to generate views inside project.
 * 
 * @param <DOC>
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component(componentAbstract = true)
public abstract class AbstractViewGenerationService<DOC> implements MVCViewGenerationService {

  // Max fields that will be included on generated view
  private static final int MAX_FIELDS_TO_ADD = 5;

  private static Logger LOGGER = HandlerUtils.getLogger(AbstractViewGenerationService.class);

  private FileManager fileManager;

  // ------------ OSGi component attributes ----------------
  protected BundleContext context;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  protected abstract DOC process(String templateName, List<FieldMetadata> fields, ViewContext ctx);

  protected abstract DOC parse(String content);

  protected abstract DOC merge(DOC existingDoc, DOC newDoc);

  protected abstract String getTemplatesLocation();

  protected abstract void writeDoc(DOC document, String viewPath);

  @Override
  public void addListView(MemberDetails entityDetails, ViewContext ctx) {

    // Getting entity fields that should be included on view
    List<FieldMetadata> fields = getFieldViewItems(entityDetails, true);

    // Process elements to generate 
    DOC newDoc = process("list", fields, ctx);

    // Getting new viewName
    String viewName =
        getViewsFolder().concat(ctx.getControllerPath()).concat("/").concat("/list")
            .concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge(loadExistingDoc(viewName), newDoc);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addShowView(MemberDetails entityDetails, ViewContext ctx) {

    // Getting entity fields that should be included on view
    List<FieldMetadata> fields = getFieldViewItems(entityDetails, false);

    // Process elements to generate 
    DOC newDoc = process("show", fields, ctx);

    // Getting new viewName
    String viewName =
        getViewsFolder().concat(ctx.getControllerPath()).concat("/").concat("/show")
            .concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge(loadExistingDoc(viewName), newDoc);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addCreateView(MemberDetails entityDetails, ViewContext ctx) {

    // Getting entity fields that should be included on view
    List<FieldMetadata> fields = getFieldViewItems(entityDetails, false);

    // Process elements to generate 
    DOC newDoc = process("create", fields, ctx);

    // Getting new viewName
    String viewName =
        getViewsFolder().concat(ctx.getControllerPath()).concat("/").concat("/create")
            .concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge(loadExistingDoc(viewName), newDoc);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addUpdateView(MemberDetails entityDetails, ViewContext ctx) {

    // Getting entity fields that should be included on view
    List<FieldMetadata> fields = getFieldViewItems(entityDetails, false);

    // Process elements to generate 
    DOC newDoc = process("edit", fields, ctx);

    // Getting new viewName
    String viewName =
        getViewsFolder().concat(ctx.getControllerPath()).concat("/").concat("/edit")
            .concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge(loadExistingDoc(viewName), newDoc);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addFinderView(MemberDetails entity, String finderName, ViewContext ctx) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addIndexView(ViewContext ctx) {

    // Process elements to generate 
    DOC newDoc = process("index", null, ctx);

    // Getting new viewName
    String viewName = getViewsFolder().concat("/index").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge(loadExistingDoc(viewName), newDoc);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addErrorView(ViewContext ctx) {

    // Process elements to generate 
    DOC newDoc = process("error", null, ctx);

    // Getting new viewName
    String viewName = getViewsFolder().concat("/error").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge(loadExistingDoc(viewName), newDoc);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addDefaultLayout(ViewContext ctx) {

    // Process elements to generate 
    DOC newDoc = process("layouts/default-layout", null, ctx);

    // Getting new viewName
    String viewName = getLayoutsFolder().concat("/default-layout").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge(loadExistingDoc(viewName), newDoc);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addFooter(ViewContext ctx) {
    // Process elements to generate 
    DOC newDoc = process("fragments/footer", null, ctx);

    // Getting new viewName
    String viewName = getFragmentsFolder().concat("/footer").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge(loadExistingDoc(viewName), newDoc);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addHeader(ViewContext ctx) {
    // Process elements to generate 
    DOC newDoc = process("fragments/header", null, ctx);

    // Getting new viewName
    String viewName = getFragmentsFolder().concat("/header").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge(loadExistingDoc(viewName), newDoc);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addMenu(ViewContext ctx) {
    // Process elements to generate 
    DOC newDoc = process("fragments/menu", null, ctx);

    // Getting new viewName
    String viewName = getFragmentsFolder().concat("/menu").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge(loadExistingDoc(viewName), newDoc);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addSession(ViewContext ctx) {
    // Process elements to generate 
    DOC newDoc = process("fragments/session", null, ctx);

    // Getting new viewName
    String viewName = getFragmentsFolder().concat("/session").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge(loadExistingDoc(viewName), newDoc);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public String getLayoutsFolder() {
    // Default implementation
    return getViewsFolder();
  }

  @Override
  public String getFragmentsFolder() {
    // Default implementation
    return getViewsFolder();
  }

  /**
   * This method obtains all necessary information about fields from entity
   * and returns a List of FieldMetadata.
   * 
   * If provided entity has more than 5 fields, only the first 5 ones will be
   * included on generated view.
   *  
   * @param entityDetails
   * 
   * @return List that contains FieldMetadata that will be added to the view.
   */
  protected List<FieldMetadata> getFieldViewItems(MemberDetails entityDetails,
      boolean checkMaxFields) {
    // Getting entity fields
    List<FieldMetadata> entityFields = entityDetails.getFields();
    int addedFields = 0;

    // Get the MAX_FIELDS_TO_ADD
    List<FieldMetadata> fieldViewItems = new ArrayList<FieldMetadata>();
    for (FieldMetadata entityField : entityFields) {
      fieldViewItems.add(entityField);
      addedFields++;
      if (addedFields == MAX_FIELDS_TO_ADD && checkMaxFields) {
        break;
      }
    }
    return fieldViewItems.isEmpty() ? null : fieldViewItems;
  }

  /**
   * This method load the provided file and get its content in String format.
   * 
   * After that, uses parse method to generate a valid DOC object.
   * 
   * @param path
   * @return
   */
  protected DOC loadExistingDoc(String path) {
    String content = "";
    try {
      // Load file and get STRING content
      content = FileUtils.readFileToString(new File(path));

    } catch (IOException e) {
      throw new RuntimeException(String.format("ERROR: Error trying to load existing doc %s", path));
    }

    // Parse String content to obtain the same type of object
    return parse(content);
  }

  /**
   * This method check if the provided viewPath file exists
   * 
   * @param viewName
   * @return true if exists the provided view path
   */
  protected boolean existsFile(String viewPath) {
    return getFileManager().exists(viewPath);
  }

  public FileManager getFileManager() {
    if (fileManager == null) {
      // Get all Services implement FileManager interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(FileManager.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          fileManager = (FileManager) this.context.getService(ref);
          return fileManager;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load FileManager on AbstractViewGenerationService.");
        return null;
      }
    } else {
      return fileManager;
    }
  }

}
