package org.springframework.roo.addon.dod;

import java.lang.reflect.Modifier;
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
	private static final JavaType COLUMN = new JavaType("javax.persistence.Column");
	private static final JavaType BIG_INTEGER = new JavaType("java.math.BigInteger");
	private static final JavaType BIG_DECIMAL = new JavaType("java.math.BigDecimal");

	private DataOnDemandAnnotationValues annotationValues;
	private MethodMetadata identifierAccessorMethod;
	private MethodMetadata findMethod;
	private MethodMetadata findEntriesMethod;
	private MethodMetadata persistMethod;
	private MethodMetadata flushMethod;
	private Map<MethodMetadata, CollaboratingDataOnDemandMetadataHolder> locatedMutators;
	private JavaType entityType;
	
	private Map<MethodMetadata, String> mandatoryMutators = new LinkedHashMap<MethodMetadata, String>();
	private List<JavaType> requiredDataOnDemandCollaborators = new LinkedList<JavaType>();

	public DataOnDemandMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, DataOnDemandAnnotationValues annotationValues, MethodMetadata identifierAccessor, MethodMetadata findMethod, MethodMetadata findEntriesMethod, MethodMetadata persistMethod, MethodMetadata flushMethod, Map<MethodMetadata, CollaboratingDataOnDemandMetadataHolder> locatedMutators, JavaType entityType) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.notNull(identifierAccessor, "Identifier accessor method required");
		Assert.notNull(findMethod, "Find method required");
		Assert.notNull(findEntriesMethod, "Find entries method required");
		Assert.notNull(persistMethod, "Persist method required");
		Assert.notNull(flushMethod, "Flush method required");
		Assert.notNull(locatedMutators, "Located mutator methods map required");
		Assert.notNull(entityType, "Entity required");

		if (!isValid()) {
			return;
		}

		this.annotationValues = annotationValues;
		this.identifierAccessorMethod = identifierAccessor;
		this.findMethod = findMethod;
		this.findEntriesMethod = findEntriesMethod;
		this.persistMethod = persistMethod;
		this.flushMethod = flushMethod;
		this.locatedMutators = locatedMutators;
		this.entityType = entityType;

		// Calculate and store field initializers
		storeFieldInitializers();

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
		if (isComponentAnnotationIntroduced()) {
			AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(COMPONENT);
			return annotationBuilder.build();
		}
		return MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, COMPONENT);
	}

	/**
	 * Indicates whether the @org.springframework.stereotype.Component annotation will be introduced via this ITD.
	 * 
	 * @return true if it will be introduced, false otherwise
	 */
	public boolean isComponentAnnotationIntroduced() {
		return MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, COMPONENT) == null;
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
		JavaSymbolName methodName = new JavaSymbolName("getNewTransient" + entityType.getSimpleTypeName());
		
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		paramTypes.add(JavaType.INT_PRIMITIVE);
		
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("index"));
		
		// Locate user-defined method
		MethodMetadata userMethod = MemberFindingUtils.getMethod(governorTypeDetails, methodName, paramTypes);
		if (userMethod != null) {
			Assert.isTrue(userMethod.getReturnType().equals(entityType), "Method '" + methodName + "' on '" + governorTypeDetails.getName() + "' must return '" + entityType.getNameIncludingTypeParameters() + "'");
			return userMethod;
		}

		// Create method
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(entityType.getFullyQualifiedTypeName() + " obj = new " + entityType.getFullyQualifiedTypeName() + "();");

		for (MethodMetadata mutator : mandatoryMutators.keySet()) {
			String initializer = mandatoryMutators.get(mutator);
			String mutatorName = mutator.getMethodName().getSymbolName();
			Assert.hasText(initializer, "Internal error: unable to locate initializer for " + mutatorName);

			FieldMetadata field = locatedMutators.get(mutator).getField();
			String fieldType = field.getFieldType().getFullyQualifiedTypeName();
			String fieldName = field.getFieldName().getSymbolName();
			
			String suffix = "";
			if (field.getFieldType().equals(JavaType.LONG_OBJECT) || field.getFieldType().equals(JavaType.LONG_PRIMITIVE)) {
				suffix = "L";
			} else if (field.getFieldType().equals(JavaType.FLOAT_OBJECT) || field.getFieldType().equals(JavaType.FLOAT_PRIMITIVE)) {
				suffix = "F";
			} else if (field.getFieldType().equals(JavaType.DOUBLE_OBJECT) || field.getFieldType().equals(JavaType.DOUBLE_PRIMITIVE)) {
				suffix = "D";
			}

			if (field.getFieldType().equals(JavaType.STRING_OBJECT)) {
				// Check for @Size or @Column with length attribute
				AnnotationMetadata sizeAnnotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), SIZE);
				AnnotationMetadata columnAnnotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), COLUMN);

				if (sizeAnnotation != null && sizeAnnotation.getAttribute(new JavaSymbolName("max")) != null) {
					Integer maxValue = (Integer) sizeAnnotation.getAttribute(new JavaSymbolName("max")).getValue();
					bodyBuilder.appendFormalLine(fieldType + " " + fieldName + " = " + initializer + ";");
					bodyBuilder.appendFormalLine("if (" + fieldName + ".length() > " + maxValue + ") {");
					bodyBuilder.indent();
					bodyBuilder.appendFormalLine(fieldName + "  = " + fieldName + ".substring(0, " + maxValue + ");");
					bodyBuilder.indentRemove();
					bodyBuilder.appendFormalLine("}");
					bodyBuilder.appendFormalLine("obj." + mutatorName + "(" + fieldName + ");");
				} else if (sizeAnnotation == null && columnAnnotation != null) {
					AnnotationAttributeValue<?> lengthAttributeValue = columnAnnotation.getAttribute(new JavaSymbolName("length"));
					if (lengthAttributeValue != null) {
						Integer lengthValue = (Integer) columnAnnotation.getAttribute(new JavaSymbolName("length")).getValue();
						bodyBuilder.appendFormalLine(fieldType + " " + fieldName + " = " + initializer + ";");
						bodyBuilder.appendFormalLine("if (" + fieldName + ".length() > " + lengthValue + ") {");
						bodyBuilder.indent();
						bodyBuilder.appendFormalLine(fieldName + "  = " + fieldName + ".substring(0, " + lengthValue + ");");
						bodyBuilder.indentRemove();
						bodyBuilder.appendFormalLine("}");
						bodyBuilder.appendFormalLine("obj." + mutatorName + "(" + fieldName + ");");
					} else {
						bodyBuilder.appendFormalLine("obj." + mutatorName + "(" + initializer + ");");
					}
				} else {
					bodyBuilder.appendFormalLine("obj." + mutatorName + "(" + initializer + ");");
				}
			} else if (field.getFieldType().equals(JavaType.CHAR_OBJECT) || field.getFieldType().equals(JavaType.CHAR_PRIMITIVE)) {
				bodyBuilder.appendFormalLine("obj." + mutatorName + "('N');");
			} else if (isDecimalFieldType(field)) {
				// Check for @DecimalMin and @DecimalMax
				doDecimalMinAndDecimalMax(field, bodyBuilder, mutatorName, initializer, suffix);
			} else if (isIntegerFieldType(field)) {
				// Check for @Min and @Max
				doMinAndMax(field, bodyBuilder, mutatorName, initializer, suffix);
			} else {
				bodyBuilder.appendFormalLine("obj." + mutatorName + "(" + initializer + ");");
			}
		}

		bodyBuilder.appendFormalLine("return obj;");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, entityType, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, bodyBuilder);
		return methodBuilder.build();
	}

	private void doDecimalMinAndDecimalMax(FieldMetadata field, InvocableMemberBodyBuilder bodyBuilder, String mutatorName, String initializer, String suffix) {
		String fieldType = field.getFieldType().getFullyQualifiedTypeName();
		String fieldName = field.getFieldName().getSymbolName();
		
		AnnotationMetadata decimalMinAnnotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.DecimalMin"));
		AnnotationMetadata decimalMaxAnnotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.DecimalMax"));
		if (decimalMinAnnotation != null && decimalMaxAnnotation == null) {
			String minValue = (String) decimalMinAnnotation.getAttribute(new JavaSymbolName("value")).getValue();

			bodyBuilder.appendFormalLine(fieldType + " " + fieldName + " = " + initializer + ";");
			if (field.getFieldType().equals(BIG_DECIMAL)) {
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
			bodyBuilder.appendFormalLine("obj." + mutatorName + "(" + fieldName + ");");
		} else if (decimalMinAnnotation == null && decimalMaxAnnotation != null) {
			String maxValue = (String) decimalMaxAnnotation.getAttribute(new JavaSymbolName("value")).getValue();

			bodyBuilder.appendFormalLine(fieldType + " " + fieldName + " = " + initializer + ";");

			if (field.getFieldType().equals(BIG_DECIMAL)) {
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
			bodyBuilder.appendFormalLine("obj." + mutatorName + "(" + fieldName + ");");
		} else if (decimalMinAnnotation != null && decimalMaxAnnotation != null) {
			String minValue = (String) decimalMinAnnotation.getAttribute(new JavaSymbolName("value")).getValue();
			String maxValue = (String) decimalMaxAnnotation.getAttribute(new JavaSymbolName("value")).getValue();
			Assert.isTrue(Double.parseDouble(maxValue) >= Double.parseDouble(minValue), "The value of @DecimalMax must be greater or equal to the value of @DecimalMin for field " + fieldName);

			bodyBuilder.appendFormalLine(fieldType + " " + fieldName + " = " + initializer + ";");
			
			if (field.getFieldType().equals(BIG_DECIMAL)) {
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
			bodyBuilder.appendFormalLine("obj." + mutatorName + "(" + fieldName + ");");
		} else {
			bodyBuilder.appendFormalLine("obj." + mutatorName + "(" + initializer + ");");
		}		
	}

	private void doMinAndMax(FieldMetadata field, InvocableMemberBodyBuilder bodyBuilder, String mutatorName, String initializer, String suffix) {
		String fieldType = field.getFieldType().getFullyQualifiedTypeName();
		String fieldName = field.getFieldName().getSymbolName();

		AnnotationMetadata minAnnotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), MIN);
		AnnotationMetadata maxAnnotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), MAX);
		if (minAnnotation != null && maxAnnotation == null) {
			Number minValue = (Number) minAnnotation.getAttribute(new JavaSymbolName("value")).getValue();

			bodyBuilder.appendFormalLine(fieldType + " " + fieldName + " = " + initializer + ";");
			
			if (field.getFieldType().equals(BIG_INTEGER)) {
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
			bodyBuilder.appendFormalLine("obj." + mutatorName + "(" + fieldName + ");");
		} else if (minAnnotation == null && maxAnnotation != null) {
			Number maxValue = (Number) maxAnnotation.getAttribute(new JavaSymbolName("value")).getValue();

			bodyBuilder.appendFormalLine(fieldType + " " + fieldName + " = " + initializer + ";");
			
			if (field.getFieldType().equals(BIG_INTEGER)) {
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
			bodyBuilder.appendFormalLine("obj." + mutatorName + "(" + fieldName + ");");
		} else if (minAnnotation != null && maxAnnotation != null) {
			Number minValue = (Number) minAnnotation.getAttribute(new JavaSymbolName("value")).getValue();
			Number maxValue = (Number) maxAnnotation.getAttribute(new JavaSymbolName("value")).getValue();
			Assert.isTrue(maxValue.longValue() >= minValue.longValue(), "The value of @Max must be greater or equal to the value of @Min for field " + fieldName);

			bodyBuilder.appendFormalLine(fieldType + " " + fieldName + " = " + initializer + ";");

			if (field.getFieldType().equals(BIG_INTEGER)) {
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
			bodyBuilder.appendFormalLine("obj." + mutatorName + "(" + fieldName + ");");
		} else {
			bodyBuilder.appendFormalLine("obj." + mutatorName + "(" + initializer + ");");
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
			Assert.isTrue(userMethod.getReturnType().equals(returnType), "Method '" + methodName + "' on '" + governorTypeDetails.getName() + "' must return '" + returnType.getNameIncludingTypeParameters() + "'");
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
			Assert.isTrue(userMethod.getReturnType().equals(entityType), "Method '" + methodName + "' on '" + governorTypeDetails.getName() + "' must return '" + entityType.getNameIncludingTypeParameters() + "'");
			return userMethod;
		}

		// Create method
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("init();");
		bodyBuilder.appendFormalLine(entityType.getSimpleTypeName() + " obj = " + getDataField().getFieldName().getSymbolName() + ".get(" + getRndField().getFieldName().getSymbolName() + ".nextInt(" + getDataField().getFieldName().getSymbolName() + ".size()));");
		bodyBuilder.appendFormalLine("return " + entityType.getSimpleTypeName() + "." + findMethod.getMethodName().getSymbolName() + "(obj." + identifierAccessorMethod.getMethodName().getSymbolName() + "());");

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
			Assert.isTrue(userMethod.getReturnType().equals(entityType), "Method '" + methodName + "' on '" + governorTypeDetails.getName() + "' must return '" + entityType.getNameIncludingTypeParameters() + "'");
			return userMethod;
		}

		// Create method
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("init();");
		bodyBuilder.appendFormalLine("if (index < 0) index = 0;");
		bodyBuilder.appendFormalLine("if (index > (" + getDataField().getFieldName().getSymbolName() + ".size() - 1)) index = " + getDataField().getFieldName().getSymbolName() + ".size() - 1;");
		bodyBuilder.appendFormalLine(entityType.getSimpleTypeName() + " obj = " + getDataField().getFieldName().getSymbolName() + ".get(index);");
		bodyBuilder.appendFormalLine("return " + entityType.getSimpleTypeName() + "." + findMethod.getMethodName().getSymbolName() + "(obj." + identifierAccessorMethod.getMethodName().getSymbolName() + "());");

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
			Assert.isTrue(userMethod.getReturnType().equals(returnType), "Method '" + methodName + "' on '" + governorTypeDetails.getName() + "' must return '" + returnType.getNameIncludingTypeParameters() + "'");
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

	private void storeFieldInitializers() {
		for (MethodMetadata mutatorMethod : locatedMutators.keySet()) {
			CollaboratingDataOnDemandMetadataHolder metadataHolder = locatedMutators.get(mutatorMethod);
			FieldMetadata field = metadataHolder.getField();

			// Never include id or version fields (they shouldn't normally have a mutator anyway, but the user might have added one)
			if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.Id")) != null || MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.Version")) != null) {
				continue;
			}

			// Never include field annotated with @javax.persistence.Transient
			if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.Transient")) != null) {
				continue;
			}

			// Never include any sort of collection; user has to make such entities by hand
			if (field.getFieldType().isCommonCollectionType() || MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.OneToMany")) != null || MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.ManyToMany")) != null) {
				continue;
			}
			
			String initializer = "null";
			String fieldInitializer = field.getFieldInitializer();

			// Date fields included for DataNucleus (
			if (field.getFieldType().equals(new JavaType(Date.class.getName()))) {
				if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.Past")) != null) {
					initializer = "new java.util.Date(new java.util.Date().getTime() - 10000000L)";
				} else if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.Future")) != null) {
					initializer = "new java.util.Date(new java.util.Date().getTime() + 10000000L)";
				} else {
					initializer = "new java.util.GregorianCalendar(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR), java.util.Calendar.getInstance().get(java.util.Calendar.MONTH), java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH), java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY), java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE), java.util.Calendar.getInstance().get(java.util.Calendar.SECOND) + new Double(Math.random() * 1000).intValue()).getTime()";
					// initializer = "new java.util.Date()";
				}
			} else if (field.getFieldType().equals(JavaType.STRING_OBJECT)) {
				if (fieldInitializer != null && fieldInitializer.contains("\"")) {
					int offset = fieldInitializer.indexOf("\"");
					initializer = fieldInitializer.substring(offset + 1, fieldInitializer.lastIndexOf("\""));
				} else {
					initializer = field.getFieldName().getSymbolName();
				}
				
				// Check for @Size
				AnnotationMetadata sizeAnnotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), SIZE);
				if (sizeAnnotation != null) {
					AnnotationAttributeValue<?> maxValue = sizeAnnotation.getAttribute(new JavaSymbolName("max"));
					if (maxValue != null && (Integer) maxValue.getValue() > 1 && (initializer.length() + 2) > (Integer) maxValue.getValue()) {
						initializer = initializer.substring(0, (Integer) maxValue.getValue() - 2);
					}
					AnnotationAttributeValue<?> minValue = sizeAnnotation.getAttribute(new JavaSymbolName("min"));
					if (minValue != null && (initializer.length() + 2) < (Integer) minValue.getValue()) {
						initializer = String.format("%1$-" + ((Integer) minValue.getValue() - 2) + "s", initializer).replace(' ', 'x');
					}
				} else {
					// Check for @Column
					AnnotationMetadata columnAnnotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), COLUMN);
					if (columnAnnotation != null) {
						AnnotationAttributeValue<?> lengthValue = columnAnnotation.getAttribute(new JavaSymbolName("length"));
						if (lengthValue != null && (initializer.length() + 2) > (Integer) lengthValue.getValue()) {
							initializer = initializer.substring(0, (Integer) lengthValue.getValue() - 2);
						}
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
				initializer = StringUtils.defaultIfEmpty(fieldInitializer, "Boolean.TRUE");
			} else if (field.getFieldType().equals(JavaType.BOOLEAN_PRIMITIVE)) {
				initializer = StringUtils.defaultIfEmpty(fieldInitializer, "true");
			} else if (field.getFieldType().equals(JavaType.INT_OBJECT)) {
				initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(index)");
			} else if (field.getFieldType().equals(JavaType.INT_PRIMITIVE)) {
				initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(index)"); // Auto-boxed
			} else if (field.getFieldType().equals(JavaType.DOUBLE_OBJECT)) {
				initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(index).doubleValue()"); // Auto-boxed
			} else if (field.getFieldType().equals(JavaType.DOUBLE_PRIMITIVE)) {
				initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(index).doubleValue()");
			} else if (field.getFieldType().equals(JavaType.FLOAT_OBJECT)) {
				initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(index).floatValue()"); // Auto-boxed
			} else if (field.getFieldType().equals(JavaType.FLOAT_PRIMITIVE)) {
				initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(index).floatValue()");
			} else if (field.getFieldType().equals(JavaType.LONG_OBJECT)) {
				initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(index).longValue()"); // Auto-boxed
			} else if (field.getFieldType().equals(JavaType.LONG_PRIMITIVE)) {
				initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(index).longValue()");
			} else if (field.getFieldType().equals(JavaType.SHORT_OBJECT)) {
				initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(index).shortValue()"); // Auto-boxed
			} else if (field.getFieldType().equals(JavaType.SHORT_PRIMITIVE)) {
				initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(index).shortValue()");
			} else if (field.getFieldType().equals(BIG_DECIMAL)) {
				initializer = BIG_DECIMAL.getFullyQualifiedTypeName() + ".valueOf(index)";
			} else if (field.getFieldType().equals(BIG_INTEGER)) {
				initializer = BIG_INTEGER.getFullyQualifiedTypeName() + ".valueOf(index)";
			} else if (field.getFieldType().equals(annotationValues.getEntity())) {
				// Avoid circular references (ROO-562)
				initializer = "obj";
			} else if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.Enumerated")) != null) {
				initializer = field.getFieldType().getFullyQualifiedTypeName() + ".class.getEnumConstants()[0]";
			} else if (metadataHolder.getDataOnDemandMetadata() != null) {
				requiredDataOnDemandCollaborators.add(field.getFieldType());

				// Decide if we're dealing with a one-to-one and therefore should _try_ to keep the same id (ROO-568)
				AnnotationMetadata oneToOneAnnotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.OneToOne"));
				DataOnDemandMetadata otherMetadata = metadataHolder.getDataOnDemandMetadata();
				String collaboratingFieldName = getCollaboratingFieldName(field.getFieldType()).getSymbolName();
				if (oneToOneAnnotation != null) {
					initializer = collaboratingFieldName + "." + otherMetadata.getSpecificPersistentEntityMethod().getMethodName().getSymbolName() + "(index)";
				} else {
					initializer = collaboratingFieldName + "." + otherMetadata.getRandomPersistentEntityMethod().getMethodName().getSymbolName() + "()";
				}
			}

			mandatoryMutators.put(mutatorMethod, initializer);
		}
	}
	
	private boolean isIntegerFieldType(FieldMetadata field) {
		return field.getFieldType().equals(BIG_INTEGER) || field.getFieldType().equals(JavaType.INT_PRIMITIVE) || field.getFieldType().equals(JavaType.INT_OBJECT) || field.getFieldType().equals(JavaType.LONG_PRIMITIVE) || field.getFieldType().equals(JavaType.LONG_OBJECT) || field.getFieldType().equals(JavaType.SHORT_PRIMITIVE) || field.getFieldType().equals(JavaType.SHORT_OBJECT);
	}

	private boolean isDecimalFieldType(FieldMetadata field) {
		return field.getFieldType().equals(BIG_DECIMAL) || field.getFieldType().equals(JavaType.DOUBLE_PRIMITIVE) || field.getFieldType().equals(JavaType.DOUBLE_OBJECT) || field.getFieldType().equals(JavaType.FLOAT_PRIMITIVE) || field.getFieldType().equals(JavaType.FLOAT_OBJECT);
	}

	private JavaSymbolName getCollaboratingFieldName(JavaType entity) {
		return new JavaSymbolName(StringUtils.uncapitalize(getCollaboratingType(entity).getSimpleTypeName()));
	}

	private JavaType getCollaboratingType(JavaType entity) {
		return new JavaType(entity.getFullyQualifiedTypeName() + "DataOnDemand");
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