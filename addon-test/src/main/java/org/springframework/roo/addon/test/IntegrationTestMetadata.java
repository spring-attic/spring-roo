package org.springframework.roo.addon.test;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.addon.dod.DataOnDemandMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.DefaultFieldMetadata;
import org.springframework.roo.classpath.details.DefaultMethodMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for {@link RooIntegrationTest}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class IntegrationTestMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

	private static final String PROVIDES_TYPE_STRING = IntegrationTestMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);

	private IntegrationTestAnnotationValues annotationValues;
	private DataOnDemandMetadata dataOnDemandMetadata;
	private JavaType dodGovernor;
	
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
	
	public IntegrationTestMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, IntegrationTestAnnotationValues annotationValues, DataOnDemandMetadata dataOnDemandMetadata, MethodMetadata identifierAccessorMethod, MethodMetadata versionAccessorMethod, MethodMetadata countMethod, MethodMetadata findMethod, MethodMetadata findAllMethod, MethodMetadata findEntriesMethod, MethodMetadata flushMethod, MethodMetadata mergeMethod, MethodMetadata persistMethod, MethodMetadata removeMethod) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
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
		if (MemberFindingUtils.getAnnotationOfType(governorTypeDetails.getTypeAnnotations(), new JavaType("org.junit.runner.RunWith")) == null) {
			List<AnnotationAttributeValue<?>> runWithAttributes = new ArrayList<AnnotationAttributeValue<?>>();
			runWithAttributes.add(new ClassAttributeValue(new JavaSymbolName("value"), new JavaType("org.springframework.test.context.junit4.SpringJUnit4ClassRunner")));
			builder.addTypeAnnotation(new DefaultAnnotationMetadata(new JavaType("org.junit.runner.RunWith"), runWithAttributes));
		}
		
		// Add an @ContextConfiguration("classpath:/applicationContext.xml") annotation to the type, if the user did not define it on the governor directly
		if (MemberFindingUtils.getAnnotationOfType(governorTypeDetails.getTypeAnnotations(), new JavaType("org.springframework.test.context.ContextConfiguration")) == null) {
			List<AnnotationAttributeValue<?>> ctxCfg = new ArrayList<AnnotationAttributeValue<?>>();
			ctxCfg.add(new StringAttributeValue(new JavaSymbolName("locations"), "classpath:/META-INF/spring/applicationContext.xml"));
			builder.addTypeAnnotation(new DefaultAnnotationMetadata(new JavaType("org.springframework.test.context.ContextConfiguration"), ctxCfg));
		}
	
		// Add the data on demand field if the user did not define it on the governor directly
		FieldMetadata field = MemberFindingUtils.getField(governorTypeDetails, new JavaSymbolName("dod"));
		if (field != null) {
			Assert.isTrue(field.getFieldType().equals(dodGovernor), "Field 'dod' on '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "' must be of type '" + dodGovernor.getFullyQualifiedTypeName() + "'");
			Assert.notNull(MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("org.springframework.beans.factory.annotation.Autowired")), "Field 'dod' on '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "' must be annotated with @Autowired");
		} else {
			// Add the field via the ITD
			List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
			annotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.beans.factory.annotation.Autowired"), new ArrayList<AnnotationAttributeValue<?>>()));
			builder.addField(new DefaultFieldMetadata(getId(), Modifier.PRIVATE, new JavaSymbolName("dod"), dodGovernor, null, annotations));
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
			List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
			annotations.add(new DefaultAnnotationMetadata(new JavaType("org.junit.Test"), new ArrayList<AnnotationAttributeValue<?>>()));
			
			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine("junit.framework.Assert.assertNotNull(\"Data on demand for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to initialize correctly\", dod." + dataOnDemandMetadata.getRandomPersistentEntityMethod().getMethodName().getSymbolName() + "());");
			bodyBuilder.appendFormalLine("long count = " + annotationValues.getEntity().getFullyQualifiedTypeName() + "." + countMethod.getMethodName() + "();");
			bodyBuilder.appendFormalLine("junit.framework.Assert.assertTrue(\"Counter for '" + annotationValues.getEntity().getSimpleTypeName() + "' incorrectly reported there were no entries\", count > 0);");

			method = new DefaultMethodMetadata(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameters), new ArrayList<JavaSymbolName>(), annotations, bodyBuilder.getOutput());
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
			List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
			annotations.add(new DefaultAnnotationMetadata(new JavaType("org.junit.Test"), new ArrayList<AnnotationAttributeValue<?>>()));

			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine("junit.framework.Assert.assertNotNull(\"Data on demand for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to initialize correctly\", dod." + dataOnDemandMetadata.getRandomPersistentEntityMethod().getMethodName().getSymbolName() + "());");
			bodyBuilder.appendFormalLine(identifierAccessorMethod.getReturnType().getFullyQualifiedTypeName() + " id = dod." + dataOnDemandMetadata.getRandomPersistentEntityMethod().getMethodName().getSymbolName() + "()." + identifierAccessorMethod.getMethodName().getSymbolName() + "();");
			bodyBuilder.appendFormalLine("junit.framework.Assert.assertNotNull(\"Data on demand for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to provide an identifier\", id);");
			bodyBuilder.appendFormalLine(annotationValues.getEntity().getFullyQualifiedTypeName() + " obj = " + annotationValues.getEntity().getFullyQualifiedTypeName() + "." + findMethod.getMethodName().getSymbolName() + "(id);");
			bodyBuilder.appendFormalLine("junit.framework.Assert.assertNotNull(\"Find method for '" + annotationValues.getEntity().getSimpleTypeName() + "' illegally returned null for id '\" + id + \"'\", obj);");
			bodyBuilder.appendFormalLine("junit.framework.Assert.assertEquals(\"Find method for '" + annotationValues.getEntity().getSimpleTypeName() + "' returned the incorrect identifier\", id, obj." + identifierAccessorMethod.getMethodName().getSymbolName() + "());");

			method = new DefaultMethodMetadata(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameters), new ArrayList<JavaSymbolName>(), annotations, bodyBuilder.getOutput());
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
			List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
			annotations.add(new DefaultAnnotationMetadata(new JavaType("org.junit.Test"), new ArrayList<AnnotationAttributeValue<?>>()));

			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine("junit.framework.Assert.assertNotNull(\"Data on demand for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to initialize correctly\", dod." + dataOnDemandMetadata.getRandomPersistentEntityMethod().getMethodName().getSymbolName() + "());");
			bodyBuilder.appendFormalLine("long count = " + annotationValues.getEntity().getFullyQualifiedTypeName() + "." + countMethod.getMethodName() + "();");
			bodyBuilder.appendFormalLine("junit.framework.Assert.assertTrue(\"Too expensive to perform a find all test for '" + annotationValues.getEntity().getSimpleTypeName() + "', as there are \" + count + \" entries; set the findAllMaximum to exceed this value or set findAll=false on the integration test annotation to disable the test\", count < 250);");
			bodyBuilder.appendFormalLine("java.util.List<" + annotationValues.getEntity().getFullyQualifiedTypeName() + "> result = " + annotationValues.getEntity().getFullyQualifiedTypeName() + "." + findAllMethod.getMethodName().getSymbolName() + "();");
			bodyBuilder.appendFormalLine("junit.framework.Assert.assertNotNull(\"Find all method for '" + annotationValues.getEntity().getSimpleTypeName() + "' illegally returned null\", result);");
			bodyBuilder.appendFormalLine("junit.framework.Assert.assertTrue(\"Find all method for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to return any data\", result.size() > 0);");

			method = new DefaultMethodMetadata(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameters), new ArrayList<JavaSymbolName>(), annotations, bodyBuilder.getOutput());
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
			List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
			annotations.add(new DefaultAnnotationMetadata(new JavaType("org.junit.Test"), new ArrayList<AnnotationAttributeValue<?>>()));

			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine("junit.framework.Assert.assertNotNull(\"Data on demand for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to initialize correctly\", dod." + dataOnDemandMetadata.getRandomPersistentEntityMethod().getMethodName().getSymbolName() + "());");
			bodyBuilder.appendFormalLine("long count = " + annotationValues.getEntity().getFullyQualifiedTypeName() + "." + countMethod.getMethodName() + "();");
			bodyBuilder.appendFormalLine("if (count > 20) count = 20;");
			bodyBuilder.appendFormalLine("java.util.List<" + annotationValues.getEntity().getFullyQualifiedTypeName() + "> result = " + annotationValues.getEntity().getFullyQualifiedTypeName() + "." + findEntriesMethod.getMethodName().getSymbolName() + "(0, (int)count);");
			bodyBuilder.appendFormalLine("junit.framework.Assert.assertNotNull(\"Find entries method for '" + annotationValues.getEntity().getSimpleTypeName() + "' illegally returned null\", result);");
			bodyBuilder.appendFormalLine("junit.framework.Assert.assertEquals(\"Find entries method for '" + annotationValues.getEntity().getSimpleTypeName() + "' returned an incorrect number of entries\", count, result.size());");

			method = new DefaultMethodMetadata(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameters), new ArrayList<JavaSymbolName>(), annotations, bodyBuilder.getOutput());
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
			List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
			annotations.add(new DefaultAnnotationMetadata(new JavaType("org.junit.Test"), new ArrayList<AnnotationAttributeValue<?>>()));
			annotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.transaction.annotation.Transactional"), new ArrayList<AnnotationAttributeValue<?>>()));

			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine("junit.framework.Assert.assertNotNull(\"Data on demand for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to initialize correctly\", dod." + dataOnDemandMetadata.getRandomPersistentEntityMethod().getMethodName().getSymbolName() + "());");
			bodyBuilder.appendFormalLine(identifierAccessorMethod.getReturnType().getFullyQualifiedTypeName() + " id = dod." + dataOnDemandMetadata.getRandomPersistentEntityMethod().getMethodName().getSymbolName() + "()." + identifierAccessorMethod.getMethodName().getSymbolName() + "();");
			bodyBuilder.appendFormalLine("junit.framework.Assert.assertNotNull(\"Data on demand for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to provide an identifier\", id);");
			bodyBuilder.appendFormalLine(annotationValues.getEntity().getFullyQualifiedTypeName() + " obj = " + annotationValues.getEntity().getFullyQualifiedTypeName() + "." + findMethod.getMethodName().getSymbolName() + "(id);");
			bodyBuilder.appendFormalLine("junit.framework.Assert.assertNotNull(\"Find method for '" + annotationValues.getEntity().getSimpleTypeName() + "' illegally returned null for id '\" + id + \"'\", obj);");
			bodyBuilder.appendFormalLine("boolean modified =  dod." + dataOnDemandMetadata.getModifyMethod().getMethodName().getSymbolName() + "(obj);");
			bodyBuilder.appendFormalLine(versionAccessorMethod.getReturnType().getFullyQualifiedTypeName() + " currentVersion = obj." + versionAccessorMethod.getMethodName().getSymbolName() + "();");
			bodyBuilder.appendFormalLine("obj." + flushMethod.getMethodName().getSymbolName() + "();");
			if (versionAccessorMethod.getReturnType().getFullyQualifiedTypeName().equals("java.util.Date")) {
				bodyBuilder.appendFormalLine("junit.framework.Assert.assertTrue(\"Version for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to increment on flush directive\", obj." + versionAccessorMethod.getMethodName().getSymbolName() + "().after(currentVersion) || !modified);");
			} else {
				bodyBuilder.appendFormalLine("junit.framework.Assert.assertTrue(\"Version for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to increment on flush directive\", obj." + versionAccessorMethod.getMethodName().getSymbolName() + "() > currentVersion || !modified);");
			}
			method = new DefaultMethodMetadata(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameters), new ArrayList<JavaSymbolName>(), annotations, bodyBuilder.getOutput());
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
			List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
			annotations.add(new DefaultAnnotationMetadata(new JavaType("org.junit.Test"), new ArrayList<AnnotationAttributeValue<?>>()));
			annotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.transaction.annotation.Transactional"), new ArrayList<AnnotationAttributeValue<?>>()));
	
			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine("junit.framework.Assert.assertNotNull(\"Data on demand for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to initialize correctly\", dod." + dataOnDemandMetadata.getRandomPersistentEntityMethod().getMethodName().getSymbolName() + "());");
			bodyBuilder.appendFormalLine(identifierAccessorMethod.getReturnType().getFullyQualifiedTypeName() + " id = dod." + dataOnDemandMetadata.getRandomPersistentEntityMethod().getMethodName().getSymbolName() + "()." + identifierAccessorMethod.getMethodName().getSymbolName() + "();");
			bodyBuilder.appendFormalLine("junit.framework.Assert.assertNotNull(\"Data on demand for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to provide an identifier\", id);");
			bodyBuilder.appendFormalLine(annotationValues.getEntity().getFullyQualifiedTypeName() + " obj = " + annotationValues.getEntity().getFullyQualifiedTypeName() + "." + findMethod.getMethodName().getSymbolName() + "(id);");
			bodyBuilder.appendFormalLine("junit.framework.Assert.assertNotNull(\"Find method for '" + annotationValues.getEntity().getSimpleTypeName() + "' illegally returned null for id '\" + id + \"'\", obj);");
			bodyBuilder.appendFormalLine("boolean modified =  dod." + dataOnDemandMetadata.getModifyMethod().getMethodName().getSymbolName() + "(obj);");
			bodyBuilder.appendFormalLine(versionAccessorMethod.getReturnType().getFullyQualifiedTypeName() + " currentVersion = obj." + versionAccessorMethod.getMethodName().getSymbolName() + "();");
			bodyBuilder.appendFormalLine("obj." + mergeMethod.getMethodName().getSymbolName() + "();");
			bodyBuilder.appendFormalLine("obj." + flushMethod.getMethodName().getSymbolName() + "();");
			if (versionAccessorMethod.getReturnType().getFullyQualifiedTypeName().equals("java.util.Date")) {
				bodyBuilder.appendFormalLine("junit.framework.Assert.assertTrue(\"Version for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to increment on merge and flush directive\", obj." + versionAccessorMethod.getMethodName().getSymbolName() + "().after(currentVersion) || !modified);");
			} else {
				bodyBuilder.appendFormalLine("junit.framework.Assert.assertTrue(\"Version for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to increment on merge and flush directive\", obj." + versionAccessorMethod.getMethodName().getSymbolName() + "() > currentVersion || !modified);");
			}

			method = new DefaultMethodMetadata(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameters), new ArrayList<JavaSymbolName>(), annotations, bodyBuilder.getOutput());
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
			List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
			annotations.add(new DefaultAnnotationMetadata(new JavaType("org.junit.Test"), new ArrayList<AnnotationAttributeValue<?>>()));
			annotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.transaction.annotation.Transactional"), new ArrayList<AnnotationAttributeValue<?>>()));
			
			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine("junit.framework.Assert.assertNotNull(\"Data on demand for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to initialize correctly\", dod." + dataOnDemandMetadata.getRandomPersistentEntityMethod().getMethodName().getSymbolName() + "());");
			bodyBuilder.appendFormalLine(annotationValues.getEntity().getFullyQualifiedTypeName() + " obj = dod." + dataOnDemandMetadata.getNewTransientEntityMethod().getMethodName().getSymbolName() + "(Integer.MAX_VALUE);");
			bodyBuilder.appendFormalLine("junit.framework.Assert.assertNotNull(\"Data on demand for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to provide a new transient entity\", obj);");
			bodyBuilder.appendFormalLine("junit.framework.Assert.assertNull(\"Expected '" + annotationValues.getEntity().getSimpleTypeName() + "' identifier to be null\", obj." + identifierAccessorMethod.getMethodName().getSymbolName()  + "());");
			bodyBuilder.appendFormalLine("obj." + persistMethod.getMethodName().getSymbolName() + "();");
			bodyBuilder.appendFormalLine("obj." + flushMethod.getMethodName().getSymbolName() + "();");
			bodyBuilder.appendFormalLine("junit.framework.Assert.assertNotNull(\"Expected '" + annotationValues.getEntity().getSimpleTypeName() + "' identifier to no longer be null\", obj." + identifierAccessorMethod.getMethodName().getSymbolName()  + "());");

			method = new DefaultMethodMetadata(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameters), new ArrayList<JavaSymbolName>(), annotations, bodyBuilder.getOutput());
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
			List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
			annotations.add(new DefaultAnnotationMetadata(new JavaType("org.junit.Test"), new ArrayList<AnnotationAttributeValue<?>>()));
			annotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.transaction.annotation.Transactional"), new ArrayList<AnnotationAttributeValue<?>>()));
