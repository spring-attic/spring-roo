package org.springframework.roo.addon.gwt;

import hapax.Template;
import hapax.TemplateException;
import hapax.TemplateLoader;
import hapax.parser.TemplateParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

/**
 * Loads hapax templates from the classpath.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class TemplateResourceLoader implements TemplateLoader {

    private static final Map<String, Template> cache = new HashMap<String, Template>();
    private static final String TEMPLATE_DIR = "org/springframework/roo/addon/gwt/scaffold/templates/";

    /**
     * Creates a TemplateLoader for CTemplate language using the default
     * template directory
     */
    public static TemplateLoader create() {
        return new TemplateResourceLoader(TEMPLATE_DIR);
    }

    /**
     * Creates a TemplateLoader for CTemplate language
     */
    public static TemplateLoader create(final String base_path) {
        return new TemplateResourceLoader(base_path);
    }

    /**
     * Creates a TemplateLoader using the argument parser.
     */
    public static TemplateLoader createForParser(final String base_path,
            final TemplateParser parser) {
        return new TemplateResourceLoader(base_path, parser);
    }

    protected final String baseDir;

    protected final TemplateParser parser;

    public TemplateResourceLoader(final String baseDir) {
        this(baseDir, null);
    }

    public TemplateResourceLoader(final String baseDir,
            final TemplateParser parser) {
        this.baseDir = baseDir;
        this.parser = parser;
    }

    public Template getTemplate(final String resource) throws TemplateException {
        return getTemplate(new TemplateLoader.Context(this, baseDir), resource);
    }

    public Template getTemplate(final TemplateLoader context, String resource)
            throws TemplateException {
        if (!resource.endsWith(".xtm")) {
            resource += ".xtm";
        }

        final String templatePath = baseDir + resource;
        if (cache.containsKey(templatePath)) {
            return cache.get(templatePath);
        }

        final InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream(templatePath);
        Validate.notNull(inputStream, "template path required");
        String contents;
        try {
            contents = IOUtils.toString(inputStream);
        }
        catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        finally {
            IOUtils.closeQuietly(inputStream);
        }

        final Template template = parser == null ? new Template(contents,
                context) : new Template(parser, contents, context);

        synchronized (cache) {
            cache.put(templatePath, template);
        }

        return template;
    }

    public String getTemplateDirectory() {
        return baseDir;
    }
}
