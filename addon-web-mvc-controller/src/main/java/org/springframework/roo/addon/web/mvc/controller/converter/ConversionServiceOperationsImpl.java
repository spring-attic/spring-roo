package org.springframework.roo.addon.web.mvc.controller.converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.web.mvc.controller.WebMvcOperations;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;

/**
 * A default implementation of {@link ConversionServiceOperations}.
 * 
 * @author Rossen Stoyanchev
 * @since 1.1.1
 */
@Component 
@Service
public class ConversionServiceOperationsImpl implements ConversionServiceOperations {
	@Reference private FileManager fileManager;
	@Reference private TypeLocationService typeLocationService;
	@Reference private WebMvcOperations webMvcOperations;

	public ConversionServiceOperationsImpl() {}

	public ConversionServiceOperationsImpl(FileManager fileManager, TypeLocationService typeLocationService) {
		// For testing
		this.fileManager = fileManager;
		this.typeLocationService = typeLocationService;
	}

	public void installConversionService(JavaPackage thePackage) {
		installJavaClass(thePackage);
		webMvcOperations.installConversionService(thePackage);
		fileManager.scan();
	}

	void installJavaClass(JavaPackage thePackage) {
		JavaType javaType = new JavaType(thePackage.getFullyQualifiedPackageName() + "." + CONVERSION_SERVICE_SIMPLE_TYPE);
		String physicalPath = typeLocationService.getPhysicalLocationCanonicalPath(javaType, Path.SRC_MAIN_JAVA);
		if (fileManager.exists(physicalPath)) {
			return;
		}
		try {
			InputStream template = TemplateUtils.getTemplate(getClass(), CONVERSION_SERVICE_SIMPLE_TYPE + "-template._java");
			String input = FileCopyUtils.copyToString(new InputStreamReader(template));
			input = input.replace("__PACKAGE__", thePackage.getFullyQualifiedPackageName());
			MutableFile mutableFile = fileManager.createFile(physicalPath);
			FileCopyUtils.copy(input.getBytes(), mutableFile.getOutputStream());
		} catch (IOException e) {
			throw new IllegalStateException("Unable to create '" + physicalPath + "'", e);
		}
	}
}
