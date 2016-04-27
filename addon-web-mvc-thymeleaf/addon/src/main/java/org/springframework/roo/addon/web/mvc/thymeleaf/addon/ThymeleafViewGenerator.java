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
  protected Class<?> getResourceLoaderClass() {
    return ThymeleafViewGenerator.class;
  }

  @Override
  protected Document parse(String content) {
    return Jsoup.parse(content, "", Parser.xmlParser());
  }

  @Override
  protected Document merge(Document existingDoc, Document newDoc) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected String getTemplatesLocation() {
    return pathResolver.getRoot().concat(".roo/templates/thymeleaf");
  }

  @Override
  protected void writeDoc(Document document, String viewPath) {
    // Write doc on disk
    if (document != null && StringUtils.isNotBlank(viewPath)) {
      getFileManager().createOrUpdateTextFileIfRequired(viewPath, document.html(), false);
    }
  }

}
