package org.springframework.roo.addon.web.mvc.thymeleaf.addon;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
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

  @Override
  public Document merge(Document existingDoc, Document newDoc, List<String> requiredIds) {
    List<Element> elementsNotFound = new ArrayList<Element>();
    Element existingParent = null;
    String existingSiblingId = null;

    // Clean non user-managed elements
    for (Element existingElement : existingDoc.select("[data-z]")) {
      if (existingElement.hasAttr("id") && !existingElement.attr("data-z").equals("user-managed")) {
        final Element newElement = newDoc.select("#" + existingElement.attr("id")).first();
        if (newElement != null) {
          existingElement.replaceWith(newElement.clone());
        }
      }
    }

    if (requiredIds == null) {
      return existingDoc;
    }

    // Include required elements
    for (String id : requiredIds) {

      // Required element does not exist
      final Element newElement = newDoc.select("#" + id).first();
      if (newElement == null) {
        continue;
      }

      // Check if required element exists in the existing doc
      Element existingElement = existingDoc.select("#" + id).first();
      if (existingElement == null) {

        // Required element not found
        elementsNotFound.add(newElement);
      } else {

        // Check if element is user-managed
        if (!existingElement.hasAttr("data-z")
            || !existingElement.attr("data-z").equals("user-managed")) {
          existingElement.replaceWith(newElement.clone());
        }
        existingSiblingId = id;
      }
    }

    // Find a parent element to include non-found elements as children
    if (existingSiblingId == null) {
      existingParent = existingDoc.select("#containerFields").first();
    }

    // Include element not found
    for (Element elementNotFound : elementsNotFound) {

      if (existingSiblingId != null) {
        // Add sibling
        existingDoc.select("#" + existingSiblingId).first().after(elementNotFound.clone());
        continue;
      }


      if (existingParent == null) {
        // Find some predecessor element with id
        Element parent = elementsNotFound.get(0).parent();
        while (!parent.tag().getName().equals("body") && parent != null) {
          if (!parent.hasAttr("id")) {
            parent = parent.parent();
          } else {
            existingParent = existingDoc.select("#" + parent.attr("id")).first();
            if (existingParent != null) {
              break;
            }
            parent = parent.parent();
          }
        }
      }

      if (existingParent == null) {
        existingParent = existingDoc.select("fieldset").first();
      }

      if (existingParent == null) {
        existingParent = existingDoc.select("div.content").first();
      }

      if (existingParent == null) {
        existingParent = existingDoc.select("section[data-layout-fragment=content]").first();
      }

      if (existingParent == null) {
        existingParent = existingDoc.select("body").first();
      }

      // Add child
      existingParent.prependChild(elementNotFound);
    }

    return existingDoc;
  }

  @Override
  public String getTemplatesLocation() {
    return pathResolver.getIdentifier("", Path.ROOT_ROO_CONFIG, "templates/thymeleaf");
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
