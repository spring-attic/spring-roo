package org.springframework.roo.support.util;

import java.io.InputStream;

/**
 * Utilities for dealing with "templates", which are commonly used by ROO add-ons.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public abstract class TemplateUtils {
	
	/**
	 * Determines the path to the requested template.
	 * 
	 * @param clazz which owns the template (required)
	 * @param templateFilename the filename of the template (required)
	 * @return the full classloader-specific path to the template (never null)
	 */
	public static String getTemplatePath(Class<?> clazz, String templateFilename) {
		Assert.notNull(clazz, "Owning class required");
		Assert.hasText(templateFilename, "Template filename required");
		Assert.isTrue(!templateFilename.startsWith("/"), "Template filename shouldn't start with a slash");
		// Slashes instead of File.separatorChar is correct here, as these are classloader paths (not file system paths)
		return "/" + clazz.getPackage().getName().replace('.', '/') + "/" + templateFilename;
	}

	/**
	 * Acquires an {@link InputStream} to the requested classloader-derived template.
	 * 
	 * @param clazz which owns the template (required)
	 * @param templateFilename the filename of the template (required)
	 * @return the input stream (never null; an exception is thrown if cannot be found)
	 */
	public static InputStream getTemplate(Class<?> clazz, String templateFilename) {
		String templatePath = getTemplatePath(clazz, templateFilename);
		InputStream result = clazz.getResourceAsStream(templatePath);
		Assert.notNull(result, "Could not locate '" + templatePath + "' in classloader");
		return result;
	}
	
}
