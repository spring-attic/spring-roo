package org.springframework.roo.addon.test;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.addon.dod.DataOnDemandMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for {@link RooIntegrationTest}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class IntegrationTestMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	private static final String PROVIDES_TYPE_STRING = IntegrationTestMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private static final JavaType TEST = new JavaType("org.junit.Test");

	private IntegrationTestAnnotationValues annotationValues;
	private DataOnDemandMetadata dataOnDemandMetadata;
	private JavaType dodGovernor;
	private boolean isGaeSupported = false;
	
	private MethodMetadata identifierAccessorMethod;
	private MethodMetadata versionAccessorMethod;
	private MethodMetadata countMethod;
	private MethodMetadata findMethod;
	private MethodMetadata findAllMethod;
	private MethodMetadata findEntriesMethod;
	private MethodMetadata flushMethod;
	private MethodMetadata mergeMethod;
	private MethodMetadata persistMethod;
	private MethodMetadata removeMethod;
	
	public IntegrationTestMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, ProjectMetadata projectMetadata, IntegrationTestAnnotationValues annotationValues, DataOnDemandMetadata dataOnDemandMetadata, MethodMetadata identifierAccessorMethod, MethodMetadata versionAccessorMethod, MethodMetadata countMethod, MethodMetadata findMethod, MethodMetadata findAllMethod, MethodMetadata findEntriesMethod, MethodMetadata flushMethod, MethodMetadata mergeMethod, MethodMetadata persistMethod, MethodMetadata removeMethod) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(projectMetadata, "Project metadata required");
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.notNull(dataOnDemandMetadata, "Data on demand metadata required");
		
		if (identifierAccessorMethod == null) {
			valid = false;
		}
		
		if (!isValid()) {
			return;
		}

		this.annotationValues = annotationValues;
		this.identifierAccessorMethod = identifierAccessorMethod;
		this.versionAccessorMethod = versionAccessorMethod;
		this.dataOnDemandMetadata = dataOnDemandMetadata;
		this.countMethod = countMethod;
		this.findMethod = findMethod;
		this.findAllMethod = findAllMethod;
		this.findEntriesMethod = findEntriesMethod;
		this.flushMethod = flushMethod;
		this.mergeMethod = mergeMethod;
		this.persistMethod = persistMethod;
		this.removeMethod = removeMethod;
		
		dodGovernor = DataOnDemandMetadata.getJavaType(dataOnDemandMetadata.getId());
		
		addRequiredIntegrationTestClassIntroductions();
		
		// Add GAE LocalServiceTestHelper instance and @BeforeClass/@AfterClass methods if GAE is enabled
		if (projectMetadata.isGaeEnabled()) {
			isGaeSupported = true;
			addOptionalIntegrationTestClassIntroductions();
		}
		
		builder.addMethod(getCountMethodTest());
		builder.addMethod(getFindMethodTest());
		builder.addMethod(getFindAllMethodTest());
		builder.addMethod(getFindEntriesMethodTest());
		builder.addMethod(getFlushMethodTest());
		builder.addMethod(getMergeMethodTest());
		builder.addMethod(getPersistMethodTest());
		builder.addMethod(getRemoveMethodTest());
		
		itdTypeDetails = builder.build();
	}
	
	/**
	 * Adds the JUnit and Spring type level annotations if needed
	 */
	public void addRequiredIntegrationTestClassIntroductions() {
		// Add an @RunWith(SpringJunit4ClassRunner) annotation to the type, if the user did not define it on the governor directly
		if (MemberFindingUtils.getAnnotationOfType(governorTypeDetails.getAnnotations(), new JavaType("org.junit.runner.RunWith")) == null) {
			List<AnnotationAttributeValue<?>> runWithAttributes = new ArrayList<AnnotationAttributeValue<?>>();
			runWithAttributes.add(new ClassAttributeValue(new JavaSymbolName("value"), new JavaType("org.springframework.test.context.junit4.SpringJUnit4ClassRunner")));
			builder.addAnnotation(new AnnotationMetadataBuilder(new JavaType("org.junit.runner.RunWith"), runWithAttributes));
		}
		
		// Add an @ContextConfiguration("classpath:/applicationContext.xml") annotation to the type, if the user did not define it on the governor directly
		if (MemberFindingUtils.getAnnotationOfType(governorTypeDetails.getAnnotations(), new JavaType("org.springframework.test.context.ContextConfiguration")) == null) {
			List<AnnotationAttributeValue<?>> ctxCfg = new ArrayList<AnnotationAttributeValue<?>>();
			ctxCfg.add(new StringAttributeValue(new JavaSymbolName("locations"), "classpath:/META-INF/spring/applicationContext.xml"));
			builder.addAnnotation(new AnnotationMetadataBuilder(new JavaType("org.springframework.test.context.ContextConfiguration"), ctxCfg));
		}
		
		// Add an @Transactional, if the user did not define it on the governor directly
		if (MemberFindingUtils.getAnnotationOfType(governorTypeDetails.getAnnotations(), new JavaType("org.springframework.transaction.annotation.Transactional")) == null) {
			builder.addAnnotation(new AnnotationMetadataBuilder(new JavaType("org.springframework.transaction.annotation.Transactional")));
		}
	
		// Add the data on demand field if the user did not define it on the governor directly
		FieldMetadata field = MemberFindingUtils.getField(governorTypeDetails, new JavaSymbolName("dod"));
		if (field != null) {
			Assert.isTrue(field.getFieldType().equals(dodGovernor), "Field 'dod' on '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "' must be of type '" + dodGovernor.getFullyQualifiedTypeName() + "'");
			Assert.notNull(MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("org.springframework.beans.factory.annotation.Autowired")), "Field 'dod' on '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "' must be annotated with @Autowired");
		} else {
			// Add the field via the ITD
			List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
			annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.beans.factory.annotation.Autowired")));
			FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations, new JavaSymbolName("dod"), dodGovernor);
			builder.addField(fieldBuilder.build());
		}
	}

	public void addOptionalIntegrationTestClassIntroductions() {
		// Add the GAE test helper field if the user did not define it on the governor directly
		JavaType helperType = new JavaType("com.google.appengine.tools.development.testing.LocalServiceTestHelper");
		FieldMetadata helperField = MemberFindingUtils.getField(governorTypeDetails, new JavaSymbolName("helper"));
		if (helperField != null) {
			Assert.isTrue(helperField.getFieldType().getFullyQualifiedTypeName().equals(helperType.getFullyQualifiedTypeName()), "Field 'helper' on '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "' must be of type '" + helperType.getFullyQualifiedTypeName() + "'");
		} else {
			// Add the field via the ITD
			String initializer = "new LocalServiceTestHelper(new com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig())";
			FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL, new JavaSymbolName("helper"), helperType, initializer);
			builder.addField(fieldBuilder.build());
		}

		// Initialise parameters for setUp/tearDown methods
		List<JavaType> parameters = new ArrayList<JavaType>();

		// Prepare setUp method signature
		JavaSymbolName setUpMethodName = new JavaSymbolName("setUp");
		MethodMetadata setUpMethod = MemberFindingUtils.getMethod(governorTypeDetails, setUpMethodName, parameters);
		if (setUpMethod != null) {
			Assert.notNull(MemberFindingUtils.getAnnotationOfType(setUpMethod.getAnnotations(), new JavaType("org.junit.BeforeClass")), "Method 'setUp' on '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "' must be annotated with @BeforeClass");
		} else {
			// Add the method via the ITD
			List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
			annotations.add(new AnnotationMetadataBuilder(new JavaType("org.junit.BeforeClass")));

			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine("helper.setUp();");

			MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC | Modifier.STATIC, setUpMethodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameters), new ArrayList<JavaSymbolName>(), bodyBuilder);
			methodBuilder.setAnnotations(annotations);
			builder.addMethod(methodBuilder.build());
		}

		// Prepare tearDown method signature
		JavaSymbolName tearDownMethodName = new JavaSymbolName("tearDown");
		MethodMetadata tearDownMethod = MemberFindingUtils.getMethod(governorTypeDetails, tearDownMethodName, parameters);
		if (tearDownMethod != null) {
			Assert.notNull(MemberFindingUtils.getAnnotationOfType(tearDownMethod.getAnnotations(), new JavaType("org.junit.AfterClass")), "Method 'tearDown' on '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "' must be annotated with @AfterClass");
		} else {
			// Add the method via the ITD
			List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
			annotations.add(new AnnotationMetadataBuilder(new JavaType("org.junit.AfterClass")));

			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine("helper.tearDown();");

			MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC | Modifier.STATIC, tearDownMethodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameters), new ArrayList<JavaSymbolName>(), bodyBuilder);
			methodBuilder.setAnnotations(annotations);
			builder.addMethod(methodBuilder.build());
		}
	}

	/**
	 * @return a test for the count method, if available and requested (may return null)
	 */
	public MethodMetadata getCountMethodTest() {
		if (!annotationValues.isCount() || countMethod == null) {
			// User does not want this method
			return null;
		}

		// Prepare method signature
		JavaSymbolName methodName = new JavaSymbolName("test" + StringUtils.capitalize(countMethod.getMethodName().getSymbolName()));
		List<JavaType> parameters = new ArrayList<JavaType>();

		MethodMetadata method = MemberFindingUtils.getMethod(governorTypeDetails, methodName, parameters);
		if (method == null) {
			List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
			annotations.add(new AnnotationMetadataBuilder(TEST));

			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine("org.junit.Assert.assertNotNull(\"Data on demand for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to initialize correctly\", dod." + dataOnDemandMetadata.getRandomPersistentEntityMethod().getMethodName().getSymbolName() + "());");
			bodyBuilder.appendFormalLine("long count = " + annotationValues.getEntity().getFullyQualifiedTypeName() + "." + countMethod.getMethodName() + "();");
			bodyBuilder.appendFormalLine("org.junit.Assert.assertTrue(\"Counter for '" + annotationValues.getEntity().getSimpleTypeName() + "' incorrectly reported there were no entries\", count > 0);");

			MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameters), new ArrayList<JavaSymbolName>(), bodyBuilder);
			methodBuilder.setAnnotations(annotations);
			method = methodBuilder.build();
		}

		return method;
	}
	
	/**
	 * @return a test for the find (by ID) method, if available and requested (may return null)
	 */
	public MethodMetadata getFindMethodTest() {
		if (!annotationValues.isFind() || findMethod == null) {
			// User does not want this method
			return null;
		}

		// Prepare method signature
		JavaSymbolName methodName = new JavaSymbolName("test" + StringUtils.capitalize(findMethod.getMethodName().getSymbolName()));
		List<JavaType> parameters = new ArrayList<JavaType>();
		
		MethodMetadata method = MemberFindingUtils.getMethod(governorTypeDetails, methodName, parameters);
		if (method == null) {
			List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
			annotations.add(new AnnotationMetadataBuilder(TEST));

			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine(annotationValues.getEntity().getFullyQualifiedTypeName() + " obj = dod." + dataOnDemandMetadata.getRandomPersistentEntityMethod().getMethodName().getSymbolName() + "();");
			bodyBuilder.appendFormalLine("org.junit.Assert.assertNotNull(\"Data on demand for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to initialize correctly\", obj);");
			bodyBuilder.appendFormalLine(identifierAccessorMethod.getReturnType().getFullyQualifiedTypeName() + " id = obj." + identifierAccessorMethod.getMethodName().getSymbolName() + "();");
			bodyBuilder.appendFormalLine("org.junit.Assert.assertNotNull(\"Data on demand for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to provide an identifier\", id);");
			bodyBuilder.appendFormalLine("obj = " + annotationValues.getEntity().getFullyQualifiedTypeName() + "." + findMethod.getMethodName().getSymbolName() + "(id);");
			bodyBuilder.appendFormalLine("org.junit.Assert.assertNotNull(\"Find method for '" + annotationValues.getEntity().getSimpleTypeName() + "' illegally returned null for id '\" + id + \"'\", obj);");
			bodyBuilder.appendFormalLine("org.junit.Assert.assertEquals(\"Find method for '" + annotationValues.getEntity().getSimpleTypeName() + "' returned the incorrect identifier\", id, obj." + identifierAccessorMethod.getMethodName().getSymbolName() + "());");

			MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameters), new ArrayList<JavaSymbolName>(), bodyBuilder);
			methodBuilder.setAnnotations(annotations);
			method = methodBuilder.build();
		}

		return method;
	}

	/**
	 * @return a test for the find all  method, if available and requested (may return null)
	 */
	public MethodMetadata getFindAllMethodTest() {
		if (!annotationValues.isFindAll() || findAllMethod == null || countMethod == null) {
			// User does not want this method, or core dependencies are missing
			return null;
		}

		// Prepare method signature
		JavaSymbolName methodName = new JavaSymbolName("test" + StringUtils.capitalize(findAllMethod.getMethodName().getSymbolName()));
		List<JavaType> parameters = new ArrayList<JavaType>();
		
		MethodMetadata method = MemberFindingUtils.getMethod(governorTypeDetails, methodName, parameters);
		if (method == null) {
			List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
			annotations.add(new AnnotationMetadataBuilder(TEST));

			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine("org.junit.Assert.assertNotNull(\"Data on demand for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to initialize correctly\", dod." + dataOnDemandMetadata.getRandomPersistentEntityMethod().getMethodName().getSymbolName() + "());");
			bodyBuilder.appendFormalLine("long count = " + annotationValues.getEntity().getFullyQualifiedTypeName() + "." + countMethod.getMethodName() + "();");
			bodyBuilder.appendFormalLine("org.junit.Assert.assertTrue(\"Too expensive to perform a find all test for '" + annotationValues.getEntity().getSimpleTypeName() + "', as there are \" + count + \" entries; set the findAllMaximum to exceed this value or set findAll=false on the integration test annotation to disable the test\", count < " + annotationValues.getFindAllMaximum() +");");
			bodyBuilder.appendFormalLine("java.util.List<" + annotationValues.getEntity().getFullyQualifiedTypeName() + "> result = " + annotationValues.getEntity().getFullyQualifiedTypeName() + "." + findAllMethod.getMethodName().getSymbolName() + "();");
			bodyBuilder.appendFormalLine("org.junit.Assert.assertNotNull(\"Find all method for '" + annotationValues.getEntity().getSimpleTypeName() + "' illegally returned null\", result);");
			bodyBuilder.appendFormalLine("org.junit.Assert.assertTrue(\"Find all method for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to return any data\", result.size() > 0);");

			MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameters), new ArrayList<JavaSymbolName>(), bodyBuilder);
			methodBuilder.setAnnotations(annotations);
			method = methodBuilder.build();
		}

		return method;
	}

	/**
	 * @return a test for the find entries method, if available and requested (may return null)
	 */
	public MethodMetadata getFindEntriesMethodTest() {
		if (!annotationValues.isFindEntries() || findEntriesMethod == null || countMethod == null) {
			// User does not want this method, or core dependencies are missing
			return null;
		}

		// Prepare method signature
		JavaSymbolName methodName = new JavaSymbolName("test" + StringUtils.capitalize(findEntriesMethod.getMethodName().getSymbolName()));
		List<JavaType> parameters = new ArrayList<JavaType>();
		
		MethodMetadata method = MemberFindingUtils.getMethod(governorTypeDetails, methodName, parameters);
		if (method == null) {
			List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
			annotations.add(new AnnotationMetadataBuilder(TEST));

			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine("org.junit.Assert.assertNotNull(\"Data on demand for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to initialize correctly\", dod." + dataOnDemandMetadata.getRandomPersistentEntityMethod().getMethodName().getSymbolName() + "());");
			bodyBuilder.appendFormalLine("long count = " + annotationValues.getEntity().getFullyQualifiedTypeName() + "." + countMethod.getMethodName() + "();");
			bodyBuilder.appendFormalLine("if (count > 20) count = 20;");
			bodyBuilder.appendFormalLine("java.util.List<" + annotationValues.getEntity().getFullyQualifiedTypeName() + "> result = " + annotationValues.getEntity().getFullyQualifiedTypeName() + "." + findEntriesMethod.getMethodName().getSymbolName() + "(0, (int) count);");
			bodyBuilder.appendFormalLine("org.junit.Assert.assertNotNull(\"Find entries method for '" + annotationValues.getEntity().getSimpleTypeName() + "' illegally returned null\", result);");
			bodyBuilder.appendFormalLine("org.junit.Assert.assertEquals(\"Find entries method for '" + annotationValues.getEntity().getSimpleTypeName() + "' returned an incorrect number of entries\", count, result.size());");

			MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameters), new ArrayList<JavaSymbolName>(), bodyBuilder);
			methodBuilder.setAnnotations(annotations);
			method = methodBuilder.build();
		}

		return method;
	}

	/**
	 * @return a test for the flush method, if available and requested (may return null)
	 */
	public MethodMetadata getFlushMethodTest() {
		if (!annotationValues.isFlush() || flushMethod == null || findMethod == null || versionAccessorMethod == null) {
			// User does not want this method, or core dependencies are missing
			return null;
		}

		// Prepare method signature
		JavaSymbolName methodName = new JavaSymbolName("test" + StringUtils.capitalize(flushMethod.getMethodName().getSymbolName()));
		List<JavaType> parameters = new ArrayList<JavaType>();
		
		MethodMetadata method = MemberFindingUtils.getMethod(governorTypeDetails, methodName, parameters);
		if (method == null) {
			List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
			annotations.add(new AnnotationMetadataBuilder(TEST));

			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine(annotationValues.getEntity().getFullyQualifiedTypeName() + " obj = dod." + dataOnDemandMetadata.getRandomPersistentEntityMethod().getMethodName().getSymbolName() + "();");
			bodyBuilder.appendFormalLine("org.junit.Assert.assertNotNull(\"Data on demand for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to initialize correctly\", obj);");
			bodyBuilder.appendFormalLine(identifierAccessorMethod.getReturnType().getFullyQualifiedTypeName() + " id = obj." + identifierAccessorMethod.getMethodName().getSymbolName() + "();");
			bodyBuilder.appendFormalLine("org.junit.Assert.assertNotNull(\"Data on demand for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to provide an identifier\", id);");
			bodyBuilder.appendFormalLine("obj = " + annotationValues.getEntity().getFullyQualifiedTypeName() + "." + findMethod.getMethodName().getSymbolName() + "(id);");
			bodyBuilder.appendFormalLine("org.junit.Assert.assertNotNull(\"Find method for '" + annotationValues.getEntity().getSimpleTypeName() + "' illegally returned null for id '\" + id + \"'\", obj);");
			bodyBuilder.appendFormalLine("boolean modified =  dod." + dataOnDemandMetadata.getModifyMethod().getMethodName().getSymbolName() + "(obj);");
			bodyBuilder.appendFormalLine(versionAccessorMethod.getReturnType().getFullyQualifiedTypeName() + " currentVersion = obj." + versionAccessorMethod.getMethodName().getSymbolName() + "();");
			bodyBuilder.appendFormalLine("obj." + flushMethod.getMethodName().getSymbolName() + "();");
			if (versionAccessorMethod.getReturnType().getFullyQualifiedTypeName().equals("java.util.Date")) {
				bodyBuilder.appendFormalLine("org.junit.Assert.assertTrue(\"Version for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to increment on flush directive\", (currentVersion != null && obj." + versionAccessorMethod.getMethodName().getSymbolName() + "().after(currentVersion)) || !modified);");
			} else {
				bodyBuilder.appendFormalLine("org.junit.Assert.assertTrue(\"Version for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to increment on flush directive\", (currentVersion != null && obj." + versionAccessorMethod.getMethodName().getSymbolName() + "() > currentVersion) || !modified);");
			}
			
			MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameters), new ArrayList<JavaSymbolName>(), bodyBuilder);
			methodBuilder.setAnnotations(annotations);
			method = methodBuilder.build();
		}

		return method;
	}

	/**
	 * @return a test for the merge method, if available and requested (may return null)
	 */
	public MethodMetadata getMergeMethodTest() {
		if (!annotationValues.isMerge() || mergeMethod == null || flushMethod == null || findMethod == null || versionAccessorMethod == null) {
			// User does not want this method, or core dependencies are missing
			return null;
		}

		// Prepare method signature
		JavaSymbolName methodName = new JavaSymbolName("test" + StringUtils.capitalize(mergeMethod.getMethodName().getSymbolName()));
		List<JavaType> parameters = new ArrayList<JavaType>();
		
		MethodMetadata method = MemberFindingUtils.getMethod(governorTypeDetails, methodName, parameters);
		if (method == null) {
			List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
			annotations.add(new AnnotationMetadataBuilder(TEST));
	
			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine(annotationValues.getEntity().getFullyQualifiedTypeName() + " obj = dod." + dataOnDemandMetadata.getRandomPersistentEntityMethod().getMethodName().getSymbolName() + "();");
			bodyBuilder.appendFormalLine("org.junit.Assert.assertNotNull(\"Data on demand for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to initialize correctly\", obj);");
			bodyBuilder.appendFormalLine(identifierAccessorMethod.getReturnType().getFullyQualifiedTypeName() + " id = obj." + identifierAccessorMethod.getMethodName().getSymbolName() + "();");
			bodyBuilder.appendFormalLine("org.junit.Assert.assertNotNull(\"Data on demand for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to provide an identifier\", id);");
			bodyBuilder.appendFormalLine("obj = " + annotationValues.getEntity().getFullyQualifiedTypeName() + "." + findMethod.getMethodName().getSymbolName() + "(id);");
			bodyBuilder.appendFormalLine("boolean modified =  dod." + dataOnDemandMetadata.getModifyMethod().getMethodName().getSymbolName() + "(obj);");
			bodyBuilder.appendFormalLine(versionAccessorMethod.getReturnType().getFullyQualifiedTypeName() + " currentVersion = obj." + versionAccessorMethod.getMethodName().getSymbolName() + "();");
			bodyBuilder.appendFormalLine(annotationValues.getEntity().getFullyQualifiedTypeName() + " merged = (" + annotationValues.getEntity().getFullyQualifiedTypeName() + ") obj." + mergeMethod.getMethodName().getSymbolName() + "();");
			bodyBuilder.appendFormalLine("obj." + flushMethod.getMethodName().getSymbolName() + "();");
			bodyBuilder.appendFormalLine("org.junit.Assert.assertEquals(\"Identifier of merged object not the same as identifier of original object\", merged." +  identifierAccessorMethod.getMethodName().getSymbolName() + "(), id);");
			if (versionAccessorMethod.getReturnType().getFullyQualifiedTypeName().equals("java.util.Date")) {
				bodyBuilder.appendFormalLine("org.junit.Assert.assertTrue(\"Version for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to increment on merge and flush directive\", (currentVersion != null && obj." + versionAccessorMethod.getMethodName().getSymbolName() + "().after(currentVersion)) || !modified);");
			} else {
				bodyBuilder.appendFormalLine("org.junit.Assert.assertTrue(\"Version for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to increment on merge and flush directive\", (currentVersion != null && obj." + versionAccessorMethod.getMethodName().getSymbolName() + "() > currentVersion) || !modified);");
			}

			MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameters), new ArrayList<JavaSymbolName>(), bodyBuilder);
			methodBuilder.setAnnotations(annotations);
			method = methodBuilder.build();
		}

		return method;
	}

	/**
	 * @return a test for the persist method, if available and requested (may return null)
	 */
	public MethodMetadata getPersistMethodTest() {
		if (!annotationValues.isPersist() || persistMethod == null) {
			// User does not want this method
			return null;
		}

		// Prepare method signature
		JavaSymbolName methodName = new JavaSymbolName("test" + StringUtils.capitalize(persistMethod.getMethodName().getSymbolName()));
		List<JavaType> parameters = new ArrayList<JavaType>();
		
		MethodMetadata method = MemberFindingUtils.getMethod(governorTypeDetails, methodName, parameters);
		if (method == null) {
			List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
			annotations.add(new AnnotationMetadataBuilder(TEST));
			
			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine("org.junit.Assert.assertNotNull(\"Data on demand for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to initialize correctly\", dod." + dataOnDemandMetadata.getRandomPersistentEntityMethod().getMethodName().getSymbolName() + "());");
			bodyBuilder.appendFormalLine(annotationValues.getEntity().getFullyQualifiedTypeName() + " obj = dod." + dataOnDemandMetadata.getNewTransientEntityMethod().getMethodName().getSymbolName() + "(Integer.MAX_VALUE);");
			bodyBuilder.appendFormalLine("org.junit.Assert.assertNotNull(\"Data on demand for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to provide a new transient entity\", obj);");
			bodyBuilder.appendFormalLine("org.junit.Assert.assertNull(\"Expected '" + annotationValues.getEntity().getSimpleTypeName() + "' identifier to be null\", obj." + identifierAccessorMethod.getMethodName().getSymbolName()  + "());");
			bodyBuilder.appendFormalLine("obj." + persistMethod.getMethodName().getSymbolName() + "();");
			bodyBuilder.appendFormalLine("obj." + flushMethod.getMethodName().getSymbolName() + "();");
			bodyBuilder.appendFormalLine("org.junit.Assert.assertNotNull(\"Expected '" + annotationValues.getEntity().getSimpleTypeName() + "' identifier to no longer be null\", obj." + identifierAccessorMethod.getMethodName().getSymbolName()  + "());");

			MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameters), new ArrayList<JavaSymbolName>(), bodyBuilder);
			methodBuilder.setAnnotations(annotations);
			method = methodBuilder.build();
		}

		return method;
	}
	
	/**
	 * @return a test for the persist method, if available and requested (may return null)
	 */
	public MethodMetadata getRemoveMethodTest() {
		if (!annotationValues.isRemove() || findMethod == null || removeMethod == null) {
			// User does not want this method or one of its core dependencies
			return null;
		}

		// Prepare method signature
		JavaSymbolName methodName = new JavaSymbolName("test" + StringUtils.capitalize(removeMethod.getMethodName().getSymbolName()));
		List<JavaType> parameters = new ArrayList<JavaType>();
		
		MethodMetadata method = MemberFindingUtils.getMethod(governorTypeDetails, methodName, parameters);
		if (method == null) {
			List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
			annotations.add(new AnnotationMetadataBuilder(TEST));
			if (isGaeSupported) {
				List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
				attributes.add(new EnumAttributeValue(new JavaSymbolName("propagation"), new EnumDetails(new JavaType("org.springframework.transaction.annotation.Propagation"), new JavaSymbolName("SUPPORTS"))));
				annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.transaction.annotation.Transactional"), attributes));
			}
			
			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine(annotationValues.getEntity().getFullyQualifiedTypeName() + " obj = dod." + dataOnDemandMetadata.getRandomPersistentEntityMethod().getMethodName().getSymbolName() + "();");
			bodyBuilder.appendFormalLine("org.junit.Assert.assertNotNull(\"Data on demand for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to initialize correctly\", obj);");
			bodyBuilder.appendFormalLine(identifierAccessorMethod.getReturnType().getFullyQualifiedTypeName() + " id = obj." + identifierAccessorMethod.getMethodName().getSymbolName() + "();");
			bodyBuilder.appendFormalLine("org.junit.Assert.assertNotNull(\"Data on demand for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to provide an identifier\", id);");
			bodyBuilder.appendFormalLine("obj = " + annotationValues.getEntity().getFullyQualifiedTypeName() + "." + findMethod.getMethodName().getSymbolName() + "(id);");
			bodyBuilder.appendFormalLine("obj." + removeMethod.getMethodName().getSymbolName() + "();");
			bodyBuilder.appendFormalLine("obj." + flushMethod.getMethodName().getSymbolName() + "();");
			bodyBuilder.appendFormalLine("org.junit.Assert.assertNull(\"Failed to remove '" + annotationValues.getEntity().getSimpleTypeName() + "' with identifier '\" + id + \"'\", " + annotationValues.getEntity().getFullyQualifiedTypeName() + "." + findMethod.getMethodName().getSymbolName() + "(id));");

			MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameters), new ArrayList<JavaSymbolName>(), bodyBuilder);
			methodBuilder.setAnnotations(annotations);
			method = methodBuilder.build();
		}

		return method;
	}

	/**
	 * @return the annotation values specified via {@link RooIntegrationTest} (never null unless the metadata itself is invalid)
	 */
	public IntegrationTestAnnotationValues getAnnotationValues() {
		return annotationValues;
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("identifier", getId());
		tsc.append("valid", valid);
		tsc.append("aspectName", aspectName);
		tsc.append("destinationType", destination);
		tsc.append("governor", governorPhysicalTypeMetadata.getId());
		tsc.append("itdTypeDetails", itdTypeDetails);
		return tsc.toString();
	}

	public static final String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}
	
	public static final String createIdentifier(JavaType javaType, Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static final JavaType getJavaType(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static final Path getPath(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
}
