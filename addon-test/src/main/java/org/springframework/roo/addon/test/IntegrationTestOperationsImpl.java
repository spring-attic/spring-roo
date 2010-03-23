package org.springframework.roo.addon.test;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
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
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;

/**
 * Provides convenience methods that can be used to create mock tests.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@Component
@Service
public class IntegrationTestOperationsImpl implements IntegrationTestOperations {
	@Reference private ClasspathOperations classpathOperations;
	@Reference private MetadataService metadataService;
	
	/**
	 * Creates a mock test for the entity. Silently returns if the mock test file already exists.
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
		
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> config = new ArrayList<AnnotationAttributeValue<?>>();
		config.add(new ClassAttributeValue(new JavaSymbolName("value"), new JavaType("org.junit.runners.JUnit4")));
		annotations.add(new DefaultAnnotationMetadata(new JavaType("org.junit.runner.RunWith"), config));
		annotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.mock.staticmock.MockStaticEntityMethods"), new ArrayList<AnnotationAttributeValue<?>>()));

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
				imbb.appendFormalLine("org.springframework.mock.staticmock.AnnotationDrivenStaticEntityMockingControl.expectReturn(expectedCount);");
				imbb.appendFormalLine("org.springframework.mock.staticmock.AnnotationDrivenStaticEntityMockingControl.playback();");
				imbb.appendFormalLine("org.junit.Assert.assertEquals(expectedCount, " + countMethod + ");");
			}
		}
		
		MethodMetadata method = new DefaultMethodMetadata(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName("testMethod"), JavaType.VOID_PRIMITIVE, null, null, methodAnnotations, null, imbb.getOutput());
		methods.add(method);

		ClassOrInterfaceTypeDetails details = new DefaultClassOrInterfaceTypeDetails(declaredByMetadataId, name, Modifier.PUBLIC, PhysicalTypeCategory.CLASS, null, null, methods, null, null, null, annotations, null);
		classpathOperations.generateClassFile(details);
	}
	
}
