package org.springframework.roo.addon.web.mvc.thymeleaf.addon;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.springframework.roo.addon.web.mvc.views.template.engines.AbstractFreeMarkerViewGenerationService;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;

/**
 * 
 * This class implements AbstractFreeMarkerViewGenerationService. 
 * 
 * As a result, view generation services could delegate on this one to generate views
 *  with Thymeleaf components and structure.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class ThymeleafViewGenerator extends AbstractFreeMarkerViewGenerationService<Document> {

  @Reference
  PathResolver pathResolver;

  @Override
  public String getName() {
    return "THYMELEAF";
  }

  @Override
  public String getViewsFolder() {
    return pathResolver.getFocusedIdentifier(Path.SRC_MAIN_RESOURCES, "/templates");
  }

  @Override
  public String getViewsExtension() {
    return ".html";
  }

  @Override
  public String getLayoutsFolder() {
    return getViewsFolder().concat("/").concat("layouts");
  }

  @Override
  public String getFragmentsFolder() {
    return getViewsFolder().concat("/").concat("fragments");
  }


  @Override
  public Class<?> getResourceLoaderClass() {
    return ThymeleafViewGenerator.class;
  }

  @Override
  public Document parse(String content) {
    return Jsoup.parse(content, "", Parser.xmlParser());
  }

  @Override
  public Document merge(Document existingDoc, Document newDoc) {
    // TODO: Merge existing document and new document. Now, return always
    // new document.
    return newDoc;
  }

  @Override
  public String getTemplatesLocation() {
    return pathResolver.getFocusedIdentifier(Path.ROOT_ROO_CONFIG, "templates/thymeleaf");
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
  }

}
