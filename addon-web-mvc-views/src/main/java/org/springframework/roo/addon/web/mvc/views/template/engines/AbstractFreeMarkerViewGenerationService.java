package org.springframework.roo.addon.web.mvc.views.template.engines;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.springframework.roo.addon.web.mvc.views.AbstractViewGenerationService;
import org.springframework.roo.addon.web.mvc.views.ViewContext;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.process.manager.FileManager;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;

@Component(componentAbstract = true)
public abstract class AbstractFreeMarkerViewGenerationService<DOC> extends
    AbstractViewGenerationService<DOC> {

  @Reference
  FileManager fileManager;

  protected abstract Class<?> getResourceLoaderClass();

  protected boolean checkTemplates(String location, String templateName) {
    // Check if provided template exists and has .ftl extension
    return fileManager.exists(location.concat("/").concat(templateName).concat(".ftl"));
  }

  @Override
  public void installTemplates() {
    // Getting destination where FreeMarker templates should be installed.
    // This will allow developers to customize his own templates.
    String destination = getTemplatesLocation();

    // TODO: Load Templates to install from getResourceLoaderClass()
  }

  protected DOC process(String templateName, List<FieldMetadata> fields, ViewContext ctx) {
    String content = "";

    try {

      Configuration cfg = new Configuration(new Version(2, 3, 23));
      cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

      // Check if exists some template. If not, use classpath to locate the template
      if (checkTemplates(getTemplatesLocation(), templateName)) {
        // Process using FreeMarker and setDirectoryForTemplateLoading
        cfg.setDirectoryForTemplateLoading(new File(getTemplatesLocation()));
      } else {
        // Process using FreeMarker and setClassForTemplateLoading
        cfg.setClassForTemplateLoading(getResourceLoaderClass(), "templates");
      }

      // Prepare the template input:
      Map<String, Object> input = new HashMap<String, Object>();

      // Getting project information from ViewContext
      input.put("projectName", ctx.getProjectName());
      input.put("description", ctx.getDescription());
      input.put("version", ctx.getVersion());

      // Getting controller information from ViewContext
      input.put("controllerPath", ctx.getControllerPath());

      // Getting entity information from ViewContext
      input.put("entityName", ctx.getEntityName());
      input.put("modelAttribute", String.format("${%s}", ctx.getModelAttribute()));

      // Add all extra elements from ViewContext. This is useful if some
      // implementation wants to include its own information
      for (Entry<String, Object> extraInformation : ctx.getExtraInformation().entrySet()) {
        input.put(extraInformation.getKey(), extraInformation.getValue());
      }

      // Adding entity fields
      input.put("fields", fields);

      Template template = cfg.getTemplate(templateName.concat(".ftl"));
      StringBuilderWriter writer = new StringBuilderWriter();
      template.process(input, writer);
      writer.close();

      content = writer.toString();

      if (StringUtils.isBlank(content)) {
        throw new RuntimeException(
            "ERROR: Error trying to generate final content from provided template");
      }

      return parse(content);

    } catch (Exception e) {
      throw new RuntimeException(
          "ERROR: Error trying to generate final content from provided template. You should provide a valid .ftl file");
    }

  }

}
