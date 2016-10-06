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
import org.springframework.roo.addon.web.mvc.views.template.engines.AbstractFreeMarkerViewGenerationService;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
  public Document merge(Document existingDoc, Document newDoc, String idContainerElements,
      List<String> requiredIds) {

    // Document with the calculated values
    Document mergedDoc;

    // if <html data-z="user-managed"/> keep old code
    Elements existingDocTagHtml = existingDoc.getElementsByTag("html");
    if (existingDocTagHtml.get(0).hasAttr("data-z")
        && existingDocTagHtml.get(0).attr("data-z").equals("user-managed")) {
      mergedDoc = existingDoc.clone();

      // if <head data-z="user-managed"> keep old code
      Elements existingDocTagHead = existingDoc.getElementsByTag("head");
      if (!existingDocTagHead.get(0).hasAttr("data-z")
          || (existingDocTagHead.get(0).hasAttr("data-z") && !existingDocTagHead.get(0)
              .attr("data-z").equals("user-managed"))) {

        // Get body element
        Elements mergedDocTagHead = mergedDoc.getElementsByTag("head");
        Elements newDocTagHead = newDoc.getElementsByTag("head");

        // Replace by new value
        mergedDocTagHead.get(0).replaceWith(newDocTagHead.get(0).clone());
      }

      // if <body data-z="user-managed"> keep old code
      Elements existingDocTagBody = existingDoc.getElementsByTag("body");
      if (existingDocTagBody.get(0).hasAttr("data-z")
          && existingDocTagBody.get(0).attr("data-z").equals("user-managed")) {

        // if <footer data-z="user-managed"> keep old code
        Elements existingDocTagFooter = existingDoc.getElementsByTag("footer");
        if (!existingDocTagFooter.isEmpty()) {
          if (!existingDocTagFooter.get(0).hasAttr("data-z")
              || (existingDocTagFooter.get(0).hasAttr("data-z") && !existingDocTagFooter.get(0)
                  .attr("data-z").equals("user-managed"))) {

            // Get footer element
            Elements mergedDocTagFooter = mergedDoc.getElementsByTag("footer");
            Elements newDocTagFooter = newDoc.getElementsByTag("footer");

            // Replace by new value
            mergedDocTagFooter.get(0).replaceWith(newDocTagFooter.get(0).clone());
          }
        }

        // if <section data-z="user-managed"> keep old code
        Elements lstExistingDocTagSection = existingDoc.getElementsByTag("section");
        Element existingDocTagSection = null;
        for (Element elementTagSection : lstExistingDocTagSection) {
          if (elementTagSection.hasAttr("data-layout-fragment")
              && elementTagSection.attr("data-layout-fragment").equals("content")) {
            existingDocTagSection = elementTagSection;
            break;
          }
        }

        if (existingDocTagSection != null) {
          if (!existingDocTagSection.hasAttr("data-z")
              || (existingDocTagSection.hasAttr("data-z") && !existingDocTagSection.attr("data-z")
                  .equals("user-managed"))) {

            // Get section of new doc
            Elements lstNewDocTagSection = newDoc.getElementsByTag("section");
            Element newDocTagSection = null;
            for (Element elementTagSection : lstNewDocTagSection) {
              if (elementTagSection.hasAttr("data-layout-fragment")
                  && elementTagSection.attr("data-layout-fragment").equals("content")) {
                newDocTagSection = elementTagSection;
                break;
              }
            }

            // Get section of merged doc
            Elements lstMergedDocTagSection = mergedDoc.getElementsByTag("section");
            Element mergedDocTagSection = null;
            for (Element elementTagSection : lstMergedDocTagSection) {
              if (elementTagSection.hasAttr("data-layout-fragment")
                  && elementTagSection.attr("data-layout-fragment").equals("content")) {
                mergedDocTagSection = elementTagSection;
                break;
              }
            }

            // Replace
            if (mergedDocTagSection != null && newDocTagSection != null) {
              mergedDocTagSection.replaceWith(newDocTagSection);
            }
          }
        }

      } else {

        // Get body element
        Elements mergedDocTagBody = mergedDoc.getElementsByTag("body");
        Elements newDocTagBody = newDoc.getElementsByTag("body");

        // Replace by new value
        mergedDocTagBody.get(0).replaceWith(newDocTagBody.get(0).clone());

      }

    } else {
      mergedDoc = newDoc.clone();
    }

    if (requiredIds != null) {
      // Get the container of the elements to replace them
      Element existsDocContainerFields = existingDoc.getElementById(idContainerElements);
      Element newDocContainerFields = newDoc.getElementById(idContainerElements);

      if (existsDocContainerFields != null && newDocContainerFields != null
          && mergedDoc.getElementById(idContainerElements) != null) {

        // Put back the element 'containerFields'
        mergedDoc.getElementById(idContainerElements).replaceWith(existsDocContainerFields.clone());

        // Get the new elements to insert or replace those necessary
        Element mergedDocContainerFields = mergedDoc.getElementById(idContainerElements);

        // Elements to add before the last element 'form-group' of containerFields
        List<Element> listElementsToAdd = new ArrayList<Element>();

        Iterator<String> iterRequiredFields = requiredIds.iterator();
        while (iterRequiredFields.hasNext()) {
          String idField = iterRequiredFields.next();
          Element mergedField = mergedDocContainerFields.getElementById(idField);

          // If doesn't exist, put in the list to add the element to merged page
          if (mergedField == null) {
            listElementsToAdd.add(newDocContainerFields.getElementById(idField));
          } else {

            // If exists, check data-z attribute
            if (!mergedField.hasAttr("data-z")
                || (mergedField.hasAttr("data-z") && !mergedField.attr("data-z").equals(
                    "user-managed"))) {

              // Replace the old value by the calculated
              Element fieldToCopy = newDocContainerFields.getElementById(idField).clone();
              mergedField.replaceWith(fieldToCopy);
            }
          }
        }

        // Add new elements
        if (!listElementsToAdd.isEmpty()) {

          // Try to insert the elements after the last 'form-group' element. Normally form buttons
          Elements elementsByClassFormGroup =
              mergedDoc.getElementById(idContainerElements).getElementsByClass("form-group");
          if (elementsByClassFormGroup.isEmpty()) {
            // If doesn't exist, append elements to the end
            for (Element elementToAdd : listElementsToAdd) {
              mergedDoc.getElementById(idContainerElements).appendChild(elementToAdd.clone());
            }
          } else {

            // Create before form button group and his comment
            Element lastElement = elementsByClassFormGroup.last().previousElementSibling();
            for (Element elementToAdd : listElementsToAdd) {
              lastElement.before(elementToAdd.clone());
            }
          }
        }

        // Delete old elements without user-managed value in data-z attribute
        // and not in requiredIds list
        Elements allElementsWithClassFormGroup =
            mergedDoc.getElementById(idContainerElements).getAllElements();

        for (Element elementClassFormGroup : allElementsWithClassFormGroup) {

          // Check data-z attribute
          if ((elementClassFormGroup.hasAttr("data-z") && !elementClassFormGroup.attr("data-z")
              .equals("user-managed"))) {
            String idElement = elementClassFormGroup.attr("id");
            if (idElement != null && !requiredIds.contains(idElement)) {
              elementClassFormGroup.remove();
            }
          }
        }
      }
    }

    return mergedDoc;
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
