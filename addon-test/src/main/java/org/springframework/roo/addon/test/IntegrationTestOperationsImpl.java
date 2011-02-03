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
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Provides convenience methods that can be used to create mock tests.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component
@Service
public class IntegrationTestOperationsImpl implements IntegrationTestOperations {
	@Reference private TypeManagementService typeManagementService;
	@Reference private MetadataService metadataService;
	@Reference private MemberDetailsScanner memberDetailsScanner;
	
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
			MethodMetadata methodMetadata = em.getCountMethod();
			if (methodMetadata != null) {
				String countMethod = entity.getSimpleTypeName() + "." + methodMetadata.getMethodName().getSymbolName() + "()";
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

		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, name, PhysicalTypeCategory.CLASS);
		typeDetailsBuilder.setDeclaredMethods(methods);

		typeManagementService.generateClassFile(typeDetailsBuilder.build());
	}
	
	public void newTestStub(JavaType entity) {
		Assert.notNull(entity, "Entity to produce a test stub for is required");

		JavaType name = new JavaType(entity + "Test");
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(name, Path.SRC_TEST_JAVA);

		if (metadataService.get(declaredByMetadataId) != null) {
			// The file already exists
			return;
		}

		// Determine if the test infrastructure needs installing
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		List<AnnotationAttributeValue<?>> config = new ArrayList<AnnotationAttributeValue<?>>();
		config.add(new ClassAttributeValue(new JavaSymbolName("value"), new JavaType("org.junit.runners.JUnit4")));
		annotations.add(new AnnotationMetadataBuilder(new JavaType("org.junit.runner.RunWith"), config));

		List<MethodMetadataBuilder> methods = new ArrayList<MethodMetadataBuilder>();
		List<AnnotationMetadataBuilder> methodAnnotations = new ArrayList<AnnotationMetadataBuilder>();
		methodAnnotations.add(new AnnotationMetadataBuilder(new JavaType("org.junit.Test")));

		// Get the entity so we can hopefully make a demo method that will be usable
		String pid = PhysicalTypeIdentifier.createIdentifier(entity, Path.SRC_MAIN_JAVA);
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(pid);
		if (physicalTypeMetadata != null) {
			ClassOrInterfaceTypeDetails governorTypeDetails = (ClassOrInterfaceTypeDetails) physicalTypeMetadata.getMemberHoldingTypeDetails();
			MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(this.getClass().getName(), governorTypeDetails);
			for (MemberHoldingTypeDetails typeDetails : memberDetails.getDetails()) {
				if (!(typeDetails.getDeclaredByMetadataId().startsWith("MID:org.springframework.roo.addon.entity.EntityMetadata") || typeDetails.getDeclaredByMetadataId().startsWith("MID:org.springframework.roo.addon.tostring.ToStringMetadata"))) {
					for (MethodMetadata method : typeDetails.getDeclaredMethods()) {
						// Check if public, non-abstract method
						if (Modifier.isPublic(method.getModifier()) && !Modifier.isAbstract(method.getModifier())) {
							InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
							bodyBuilder.appendFormalLine("org.junit.Assert.assertTrue(true);");

							MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(declaredByMetadataId, Modifier.PUBLIC, method.getMethodName(), JavaType.VOID_PRIMITIVE, bodyBuilder);
							methodBuilder.setAnnotations(methodAnnotations);
							methods.add(methodBuilder);
						}
					}
				}
			}
		}

		// Only create test class if there are test methods present
		if (!methods.isEmpty()) {
			ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, name, PhysicalTypeCategory.CLASS);
			
			// Create instance of entity to test
			FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(pid);
			fieldBuilder.setModifier(Modifier.PRIVATE);
			fieldBuilder.setFieldName(new JavaSymbolName(StringUtils.uncapitalize(entity.getSimpleTypeName())));
			fieldBuilder.setFieldType(entity);
			fieldBuilder.setFieldInitializer("new " + entity.getFullyQualifiedTypeName() + "()");
			List<FieldMetadataBuilder> fields = new ArrayList<FieldMetadataBuilder>();
			fields.add(fieldBuilder);
			typeDetailsBuilder.setDeclaredFields(fields);
			
			typeDetailsBuilder.setDeclaredMethods(methods);

			typeManagementService.generateClassFile(typeDetailsBuilder.build());
		}
	}
}
