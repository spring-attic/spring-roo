package org.springframework.roo.addon.dod;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for {@link RooDataOnDemand}.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @since 1.0
 */
public class DataOnDemandMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	private static final String PROVIDES_TYPE_STRING = DataOnDemandMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private static final JavaType MAX = new JavaType("javax.validation.constraints.Max");
	private static final JavaType MIN = new JavaType("javax.validation.constraints.Min");
	private static final JavaType SIZE = new JavaType("javax.validation.constraints.Size");
	private static final JavaType COLUMN = new JavaType("javax.persistence.Column");
	private static final JavaType NOT_NULL = new JavaType("javax.validation.constraints.NotNull");

	private DataOnDemandAnnotationValues annotationValues;
	private BeanInfoMetadata beanInfoMetadata;
	private MethodMetadata identifierAccessorMethod;
	private MethodMetadata findMethod;

	/** The "persist():void" instance method for the entity we are to create (required) */
	private MethodMetadata persistMethod;
	
	/** The "flush():void" instance method for the entity we are to create (required) */
	private MethodMetadata flushMethod;

	/** Mandatory methods, in order of discovery (so we can guarantee the ITD is generated in a consistent manner for SCM compatibility) */
	private List<MethodMetadata> mandatoryMutators = new ArrayList<MethodMetadata>();

	/** key: mandatory setter to invoke; value: the argument to present to the mutator method, expressed as a string */
	private Map<MethodMetadata, String> mutatorArguments = new HashMap<MethodMetadata, String>();

	/** Other entities requiring a data on demand instance; fields must exist for each of these in the class */
	private List<JavaType> requiredDataOnDemandCollaborators = new ArrayList<JavaType>();

	// Needed to lookup other DataOnDemand metadata we depend on
	private MetadataService metadataService;
	private MetadataDependencyRegistry metadataDependencyRegistry;

	public DataOnDemandMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, DataOnDemandAnnotationValues annotationValues, BeanInfoMetadata beanInfoMetadata, MethodMetadata identifierAccessor, MethodMetadata findMethod, MethodMetadata findEntriesMethod, MethodMetadata persistMethod, MethodMetadata flushMethod, MetadataService metadataService, MetadataDependencyRegistry metadataDependencyRegistry) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.notNull(beanInfoMetadata, "Bean info metadata required");
		Assert.notNull(identifierAccessor, "Identifier accessor method required");
		Assert.notNull(findMethod, "Find method required");
		Assert.notNull(findEntriesMethod, "Find entries method required");
		Assert.notNull(persistMethod, "Persist method required");
		Assert.notNull(flushMethod, "Flush method required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(metadataDependencyRegistry, "Metadata dependency registry required");

		if (!isValid()) {
			return;
		}

		this.annotationValues = annotationValues;
		this.beanInfoMetadata = beanInfoMetadata;
		this.identifierAccessorMethod = identifierAccessor;
		this.findMethod = findMethod;
		this.persistMethod = persistMethod;
		this.flushMethod = flushMethod;
		this.metadataService = metadataService;
		this.metadataDependencyRegistry = metadataDependencyRegistry;

		mutatorDiscovery();

		if (isComponentAnnotationIntroduced()) {
			builder.addAnnotation(getComponentAnnotation());
		}

		builder.addField(getRndField());
		builder.addField(getDataField());

		Set<JavaSymbolName> fieldsAddedToItd = new HashSet<JavaSymbolName>();
		for (JavaType entityNeedingCollaborator : requiredDataOnDemandCollaborators) {
			JavaType collaboratorType = getCollaboratingType(entityNeedingCollaborator);
			String collaboratingFieldName = getCollaboratingFieldName(entityNeedingCollaborator).getSymbolName();

			JavaSymbolName fieldSymbolName = new JavaSymbolName(collaboratingFieldName);
			FieldMetadata candidate = MemberFindingUtils.getField(governorTypeDetails, fieldSymbolName);
			if (candidate != null) {
				// We really expect the field to be correct if we're going to rely on it
				Assert.isTrue(candidate.getFieldType().equals(collaboratorType), "Field '" + collaboratingFieldName + "' on '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "' must be of type '" + collaboratorType.getFullyQualifiedTypeName() + "'");
				Assert.isTrue(Modifier.isPrivate(candidate.getModifier()), "Field '" + collaboratingFieldName + "' on '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "' must be private");
				Assert.notNull(MemberFindingUtils.getAnnotationOfType(candidate.getAnnotations(), new JavaType("org.springframework.beans.factory.annotation.Autowired")), "Field '" + collaboratingFieldName + "' on '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "' must be @Autowired");
				// It's ok, so we can move onto the new field
				continue;
			}

			// Must make the field
			List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
			annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.beans.factory.annotation.Autowired")));
			FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations, fieldSymbolName, collaboratorType);
			FieldMetadata field = fieldBuilder.build();

			// Add it to the ITD, if it hasn't already been
			if (!fieldsAddedToItd.contains(field.getFieldName())) {
				fieldsAddedToItd.add(field.getFieldName());
				builder.addField(field);
				fieldsAddedToItd.add(field.getFieldName());
			}
		}

		builder.addMethod(getNewTransientEntityMethod());
		builder.addMethod(getSpecificPersistentEntityMethod());
		builder.addMethod(getRandomPersistentEntityMethod());
		builder.addMethod(getModifyMethod());
		builder.addMethod(getInitMethod());

		itdTypeDetails = builder.build();
	}

	/**
	 * Adds the @org.springframework.stereotype.Component annotation to the type, unless it already exists.
	 * 
	 * @return the annotation is already exists or will be created, or null if it will not be created (required)
	 */
	public AnnotationMetadata getComponentAnnotation() {
		JavaType javaType = new JavaType("org.springframework.stereotype.Component");
		if (isComponentAnnotationIntroduced()) {
			AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(javaType);
			return annotationBuilder.build();
		}
		return MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, javaType);
	}

	/**
	 * Indicates whether the @org.springframework.stereotype.Component annotation will be introduced via this ITD.
	 * 
	 * @return true if it will be introduced, false otherwise
	 */
	public boolean isComponentAnnotationIntroduced() {
		JavaType javaType = new JavaType("org.springframework.stereotype.Component");
		AnnotationMetadata result = MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, javaType);
		return result == null;
	}

	/**
	 * @return the "rnd" field to use, which is either provided by the user or produced on demand (never returns null)
	 */
	public FieldMetadata getRndField() {
		int index = -1;
		while (true) {
			// Compute the required field name
			index++;
			String fieldName = "";
			for (int i = 0; i < index; i++) {
				fieldName = fieldName + "_";
			}
			fieldName = fieldName + "rnd";

			JavaSymbolName fieldSymbolName = new JavaSymbolName(fieldName);
			FieldMetadata candidate = MemberFindingUtils.getField(governorTypeDetails, fieldSymbolName);
			if (candidate != null) {
				// Verify if candidate is suitable
				if (!Modifier.isPrivate(candidate.getModifier())) {
					// Candidate is not private, so we might run into naming clashes if someone subclasses this (therefore go onto the next possible name)
					continue;
				}

				if (!candidate.getFieldType().equals(new JavaType("java.util.Random"))) {
					// Candidate isn't a java.util.Random, so it isn't suitable
					continue;
				}

				// If we got this far, we found a valid candidate
				// We don't check if there is a corresponding initializer, but we assume the user knows what they're doing and have made one
				return candidate;
			}

			// Candidate not found, so let's create one
			FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId());
			fieldBuilder.setModifier(Modifier.PRIVATE);
			fieldBuilder.setFieldName(fieldSymbolName);
			fieldBuilder.setFieldType(new JavaType("java.util.Random"));
			fieldBuilder.setFieldInitializer("new java.security.SecureRandom()");
			return fieldBuilder.build();
		}
	}

	/**
	 * @return the "data" field to use, which is either provided by the user or produced on demand (never returns null)
	 */
	public FieldMetadata getDataField() {
		int index = -1;
		while (true) {
			// Compute the required field name
			index++;
			String fieldName = "";
			for (int i = 0; i < index; i++) {
				fieldName = fieldName + "_";
			}
			fieldName = fieldName + "data";

			// The type parameters to be used by the field type
			List<JavaType> typeParams = new ArrayList<JavaType>();
			typeParams.add(annotationValues.getEntity());

			JavaSymbolName fieldSymbolName = new JavaSymbolName(fieldName);
			FieldMetadata candidate = MemberFindingUtils.getField(governorTypeDetails, fieldSymbolName);
			if (candidate != null) {
				// Verify if candidate is suitable
				if (!Modifier.isPrivate(candidate.getModifier())) {
					// Candidate is not private, so we might run into naming clashes if someone subclasses this (therefore go onto the next possible name)
					continue;
				}

				if (!candidate.getFieldType().equals(new JavaType("java.util.List", 0, DataType.TYPE, null, typeParams))) {
					// Candidate isn't a java.util.List<theEntity>, so it isn't suitable
					// The equals method also verifies type params are present
					continue;
				}

				// If we got this far, we found a valid candidate
				// We don't check if there is a corresponding initializer, but we assume the user knows what they're doing and have made one
				return candidate;
			}

			// Candidate not found, so let's create one
			FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId());
			fieldBuilder.setModifier(Modifier.PRIVATE);
			fieldBuilder.setFieldName(fieldSymbolName);
			fieldBuilder.setFieldType(new JavaType("java.util.List", 0, DataType.TYPE, null, typeParams));
			return fieldBuilder.build();
		}
	}

	/**
	 * @return the "getNewTransientEntity(int index):Entity" method (never returns null)
	 */
	public MethodMetadata getNewTransientEntityMethod() {
		// Method definition to find or build
		JavaSymbolName methodName = new JavaSymbolName("getNewTransient" + beanInfoMetadata.getJavaBean().getSimpleTypeName());
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		paramTypes.add(JavaType.INT_PRIMITIVE);
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("index"));
		JavaType returnType = beanInfoMetadata.getJavaBean();

		// Locate user-defined method
		MethodMetadata userMethod = MemberFindingUtils.getMethod(governorTypeDetails, methodName, paramTypes);
		if (userMethod != null) {
			Assert.isTrue(userMethod.getReturnType().equals(returnType), "Method '" + methodName + "' on '" + governorTypeDetails.getName() + "' must return '" + returnType.getNameIncludingTypeParameters() + "'");
			return userMethod;
		}

		// Create method
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName() + " obj = new " + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName() + "();");

		for (MethodMetadata mutator : mandatoryMutators) {
			String initializer = mutatorArguments.get(mutator);
			Assert.hasText(initializer, "Internal error: unable to locate initializer for " + mutator);

			JavaSymbolName propertyName = BeanInfoMetadata.getPropertyNameForJavaBeanMethod(mutator);
			FieldMetadata field = beanInfoMetadata.getFieldForPropertyName(propertyName);
			if (field.getFieldType().equals(JavaType.STRING_OBJECT) && (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), NOT_NULL) != null || MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), SIZE) != null || MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), MIN) != null || MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), MAX) != null)) {
				// Check for @Size or @Column with length attribute
				AnnotationMetadata sizeAnnotationMetadata = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), SIZE);
				AnnotationMetadata columnAnnotationMetadata = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), COLUMN);

				if (sizeAnnotationMetadata != null && sizeAnnotationMetadata.getAttribute(new JavaSymbolName("max")) != null) {
					Integer maxValue = (Integer) sizeAnnotationMetadata.getAttribute(new JavaSymbolName("max")).getValue();
					bodyBuilder.appendFormalLine(field.getFieldType().getFullyQualifiedTypeName() + " " + field.getFieldName().getSymbolName() + " = " + initializer + ";");
					bodyBuilder.appendFormalLine("if (" + field.getFieldName().getSymbolName() + ".length() > " + maxValue + ") {");
					bodyBuilder.indent();
					bodyBuilder.appendFormalLine(field.getFieldName().getSymbolName() + "  = " + field.getFieldName().getSymbolName() + ".substring(0, " + maxValue + ");");
					bodyBuilder.indentRemove();
					bodyBuilder.appendFormalLine("}");
					bodyBuilder.appendFormalLine("obj." + mutator.getMethodName() + "(" + field.getFieldName().getSymbolName() + ");");
				} else if (sizeAnnotationMetadata == null && columnAnnotationMetadata != null) {
					AnnotationAttributeValue<?> lengthAttributeValue = columnAnnotationMetadata.getAttribute(new JavaSymbolName("length"));
					if (lengthAttributeValue != null) {
						Integer lengthValue = (Integer) columnAnnotationMetadata.getAttribute(new JavaSymbolName("length")).getValue();
						bodyBuilder.appendFormalLine(field.getFieldType().getFullyQualifiedTypeName() + " " + field.getFieldName().getSymbolName() + " = " + initializer + ";");
						bodyBuilder.appendFormalLine("if (" + field.getFieldName().getSymbolName() + ".length() > " + lengthValue + ") {");
						bodyBuilder.indent();
						bodyBuilder.appendFormalLine(field.getFieldName().getSymbolName() + "  = " + field.getFieldName().getSymbolName() + ".substring(0, " + lengthValue + ");");
						bodyBuilder.indentRemove();
						bodyBuilder.appendFormalLine("}");
						bodyBuilder.appendFormalLine("obj." + mutator.getMethodName() + "(" + field.getFieldName().getSymbolName() + ");");
					} else {
						bodyBuilder.appendFormalLine("obj." + mutator.getMethodName() + "(" + initializer + ");");
					}
				} else {
					bodyBuilder.appendFormalLine("obj." + mutator.getMethodName() + "(" + initializer + ");");
				}
			} else if (field.getFieldType().equals(JavaType.CHAR_OBJECT) || field.getFieldType().equals(JavaType.CHAR_PRIMITIVE)) {
				bodyBuilder.appendFormalLine("obj." + mutator.getMethodName() + "('X');");
			} else if (isNumericFieldType(field)) {
				// Check for @Min and @Max
				AnnotationMetadata minAnnotationMetadata = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), MIN);
				AnnotationMetadata maxAnnotationMetadata = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), MAX);
				String suffix = "";
				if (field.getFieldType().equals(JavaType.LONG_OBJECT) || field.getFieldType().equals(JavaType.LONG_PRIMITIVE)) {
					suffix = "L";
				} else if (field.getFieldType().equals(JavaType.FLOAT_OBJECT) || field.getFieldType().equals(JavaType.FLOAT_PRIMITIVE)) {
					suffix = "F";
				} else if (field.getFieldType().equals(JavaType.DOUBLE_OBJECT) || field.getFieldType().equals(JavaType.DOUBLE_PRIMITIVE)) {
					suffix = "D";
				}

				if (minAnnotationMetadata != null && maxAnnotationMetadata == null) {
					Long minValue = (Long) minAnnotationMetadata.getAttribute(new JavaSymbolName("value")).getValue();

					bodyBuilder.appendFormalLine(field.getFieldType().getFullyQualifiedTypeName() + " " + field.getFieldName().getSymbolName() + " = " + initializer + ";");
					bodyBuilder.appendFormalLine("if (" + field.getFieldName().getSymbolName() + " < " + minValue + ") {");
					bodyBuilder.indent();
					bodyBuilder.appendFormalLine(field.getFieldName().getSymbolName() + " = " + minValue + suffix + ";");
					bodyBuilder.indentRemove();
					bodyBuilder.appendFormalLine("}");
					bodyBuilder.appendFormalLine("obj." + mutator.getMethodName() + "(" + field.getFieldName().getSymbolName() + ");");
				} else if (minAnnotationMetadata == null && maxAnnotationMetadata != null) {
					Long maxValue = (Long) maxAnnotationMetadata.getAttribute(new JavaSymbolName("value")).getValue();

					bodyBuilder.appendFormalLine(field.getFieldType().getFullyQualifiedTypeName() + " " + field.getFieldName().getSymbolName() + " = " + initializer + ";");
					bodyBuilder.appendFormalLine("if (" + field.getFieldName().getSymbolName() + " > " + maxValue + ") {");
					bodyBuilder.indent();
					bodyBuilder.appendFormalLine(field.getFieldName().getSymbolName() + " = " + maxValue + suffix + ";");
					bodyBuilder.indentRemove();
					bodyBuilder.appendFormalLine("}");
					bodyBuilder.appendFormalLine("obj." + mutator.getMethodName() + "(" + field.getFieldName().getSymbolName() + ");");
				} else if (minAnnotationMetadata != null && maxAnnotationMetadata != null) {
					Long minValue = (Long) minAnnotationMetadata.getAttribute(new JavaSymbolName("value")).getValue();
					Long maxValue = (Long) maxAnnotationMetadata.getAttribute(new JavaSymbolName("value")).getValue();
					Assert.isTrue(maxValue >= minValue, "The value of @Max must be greater or equal to the value of @Min for field " + field.getFieldName().getSymbolName());

					bodyBuilder.appendFormalLine(field.getFieldType().getFullyQualifiedTypeName() + " " + field.getFieldName().getSymbolName() + " = " + initializer + ";");
					bodyBuilder.appendFormalLine("if (" + field.getFieldName().getSymbolName() + " < " + minValue + suffix + " || " + field.getFieldName().getSymbolName() + " > " + maxValue + suffix + ") {");
					bodyBuilder.indent();
					bodyBuilder.appendFormalLine(field.getFieldName().getSymbolName() + " = " + maxValue + suffix + ";");
					bodyBuilder.indentRemove();
					bodyBuilder.appendFormalLine("}");
					bodyBuilder.appendFormalLine("obj." + mutator.getMethodName() + "(" + field.getFieldName().getSymbolName() + ");");
				} else {
					bodyBuilder.appendFormalLine("obj." + mutator.getMethodName() + "(" + initializer + ");");
				}
			} else {
				bodyBuilder.appendFormalLine("obj." + mutator.getMethodName() + "(" + initializer + ");");
			}
		}

		bodyBuilder.appendFormalLine("return obj;");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, bodyBuilder);
		return methodBuilder.build();
	}

	/**
	 * @return the "modifyEntity(Entity):boolean" method (never returns null)
	 */
	public MethodMetadata getModifyMethod() {
		// Method definition to find or build
		JavaSymbolName methodName = new JavaSymbolName("modify" + beanInfoMetadata.getJavaBean().getSimpleTypeName());
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		paramTypes.add(beanInfoMetadata.getJavaBean());
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("obj"));
		JavaType returnType = JavaType.BOOLEAN_PRIMITIVE;

		// Locate user-defined method
		MethodMetadata userMethod = MemberFindingUtils.getMethod(governorTypeDetails, methodName, paramTypes);
		if (userMethod != null) {
			Assert.isTrue(userMethod.getReturnType().equals(returnType), "Method '" + methodName + "' on '" + governorTypeDetails.getName() + "' must return '" + returnType.getNameIncludingTypeParameters() + "'");
			return userMethod;
		}

		// Create method
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		// TODO: We should port this more fully from original code base
		bodyBuilder.appendFormalLine("return false;");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, bodyBuilder);
		return methodBuilder.build();
	}

	/**
	 * @return the "getRandomEntity():Entity" method (never returns null)
	 */
	public MethodMetadata getRandomPersistentEntityMethod() {
		// Method definition to find or build
		JavaSymbolName methodName = new JavaSymbolName("getRandom" + beanInfoMetadata.getJavaBean().getSimpleTypeName());
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		JavaType returnType = beanInfoMetadata.getJavaBean();

		// Locate user-defined method
		MethodMetadata userMethod = MemberFindingUtils.getMethod(governorTypeDetails, methodName, paramTypes);
		if (userMethod != null) {
			Assert.isTrue(userMethod.getReturnType().equals(returnType), "Method '" + methodName + "' on '" + governorTypeDetails.getName() + "' must return '" + returnType.getNameIncludingTypeParameters() + "'");
			return userMethod;
		}

		// Create method
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("init();");
		bodyBuilder.appendFormalLine(beanInfoMetadata.getJavaBean().getSimpleTypeName() + " obj = " + getDataField().getFieldName().getSymbolName() + ".get(" + getRndField().getFieldName().getSymbolName() + ".nextInt(" + getDataField().getFieldName().getSymbolName() + ".size()));");
		bodyBuilder.appendFormalLine("return " + beanInfoMetadata.getJavaBean().getSimpleTypeName() + "." + findMethod.getMethodName().getSymbolName() + "(obj." + identifierAccessorMethod.getMethodName().getSymbolName() + "());");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, bodyBuilder);
		return methodBuilder.build();
	}

	/**
	 * @return the "getSpecificEntity(int):Entity" method (never returns null)
	 */
	public MethodMetadata getSpecificPersistentEntityMethod() {
		// Method definition to find or build
		JavaSymbolName methodName = new JavaSymbolName("getSpecific" + beanInfoMetadata.getJavaBean().getSimpleTypeName());
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		paramTypes.add(JavaType.INT_PRIMITIVE);
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("index"));
		JavaType returnType = beanInfoMetadata.getJavaBean();

		// Locate user-defined method
		MethodMetadata userMethod = MemberFindingUtils.getMethod(governorTypeDetails, methodName, paramTypes);
		if (userMethod != null) {
			Assert.isTrue(userMethod.getReturnType().equals(returnType), "Method '" + methodName + "' on '" + governorTypeDetails.getName() + "' must return '" + returnType.getNameIncludingTypeParameters() + "'");
			return userMethod;
		}

		// Create method
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("init();");
		bodyBuilder.appendFormalLine("if (index < 0) index = 0;");
		bodyBuilder.appendFormalLine("if (index > (" + getDataField().getFieldName().getSymbolName() + ".size() - 1)) index = " + getDataField().getFieldName().getSymbolName() + ".size() - 1;");
		bodyBuilder.appendFormalLine(beanInfoMetadata.getJavaBean().getSimpleTypeName() + " obj = " + getDataField().getFieldName().getSymbolName() + ".get(index);");
		bodyBuilder.appendFormalLine("return " + beanInfoMetadata.getJavaBean().getSimpleTypeName() + "." + findMethod.getMethodName().getSymbolName() + "(obj." + identifierAccessorMethod.getMethodName().getSymbolName() + "());");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, bodyBuilder);
		return methodBuilder.build();
	}

	/**
	 * @return the "init():void" method (never returns null)
	 */
	public MethodMetadata getInitMethod() {
		// Method definition to find or build
		JavaSymbolName methodName = new JavaSymbolName("init");
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		JavaType returnType = JavaType.VOID_PRIMITIVE;

		// Locate user-defined method
		MethodMetadata userMethod = MemberFindingUtils.getMethod(governorTypeDetails, methodName, paramTypes);
		if (userMethod != null) {
			Assert.isTrue(userMethod.getReturnType().equals(returnType), "Method '" + methodName + "' on '" + governorTypeDetails.getName() + "' must return '" + returnType.getNameIncludingTypeParameters() + "'");
			return userMethod;
		}

		// Create the method

		// Create the body
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		String dataField = getDataField().getFieldName().getSymbolName();
		bodyBuilder.appendFormalLine(dataField + " = new java.util.ArrayList<" + getDataField().getFieldType().getParameters().get(0).getNameIncludingTypeParameters() + ">();");
		bodyBuilder.appendFormalLine("for (int i = 0; i < " + annotationValues.getQuantity() + "; i++) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName() + " obj = " + getNewTransientEntityMethod().getMethodName() + "(i);");
		bodyBuilder.appendFormalLine("obj." + persistMethod.getMethodName().getSymbolName() + "();");
		bodyBuilder.appendFormalLine("obj." + flushMethod.getMethodName().getSymbolName() + "();");
		bodyBuilder.appendFormalLine(dataField + ".add(obj);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, bodyBuilder);
		return methodBuilder.build();
	}

	private void mutatorDiscovery() {
		for (MethodMetadata mutatorMethod : beanInfoMetadata.getPublicMutators()) {
			JavaSymbolName propertyName = BeanInfoMetadata.getPropertyNameForJavaBeanMethod(mutatorMethod);
			FieldMetadata field = beanInfoMetadata.getFieldForPropertyName(propertyName);

			if (field == null) {
				// There is no field for this mutator, so chances are it's not mandatory
				continue;
			}

			// Never include id or version fields (they shouldn't normally have a mutator anyway, but the user might have added one)
			if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.Id")) != null || MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.Version")) != null) {
				continue;
			}

			// Never include field annotated with @javax.persistence.Transient
			if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.Transient")) != null) {
				continue;
			}

			// Never include any sort of collection; user has to make such entities by hand
			if (field.getFieldType().isCommonCollectionType() || MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.OneToMany")) != null) {
				continue;
			}

			// Check for @ManyToOne annotation with 'optional = false' attribute (ROO-1075)
			boolean hasManyToOne = false;
			AnnotationMetadata manyToOneAnnotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.ManyToOne"));
			if (manyToOneAnnotation != null) {
				AnnotationAttributeValue<?> optionalAttribute = manyToOneAnnotation.getAttribute(new JavaSymbolName("optional"));
				hasManyToOne = optionalAttribute != null && !((Boolean) optionalAttribute.getValue());
			}

			AnnotationMetadata oneToOneAnnotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.OneToOne"));

			String initializer = "null";

			// Date fields included for DataNucleus (
			if (field.getFieldType().equals(new JavaType(Date.class.getName()))) {
				if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.Past")) != null) {
					initializer = "new java.util.Date(new java.util.Date().getTime() - 10000000L)";
				} else if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.Future")) != null) {
					initializer = "new java.util.Date(new java.util.Date().getTime() + 10000000L)";
				} else {
					initializer = "new java.util.Date()";
				}
			} else if (field.getFieldType().equals(JavaType.BOOLEAN_PRIMITIVE) && MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), NOT_NULL) == null) {
				initializer = "true";
			} else if (isNumericPrimitive(field) || MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), NOT_NULL) != null || MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), SIZE) != null || MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), MIN) != null || MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), MAX) != null || hasManyToOne || field.getAnnotations().size() == 0) {
				// Only include the field if it's really required (ie marked with JSR 303 NotNull), is a numeric primitive field, or it has no annotations and is therefore probably simple to invoke
				if (field.getFieldType().equals(JavaType.STRING_OBJECT)) {
					initializer = field.getFieldName().getSymbolName();

					// Check for @Size
					AnnotationMetadata sizeAnnotationMetadata = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), SIZE);
					if (sizeAnnotationMetadata != null) {
						AnnotationAttributeValue<?> maxValue = sizeAnnotationMetadata.getAttribute(new JavaSymbolName("max"));
						if (maxValue != null && (Integer) maxValue.getValue() > 1 && (initializer.length() + 2) > (Integer) maxValue.getValue()) {
							initializer = initializer.substring(0, (Integer) maxValue.getValue() - 2);
						}
						AnnotationAttributeValue<?> minValue = sizeAnnotationMetadata.getAttribute(new JavaSymbolName("min"));
						if (minValue != null && (initializer.length() + 2) < (Integer) minValue.getValue()) {
							initializer = String.format("%1$-" + ((Integer) minValue.getValue() - 2) + "s", initializer).replace(' ', 'x');
						}
					}

					// Check for @Column
					AnnotationMetadata columnAnnotationMetadata = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), COLUMN);
					if (columnAnnotationMetadata != null) {
						AnnotationAttributeValue<?> lengthValue = columnAnnotationMetadata.getAttribute(new JavaSymbolName("length"));
						if (lengthValue != null && (initializer.length() + 2) > (Integer) lengthValue.getValue()) {
							initializer = initializer.substring(0, (Integer) lengthValue.getValue() - 2);
						}
					}

					initializer = "\"" + initializer + "_\" + index";
				} else if (field.getFieldType().equals(new JavaType(Calendar.class.getName()))) {
					if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.Past")) != null) {
						initializer = "new java.util.GregorianCalendar(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR), java.util.Calendar.getInstance().get(java.util.Calendar.MONTH), java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH) - 1)";
					} else if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.Future")) != null) {
						initializer = "new java.util.GregorianCalendar(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR), java.util.Calendar.getInstance().get(java.util.Calendar.MONTH), java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH) + 1)";
					} else {
						initializer = "java.util.Calendar.getInstance()";
					}
				} else if (field.getFieldType().equals(JavaType.BOOLEAN_OBJECT)) {
					initializer = "Boolean.TRUE";
				} else if (field.getFieldType().equals(JavaType.BOOLEAN_PRIMITIVE)) {
					initializer = "true";
				} else if (field.getFieldType().equals(JavaType.INT_OBJECT)) {
					initializer = "new Integer(index)";
				} else if (field.getFieldType().equals(JavaType.INT_PRIMITIVE)) {
					initializer = "new Integer(index)"; // Auto-boxed
				} else if (field.getFieldType().equals(JavaType.DOUBLE_OBJECT)) {
					initializer = "new Integer(index).doubleValue()"; // Auto-boxed
				} else if (field.getFieldType().equals(JavaType.DOUBLE_PRIMITIVE)) {
					initializer = "new Integer(index).doubleValue()";
				} else if (field.getFieldType().equals(JavaType.FLOAT_OBJECT)) {
					initializer = "new Integer(index).floatValue()"; // Auto-boxed
				} else if (field.getFieldType().equals(JavaType.FLOAT_PRIMITIVE)) {
					initializer = "new Integer(index).floatValue()";
				} else if (field.getFieldType().equals(JavaType.LONG_OBJECT)) {
					initializer = "new Integer(index).longValue()"; // Auto-boxed
				} else if (field.getFieldType().equals(JavaType.LONG_PRIMITIVE)) {
					initializer = "new Integer(index).longValue()";
				} else if (field.getFieldType().equals(JavaType.SHORT_OBJECT)) {
					initializer = "new Integer(index).shortValue()"; // Auto-boxed
				} else if (field.getFieldType().equals(JavaType.SHORT_PRIMITIVE)) {
					initializer = "new Integer(index).shortValue()";
				} else if (field.getFieldType().equals(new JavaType("java.math.BigDecimal"))) {
					initializer = "new java.math.BigDecimal(index)";
				} else if (field.getFieldType().equals(new JavaType("java.math.BigInteger"))) {
					initializer = "java.math.BigInteger.valueOf(index)";
				} else if (manyToOneAnnotation != null || oneToOneAnnotation != null) {
					if (field.getFieldType().equals(this.getAnnotationValues().getEntity())) {
						// Avoid circular references (ROO-562)
						initializer = "obj";
					} else {
						requiredDataOnDemandCollaborators.add(field.getFieldType());
						String collaboratingFieldName = getCollaboratingFieldName(field.getFieldType()).getSymbolName();

						// Look up the metadata we are relying on
						String otherProvider = DataOnDemandMetadata.createIdentifier(new JavaType(field.getFieldType() + "DataOnDemand"), Path.SRC_TEST_JAVA);

						// Decide if we're dealing with a one-to-one and therefore should _try_ to keep the same id (ROO-568)
						boolean oneToOne = oneToOneAnnotation != null;

						metadataDependencyRegistry.registerDependency(otherProvider, getId());
						DataOnDemandMetadata otherMd = (DataOnDemandMetadata) metadataService.get(otherProvider);
						if (otherMd == null || !otherMd.isValid()) {
							// There is no metadata around, so we'll just make some basic assumptions
							if (oneToOne) {
								initializer = collaboratingFieldName + ".getSpecific" + field.getFieldType().getSimpleTypeName() + "(index)";
							} else {
								initializer = collaboratingFieldName + ".getRandom" + field.getFieldType().getSimpleTypeName() + "()";
							}
						} else {
							// We can use the correct name
							if (oneToOne) {
								initializer = collaboratingFieldName + "." + otherMd.getSpecificPersistentEntityMethod().getMethodName().getSymbolName() + "(index)";
							} else {
								initializer = collaboratingFieldName + "." + otherMd.getRandomPersistentEntityMethod().getMethodName().getSymbolName() + "()";
							}
						}
					}
				} else if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.Enumerated")) != null) {
					initializer = field.getFieldType().getFullyQualifiedTypeName() + ".class.getEnumConstants()[0]";
				}
			}

			mandatoryMutators.add(mutatorMethod);
			mutatorArguments.put(mutatorMethod, initializer);
		}
	}
	
	private boolean isNumericFieldType(FieldMetadata field) {
		return isNumericPrimitive(field) || field.getFieldType().equals(JavaType.INT_OBJECT) || field.getFieldType().equals(JavaType.DOUBLE_OBJECT) || field.getFieldType().equals(JavaType.FLOAT_OBJECT) || field.getFieldType().equals(JavaType.LONG_OBJECT) || field.getFieldType().equals(JavaType.SHORT_OBJECT);
	}

	private boolean isNumericPrimitive(FieldMetadata field) {
		return field.getFieldType().equals(JavaType.INT_PRIMITIVE) || field.getFieldType().equals(JavaType.FLOAT_PRIMITIVE) || field.getFieldType().equals(JavaType.DOUBLE_PRIMITIVE) || field.getFieldType().equals(JavaType.LONG_PRIMITIVE) || field.getFieldType().equals(JavaType.SHORT_PRIMITIVE);
	}

	private JavaSymbolName getCollaboratingFieldName(JavaType entity) {
		return new JavaSymbolName(StringUtils.uncapitalize(getCollaboratingType(entity).getSimpleTypeName()));
	}

	private JavaType getCollaboratingType(JavaType entity) {
		return new JavaType(entity.getFullyQualifiedTypeName() + "DataOnDemand");
	}

	/**
	 * @return the physical type identifier for the {@link BeanInfoMetadata} (never null or empty unless metadata is invalid)
	 */
	public String getIdentifierForBeanInfoMetadata() {
		return beanInfoMetadata.getId();
	}

	/**
	 * @return the annotation values specified via {@link RooDataOnDemand} (never null unless the metadata itself is invalid)
	 */
	public DataOnDemandAnnotationValues getAnnotationValues() {
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
