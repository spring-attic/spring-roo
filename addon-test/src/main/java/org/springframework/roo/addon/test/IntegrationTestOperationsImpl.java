package org.springframework.roo.addon.test;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.dod.DataOnDemandOperations;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
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
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
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
	@Reference private DataOnDemandOperations dataOnDemandOperations;
	@Reference private FileManager fileManager;
	@Reference private MetadataService metadataService;
	@Reference private MemberDetailsScanner memberDetailsScanner;
	@Reference private ProjectOperations projectOperations;
	@Reference private TypeManagementService typeManagementService;
	
	public boolean isPersistentClassAvailable() {
		return projectOperations.isProjectAvailable() && fileManager.exists(projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml"));
	}

	public void newIntegrationTest(JavaType entity) {
		Assert.notNull(entity, "Entity to produce an integration test for is required");

		// Verify the requested entity actually exists as a class and is not abstract
		ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = getEntity(entity);
		Assert.isTrue(!Modifier.isAbstract(classOrInterfaceTypeDetails.getModifier()), "Type " + entity.getFullyQualifiedTypeName() + " is abstract");
		
		dataOnDemandOperations.newDod(entity, new JavaType(entity.getFullyQualifiedTypeName() + "DataOnDemand"), Path.SRC_TEST_JAVA);
		
		JavaType name = new JavaType(entity + "IntegrationTest");
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(name, Path.SRC_TEST_JAVA);

		if (metadataService.get(declaredByMetadataId) != null) {
			// The file already exists
			return;
		}

		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		List<AnnotationAttributeValue<?>> config = new ArrayList<AnnotationAttributeValue<?>>();
		config.add(new ClassAttributeValue(new JavaSymbolName("entity"), entity));
		annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.test.RooIntegrationTest"), config));

		List<MethodMetadataBuilder> methods = new ArrayList<MethodMetadataBuilder>();
		List<AnnotationMetadataBuilder> methodAnnotations = new ArrayList<AnnotationMetadataBuilder>();
		methodAnnotations.add(new AnnotationMetadataBuilder(new JavaType("org.junit.Test")));
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName("testMarkerMethod"), JavaType.VOID_PRIMITIVE, new InvocableMemberBodyBuilder());
		methodBuilder.setAnnotations(methodAnnotations);
		methods.add(methodBuilder);

		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, name, PhysicalTypeCategory.CLASS);
		typeDetailsBuilder.setAnnotations(annotations);
		typeDetailsBuilder.setDeclaredMethods(methods);
		
		typeManagementService.generateClassFile(typeDetailsBuilder.build());
	}

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
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(entity, Path.SRC_MAIN_JAVA));
		if (physicalTypeMetadata != null) {
			ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = (ClassOrInterfaceTypeDetails) physicalTypeMetadata.getMemberHoldingTypeDetails();
			if (classOrInterfaceTypeDetails != null) {
				MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(IntegrationTestOperationsImpl.class.getName(), classOrInterfaceTypeDetails);
				List<MethodMetadata> countMethods = MemberFindingUtils.getMethodsWithTag(memberDetails, PersistenceCustomDataKeys.COUNT_ALL_METHOD);
				if (countMethods.size() == 1) {
					String countMethod = entity.getSimpleTypeName() + "." + countMethods.get(0).getMethodName().getSymbolName() + "()";
					bodyBuilder.appendFormalLine("int expectedCount = 13;");
					bodyBuilder.appendFormalLine(countMethod + ";");
					bodyBuilder.appendFormalLine("org.springframework.mock.staticmock.AnnotationDrivenStaticEntityMockingControl.expectReturn(expectedCount);");
					bodyBuilder.appendFormalLine("org.springframework.mock.staticmock.AnnotationDrivenStaticEntityMockingControl.playback();");
					bodyBuilder.appendFormalLine("org.junit.Assert.assertEquals(expectedCount, " + countMethod + ");");
				}
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
				if (!(typeDetails.getCustomData().keySet().contains(PersistenceCustomDataKeys.PERSISTENT_TYPE) || typeDetails.getDeclaredByMetadataId().startsWith("MID:org.springframework.roo.addon.tostring.ToStringMetadata"))) {
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
	
	/**
	 * @param entity the entity to lookup required 
	 * @return the type details (never null; throws an exception if it cannot be obtained or parsed)
	 */
	private ClassOrInterfaceTypeDetails getEntity(JavaType entity) {
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(entity, Path.SRC_MAIN_JAVA);
		PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(physicalTypeIdentifier);
		Assert.notNull(ptm, "Java source code unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier));
		PhysicalTypeDetails ptd = ptm.getMemberHoldingTypeDetails();
		Assert.notNull(ptd, "Java source code details unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier));
		Assert.isInstanceOf(ClassOrInterfaceTypeDetails.class, ptd, "Java source code is immutable for type " + PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier));
		return (ClassOrInterfaceTypeDetails) ptd;
	}
}