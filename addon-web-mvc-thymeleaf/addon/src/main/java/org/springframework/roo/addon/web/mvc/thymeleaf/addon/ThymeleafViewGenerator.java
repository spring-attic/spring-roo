package org.springframework.roo.addon.web.mvc.thymeleaf.addon;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.springframework.roo.addon.web.mvc.views.ViewContext;
import org.springframework.roo.addon.web.mvc.views.components.DetailEntityItem;
import org.springframework.roo.addon.web.mvc.views.components.EntityItem;
import org.springframework.roo.addon.web.mvc.views.components.FieldItem;
import org.springframework.roo.addon.web.mvc.views.components.MenuEntry;
import org.springframework.roo.addon.web.mvc.views.template.engines.AbstractFreeMarkerViewGenerationService;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.settings.project.ProjectSettingsService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * This class implements AbstractFreeMarkerViewGenerationService.
 *
 * As a result, view generation services could delegate on this one to generate
 * views with Thymeleaf components and structure.
 *
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class ThymeleafViewGenerator extends AbstractFreeMarkerViewGenerationService<Document> {

  private static final String SPRING_ROO_THYMELEAF_TEMPLATES_LOCATION =
      "spring.roo.thymeleaf.templates-location";

  @Reference
  PathResolver pathResolver;

  @Reference
  ProjectSettingsService projectSettings;

  @Override
  public String getName() {
    return "THYMELEAF";
  }

  @Override
  public String getViewsFolder(String moduleName) {
    return pathResolver.getIdentifier(moduleName, Path.SRC_MAIN_RESOURCES, "/templates");
  }

  @Override
  public String getViewsExtension() {
    return ".html";
  }

  @Override
  public String getLayoutsFolder(String moduleName) {
    return getViewsFolder(moduleName).concat("/").concat("layouts");
  }

  @Override
  public String getFragmentsFolder(String moduleName) {
    return getViewsFolder(moduleName).concat("/").concat("fragments");
  }

  @Override
  public Class<?> getResourceLoaderClass() {
    return ThymeleafViewGenerator.class;
  }

  @Override
  public Document parse(String content) {
    return Jsoup.parse(content, "", Parser.xmlParser());
  }

  /**
   * Get ids and codes of old document that have 'user-managed' value in data-z attribute.
   *
   * @param loadExistingDoc
   * @return
   */
  private Map<String, String> mergeStructure(Document loadExistingDoc) {
    Map<String, String> structureUserManaged = new HashMap<String, String>();
    Elements elementsUserManaged =
        loadExistingDoc.getElementsByAttributeValue("data-z", "user-managed");
    for (Element elementUserManaged : elementsUserManaged) {
      String id = elementUserManaged.attr("id");
      if (id != null) {
        String code = elementsUserManaged.outerHtml();
        structureUserManaged.put(id, code);
      }
    }
    return structureUserManaged;
  }

  @Override
  public Document merge(String templateName, Document loadExistingDoc, ViewContext ctx,
      List<FieldItem> fields) {
    for (FieldItem field : fields) {
      // Get field code if data-z attribute value is equals to
      // user-managed
      Element elementField = loadExistingDoc.getElementById(field.getFieldId());
      if (elementField != null && elementField.hasAttr("data-z")
          && elementField.attr("data-z").equals("user-managed")) {
        field.setUserManaged(true);
        field.setCodeManaged(elementField.outerHtml());
      }
    }
    ctx.addExtraParameter("userManagedComponents", mergeStructure(loadExistingDoc));
    ctx.addExtraParameter("fields", fields);
    Document newDoc = process(templateName, ctx);
    return newDoc;
  }

  @Override
  public Document mergeListView(String templateName, Document loadExistingDoc, ViewContext ctx,
      EntityItem entity, List<FieldItem> fields, List<DetailEntityItem> details) {

    // Get field code if data-z attribute value is equals to user-managed
    Element elementField =
        loadExistingDoc.getElementById(entity.getEntityItemId().concat("-table"));
    if (elementField != null && elementField.hasAttr("data-z")
        && elementField.attr("data-z").equals("user-managed")) {
      entity.setUserManaged(true);
      entity.setCodeManaged(elementField.outerHtml());
    }

    // Get javascript associated to field if data-z attribute value is
    // equals to user-managed
    Map<String, String> javascriptCode = new HashMap<String, String>();
    Element elementJavascriptField =
        loadExistingDoc.getElementById(entity.getEntityItemId().concat("-table-javascript"));
    if (elementJavascriptField != null && elementJavascriptField.hasAttr("data-z")
        && elementJavascriptField.attr("data-z").equals("user-managed")) {
      javascriptCode.put(entity.getEntityItemId().concat("-table-javascript"),
          elementJavascriptField.outerHtml());
    }

    // Check if has details because in this case it has a special javascript
    if (!details.isEmpty()) {
      Element elementJavascriptDetailField =
          loadExistingDoc.getElementById(entity.getEntityItemId().concat(
              "-table-javascript-firstdetail"));
      if (elementJavascriptDetailField != null && elementJavascriptDetailField.hasAttr("data-z")
          && elementJavascriptDetailField.attr("data-z").equals("user-managed")) {
        javascriptCode.put(entity.getEntityItemId().concat("-table-javascript-firstdetail"),
            elementJavascriptDetailField.outerHtml());
      }
    }

    entity.setJavascriptCode(javascriptCode);

    for (DetailEntityItem detail : details) {

      // Get detail code if data-z attribute value is equals to
      // user-managed
      Element elementDetail =
          loadExistingDoc.getElementById(detail.getEntityItemId().concat("-table"));
      if (elementDetail != null && elementDetail.hasAttr("data-z")
          && elementDetail.attr("data-z").equals("user-managed")) {
        detail.setUserManaged(true);
        detail.setCodeManaged(elementDetail.outerHtml());
      }

      // Get javascript associated to field if data-z attribute value is
      // equals to user-managed
      Map<String, String> javascriptDetailCode = new HashMap<String, String>();
      Element elementJavascriptDetailField =
          loadExistingDoc.getElementById(detail.getEntityItemId().concat("-table-javascript"));
      if (elementJavascriptDetailField != null && elementJavascriptDetailField.hasAttr("data-z")
          && elementJavascriptDetailField.attr("data-z").equals("user-managed")) {
        javascriptDetailCode.put(detail.getEntityItemId().concat("-table-javascript"),
            elementJavascriptDetailField.outerHtml());
      }

      detail.setJavascriptCode(javascriptDetailCode);

      Element elementTabCodeDetailField =
          loadExistingDoc.getElementById(detail.getEntityItemId().concat("-table-tab"));
      if (elementTabCodeDetailField != null && elementTabCodeDetailField.hasAttr("data-z")
          && elementTabCodeDetailField.attr("data-z").equals("user-managed")) {
        detail.setTabLinkCode(elementTabCodeDetailField.outerHtml());
      }

    }
    ctx.addExtraParameter("userManagedComponents", mergeStructure(loadExistingDoc));
    ctx.addExtraParameter("entity", entity);
    ctx.addExtraParameter("fields", fields);
    ctx.addExtraParameter("details", details);
    Document newDoc = process(templateName, ctx);
    return newDoc;
  }

  @Override
  public Document mergeMenu(String templateName, Document loadExistingDoc, ViewContext ctx,
      List<MenuEntry> menuEntries) {

    for (MenuEntry menuEntry : menuEntries) {

      // Get field code if data-z attribute value is equals to
      // user-managed
      Element elementMenu = loadExistingDoc.getElementById(menuEntry.getId().concat("-entry"));
      if (elementMenu != null && elementMenu.hasAttr("data-z")
          && elementMenu.attr("data-z").equals("user-managed")) {
        menuEntry.setUserManaged(true);
        menuEntry.setCodeManaged(elementMenu.outerHtml());
      }
    }
    ctx.addExtraParameter("userManagedComponents", mergeStructure(loadExistingDoc));
    ctx.addExtraParameter("menuEntries", menuEntries);
    Document newDoc = process(templateName, ctx);
    return newDoc;
  }

  @Override
  public Document merge(String templateName, Document loadExistingDoc, ViewContext ctx) {
    // TODO: TO BE FIXED WITH NEW COMMAND 'web mvc view update'
    ctx.addExtraParameter("userManagedComponents", mergeStructure(loadExistingDoc));
    Document newDoc = process(templateName, ctx);
    return newDoc;
  }



  @Override
  public String getTemplatesLocation() {
    String thymeleafTemplatesLocation =
        projectSettings.getProperty(SPRING_ROO_THYMELEAF_TEMPLATES_LOCATION);
    if (thymeleafTemplatesLocation != null) {
      return pathResolver.getIdentifier("", Path.ROOT_ROO_CONFIG, thymeleafTemplatesLocation);
    }
    return pathResolver.getIdentifier("", Path.ROOT_ROO_CONFIG, "templates/thymeleaf/default");
  }

  @Override
  public void writeDoc(Document document, String viewPath) {
    // Write doc on disk
    if (document != null && StringUtils.isNotBlank(viewPath)) {
      getFileManager().createOrUpdateTextFileIfRequired(viewPath, document.html(), false);
    }
  }

  @Override
  public void installTemplates() {
    // Getting destination where FreeMarker templates should be installed.
    // This will allow developers to customize his own templates.
    copyDirectoryContents("templates/*.ftl", getTemplatesLocation(), true);
    copyDirectoryContents("templates/fragments/*.ftl",
        getTemplatesLocation().concat("/").concat("fragments"), true);
    copyDirectoryContents("templates/layouts/*.ftl",
        getTemplatesLocation().concat("/").concat("layouts"), true);
    copyDirectoryContents("templates/fields/*.ftl",
        getTemplatesLocation().concat("/").concat("fields"), true);
  }

}
