package org.springframework.roo.addon.dod;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.ConstructorMetadata;
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
import org.springframework.roo.metadata.MetadataIdentificationUtils;
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
	private static final JavaType COMPONENT = new JavaType("org.springframework.stereotype.Component");
	private static final JavaType MAX = new JavaType("javax.validation.constraints.Max");
	private static final JavaType MIN = new JavaType("javax.validation.constraints.Min");
	private static final JavaType SIZE = new JavaType("javax.validation.constraints.Size");
	private static final JavaType BIG_INTEGER = new JavaType("java.math.BigInteger");
	private static final JavaType BIG_DECIMAL = new JavaType("java.math.BigDecimal");

	private DataOnDemandAnnotationValues annotationValues;
	private MethodMetadata identifierAccessor;
	private MethodMetadata findMethod;
	private MethodMetadata findEntriesMethod;
	private MethodMetadata persistMethod;
	private MethodMetadata flushMethod;
	private Map<MethodMetadata, CollaboratingDataOnDemandMetadataHolder> locatedMutators;
	private JavaType entityType;
	private EmbeddedIdentifierHolder embeddedIdentifierHolder;

	private Map<MethodMetadata, String> fieldInitializers = new LinkedHashMap<MethodMetadata, String>();
	private Map<FieldMetadata, String> embeddedIdInitializers = new LinkedHashMap<FieldMetadata, String>();
	private List<JavaType> requiredDataOnDemandCollaborators = new LinkedList<JavaType>();

	public DataOnDemandMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, DataOnDemandAnnotationValues annotationValues, MethodMetadata identifierAccessor, MethodMetadata findMethod, MethodMetadata findEntriesMethod, MethodMetadata persistMethod, MethodMetadata flushMethod, Map<MethodMetadata, CollaboratingDataOnDemandMetadataHolder> locatedMutators, JavaType entityType, EmbeddedIdentifierHolder embeddedIdentifierHolder) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.notNull(identifierAccessor, "Identifier accessor method required");
		Assert.notNull(findMethod, "Find method required");
		Assert.notNull(findEntriesMethod, "Find entries method required");
		Assert.notNull(persistMethod, "Persist method required");
		Assert.notNull(flushMethod, "Flush method required");
		Assert.notNull(locatedMutators, "Located mutator methods map required");
		Assert.notNull(entityType, "Entity type required");

		if (!isValid()) {
			return;
		}

		this.annotationValues = annotationValues;
		this.identifierAccessor = identifierAccessor;
		this.findMethod = findMethod;
		this.findEntriesMethod = findEntriesMethod;
		this.persistMethod = persistMethod;
		this.flushMethod = flushMethod;
		this.locatedMutators = locatedMutators;
		this.entityType = entityType;
		this.embeddedIdentifierHolder = embeddedIdentifierHolder;

		// Calculate and store field initializers
		storeEmbeddedIdInitializers();
		storeFieldInitializers();

		builder.addAnnotation(getComponentAnnotation());
		builder.addField(getRndField());
		builder.addField(getDataField());

		addFieldsToBuilder();

		builder.addMethod(getNewTransientEntityMethod());
		builder.addMethod(getEmbeddedIdMethod());

		addMethodsToBuilder();

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
		if (MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, COMPONENT) != null) {
			return null;
		}
		AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(COMPONENT);
		return annotationBuilder.build();
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

	private void addFieldsToBuilder() {
		Set<JavaSymbolName> fields = new HashSet<JavaSymbolName>();
		for (JavaType entityNeedingCollaborator : requiredDataOnDemandCollaborators) {
			JavaType collaboratorType = getCollaboratingType(entityNeedingCollaborator);
			String collaboratingFieldName = getCollaboratingFieldName(entityNeedingCollaborator).getSymbolName();

			JavaSymbolName fieldSymbolName = new JavaSymbolName(collaboratingFieldName);
			FieldMetadata candidate = MemberFindingUtils.getField(governorTypeDetails, fieldSymbolName);
			if (candidate != null) {
				// We really expect the field to be correct if we're going to rely on it
				Assert.isTrue(candidate.getFieldType().equals(collaboratorType), "Field '" + collaboratingFieldName + "' on '" + destination.getFullyQualifiedTypeName() + "' must be of type '" + collaboratorType.getFullyQualifiedTypeName() + "'");
				Assert.isTrue(Modifier.isPrivate(candidate.getModifier()), "Field '" + collaboratingFieldName + "' on '" + destination.getFullyQualifiedTypeName() + "' must be private");
				Assert.notNull(MemberFindingUtils.getAnnotationOfType(candidate.getAnnotations(), new JavaType("org.springframework.beans.factory.annotation.Autowired")), "Field '" + collaboratingFieldName + "' on '" + destination.getFullyQualifiedTypeName() + "' must be @Autowired");
				// It's ok, so we can move onto the new field
				continue;
			}

			// Must make the field
			List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
			annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.beans.factory.annotation.Autowired")));
			FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations, fieldSymbolName, collaboratorType);
			FieldMetadata field = fieldBuilder.build();

			// Add it to the ITD, if it hasn't already been
			if (!fields.contains(field.getFieldName())) {
				fields.add(field.getFieldName());
				builder.addField(field);
				fields.add(field.getFieldName());
			}
		}
	}

	/**
	 * @return the "getNewTransientEntity(int index):Entity" method (never returns null)
	 */
	public MethodMetadata getNewTransientEntityMethod() {
		// Method definition to find or build
		JavaSymbolName methodName = new JavaSymbolName("getNewTransient" + entityType.getSimpleTypeName());

		List<JavaType> paramTypes = new ArrayList<JavaType>();
		paramTypes.add(JavaType.INT_PRIMITIVE);

		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("index"));

		// Locate user-defined method
		MethodMetadata userMethod = MemberFindingUtils.getMethod(governorTypeDetails, methodName, paramTypes);
		if (userMethod != null) {
			Assert.isTrue(userMethod.getReturnType().equals(entityType), "Method '" + methodName + "' on '" + destination + "' must return '" + entityType.getNameIncludingTypeParameters() + "'");
			return userMethod;
		}

		// Create method
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(entityType.getFullyQualifiedTypeName() + " obj = new " + entityType.getFullyQualifiedTypeName() + "();");

		// Create the composite key embedded id if required
		if (embeddedIdentifierHolder != null) {
			bodyBuilder.appendFormalLine(getEmbeddedIdMutatorMethod() + "(obj, index);");
		}

		for (MethodMetadata mutator : fieldInitializers.keySet()) {
			bodyBuilder.appendFormalLine(mutator.getMethodName() + "(obj, index);");
		}

		bodyBuilder.appendFormalLine("return obj;");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, entityType, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, bodyBuilder);
		return methodBuilder.build();
	}

	public MethodMetadata getEmbeddedIdMethod() {
		if (!hasEmbeddedIdentifier()) {
			return null;
		}

		JavaSymbolName embeddedIdentifierMutator = embeddedIdentifierHolder.getEmbeddedIdentifierMutator();
		JavaSymbolName methodName = getEmbeddedIdMutatorMethod();
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		paramTypes.add(entityType);
		paramTypes.add(JavaType.INT_PRIMITIVE);

		// Locate user-defined method
		if (MemberFindingUtils.getMethod(governorTypeDetails, methodName, paramTypes) != null) {
			// Method found in governor so do not create method in ITD
			return null;
		}

		StringBuilder builder = new StringBuilder();
		for (FieldMetadata identifierField : embeddedIdInitializers.keySet()) {
			String initializer = embeddedIdInitializers.get(identifierField);
			Assert.hasText(initializer, "Internal error: unable to locate initializer for " + embeddedIdentifierMutator.getSymbolName());
			String constructorFieldInitializer = getConstructorFieldInitializer(identifierField, initializer, getRequiredMutatorName(identifierField));
			builder.append(constructorFieldInitializer);
		}

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.append(builder.toString());

		// Create constructor for embedded id class
		String identifierType = embeddedIdentifierHolder.getEmbeddedIdentifierField().getFieldType().getFullyQualifiedTypeName();
		ConstructorMetadata constructorMetadata = embeddedIdentifierHolder.getIdentifierConstructor();

		builder.delete(0, builder.length());
		builder.append(identifierType).append(" embeddedIdClass = new ").append(identifierType);
		builder.append("(");
		builder.append(StringUtils.collectionToDelimitedString(constructorMetadata.getParameterNames(), ", "));
		builder.append(");");

		bodyBuilder.appendFormalLine(builder.toString());
		bodyBuilder.appendFormalLine("obj." + embeddedIdentifierMutator + "(embeddedIdClass);");

		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("obj"));
		paramNames.add(new JavaSymbolName("index"));

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PRIVATE, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, bodyBuilder);
		return methodBuilder.build();
	}

	private void addMethodsToBuilder() {
		List<JavaSymbolName> methodNames = new ArrayList<JavaSymbolName>();
		for (MethodMetadataBuilder itdMethod : builder.getDeclaredMethods()) {
			methodNames.add(itdMethod.getMethodName());
		}
		for (MethodMetadata fieldInitializerMethod : getFieldInitializerMethods()) {
			if (!methodNames.contains(fieldInitializerMethod.getMethodName())) {
				builder.addMethod(fieldInitializerMethod);
				methodNames.add(fieldInitializerMethod.getMethodName());
			}
		}
	}

	public List<MethodMetadata> getFieldInitializerMethods() {
		List<MethodMetadata> fieldInitializerMethods = new LinkedList<MethodMetadata>();

		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("obj"));
		paramNames.add(new JavaSymbolName("index"));

		List<JavaType> paramTypes = new ArrayList<JavaType>();
		paramTypes.add(entityType);
		paramTypes.add(JavaType.INT_PRIMITIVE);

		for (MethodMetadata mutator : fieldInitializers.keySet()) {
			// Locate user-defined method
			if (MemberFindingUtils.getMethod(governorTypeDetails, mutator.getMethodName(), paramTypes) != null) {
				// Method found in governor so do not create method in ITD
				continue;
			}

			// Method not on governor so need to create it
			String initializer = fieldInitializers.get(mutator);
			Assert.hasText(initializer, "Internal error: unable to locate initializer for " + mutator.getMethodName().getSymbolName());

			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.append(getFieldInitializer(locatedMutators.get(mutator).getField(), initializer, mutator.getMethodName(), true));

			MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, mutator.getMethodName(), JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, bodyBuilder);
			fieldInitializerMethods.add(methodBuilder.build());
		}

		return fieldInitializerMethods;
	}

	private String getConstructorFieldInitializer(FieldMetadata field, String initializer, JavaSymbolName mutatorName) {
		return getFieldInitializer(field, initializer, mutatorName, false);
	}

	private String getFieldInitializer(FieldMetadata field, String initializer, JavaSymbolName mutatorName, boolean isMutatorField) {
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

		String fieldName = field.getFieldName().getSymbolName();
		JavaType fieldType = field.getFieldType();

		String suffix = "";
		if (fieldType.equals(JavaType.LONG_OBJECT) || fieldType.equals(JavaType.LONG_PRIMITIVE)) {
			suffix = "L";
		} else if (fieldType.equals(JavaType.FLOAT_OBJECT) || fieldType.equals(JavaType.FLOAT_PRIMITIVE)) {
			suffix = "F";
		} else if (fieldType.equals(JavaType.DOUBLE_OBJECT) || fieldType.equals(JavaType.DOUBLE_PRIMITIVE)) {
			suffix = "D";
		}

		bodyBuilder.appendFormalLine(getTypeStr(fieldType) + " " + fieldName + " = " + initializer + ";");

		if (fieldType.equals(JavaType.STRING_OBJECT)) {
			// Check for @Size or @Column with length attribute
			AnnotationMetadata sizeAnnotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), SIZE);
			if (sizeAnnotation != null && sizeAnnotation.getAttribute(new JavaSymbolName("max")) != null) {
				Integer maxValue = (Integer) sizeAnnotation.getAttribute(new JavaSymbolName("max")).getValue();
				bodyBuilder.appendFormalLine("if (" + fieldName + ".length() > " + maxValue + ") {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine(fieldName + " = " + fieldName + ".substring(0, " + maxValue + ");");
				bodyBuilder.indentRemove();
				bodyBuilder.appendFormalLine("}");
			} else if (sizeAnnotation == null && field.getCustomData().keySet().contains(PersistenceCustomDataKeys.COLUMN_FIELD)) {
				@SuppressWarnings("unchecked") Map<String, Object> values = (Map<String, Object>) field.getCustomData().get(PersistenceCustomDataKeys.COLUMN_FIELD);
				if (values != null && values.containsKey("length")) {
					Integer lengthValue = (Integer) values.get("length");
					bodyBuilder.appendFormalLine("if (" + fieldName + ".length() > " + lengthValue + ") {");
					bodyBuilder.indent();
					bodyBuilder.appendFormalLine(fieldName + " = " + fieldName + ".substring(0, " + lengthValue + ");");
					bodyBuilder.indentRemove();
					bodyBuilder.appendFormalLine("}");
				}
			}
		} else if (isDecimalFieldType(fieldType)) {
			// Check for @Digits, @DecimalMax, @DecimalMin
			AnnotationMetadata digitsAnnotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.Digits"));
			AnnotationMetadata decimalMinAnnotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.DecimalMin"));
			AnnotationMetadata decimalMaxAnnotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.DecimalMax"));

			if (digitsAnnotation != null) {
				doDigits(field, digitsAnnotation, bodyBuilder, mutatorName, initializer, suffix);
			} else if (decimalMinAnnotation != null || decimalMaxAnnotation != null) {
				doDecimalMinAndDecimalMax(field, decimalMinAnnotation, decimalMaxAnnotation, bodyBuilder, mutatorName, initializer, suffix);
			} else if (field.getCustomData().keySet().contains(PersistenceCustomDataKeys.COLUMN_FIELD)) {
				@SuppressWarnings("unchecked") Map<String, Object> values = (Map<String, Object>) field.getCustomData().get(PersistenceCustomDataKeys.COLUMN_FIELD);
				doColumnPrecisionAndScale(field, values, bodyBuilder, mutatorName, initializer, suffix);
			}
		} else if (isIntegerFieldType(fieldType)) {
			// Check for @Min and @Max
			doMinAndMax(field, bodyBuilder, mutatorName, initializer, suffix);
		}

		if (isMutatorField) {
			bodyBuilder.appendFormalLine("obj." + mutatorName.getSymbolName() + "(" + fieldName + ");");
		}

		return bodyBuilder.getOutput();
	}

	private String getTypeStr(JavaType fieldType) {
		String arrayStr = fieldType.isArray() ? "[]" : "";
		String typeStr = fieldType.getFullyQualifiedTypeName();
		if (fieldType.getFullyQualifiedTypeName().equals(JavaType.FLOAT_PRIMITIVE.getFullyQualifiedTypeName()) && fieldType.isPrimitive()) {
			typeStr = "float" + arrayStr;
		} else if (fieldType.getFullyQualifiedTypeName().equals(JavaType.DOUBLE_PRIMITIVE.getFullyQualifiedTypeName()) && fieldType.isPrimitive()) {
			typeStr = "double" + arrayStr;
		} else if (fieldType.getFullyQualifiedTypeName().equals(JavaType.INT_PRIMITIVE.getFullyQualifiedTypeName()) && fieldType.isPrimitive()) {
			typeStr = "int" + arrayStr;
		} else if (fieldType.getFullyQualifiedTypeName().equals(JavaType.SHORT_PRIMITIVE.getFullyQualifiedTypeName()) && fieldType.isPrimitive()) {
			typeStr = "short" + arrayStr;
		} else if (fieldType.getFullyQualifiedTypeName().equals(JavaType.BYTE_PRIMITIVE.getFullyQualifiedTypeName()) && fieldType.isPrimitive()) {
			typeStr = "byte" + arrayStr;
		} else if (fieldType.getFullyQualifiedTypeName().equals(JavaType.CHAR_PRIMITIVE.getFullyQualifiedTypeName()) && fieldType.isPrimitive()) {
			typeStr = "char" + arrayStr;
		} else if (fieldType.equals(new JavaType("java.lang.String", 1, DataType.TYPE, null, null))) {
			typeStr = "String[]";
		}
		return typeStr;
	}

	private void doDigits(FieldMetadata field, AnnotationMetadata digitsAnnotation, InvocableMemberBodyBuilder bodyBuilder, JavaSymbolName mutatorName, String initializer, String suffix) {
		Integer integerValue = (Integer) digitsAnnotation.getAttribute(new JavaSymbolName("integer")).getValue();
		Integer fractionValue = (Integer) digitsAnnotation.getAttribute(new JavaSymbolName("fraction")).getValue();

		String fieldName = field.getFieldName().getSymbolName();
		JavaType fieldType = field.getFieldType();

		BigDecimal maxValue = new BigDecimal(StringUtils.padRight("9", integerValue, '9') + "." + StringUtils.padRight("9", fractionValue, '9'));
		if (fieldType.equals(BIG_DECIMAL)) {
			bodyBuilder.appendFormalLine("if (" + fieldName + ".compareTo(new " + BIG_DECIMAL.getFullyQualifiedTypeName() + "(\"" + maxValue + "\")) == 1) {");
			bodyBuilder.indent();
			bodyBuilder.appendFormalLine(fieldName + " = new " + BIG_DECIMAL.getFullyQualifiedTypeName() + "(\"" + maxValue + "\");");
		} else {
			bodyBuilder.appendFormalLine("if (" + fieldName + " > " + maxValue.doubleValue() + suffix + ") {");
			bodyBuilder.indent();
			bodyBuilder.appendFormalLine(fieldName + " = " + maxValue.doubleValue() + suffix + ";");
		}

		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
	}

	private void doDecimalMinAndDecimalMax(FieldMetadata field, AnnotationMetadata decimalMinAnnotation, AnnotationMetadata decimalMaxAnnotation, InvocableMemberBodyBuilder bodyBuilder, JavaSymbolName mutatorName, String initializer, String suffix) {
		String fieldName = field.getFieldName().getSymbolName();
		JavaType fieldType = field.getFieldType();

		if (decimalMinAnnotation != null && decimalMaxAnnotation == null) {
			String minValue = (String) decimalMinAnnotation.getAttribute(new JavaSymbolName("value")).getValue();

			if (fieldType.equals(BIG_DECIMAL)) {
				bodyBuilder.appendFormalLine("if (" + fieldName + ".compareTo(new " + BIG_DECIMAL.getFullyQualifiedTypeName() + "(\"" + minValue + "\")) == -1) {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine(fieldName + " = new " + BIG_DECIMAL.getFullyQualifiedTypeName() + "(\"" + minValue + "\");");
			} else {
				bodyBuilder.appendFormalLine("if (" + fieldName + " < " + minValue + suffix + ") {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine(fieldName + " = " + minValue + suffix + ";");
			}

			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("}");
		} else if (decimalMinAnnotation == null && decimalMaxAnnotation != null) {
			String maxValue = (String) decimalMaxAnnotation.getAttribute(new JavaSymbolName("value")).getValue();

			if (fieldType.equals(BIG_DECIMAL)) {
				bodyBuilder.appendFormalLine("if (" + fieldName + ".compareTo(new " + BIG_DECIMAL.getFullyQualifiedTypeName() + "(\"" + maxValue + "\")) == 1) {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine(fieldName + " = new " + BIG_DECIMAL.getFullyQualifiedTypeName() + "(\"" + maxValue + "\");");
			} else {
				bodyBuilder.appendFormalLine("if (" + fieldName + " > " + maxValue + suffix + ") {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine(fieldName + " = " + maxValue + suffix + ";");
			}

			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("}");
		} else if (decimalMinAnnotation != null && decimalMaxAnnotation != null) {
			String minValue = (String) decimalMinAnnotation.getAttribute(new JavaSymbolName("value")).getValue();
			String maxValue = (String) decimalMaxAnnotation.getAttribute(new JavaSymbolName("value")).getValue();
			Assert.isTrue(Double.parseDouble(maxValue) >= Double.parseDouble(minValue), "The value of @DecimalMax must be greater or equal to the value of @DecimalMin for field " + fieldName);

			if (fieldType.equals(BIG_DECIMAL)) {
				bodyBuilder.appendFormalLine("if (" + fieldName + ".compareTo(new " + BIG_DECIMAL.getFullyQualifiedTypeName() + "(\"" + minValue + "\")) == -1 || " + fieldName + ".compareTo(new " + BIG_DECIMAL.getFullyQualifiedTypeName() + "(\"" + maxValue + "\")) == 1) {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine(fieldName + " = new " + BIG_DECIMAL.getFullyQualifiedTypeName() + "(\"" + maxValue + "\");");
			} else {
				bodyBuilder.appendFormalLine("if (" + fieldName + " < " + minValue + suffix + " || " + fieldName + " > " + maxValue + suffix + ") {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine(fieldName + " = " + maxValue + suffix + ";");
			}

			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("}");
		}
	}

	private void doColumnPrecisionAndScale(FieldMetadata field, Map<String, Object> values, InvocableMemberBodyBuilder bodyBuilder, JavaSymbolName mutatorName, String initializer, String suffix) {
		if (values == null || !values.containsKey("precision")) {
			return;
		}

		Integer precision = (Integer) values.get("precision");
		Integer scale = (Integer) values.get("scale");
		scale = scale == null ? 0 : scale;

		String fieldName = field.getFieldName().getSymbolName();
		JavaType fieldType = field.getFieldType();

		BigDecimal maxValue = new BigDecimal(StringUtils.padRight("9", (precision - scale), '9') + "." + StringUtils.padRight("9", scale, '9'));
		if (fieldType.equals(BIG_DECIMAL)) {
			bodyBuilder.appendFormalLine("if (" + fieldName + ".compareTo(new " + BIG_DECIMAL.getFullyQualifiedTypeName() + "(\"" + maxValue + "\")) == 1) {");
			bodyBuilder.indent();
			bodyBuilder.appendFormalLine(fieldName + " = new " + BIG_DECIMAL.getFullyQualifiedTypeName() + "(\"" + maxValue + "\");");
		} else {
			bodyBuilder.appendFormalLine("if (" + fieldName + " > " + maxValue.doubleValue() + suffix + ") {");
			bodyBuilder.indent();
			bodyBuilder.appendFormalLine(fieldName + " = " + maxValue.doubleValue() + suffix + ";");
		}

		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
	}

	private void doMinAndMax(FieldMetadata field, InvocableMemberBodyBuilder bodyBuilder, JavaSymbolName mutatorName, String initializer, String suffix) {
		String fieldName = field.getFieldName().getSymbolName();
		JavaType fieldType = field.getFieldType();

		AnnotationMetadata minAnnotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), MIN);
		AnnotationMetadata maxAnnotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), MAX);
		if (minAnnotation != null && maxAnnotation == null) {
			Number minValue = (Number) minAnnotation.getAttribute(new JavaSymbolName("value")).getValue();

			if (fieldType.equals(BIG_INTEGER)) {
				bodyBuilder.appendFormalLine("if (" + fieldName + ".compareTo(new " + BIG_INTEGER.getFullyQualifiedTypeName() + "(\"" + minValue + "\")) == -1) {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine(fieldName + " = new " + BIG_INTEGER.getFullyQualifiedTypeName() + "(\"" + minValue + "\");");
			} else {
				bodyBuilder.appendFormalLine("if (" + fieldName + " < " + minValue + suffix + ") {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine(fieldName + " = " + minValue + suffix + ";");
			}

			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("}");
		} else if (minAnnotation == null && maxAnnotation != null) {
			Number maxValue = (Number) maxAnnotation.getAttribute(new JavaSymbolName("value")).getValue();

			if (fieldType.equals(BIG_INTEGER)) {
				bodyBuilder.appendFormalLine("if (" + fieldName + ".compareTo(new " + BIG_INTEGER.getFullyQualifiedTypeName() + "(\"" + maxValue + "\")) == 1) {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine(fieldName + " = new " + BIG_INTEGER.getFullyQualifiedTypeName() + "(\"" + maxValue + "\");");
			} else {
				bodyBuilder.appendFormalLine("if (" + fieldName + " > " + maxValue + suffix + ") {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine(fieldName + " = " + maxValue + suffix + ";");
			}

			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("}");
		} else if (minAnnotation != null && maxAnnotation != null) {
			Number minValue = (Number) minAnnotation.getAttribute(new JavaSymbolName("value")).getValue();
			Number maxValue = (Number) maxAnnotation.getAttribute(new JavaSymbolName("value")).getValue();
			Assert.isTrue(maxValue.longValue() >= minValue.longValue(), "The value of @Max must be greater or equal to the value of @Min for field " + fieldName);

			if (fieldType.equals(BIG_INTEGER)) {
				bodyBuilder.appendFormalLine("if (" + fieldName + ".compareTo(new " + BIG_INTEGER.getFullyQualifiedTypeName() + "(\"" + minValue + "\")) == -1 || " + fieldName + ".compareTo(new " + BIG_INTEGER.getFullyQualifiedTypeName() + "(\"" + maxValue + "\")) == 1) {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine(fieldName + " = new " + BIG_INTEGER.getFullyQualifiedTypeName() + "(\"" + maxValue + "\");");
			} else {
				bodyBuilder.appendFormalLine("if (" + fieldName + " < " + minValue + suffix + " || " + fieldName + " > " + maxValue + suffix + ") {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine(fieldName + " = " + maxValue + suffix + ";");
			}

			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("}");
		}
	}

	/**
	 * @return the "modifyEntity(Entity):boolean" method (never returns null)
	 */
	public MethodMetadata getModifyMethod() {
		// Method definition to find or build
		JavaSymbolName methodName = new JavaSymbolName("modify" + entityType.getSimpleTypeName());
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		paramTypes.add(entityType);
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("obj"));
		JavaType returnType = JavaType.BOOLEAN_PRIMITIVE;

		// Locate user-defined method
		MethodMetadata userMethod = MemberFindingUtils.getMethod(governorTypeDetails, methodName, paramTypes);
		if (userMethod != null) {
			Assert.isTrue(userMethod.getReturnType().equals(returnType), "Method '" + methodName + "' on '" + destination + "' must return '" + returnType.getNameIncludingTypeParameters() + "'");
			return userMethod;
		}

		// Create method
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return false;");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, bodyBuilder);
		return methodBuilder.build();
	}

	/**
	 * @return the "getRandomEntity():Entity" method (never returns null)
	 */
	public MethodMetadata getRandomPersistentEntityMethod() {
		// Method definition to find or build
		JavaSymbolName methodName = new JavaSymbolName("getRandom" + entityType.getSimpleTypeName());
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();

		// Locate user-defined method
		MethodMetadata userMethod = MemberFindingUtils.getMethod(governorTypeDetails, methodName, paramTypes);
		if (userMethod != null) {
			Assert.isTrue(userMethod.getReturnType().equals(entityType), "Method '" + methodName + "' on '" + destination + "' must return '" + entityType.getNameIncludingTypeParameters() + "'");
			return userMethod;
		}

		// Create method
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("init();");
		bodyBuilder.appendFormalLine(entityType.getSimpleTypeName() + " obj = " + getDataField().getFieldName().getSymbolName() + ".get(" + getRndField().getFieldName().getSymbolName() + ".nextInt(" + getDataField().getFieldName().getSymbolName() + ".size()));");
		bodyBuilder.appendFormalLine("return " + entityType.getSimpleTypeName() + "." + findMethod.getMethodName().getSymbolName() + "(obj." + identifierAccessor.getMethodName().getSymbolName() + "());");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, entityType, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, bodyBuilder);
		return methodBuilder.build();
	}

	/**
	 * @return the "getSpecificEntity(int):Entity" method (never returns null)
	 */
	public MethodMetadata getSpecificPersistentEntityMethod() {
		// Method definition to find or build
		JavaSymbolName methodName = new JavaSymbolName("getSpecific" + entityType.getSimpleTypeName());
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		paramTypes.add(JavaType.INT_PRIMITIVE);
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("index"));

		// Locate user-defined method
		MethodMetadata userMethod = MemberFindingUtils.getMethod(governorTypeDetails, methodName, paramTypes);
		if (userMethod != null) {
			Assert.isTrue(userMethod.getReturnType().equals(entityType), "Method '" + methodName + "' on '" + destination + "' must return '" + entityType.getNameIncludingTypeParameters() + "'");
			return userMethod;
		}

		// Create method
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("init();");
		bodyBuilder.appendFormalLine("if (index < 0) index = 0;");
		bodyBuilder.appendFormalLine("if (index > (" + getDataField().getFieldName().getSymbolName() + ".size() - 1)) index = " + getDataField().getFieldName().getSymbolName() + ".size() - 1;");
		bodyBuilder.appendFormalLine(entityType.getSimpleTypeName() + " obj = " + getDataField().getFieldName().getSymbolName() + ".get(index);");
		bodyBuilder.appendFormalLine("return " + entityType.getSimpleTypeName() + "." + findMethod.getMethodName().getSymbolName() + "(obj." + identifierAccessor.getMethodName().getSymbolName() + "());");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, entityType, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, bodyBuilder);
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
			Assert.isTrue(userMethod.getReturnType().equals(returnType), "Method '" + methodName + "' on '" + destination + "' must return '" + returnType.getNameIncludingTypeParameters() + "'");
			return userMethod;
		}

		// Create the method body
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		String dataField = getDataField().getFieldName().getSymbolName();
		bodyBuilder.appendFormalLine(dataField + " = " + entityType.getFullyQualifiedTypeName() + "." + findEntriesMethod.getMethodName().getSymbolName() + "(0, " + annotationValues.getQuantity() + ");");
		bodyBuilder.appendFormalLine("if (data == null) throw new IllegalStateException(\"Find entries implementation for '" + entityType.getSimpleTypeName() + "' illegally returned null\");");
		bodyBuilder.appendFormalLine("if (!" + dataField + ".isEmpty()) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return;");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("");
		bodyBuilder.appendFormalLine(dataField + " = new java.util.ArrayList<" + getDataField().getFieldType().getParameters().get(0).getNameIncludingTypeParameters() + ">();");
		bodyBuilder.appendFormalLine("for (int i = 0; i < " + annotationValues.getQuantity() + "; i++) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(entityType.getFullyQualifiedTypeName() + " obj = " + getNewTransientEntityMethod().getMethodName() + "(i);");
		bodyBuilder.appendFormalLine("obj." + persistMethod.getMethodName().getSymbolName() + "();");
		bodyBuilder.appendFormalLine("obj." + flushMethod.getMethodName().getSymbolName() + "();");
		bodyBuilder.appendFormalLine(dataField + ".add(obj);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");

		// Create the method
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, bodyBuilder);
		return methodBuilder.build();
	}

	public boolean hasEmbeddedIdentifier() {
		return embeddedIdentifierHolder != null;
	}

	private void storeEmbeddedIdInitializers() {
		if (!hasEmbeddedIdentifier()) {
			return;
		}

		for (FieldMetadata field : embeddedIdentifierHolder.getIdentifierFields()) {
			String initializer = getFieldInitializer(field, null);
			embeddedIdInitializers.put(field, initializer);
		}
	}

	private void storeFieldInitializers() {
		for (MethodMetadata mutatorMethod : locatedMutators.keySet()) {
			CollaboratingDataOnDemandMetadataHolder metadataHolder = locatedMutators.get(mutatorMethod);
			FieldMetadata field = metadataHolder.getField();
			String initializer = getFieldInitializer(field, metadataHolder.getDataOnDemandMetadata());
			fieldInitializers.put(mutatorMethod, initializer);
		}
	}

	private String getFieldInitializer(FieldMetadata field, DataOnDemandMetadata collaboratingMetadata) {
		JavaType fieldType = field.getFieldType();
		String initializer = "null";
		String fieldInitializer = field.getFieldInitializer();
		Set<Object> fieldCustomDataKeys = field.getCustomData().keySet();

		// Date fields included for DataNucleus (
		if (fieldType.equals(new JavaType(Date.class.getName()))) {
			if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.Past")) != null) {
				initializer = "new java.util.Date(new java.util.Date().getTime() - 10000000L)";
			} else if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.Future")) != null) {
				initializer = "new java.util.Date(new java.util.Date().getTime() + 10000000L)";
			} else {
				initializer = "new java.util.GregorianCalendar(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR), java.util.Calendar.getInstance().get(java.util.Calendar.MONTH), java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH), java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY), java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE), java.util.Calendar.getInstance().get(java.util.Calendar.SECOND) + new Double(Math.random() * 1000).intValue()).getTime()";
				// initializer = "new java.util.Date()";
			}
		} else if (fieldType.equals(JavaType.STRING_OBJECT)) {
			if (fieldInitializer != null && fieldInitializer.contains("\"")) {
				int offset = fieldInitializer.indexOf("\"");
				initializer = fieldInitializer.substring(offset + 1, fieldInitializer.lastIndexOf("\""));
			} else {
				initializer = field.getFieldName().getSymbolName();
			}

			int maxLength = Integer.MAX_VALUE;

			// Check for @Size
			AnnotationMetadata sizeAnnotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), SIZE);
			if (sizeAnnotation != null) {
				AnnotationAttributeValue<?> maxValue = sizeAnnotation.getAttribute(new JavaSymbolName("max"));
				if (maxValue != null) {
					maxLength = ((Integer) maxValue.getValue()).intValue();
				}
				AnnotationAttributeValue<?> minValue = sizeAnnotation.getAttribute(new JavaSymbolName("min"));
				if (minValue != null) {
					int minLength = ((Integer) minValue.getValue()).intValue();
					Assert.isTrue(maxLength >= minLength, "@Size attribute 'max' must be greater than 'min' for field '" + field.getFieldName().getSymbolName() + "' in " + entityType.getFullyQualifiedTypeName());
					if (initializer.length() + 2 < minLength) {
						initializer = String.format("%1$-" + (minLength - 2) + "s", initializer).replace(' ', 'x');
					}
				}
			} else {
				if (field.getCustomData().keySet().contains(PersistenceCustomDataKeys.COLUMN_FIELD)) {
					@SuppressWarnings("unchecked") Map<String, Object> columnValues = (Map<String, Object>) field.getCustomData().get(PersistenceCustomDataKeys.COLUMN_FIELD);
					if (columnValues.keySet().contains("length")) {
						maxLength = ((Integer) columnValues.get("length")).intValue();
					}
				}
			}

			switch (maxLength) {
				case 0:
					initializer = "\"\"";
					break;
				case 1:
					initializer = "String.valueOf(index)";
					break;
				case 2:
					initializer = "\"" + initializer.charAt(0) + "\" + index";
					break;
				default:
					if (initializer.length() + 2 > maxLength) {
						initializer = "\"" + initializer.substring(0, maxLength - 2) + "_\" + index";
					} else {
						initializer = "\"" + initializer + "_\" + index";
					}
			}
		} else if (fieldType.equals(new JavaType(Calendar.class.getName()))) {
			String calendarString = "new java.util.GregorianCalendar(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR), java.util.Calendar.getInstance().get(java.util.Calendar.MONTH), java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)";
			if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.Past")) != null) {
				initializer = calendarString + " - 1)";
			} else if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.Future")) != null) {
				initializer = calendarString + " + 1)";
			} else {
				initializer = "java.util.Calendar.getInstance()";
			}
		} else if (fieldType.equals(new JavaType("java.lang.String", 1, DataType.TYPE, null, null))) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "{ \"Y\", \"N\" }");
		} else if (fieldType.equals(JavaType.BOOLEAN_OBJECT)) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "Boolean.TRUE");
		} else if (fieldType.equals(JavaType.BOOLEAN_PRIMITIVE)) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "true");
		} else if (fieldType.equals(JavaType.INT_OBJECT)) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(index)");
		} else if (fieldType.equals(JavaType.INT_PRIMITIVE)) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "index");
		} else if (fieldType.equals(new JavaType("java.lang.Integer", 1, DataType.PRIMITIVE, null, null))) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "{ index, index }");
		} else if (fieldType.equals(JavaType.DOUBLE_OBJECT)) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(index).doubleValue()"); // Auto-boxed
		} else if (fieldType.equals(JavaType.DOUBLE_PRIMITIVE)) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(index).doubleValue()");
		} else if (fieldType.equals(new JavaType("java.lang.Double", 1, DataType.PRIMITIVE, null, null))) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "{ new Integer(index).doubleValue(), new Integer(index).doubleValue() }");
		} else if (fieldType.equals(JavaType.FLOAT_OBJECT)) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(index).floatValue()"); // Auto-boxed
		} else if (fieldType.equals(JavaType.FLOAT_PRIMITIVE)) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(index).floatValue()");
		} else if (fieldType.equals(new JavaType("java.lang.Float", 1, DataType.PRIMITIVE, null, null))) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "{ new Integer(index).floatValue(), new Integer(index).floatValue() }");
		} else if (fieldType.equals(JavaType.LONG_OBJECT)) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(index).longValue()"); // Auto-boxed
		} else if (fieldType.equals(JavaType.LONG_PRIMITIVE)) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(index).longValue()");
		} else if (fieldType.equals(new JavaType("java.lang.Long", 1, DataType.PRIMITIVE, null, null))) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "{ new Integer(index).longValue(), new Integer(index).longValue() }");
		} else if (fieldType.equals(JavaType.SHORT_OBJECT)) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(index).shortValue()"); // Auto-boxed
		} else if (fieldType.equals(JavaType.SHORT_PRIMITIVE)) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(index).shortValue()");
		} else if (fieldType.equals(new JavaType("java.lang.Short", 1, DataType.PRIMITIVE, null, null))) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "{ new Integer(index).shortValue(), new Integer(index).shortValue() }");
		} else if (fieldType.equals(JavaType.CHAR_OBJECT)) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Character('N')");
		} else if (fieldType.equals(JavaType.CHAR_PRIMITIVE)) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "'N'");
		} else if (fieldType.equals(new JavaType("java.lang.Character", 1, DataType.PRIMITIVE, null, null))) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "{ 'Y', 'N' }");
		} else if (fieldType.equals(BIG_DECIMAL)) {
			initializer = BIG_DECIMAL.getFullyQualifiedTypeName() + ".valueOf(index)";
		} else if (fieldType.equals(BIG_INTEGER)) {
			initializer = BIG_INTEGER.getFullyQualifiedTypeName() + ".valueOf(index)";
		} else if (fieldType.equals(JavaType.BYTE_OBJECT)) {
			initializer = "new Byte(" + StringUtils.defaultIfEmpty(fieldInitializer, "\"1\"") + ")";
		} else if (fieldType.equals(JavaType.BYTE_PRIMITIVE)) {
			initializer = "new Byte(" + StringUtils.defaultIfEmpty(fieldInitializer, "\"1\"") + ").byteValue()";
		} else if (fieldType.equals(new JavaType("java.lang.Byte", 1, DataType.PRIMITIVE, null, null))) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "String.valueOf(index).getBytes()");
		} else if (fieldType.equals(annotationValues.getEntity())) {
			// Avoid circular references (ROO-562)
			initializer = "obj";
		} else if (fieldCustomDataKeys.contains(PersistenceCustomDataKeys.ENUMERATED_FIELD)) {
			initializer = field.getFieldType().getFullyQualifiedTypeName() + ".class.getEnumConstants()[0]";
		} else if (collaboratingMetadata != null) {
			requiredDataOnDemandCollaborators.add(field.getFieldType());

			String collaboratingFieldName = getCollaboratingFieldName(field.getFieldType()).getSymbolName();
			// Decide if we're dealing with a one-to-one and therefore should _try_ to keep the same id (ROO-568)
			if (fieldCustomDataKeys.contains(PersistenceCustomDataKeys.ONE_TO_ONE_FIELD)) {
				initializer = collaboratingFieldName + "." + collaboratingMetadata.getSpecificPersistentEntityMethod().getMethodName().getSymbolName() + "(index)";
			} else {
				initializer = collaboratingFieldName + "." + collaboratingMetadata.getRandomPersistentEntityMethod().getMethodName().getSymbolName() + "()";
			}
		}

		return initializer;
	}

	private boolean isIntegerFieldType(JavaType fieldType) {
		return fieldType.equals(BIG_INTEGER) || fieldType.equals(JavaType.INT_PRIMITIVE) || fieldType.equals(JavaType.INT_OBJECT) || fieldType.equals(JavaType.LONG_PRIMITIVE) || fieldType.equals(JavaType.LONG_OBJECT) || fieldType.equals(JavaType.SHORT_PRIMITIVE) || fieldType.equals(JavaType.SHORT_OBJECT);
	}

	private boolean isDecimalFieldType(JavaType fieldType) {
		return fieldType.equals(BIG_DECIMAL) || fieldType.equals(JavaType.DOUBLE_PRIMITIVE) || fieldType.equals(JavaType.DOUBLE_OBJECT) || fieldType.equals(JavaType.FLOAT_PRIMITIVE) || fieldType.equals(JavaType.FLOAT_OBJECT);
	}

	private JavaSymbolName getCollaboratingFieldName(JavaType entity) {
		return new JavaSymbolName(StringUtils.uncapitalize(getCollaboratingType(entity).getSimpleTypeName()));
	}

	private JavaType getCollaboratingType(JavaType entity) {
		return new JavaType(entity.getFullyQualifiedTypeName() + "DataOnDemand");
	}

	private JavaSymbolName getRequiredMutatorName(FieldMetadata field) {
		return new JavaSymbolName("set" + StringUtils.capitalize(field.getFieldName().getSymbolName()));
	}

	private JavaSymbolName getEmbeddedIdMutatorMethod() {
		List<JavaSymbolName> fieldNames = new ArrayList<JavaSymbolName>();
		for (MethodMetadata mutator : fieldInitializers.keySet()) {
			fieldNames.add(locatedMutators.get(mutator).getField().getFieldName());
		}

		int index = -1;
		JavaSymbolName embeddedIdField;
		while (true) {
			// Compute the required field name
			index++;
			String fieldName = "";
			for (int i = 0; i < index; i++) {
				fieldName = fieldName + "_";
			}
			fieldName = "embeddedId" + fieldName;

			embeddedIdField = new JavaSymbolName(fieldName);
			if (!fieldNames.contains(embeddedIdField)) {
				// Found a usable name
				break;
			}
		}
		return new JavaSymbolName("set" + StringUtils.capitalize(embeddedIdField.getSymbolName()));
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

	public static String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}

	public static String createIdentifier(JavaType javaType, Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static JavaType getJavaType(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static Path getPath(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
}