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
import org.springframework.roo.settings.project.ProjectSettingsService;

import java.util.ArrayList;
import java.util.List;

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

  @Override
  public Document merge(Document existingDoc, Document newDoc, String idContainerElements,
      List<String> requiredIds) {

    // Document with the calculated values
    Document mergedDoc = newDoc.clone();

    // if <html data-z="user-managed"/> keep old code
    Elements existingDocTagHtml = existingDoc.getElementsByTag("html");
    if (!existingDocTagHtml.isEmpty() && existingDocTagHtml.get(0).hasAttr("data-z")
        && existingDocTagHtml.get(0).attr("data-z").equals("user-managed")) {
      mergedDoc = existingDoc.clone();
    } else {

      // if <head data-z="user-managed"> keep old code
      Elements existingDocTagHead = existingDoc.getElementsByTag("head");
      if (!existingDocTagHead.isEmpty() && existingDocTagHead.get(0).hasAttr("data-z")
          && existingDocTagHead.get(0).attr("data-z").equals("user-managed")) {
        // Get head element
        Elements mergedDocTagHead = mergedDoc.getElementsByTag("head");

        // Replace by new value
        mergedDocTagHead.get(0).replaceWith(existingDocTagHead.get(0).clone());
      }

      // if <body data-z="user-managed"> keep old code
      Elements existingDocTagBody = existingDoc.getElementsByTag("body");
      if (!existingDocTagBody.isEmpty() && existingDocTagBody.get(0).hasAttr("data-z")
          && existingDocTagBody.get(0).attr("data-z").equals("user-managed")) {
        // Get body element
        Elements mergedDocTagBody = mergedDoc.getElementsByTag("body");

        // Replace by new value
        mergedDocTagBody.get(0).replaceWith(existingDocTagBody.get(0).clone());
      } else {
        // if <footer data-z="user-managed"> keep old code
        Elements existingDocTagFooter = existingDoc.getElementsByTag("footer");
        if (!existingDocTagFooter.isEmpty() && existingDocTagFooter.get(0).hasAttr("data-z")
            && existingDocTagFooter.get(0).attr("data-z").equals("user-managed")) {
          // Get footer element
          Elements mergedDocTagFooter = mergedDoc.getElementsByTag("footer");

          // Replace by new value
          mergedDocTagFooter.get(0).replaceWith(existingDocTagFooter.get(0).clone());
        }

        // if <section data-layout-fragment="content"
        // data-z="user-managed"> keep old code
        Elements lstExistingDocTagSection = existingDoc.getElementsByTag("section");
        Element existingDocTagSection = null;
        for (Element elementTagSection : lstExistingDocTagSection) {
          if (elementTagSection.hasAttr("data-layout-fragment")
              && elementTagSection.attr("data-layout-fragment").equals("content")) {
            existingDocTagSection = elementTagSection.clone();
            break;
          }
        }

        if (existingDocTagSection != null && existingDocTagSection.hasAttr("data-z")
            && existingDocTagSection.attr("data-z").equals("user-managed")) {
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
          if (mergedDocTagSection != null) {
            mergedDocTagSection.replaceWith(existingDocTagSection);
          }
        }
      }
    }

    if (requiredIds != null) {
      // Get the container of the elements to replace them
      Element existsDocContainer = existingDoc.getElementById(idContainerElements);
      Element newDocContainer = newDoc.getElementById(idContainerElements);

      if (existsDocContainer != null && newDocContainer != null
          && mergedDoc.getElementById(idContainerElements) != null) {

        // Put back the element 'container'
        mergedDoc.getElementById(idContainerElements).replaceWith(existsDocContainer.clone());

        // Get the new elements to insert or replace those necessary
        Element mergedDocContainer = mergedDoc.getElementById(idContainerElements);

        // Elements to add before the last element 'form-group' of
        // container
        List<Element> listElementsToAdd = new ArrayList<Element>();

        Iterator<String> iterRequiredFields = requiredIds.iterator();
        while (iterRequiredFields.hasNext()) {
          String idField = iterRequiredFields.next();
          Element mergedField = mergedDocContainer.getElementById(idField);

          // If doesn't exist, put in the list to add the element to
          // merged page
          if (mergedField == null) {
            listElementsToAdd.add(newDocContainer.getElementById(idField));
          } else {

            // If exists, check data-z attribute
            if (!mergedField.hasAttr("data-z")
                || (mergedField.hasAttr("data-z") && !mergedField.attr("data-z").equals(
                    "user-managed"))) {

              // Replace the old value by the calculated
              Element fieldToCopy = newDocContainer.getElementById(idField).clone();
              mergedField.replaceWith(fieldToCopy);
            }
          }
        }

        // Add new elements
        if (!listElementsToAdd.isEmpty()) {

          // Try to insert the elements after the last 'form-group'
          // element. Normally form buttons
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

        // Delete old elements without user-managed value in data-z
        // attribute
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
  public Document mergeListView(Document existingDoc, Document newDoc, String idContainerElements,
      List<String> requiredIds, List<String> namesDetails) {

    Document mergedDoc = merge(existingDoc, newDoc, idContainerElements, requiredIds);

    // restore detail code from old doc
    if (mergedDoc.getElementById("nav-tabs") != null
        && existingDoc.getElementById("nav-tabs") != null) {
      mergedDoc.getElementById("nav-tabs").replaceWith(
          existingDoc.getElementById("nav-tabs").clone());
    }
    if (mergedDoc.getElementById("tab-content") != null
        && existingDoc.getElementById("tab-content") != null) {
      mergedDoc.getElementById("tab-content").replaceWith(
          existingDoc.getElementById("tab-content").clone());
    }

    // Set Entity datatable javascript depends of 'data-z' attribute
    String javascriptId = requiredIds.get(0).concat("Javascript");
    Element elementRequiredJavascript = mergedDoc.getElementById(javascriptId);
    if (elementRequiredJavascript == null && !namesDetails.isEmpty()) {
      mergedDoc.getElementsByAttributeValue("data-layout-fragment", "javascript").get(0)
          .appendChild(newDoc.getElementById(javascriptId).clone());
    } else {
      if (!elementRequiredJavascript.hasAttr("data-z")
          || (elementRequiredJavascript.hasAttr("data-z") && !elementRequiredJavascript.attr(
              "data-z").equals("user-managed"))) {
        mergedDoc.getElementById(javascriptId).replaceWith(
            newDoc.getElementById(javascriptId).clone());
      }
    }

    if (!namesDetails.isEmpty()) {
      if (mergedDoc.getElementById("nav-tabs") == null) {
        mergedDoc.getElementById(idContainerElements).before(newDoc.getElementById("nav-tabs"));
      }

      // Set tabs details depends of 'data-z' attribute
      for (String nameDetail : namesDetails) {
        String nameTab = nameDetail.replace("Table", "Tab");
        Element datatableDetailTab = mergedDoc.getElementById(nameTab);
        if (datatableDetailTab == null) {
          mergedDoc.getElementById("nav-tabs").appendChild(
              newDoc.getElementById(nameTab).parentNode().clone());
        } else {
          if (!datatableDetailTab.hasAttr("data-z")
              || (datatableDetailTab.hasAttr("data-z") && !datatableDetailTab.attr("data-z")
                  .equals("user-managed"))) {
            mergedDoc.getElementById(nameTab).replaceWith(newDoc.getElementById(nameTab).clone());
          }
        }
      }

      if (mergedDoc.getElementById("tab-content") == null) {
        mergedDoc.getElementById("nav-tabs").before(newDoc.getElementById("tab-content"));
      }

      // Set datatables details depends of 'data-z' attribute
      for (String nameDetail : namesDetails) {
        String divDetailName = "detail-".concat(nameDetail.replace("Table", ""));
        Element datatableDetail = mergedDoc.getElementById(divDetailName);
        if (datatableDetail == null) {
          mergedDoc.getElementById("tab-content").appendChild(
              newDoc.getElementById(divDetailName).clone());
        } else {
          if (!datatableDetail.hasAttr("data-z")
              || (datatableDetail.hasAttr("data-z") && !datatableDetail.attr("data-z").equals(
                  "user-managed"))) {
            mergedDoc.getElementById(divDetailName).replaceWith(
                newDoc.getElementById(divDetailName).clone());
          }
        }
      }

      // Set datatables javascript details depends of 'data-z' attribute
      for (String nameDetail : namesDetails) {
        javascriptId = nameDetail.concat("Javascript");
        elementRequiredJavascript = mergedDoc.getElementById(javascriptId);
        if (elementRequiredJavascript == null) {
          mergedDoc.getElementsByAttributeValue("data-layout-fragment", "javascript").get(0)
              .appendChild(newDoc.getElementById(javascriptId).clone());
        } else {
          if (!elementRequiredJavascript.hasAttr("data-z")
              || (elementRequiredJavascript.hasAttr("data-z") && !elementRequiredJavascript.attr(
                  "data-z").equals("user-managed"))) {
            mergedDoc.getElementById(javascriptId).replaceWith(
                newDoc.getElementById(javascriptId).clone());
          }
        }
      }
    }

    // Set EntityTableFirstDetailJavascript depends of 'data-z' attribute
    javascriptId = requiredIds.get(0).concat("FirstDetailJavascript");
    Element elementRequiredDetailJavascript = mergedDoc.getElementById(javascriptId);
    if (elementRequiredDetailJavascript == null) {
      if (!namesDetails.isEmpty()) {
        mergedDoc.getElementsByAttributeValue("data-layout-fragment", "javascript").get(0)
            .appendChild(newDoc.getElementById(javascriptId).clone());
      }
    } else {
      if (!elementRequiredDetailJavascript.hasAttr("data-z")
          || (elementRequiredDetailJavascript.hasAttr("data-z") && !elementRequiredDetailJavascript
              .attr("data-z").equals("user-managed"))) {

        // if it's empty and not 'user-managed' is because the master doesn't have details. Must be removed.
        if (namesDetails.isEmpty()) {
          elementRequiredJavascript.remove();
        } else {
          mergedDoc.getElementById(javascriptId).replaceWith(
              newDoc.getElementById(javascriptId).clone());
        }
      }
    }

    // Delete old elements without user-managed value in data-z attribute
    // and not in requiredIds list

    // content
    if (mergedDoc.getElementById("tab-content") != null) {
      Elements allDetails = mergedDoc.getElementById("tab-content").getElementsByTag("table");
      for (Element detail : allDetails) {
        // Check data-z attribute
        if ((detail.hasAttr("data-z") && !detail.attr("data-z").equals("user-managed"))) {
          String idElement = detail.attr("id");
          if (idElement != null && !namesDetails.contains(idElement)) {
            detail.parent().parent().remove();
          }
        }
      }
    }

    // tabs
    if (mergedDoc.getElementById("nav-tabs") != null) {
      Elements allDetailTabs = mergedDoc.getElementById("nav-tabs").getElementsByTag("a");
      for (Element detailTab : allDetailTabs) {
        // Check data-z attribute
        if ((detailTab.hasAttr("data-z") && !detailTab.attr("data-z").equals("user-managed"))) {
          String idElement = detailTab.attr("id");
          if (idElement != null) {
            String nameToCompare = idElement.replace("Tab", "Table");
            if (!namesDetails.contains(nameToCompare)) {
              detailTab.parent().remove();
            }
          }
        }
      }
    }

    // javascript
    Elements allDetailJavascript =
        mergedDoc.getElementsByAttributeValueContaining("id", "TableJavascript");
    for (Element detailJavascript : allDetailJavascript) {
      if ((detailJavascript.hasAttr("data-z") && !detailJavascript.attr("data-z").equals(
          "user-managed"))) {
        String idElement = detailJavascript.attr("id");
        if (idElement != null) {
          String nameToCompare = idElement.replace("Javascript", "");
          if (!namesDetails.contains(nameToCompare) && !requiredIds.contains(nameToCompare)) {
            detailJavascript.remove();
          }
        }
      }
    }

    return mergedDoc;
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
