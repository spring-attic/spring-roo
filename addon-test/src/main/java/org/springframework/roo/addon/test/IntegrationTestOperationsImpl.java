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
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
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
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		List<AnnotationAttributeValue<?>> config = new ArrayList<AnnotationAttributeValue<?>>();
		config.add(new ClassAttributeValue(new JavaSymbolName("value"), new JavaType("org.junit.runners.JUnit4")));
		annotations.add(new AnnotationMetadataBuilder(new JavaType("org.junit.runner.RunWith"), config));
		annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.mock.staticmock.MockStaticEntityMethods")));

		List<MethodMetadataBuilder> methods = new ArrayList<MethodMetadataBuilder>();
		List<AnnotationMetadataBuilder> methodAnnotations = new ArrayList<AnnotationMetadataBuilder>();
		methodAnnotations.add(new AnnotationMetadataBuilder(new JavaType("org.junit.Test")));

		// Get the entity so we can hopefully make a demo method that will be usable
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		EntityMetadata em = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(entity, Path.SRC_MAIN_JAVA));
		if (em != null) {
			MethodMetadata mm = em.getCountMethod();
			if (mm != null) {
				String countMethod = entity.getSimpleTypeName() + "." + mm.getMethodName().getSymbolName() + "()";
				bodyBuilder.appendFormalLine("int expectedCount = 13;");
				bodyBuilder.appendFormalLine(countMethod + ";");
				bodyBuilder.appendFormalLine("org.springframework.mock.staticmock.AnnotationDrivenStaticEntityMockingControl.expectReturn(expectedCount);");
				bodyBuilder.appendFormalLine("org.springframework.mock.staticmock.AnnotationDrivenStaticEntityMockingControl.playback();");
				bodyBuilder.appendFormalLine("org.junit.Assert.assertEquals(expectedCount, " + countMethod + ");");
			}
		}

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName("testMethod"), JavaType.VOID_PRIMITIVE, bodyBuilder);
		methodBuilder.setAnnotations(methodAnnotations);
		methods.add(methodBuilder);

		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId);
		typeDetailsBuilder.setModifier(Modifier.PUBLIC);
		typeDetailsBuilder.setName(name);
		typeDetailsBuilder.setPhysicalTypeCategory(PhysicalTypeCategory.CLASS);
		typeDetailsBuilder.setDeclaredMethods(methods);

		classpathOperations.generateClassFile(typeDetailsBuilder.build());
	}
}
