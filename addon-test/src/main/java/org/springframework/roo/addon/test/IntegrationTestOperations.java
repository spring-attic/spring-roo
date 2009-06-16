package org.springframework.roo.addon.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultMethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.operations.ClasspathOperations;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;

/**
 * Provides convenience methods that can be used to create mock tests.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopment
public class IntegrationTestOperations {
	private ClasspathOperations classpathOperations;
	private MetadataService metadataService;
	private FileManager fileManager;
	
	public IntegrationTestOperations(ClasspathOperations classpathOperations, MetadataService metadataService, FileManager fileManager) {
		Assert.notNull(classpathOperations, "Classpath operations required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(fileManager, "File manager required");
		this.classpathOperations = classpathOperations;
		this.metadataService = metadataService;
		this.fileManager = fileManager;
	}

	/**
	 * Creates a mock test for the entity. Automatically adds mocking classes if they don't already exist.
	 * Silently returns if the mock test file already exists.
	 * 
	 * @param entity to produce a mock test for (required)
	 */
	public void newMockTest(JavaType entity) {
		Assert.notNull(entity, "Entity to produce a mock test for is required");
		
		JavaType name = new JavaType(entity + "Test");
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(name, Path.SRC_TEST_JAVA);

		if (metadataService.get(declaredByMetadataId) != null) {
			// The file already exists
			return;
		}

		// Determine if the mocking infrastructure needs installing
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata unavailable");
		installIfNeeded("AbstractMethodMockingControl.aj", projectMetadata);
		installIfNeeded("JUnitStaticEntityMockingControl.aj", projectMetadata);
		installIfNeeded("MockStaticEntityMethods.java", projectMetadata);
		
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> config = new ArrayList<AnnotationAttributeValue<?>>();
		config.add(new ClassAttributeValue(new JavaSymbolName("value"), new JavaType("org.junit.runners.JUnit4")));
		annotations.add(new DefaultAnnotationMetadata(new JavaType("org.junit.runner.RunWith"), config));
		String mockPackageWithTrailingDot = projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName() + ".mock.";
		annotations.add(new DefaultAnnotationMetadata(new JavaType(mockPackageWithTrailingDot + "MockStaticEntityMethods"), new ArrayList<AnnotationAttributeValue<?>>()));

		List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
		List<AnnotationMetadata> methodAnnotations = new ArrayList<AnnotationMetadata>();
		methodAnnotations.add(new DefaultAnnotationMetadata(new JavaType("org.junit.Test"), new ArrayList<AnnotationAttributeValue<?>>()));
		
		// Get the entity so we can hopefully make a demo method that will be usable
		InvocableMemberBodyBuilder imbb = new InvocableMemberBodyBuilder();
		EntityMetadata em = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(entity, Path.SRC_MAIN_JAVA));
		if (em != null) {
			MethodMetadata mm = em.getCountMethod();
			if (mm != null) {
				String countMethod = entity.getSimpleTypeName() + "." + mm.getMethodName().getSymbolName() + "()";
				imbb.appendFormalLine("int expectedCount = 13;");
				imbb.appendFormalLine(countMethod + ";");
				imbb.appendFormalLine(mockPackageWithTrailingDot + "JUnitStaticEntityMockingControl.expectReturn(expectedCount);");
				imbb.appendFormalLine(mockPackageWithTrailingDot + "JUnitStaticEntityMockingControl.playback();");
				imbb.appendFormalLine("junit.framework.Assert.assertEquals(expectedCount, " + countMethod + ");");
			}
		}
		
		MethodMetadata method = new DefaultMethodMetadata(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName("testMethod"), JavaType.VOID_PRIMITIVE, null, null, methodAnnotations, imbb.getOutput());
		methods.add(method);

		ClassOrInterfaceTypeDetails details = new DefaultClassOrInterfaceTypeDetails(declaredByMetadataId, name, Modifier.PUBLIC, PhysicalTypeCategory.CLASS, null, null, methods, null, null, null, annotations, null);
		classpathOperations.generateClassFile(details);
	}
	
	private void installIfNeeded(String targetFilename, ProjectMetadata projectMetadata) {
		String packagePath = projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName().replace('.', '/');
		String destinationFile = projectMetadata.getPathResolver().getIdentifier(Path.SRC_TEST_JAVA, packagePath + "/" + "mock/" + targetFilename);
		if (!fileManager.exists(destinationFile)) {
			InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), targetFilename + "-template");
			try {
				// Read template and insert the user's package
				String input = FileCopyUtils.copyToString(new InputStreamReader(templateInputStream));
				input = input.replace("__TOP_LEVEL_PACKAGE__", projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName());
				
				// Output the file for the user
				MutableFile mutableFile = fileManager.createFile(destinationFile);
				FileCopyUtils.copy(input.getBytes(), mutableFile.getOutputStream());
			} catch (IOException ioe) {
				throw new IllegalStateException("Unable to create '" + targetFilename + "'", ioe);
			}
		}
		
	}
	
}