/*			
		        junit.framework.Assert.assertNotNull("Data on demand for 'PetType' failed to initialize correctly", petTypeDataOnDemand.getRandomPersistentEntity());        
        java.lang.Long id = petTypeDataOnDemand.getRandomPersistentEntity().getId();        
        junit.framework.Assert.assertNotNull("Data on demand for 'PetType' failed to provide an identifier", id);        
        PetType.findPetType(id).remove();        
        junit.framework.Assert.assertNull("Failed to remove 'PetType' with identifier '" + id + "'", PetType.findPetType(id));        
	
	*/		
			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine("junit.framework.Assert.assertNotNull(\"Data on demand for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to initialize correctly\", dod." + dataOnDemandMetadata.getRandomPersistentEntityMethod().getMethodName().getSymbolName() + "());");
			bodyBuilder.appendFormalLine(identifierAccessorMethod.getReturnType().getFullyQualifiedTypeName() + " id = dod." + dataOnDemandMetadata.getRandomPersistentEntityMethod().getMethodName().getSymbolName() + "()." + identifierAccessorMethod.getMethodName().getSymbolName() + "();");
			bodyBuilder.appendFormalLine("junit.framework.Assert.assertNotNull(\"Data on demand for '" + annotationValues.getEntity().getSimpleTypeName() + "' failed to provide an identifier\", id);");
			bodyBuilder.appendFormalLine(annotationValues.getEntity().getFullyQualifiedTypeName() + " obj = " + annotationValues.getEntity().getFullyQualifiedTypeName() + "." + findMethod.getMethodName().getSymbolName() + "(id);");
			bodyBuilder.appendFormalLine("junit.framework.Assert.assertNotNull(\"Find method for '" + annotationValues.getEntity().getSimpleTypeName() + "' illegally returned null for id '\" + id + \"'\", obj);");
			bodyBuilder.appendFormalLine("obj." + removeMethod.getMethodName().getSymbolName() + "();");
			bodyBuilder.appendFormalLine("junit.framework.Assert.assertNull(\"Failed to remove '" + annotationValues.getEntity().getSimpleTypeName() + "' with identifier '\" + id + \"'\", " + annotationValues.getEntity().getFullyQualifiedTypeName() + "." + findMethod.getMethodName().getSymbolName() + "(id));");

			method = new DefaultMethodMetadata(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameters), new ArrayList<JavaSymbolName>(), annotations, bodyBuilder.getOutput());
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
