package org.springframework.roo.addon.web.mvc.views.template.engines;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.springframework.roo.addon.web.mvc.views.AbstractViewGenerationService;
import org.springframework.roo.addon.web.mvc.views.ViewContext;
import org.springframework.roo.addon.web.mvc.views.components.FieldItem;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.support.osgi.OSGiUtils;
import org.springframework.roo.support.util.FileUtils;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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

  protected DOC process(String templateName, ViewContext ctx) {
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
      if (ctx.getEntityName() != null) {
        input.put("entityLabel", FieldItem.buildLabel(ctx.getEntityName(), ""));
        input.put("entityLabelPlural", FieldItem.buildLabel(ctx.getEntityName(), "plural"));
        input.put("z", new FieldItem("", ctx.getEntityName()).getZ());
      }
      input.put("identifierField", ctx.getIdentifierField());
      input.put("modelAttribute", String.format("${%s}", ctx.getModelAttribute()));
      input.put("modelAttributeName", ctx.getModelAttributeName());

      // Add all extra elements from ViewContext. This is useful if some
      // implementation wants to include its own information
      for (Entry<String, Object> extraInformation : ctx.getExtraInformation().entrySet()) {
        input.put(extraInformation.getKey(), extraInformation.getValue());
      }

      Template template = cfg.getTemplate(templateName.concat(".ftl"));
      StringBuilderWriter writer = new StringBuilderWriter();
      template.process(input, writer);
      writer.close();

      content = writer.toString();

      if (StringUtils.isBlank(content)) {
        throw new RuntimeException(String.format(
            "ERROR: Error trying to generate final content from provided template '%s.ftl'",
            templateName));
      }

      return parse(content);

    } catch (Exception e) {
      throw new RuntimeException(
          String.format(
              "ERROR: Error trying to generate final content from provided template '%s.ftl'. You should provide a valid .ftl file",
              templateName), e);
    }

  }

  /**
   * This method will copy the contents of a directory to another if the
   * resource does not already exist in the target directory
   *
   * @param sourceAntPath the source path
   * @param targetDirectory the target directory
   */
  protected void copyDirectoryContents(final String sourceAntPath, String targetDirectory,
      final boolean replace) {
    Validate.notBlank(sourceAntPath, "Source path required");
    Validate.notBlank(targetDirectory, "Target directory required");

    if (!targetDirectory.endsWith("/")) {
      targetDirectory += "/";
    }

    if (!fileManager.exists(targetDirectory)) {
      fileManager.createDirectory(targetDirectory);
    }

    final String path = FileUtils.getPath(getClass(), sourceAntPath);
    final Iterable<URL> urls = OSGiUtils.findEntriesByPattern(context, path);
    Validate.notNull(urls, "Could not search bundles for resources for Ant Path '%s'", path);
    for (final URL url : urls) {
      final String fileName = url.getPath().substring(url.getPath().lastIndexOf("/") + 1);
      if (replace) {
        try {
          String contents = IOUtils.toString(url);
          fileManager.createOrUpdateTextFileIfRequired(targetDirectory + fileName, contents, false);
        } catch (final Exception e) {
          throw new IllegalStateException(e);
        }
      } else {
        if (!fileManager.exists(targetDirectory + fileName)) {
          InputStream inputStream = null;
          OutputStream outputStream = null;
          try {
            inputStream = url.openStream();
            outputStream = fileManager.createFile(targetDirectory + fileName).getOutputStream();
            IOUtils.copy(inputStream, outputStream);
          } catch (final Exception e) {
            throw new IllegalStateException(
                "Encountered an error during copying of resources for the add-on.", e);
          } finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(outputStream);
          }
        }
      }
    }
  }

}
