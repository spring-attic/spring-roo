package org.springframework.roo.addon.dod;

import static org.springframework.roo.model.HibernateJavaType.VALIDATOR_CONSTRAINTS_EMAIL;
import static org.springframework.roo.model.JavaType.STRING;
import static org.springframework.roo.model.JdkJavaType.ARRAY_LIST;
import static org.springframework.roo.model.JdkJavaType.BIG_DECIMAL;
import static org.springframework.roo.model.JdkJavaType.BIG_INTEGER;
import static org.springframework.roo.model.JdkJavaType.CALENDAR;
import static org.springframework.roo.model.JdkJavaType.DATE;
import static org.springframework.roo.model.JdkJavaType.GREGORIAN_CALENDAR;
import static org.springframework.roo.model.JdkJavaType.ITERATOR;
import static org.springframework.roo.model.JdkJavaType.LIST;
import static org.springframework.roo.model.JdkJavaType.RANDOM;
import static org.springframework.roo.model.JdkJavaType.SECURE_RANDOM;
import static org.springframework.roo.model.Jsr303JavaType.CONSTRAINT_VIOLATION;
import static org.springframework.roo.model.Jsr303JavaType.CONSTRAINT_VIOLATION_EXCEPTION;
import static org.springframework.roo.model.Jsr303JavaType.DECIMAL_MAX;
import static org.springframework.roo.model.Jsr303JavaType.DECIMAL_MIN;
import static org.springframework.roo.model.Jsr303JavaType.DIGITS;
import static org.springframework.roo.model.Jsr303JavaType.FUTURE;
import static org.springframework.roo.model.Jsr303JavaType.MAX;
import static org.springframework.roo.model.Jsr303JavaType.MIN;
import static org.springframework.roo.model.Jsr303JavaType.PAST;
import static org.springframework.roo.model.Jsr303JavaType.SIZE;
import static org.springframework.roo.model.SpringJavaType.AUTOWIRED;
import static org.springframework.roo.model.SpringJavaType.COMPONENT;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
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
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.project.ContextualPath;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for {@link RooDataOnDemand}.
 *
 * @author Ben Alex
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @author Greg Turnquist
 * @author Andrew Swan
 * @since 1.0
 */
public class DataOnDemandMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

	// Constants
	private static final String PROVIDES_TYPE_STRING = DataOnDemandMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private static final JavaSymbolName INDEX = new JavaSymbolName("index");
	private static final JavaSymbolName VALUE = new JavaSymbolName("value");

	// Fields
	private DataOnDemandAnnotationValues annotationValues;
	private Map<MethodMetadata, CollaboratingDataOnDemandMetadataHolder> locatedMutators;
	private EmbeddedIdentifierHolder embeddedIdentifierHolder;
	private List<EmbeddedHolder> embeddedHolders;
	private final Map<MethodMetadata, String> fieldInitializers = new LinkedHashMap<MethodMetadata, String>();
	private final Map<FieldMetadata, Map<FieldMetadata, String>> embeddedFieldInitializers = new LinkedHashMap<FieldMetadata, Map<FieldMetadata, String>>();
	private final List<JavaType> requiredDataOnDemandCollaborators = new ArrayList<JavaType>();
	private MemberTypeAdditions findMethod;
	private MethodMetadata identifierAccessor;
	private JavaType identifierType;
	private JavaType entity;

	/**
	 * Constructor
	 *
	 * @param identifier
	 * @param aspectName
	 * @param governorPhysicalTypeMetadata
	 * @param annotationValues
	 * @param identifierAccessor
	 * @param findMethodAdditions
	 * @param findEntriesMethod
	 * @param persistMethodAdditions
	 * @param flushMethod
	 * @param locatedMutators
	 * @param entity
	 * @param embeddedIdentifierHolder
	 * @param embeddedHolders
	 */
	public DataOnDemandMetadata(final String identifier, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final DataOnDemandAnnotationValues annotationValues, final MethodMetadata identifierAccessor, final MemberTypeAdditions findMethodAdditions, final MemberTypeAdditions findEntriesMethodAdditions, final MemberTypeAdditions persistMethodAdditions, final MemberTypeAdditions flushMethod, final Map<MethodMetadata, CollaboratingDataOnDemandMetadataHolder> locatedMutators, final JavaType identifierType, final EmbeddedIdentifierHolder embeddedIdentifierHolder, final List<EmbeddedHolder> embeddedHolders) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.notNull(identifierAccessor, "Identifier accessor method required");
		Assert.notNull(locatedMutators, "Located mutator methods map required");
		Assert.notNull(embeddedHolders, "Embedded holders list required");

		if (!isValid()) {
			return;
		}

		if (findEntriesMethodAdditions == null || persistMethodAdditions == null || findMethodAdditions == null) {
			valid = false;
			return;
		}

		this.annotationValues = annotationValues;
		this.locatedMutators = locatedMutators;
		this.embeddedIdentifierHolder = embeddedIdentifierHolder;
		this.embeddedHolders = embeddedHolders;
		this.identifierAccessor = identifierAccessor;
		this.findMethod = findMethodAdditions;
		this.identifierType = identifierType;

		entity = this.annotationValues.getEntity();

		// Calculate and store field initializers
		storeFieldInitializers();
		storeEmbeddedFieldInitializers();

		builder.addAnnotation(getComponentAnnotation());
		builder.addField(getRndField());
		builder.addField(getDataField());

		addCollaboratingDoDFieldsToBuilder();

		builder.addMethod(getNewTransientEntityMethod());

		builder.addMethod(getEmbeddedIdMutatorMethod());

		for (EmbeddedHolder embeddedHolder : embeddedHolders) {
			builder.addMethod(getEmbeddedClassMutatorMethod(embeddedHolder));
			addEmbeddedClassFieldMutatorMethodsToBuilder(embeddedHolder);
		}

		addFieldMutatorMethodsToBuilder();

		builder.addMethod(getSpecificPersistentEntityMethod());
		builder.addMethod(getRandomPersistentEntityMethod());
		builder.addMethod(getModifyMethod());
		builder.addMethod(getInitMethod(findEntriesMethodAdditions, persistMethodAdditions, flushMethod));

		itdTypeDetails = builder.build();
	}

	/**
	 * Adds the @org.springframework.stereotype.Component annotation to the type, unless it already exists.
	 *
	 * @return the annotation is already exists or will be created, or null if it will not be created (required)
	 */
	public AnnotationMetadata getComponentAnnotation() {
		if (governorTypeDetails.getAnnotation(COMPONENT) != null) {
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
			JavaSymbolName fieldSymbolName = new JavaSymbolName(StringUtils.repeat("_", index) + "rnd");
			FieldMetadata candidate = governorTypeDetails.getField(fieldSymbolName);
			if (candidate != null) {
				// Verify if candidate is suitable
				if (!Modifier.isPrivate(candidate.getModifier())) {
					// Candidate is not private, so we might run into naming clashes if someone subclasses this (therefore go onto the next possible name)
					continue;
				}
				if (!candidate.getFieldType().equals(RANDOM)) {
					// Candidate isn't a java.util.Random, so it isn't suitable
					continue;
				}
				// If we got this far, we found a valid candidate
				// We don't check if there is a corresponding initializer, but we assume the user knows what they're doing and have made one
				return candidate;
			}

			// Candidate not found, so let's create one
			ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
			imports.addImport(RANDOM);
			imports.addImport(SECURE_RANDOM);

			FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId());
			fieldBuilder.setModifier(Modifier.PRIVATE);
			fieldBuilder.setFieldName(fieldSymbolName);
			fieldBuilder.setFieldType(RANDOM);
			fieldBuilder.setFieldInitializer("new SecureRandom()");
			return fieldBuilder.build();
		}
	}

	/**
	 * @return the "data" field to use, which is either provided by the user or produced on demand (never returns null)
	 */
	private FieldMetadata getDataField() {
		int index = -1;
		while (true) {
			// Compute the required field name
			index++;

			// The type parameters to be used by the field type
			List<JavaType> parameterTypes = Arrays.asList(entity);
			JavaSymbolName fieldSymbolName = new JavaSymbolName(StringUtils.repeat("_", index) + "data");
			FieldMetadata candidate = governorTypeDetails.getField(fieldSymbolName);
			if (candidate != null) {
				// Verify if candidate is suitable
				if (!Modifier.isPrivate(candidate.getModifier())) {
					// Candidate is not private, so we might run into naming clashes if someone subclasses this (therefore go onto the next possible name)
					continue;
				}

				if (!candidate.getFieldType().equals(new JavaType(LIST.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, parameterTypes))) {
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
			fieldBuilder.setFieldType(new JavaType(LIST.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, parameterTypes));
			return fieldBuilder.build();
		}
	}

	private void addCollaboratingDoDFieldsToBuilder() {
		Set<JavaSymbolName> fields = new LinkedHashSet<JavaSymbolName>();
		for (JavaType entityNeedingCollaborator : requiredDataOnDemandCollaborators) {
			JavaType collaboratorType = getCollaboratingType(entityNeedingCollaborator);
			String collaboratingFieldName = getCollaboratingFieldName(entityNeedingCollaborator).getSymbolName();

			JavaSymbolName fieldSymbolName = new JavaSymbolName(collaboratingFieldName);
			FieldMetadata candidate = governorTypeDetails.getField(fieldSymbolName);
			if (candidate != null) {
				// We really expect the field to be correct if we're going to rely on it
				Assert.isTrue(candidate.getFieldType().equals(collaboratorType), "Field '" + collaboratingFieldName + "' on '" + destination.getFullyQualifiedTypeName() + "' must be of type '" + collaboratorType.getFullyQualifiedTypeName() + "'");
				Assert.isTrue(Modifier.isPrivate(candidate.getModifier()), "Field '" + collaboratingFieldName + "' on '" + destination.getFullyQualifiedTypeName() + "' must be private");
				Assert.notNull(MemberFindingUtils.getAnnotationOfType(candidate.getAnnotations(), AUTOWIRED), "Field '" + collaboratingFieldName + "' on '" + destination.getFullyQualifiedTypeName() + "' must be @Autowired");
				// It's ok, so we can move onto the new field
				continue;
			}

			// Create field and add it to the ITD, if it hasn't already been
			if (!fields.contains(fieldSymbolName)) {
				// Must make the field
				List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
				annotations.add(new AnnotationMetadataBuilder(AUTOWIRED));
				FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations, fieldSymbolName, collaboratorType);
				FieldMetadata field = fieldBuilder.build();
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
		JavaSymbolName methodName = new JavaSymbolName("getNewTransient" + entity.getSimpleTypeName());

		final JavaType parameterType = JavaType.INT_PRIMITIVE;
		List<JavaSymbolName> parameterNames = Arrays.asList(INDEX);

		// Locate user-defined method
		MethodMetadata userMethod = getGovernorMethod(methodName, parameterType);
		if (userMethod != null) {
			Assert.isTrue(userMethod.getReturnType().equals(entity), "Method '" + methodName + "' on '" + destination + "' must return '" + entity.getNameIncludingTypeParameters() + "'");
			return userMethod;
		}

		// Create method
		ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(entity);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(entity.getSimpleTypeName() + " obj = new " + entity.getSimpleTypeName() + "();");

		// Create the composite key embedded id method call if required
		if (hasEmbeddedIdentifier()) {
			bodyBuilder.appendFormalLine(getEmbeddedIdMutatorMethodName() + "(obj, index);");
		}

		// Create a mutator method call for each embedded class
		for (EmbeddedHolder embeddedHolder : embeddedHolders) {
			bodyBuilder.appendFormalLine(getEmbeddedFieldMutatorMethodName(embeddedHolder.getEmbeddedField()) + "(obj, index);");
		}

		// Create mutator method calls for each entity field
		for (MethodMetadata mutator : fieldInitializers.keySet()) {
			bodyBuilder.appendFormalLine(mutator.getMethodName() + "(obj, index);");
		}

		bodyBuilder.appendFormalLine("return obj;");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, entity, AnnotatedJavaType.convertFromJavaTypes(parameterType), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getEmbeddedIdMutatorMethod() {
		if (!hasEmbeddedIdentifier()) {
			return null;
		}

		JavaSymbolName embeddedIdentifierMutator = embeddedIdentifierHolder.getEmbeddedIdentifierMutator();
		JavaSymbolName methodName = getEmbeddedIdMutatorMethodName();
		final JavaType[] parameterTypes = { entity, JavaType.INT_PRIMITIVE };

		// Locate user-defined method
		if (governorHasMethod(methodName, parameterTypes)) {
			// Method found in governor so do not create method in ITD
			return null;
		}

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		ImportRegistrationResolver imports = builder.getImportRegistrationResolver();

		// Create constructor for embedded id class
		JavaType embeddedIdentifierFieldType = embeddedIdentifierHolder.getEmbeddedIdentifierField().getFieldType();
		imports.addImport(embeddedIdentifierFieldType);

		StringBuilder sb = new StringBuilder();
		List<FieldMetadata> identifierFields = embeddedIdentifierHolder.getIdentifierFields();
		for (int i = 0, n = identifierFields.size(); i < n; i++) {
			FieldMetadata field = identifierFields.get(i);
			String fieldName = field.getFieldName().getSymbolName();
			JavaType fieldType = field.getFieldType();
			imports.addImport(fieldType);
			String initializer =  getFieldInitializer(field, null);
			bodyBuilder.append(getFieldValidationBody(field, initializer, null, true));
			sb.append(fieldName);
			if (i < n - 1) {
				sb.append(", ");
			}
		}
		bodyBuilder.appendFormalLine("");
		bodyBuilder.appendFormalLine(embeddedIdentifierFieldType.getSimpleTypeName() + " embeddedIdClass = new " + embeddedIdentifierFieldType.getSimpleTypeName() + "(" + sb.toString() + ");");
		bodyBuilder.appendFormalLine("obj." + embeddedIdentifierMutator + "(embeddedIdClass);");

		List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("obj"), INDEX);

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getEmbeddedClassMutatorMethod(final EmbeddedHolder embeddedHolder) {
		JavaSymbolName methodName = getEmbeddedFieldMutatorMethodName(embeddedHolder.getEmbeddedField());
		final JavaType[] parameterTypes = { entity, JavaType.INT_PRIMITIVE };

		// Locate user-defined method
		if (governorHasMethod(methodName, parameterTypes)) {
			// Method found in governor so do not create method in ITD
			return null;
		}

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		ImportRegistrationResolver imports = builder.getImportRegistrationResolver();

		// Create constructor for embedded class
		JavaType embeddedFieldType = embeddedHolder.getEmbeddedField().getFieldType();
		imports.addImport(embeddedFieldType);
		bodyBuilder.appendFormalLine(embeddedFieldType.getSimpleTypeName() + " embeddedClass = new " + embeddedFieldType.getSimpleTypeName() + "();");
		for (FieldMetadata field : embeddedHolder.getFields()) {
			bodyBuilder.appendFormalLine(field.getFieldName().getSymbolNameTurnedIntoMutatorMethodName() + "(embeddedClass, index);");
		}
		bodyBuilder.appendFormalLine("obj." + embeddedHolder.getEmbeddedMutatorMethodName() + "(embeddedClass);");

		List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("obj"), INDEX);

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}

	private JavaSymbolName getEmbeddedFieldMutatorMethodName(final FieldMetadata embeddedField) {
		return new JavaSymbolName(embeddedField.getFieldName().getSymbolNameTurnedIntoMutatorMethodName());
	}

	private void addEmbeddedClassFieldMutatorMethodsToBuilder(final EmbeddedHolder embeddedHolder) {
		List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("obj"), INDEX);

		JavaType embeddedFieldType = embeddedHolder.getEmbeddedField().getFieldType();
		final JavaType[] parameterTypes = { embeddedFieldType, JavaType.INT_PRIMITIVE };

		for (FieldMetadata field : embeddedHolder.getFields()) {
			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

			String initializer = getFieldInitializer(field, null);
			JavaSymbolName fieldMutatorMethodName = new JavaSymbolName(field.getFieldName().getSymbolNameTurnedIntoMutatorMethodName());
			bodyBuilder.append(getFieldValidationBody(field, initializer, fieldMutatorMethodName, false));

			JavaSymbolName embeddedClassMethodName = new JavaSymbolName(field.getFieldName().getSymbolNameTurnedIntoMutatorMethodName());
			MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, embeddedClassMethodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
			MethodMetadata fieldInitializerMethod = methodBuilder.build();
			if (getGovernorMethod(embeddedClassMethodName, parameterTypes) != null) {
				// Method found in governor so do not create method in ITD
				continue;
			}

			builder.addMethod(fieldInitializerMethod);
		}
	}

	private void addFieldMutatorMethodsToBuilder() {
		for (MethodMetadata fieldInitializerMethod : getFieldMutatorMethods()) {
			builder.addMethod(fieldInitializerMethod);
		}
	}

	private List<MethodMetadata> getFieldMutatorMethods() {
		List<MethodMetadata> fieldMutatorMethods = new ArrayList<MethodMetadata>();

		List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("obj"), INDEX);
		final JavaType[] parameterTypes = { entity, JavaType.INT_PRIMITIVE };

		Set<String> existingMutators = new HashSet<String>();

		for (Map.Entry<MethodMetadata, String> entry : fieldInitializers.entrySet()) {
			MethodMetadata mutator = entry.getKey();

			// Locate user-defined method
			if (getGovernorMethod(mutator.getMethodName(), parameterTypes) != null) {
				// Method found in governor so do not create method in ITD
				continue;
			}

			// Check to see if the mutator has already been added
			String mutatorId = mutator.getMethodName() + " - " + mutator.getParameterTypes().size();
			if (existingMutators.contains(mutatorId)) {
				continue;
			}
			existingMutators.add(mutatorId);

			// Method not on governor so need to create it
			String initializer = entry.getValue();
			Assert.hasText(initializer, "Internal error: unable to locate initializer for " + mutator.getMethodName().getSymbolName());

			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.append(getFieldValidationBody(locatedMutators.get(mutator).getField(), initializer, mutator.getMethodName(), false));

			MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, mutator.getMethodName(), JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
			fieldMutatorMethods.add(methodBuilder.build());
		}

		return fieldMutatorMethods;
	}

	private String getFieldValidationBody(final FieldMetadata field, final String initializer, final JavaSymbolName mutatorName, final boolean isFieldOfEmbeddableType) {
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

		if (fieldType.equals(JavaType.STRING)) {
			boolean isUnique = isFieldOfEmbeddableType;
			@SuppressWarnings("unchecked") Map<String, Object> values = (Map<String, Object>) field.getCustomData().get(CustomDataKeys.COLUMN_FIELD);
			if (!isUnique && values != null && values.containsKey("unique")) {
				isUnique = (Boolean) values.get("unique");
			}

			// Check for @Size or @Column with length attribute
			AnnotationMetadata sizeAnnotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), SIZE);
			if (sizeAnnotation != null && sizeAnnotation.getAttribute(new JavaSymbolName("max")) != null) {
				Integer maxValue = (Integer) sizeAnnotation.getAttribute(new JavaSymbolName("max")).getValue();
				bodyBuilder.appendFormalLine("if (" + fieldName + ".length() > " + maxValue + ") {");
				bodyBuilder.indent();
				if (isUnique) {
					bodyBuilder.appendFormalLine(fieldName + " = new Random().nextInt(10) + " + fieldName + ".substring(1, " + maxValue + ");");
				} else {
					bodyBuilder.appendFormalLine(fieldName + " = " + fieldName + ".substring(0, " + maxValue + ");");
				}
				bodyBuilder.indentRemove();
				bodyBuilder.appendFormalLine("}");
			} else if (sizeAnnotation == null && values != null) {
				if (values.containsKey("length")) {
					Integer lengthValue = (Integer) values.get("length");
					bodyBuilder.appendFormalLine("if (" + fieldName + ".length() > " + lengthValue + ") {");
					bodyBuilder.indent();
					if (isUnique) {
						bodyBuilder.appendFormalLine(fieldName + " = new Random().nextInt(10) + " + fieldName + ".substring(1, " + lengthValue + ");");
					} else {
						bodyBuilder.appendFormalLine(fieldName + " = " + fieldName + ".substring(0, " + lengthValue + ");");
					}
					bodyBuilder.indentRemove();
					bodyBuilder.appendFormalLine("}");
				}
			}
		} else if (JdkJavaType.isDecimalType(fieldType)) {
			// Check for @Digits, @DecimalMax, @DecimalMin
			AnnotationMetadata digitsAnnotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), DIGITS);
			AnnotationMetadata decimalMinAnnotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), DECIMAL_MIN);
			AnnotationMetadata decimalMaxAnnotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), DECIMAL_MAX);

			if (digitsAnnotation != null) {
				bodyBuilder.append(getDigitsBody(field, digitsAnnotation, suffix));
			} else if (decimalMinAnnotation != null || decimalMaxAnnotation != null) {
				bodyBuilder.append(getDecimalMinAndDecimalMaxBody(field, decimalMinAnnotation, decimalMaxAnnotation, suffix));
			} else if (field.getCustomData().keySet().contains(CustomDataKeys.COLUMN_FIELD)) {
				@SuppressWarnings("unchecked") Map<String, Object> values = (Map<String, Object>) field.getCustomData().get(CustomDataKeys.COLUMN_FIELD);
				bodyBuilder.append(getColumnPrecisionAndScaleBody(field, values, suffix));
			}
		} else if (JdkJavaType.isIntegerType(fieldType)) {
			// Check for @Min and @Max
			bodyBuilder.append(getMinAndMaxBody(field, suffix));
		}

		if (mutatorName != null) {
			bodyBuilder.appendFormalLine("obj." + mutatorName.getSymbolName() + "(" + fieldName + ");");
		}

		return bodyBuilder.getOutput();
	}

	private String getTypeStr(final JavaType fieldType) {
		ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(fieldType);

		String arrayStr = fieldType.isArray() ? "[]" : "";
		String typeStr = fieldType.getSimpleTypeName();

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
		} else if (fieldType.equals(new JavaType(STRING.getFullyQualifiedTypeName(), 1, DataType.TYPE, null, null))) {
			typeStr = "String[]";
		}
		return typeStr;
	}

	private String getDigitsBody(final FieldMetadata field, final AnnotationMetadata digitsAnnotation, final String suffix) {
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

		Integer integerValue = (Integer) digitsAnnotation.getAttribute(new JavaSymbolName("integer")).getValue();
		Integer fractionValue = (Integer) digitsAnnotation.getAttribute(new JavaSymbolName("fraction")).getValue();

		String fieldName = field.getFieldName().getSymbolName();
		JavaType fieldType = field.getFieldType();

		BigDecimal maxValue = new BigDecimal(StringUtils.padRight("9", integerValue, '9') + "." + StringUtils.padRight("9", fractionValue, '9'));
		if (fieldType.equals(BIG_DECIMAL)) {
			bodyBuilder.appendFormalLine("if (" + fieldName + ".compareTo(new " + BIG_DECIMAL.getSimpleTypeName() + "(\"" + maxValue + "\")) == 1) {");
			bodyBuilder.indent();
			bodyBuilder.appendFormalLine(fieldName + " = new " + BIG_DECIMAL.getSimpleTypeName() + "(\"" + maxValue + "\");");
		} else {
			bodyBuilder.appendFormalLine("if (" + fieldName + " > " + maxValue.doubleValue() + suffix + ") {");
			bodyBuilder.indent();
			bodyBuilder.appendFormalLine(fieldName + " = " + maxValue.doubleValue() + suffix + ";");
		}

		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");

		return bodyBuilder.getOutput();
	}

	private String getDecimalMinAndDecimalMaxBody(final FieldMetadata field, final AnnotationMetadata decimalMinAnnotation, final AnnotationMetadata decimalMaxAnnotation, final String suffix) {
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

		String fieldName = field.getFieldName().getSymbolName();
		JavaType fieldType = field.getFieldType();

		if (decimalMinAnnotation != null && decimalMaxAnnotation == null) {
			String minValue = (String) decimalMinAnnotation.getAttribute(VALUE).getValue();

			if (fieldType.equals(BIG_DECIMAL)) {
				bodyBuilder.appendFormalLine("if (" + fieldName + ".compareTo(new " + BIG_DECIMAL.getSimpleTypeName() + "(\"" + minValue + "\")) == -1) {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine(fieldName + " = new " + BIG_DECIMAL.getSimpleTypeName() + "(\"" + minValue + "\");");
			} else {
				bodyBuilder.appendFormalLine("if (" + fieldName + " < " + minValue + suffix + ") {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine(fieldName + " = " + minValue + suffix + ";");
			}

			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("}");
		} else if (decimalMinAnnotation == null && decimalMaxAnnotation != null) {
			String maxValue = (String) decimalMaxAnnotation.getAttribute(VALUE).getValue();

			if (fieldType.equals(BIG_DECIMAL)) {
				bodyBuilder.appendFormalLine("if (" + fieldName + ".compareTo(new " + BIG_DECIMAL.getSimpleTypeName() + "(\"" + maxValue + "\")) == 1) {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine(fieldName + " = new " + BIG_DECIMAL.getSimpleTypeName() + "(\"" + maxValue + "\");");
			} else {
				bodyBuilder.appendFormalLine("if (" + fieldName + " > " + maxValue + suffix + ") {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine(fieldName + " = " + maxValue + suffix + ";");
			}

			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("}");
		} else if (decimalMinAnnotation != null && decimalMaxAnnotation != null) {
			String minValue = (String) decimalMinAnnotation.getAttribute(VALUE).getValue();
			String maxValue = (String) decimalMaxAnnotation.getAttribute(VALUE).getValue();
			Assert.isTrue(Double.parseDouble(maxValue) >= Double.parseDouble(minValue), "The value of @DecimalMax must be greater or equal to the value of @DecimalMin for field " + fieldName);

			if (fieldType.equals(BIG_DECIMAL)) {
				bodyBuilder.appendFormalLine("if (" + fieldName + ".compareTo(new " + BIG_DECIMAL.getSimpleTypeName() + "(\"" + minValue + "\")) == -1 || " + fieldName + ".compareTo(new " + BIG_DECIMAL.getSimpleTypeName() + "(\"" + maxValue + "\")) == 1) {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine(fieldName + " = new " + BIG_DECIMAL.getSimpleTypeName() + "(\"" + maxValue + "\");");
			} else {
				bodyBuilder.appendFormalLine("if (" + fieldName + " < " + minValue + suffix + " || " + fieldName + " > " + maxValue + suffix + ") {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine(fieldName + " = " + maxValue + suffix + ";");
			}

			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("}");
		}

		return bodyBuilder.getOutput();
	}

	private String getColumnPrecisionAndScaleBody(final FieldMetadata field, final Map<String, Object> values, final String suffix) {
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

		if (values == null || !values.containsKey("precision")) {
			return bodyBuilder.getOutput();
		}

		Integer precision = (Integer) values.get("precision");
		Integer scale = (Integer) values.get("scale");
		scale = scale == null ? 0 : scale;

		String fieldName = field.getFieldName().getSymbolName();
		JavaType fieldType = field.getFieldType();

		BigDecimal maxValue = new BigDecimal(StringUtils.padRight("9", (precision - scale), '9') + "." + StringUtils.padRight("9", scale, '9'));
		if (fieldType.equals(BIG_DECIMAL)) {
			bodyBuilder.appendFormalLine("if (" + fieldName + ".compareTo(new " + BIG_DECIMAL.getSimpleTypeName() + "(\"" + maxValue + "\")) == 1) {");
			bodyBuilder.indent();
			bodyBuilder.appendFormalLine(fieldName + " = new " + BIG_DECIMAL.getSimpleTypeName() + "(\"" + maxValue + "\");");
		} else {
			bodyBuilder.appendFormalLine("if (" + fieldName + " > " + maxValue.doubleValue() + suffix + ") {");
			bodyBuilder.indent();
			bodyBuilder.appendFormalLine(fieldName + " = " + maxValue.doubleValue() + suffix + ";");
		}

		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");

		return bodyBuilder.getOutput();
	}

	private String getMinAndMaxBody(final FieldMetadata field, final String suffix) {
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

		String fieldName = field.getFieldName().getSymbolName();
		JavaType fieldType = field.getFieldType();

		AnnotationMetadata minAnnotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), MIN);
		AnnotationMetadata maxAnnotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), MAX);
		if (minAnnotation != null && maxAnnotation == null) {
			Number minValue = (Number) minAnnotation.getAttribute(VALUE).getValue();

			if (fieldType.equals(BIG_INTEGER)) {
				bodyBuilder.appendFormalLine("if (" + fieldName + ".compareTo(new " + BIG_INTEGER.getSimpleTypeName() + "(\"" + minValue + "\")) == -1) {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine(fieldName + " = new " + BIG_INTEGER.getSimpleTypeName() + "(\"" + minValue + "\");");
			} else {
				bodyBuilder.appendFormalLine("if (" + fieldName + " < " + minValue + suffix + ") {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine(fieldName + " = " + minValue + suffix + ";");
			}

			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("}");
		} else if (minAnnotation == null && maxAnnotation != null) {
			Number maxValue = (Number) maxAnnotation.getAttribute(VALUE).getValue();

			if (fieldType.equals(BIG_INTEGER)) {
				bodyBuilder.appendFormalLine("if (" + fieldName + ".compareTo(new " + BIG_INTEGER.getSimpleTypeName() + "(\"" + maxValue + "\")) == 1) {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine(fieldName + " = new " + BIG_INTEGER.getSimpleTypeName() + "(\"" + maxValue + "\");");
			} else {
				bodyBuilder.appendFormalLine("if (" + fieldName + " > " + maxValue + suffix + ") {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine(fieldName + " = " + maxValue + suffix + ";");
			}

			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("}");
		} else if (minAnnotation != null && maxAnnotation != null) {
			Number minValue = (Number) minAnnotation.getAttribute(VALUE).getValue();
			Number maxValue = (Number) maxAnnotation.getAttribute(VALUE).getValue();
			Assert.isTrue(maxValue.longValue() >= minValue.longValue(), "The value of @Max must be greater or equal to the value of @Min for field " + fieldName);

			if (fieldType.equals(BIG_INTEGER)) {
				bodyBuilder.appendFormalLine("if (" + fieldName + ".compareTo(new " + BIG_INTEGER.getSimpleTypeName() + "(\"" + minValue + "\")) == -1 || " + fieldName + ".compareTo(new " + BIG_INTEGER.getSimpleTypeName() + "(\"" + maxValue + "\")) == 1) {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine(fieldName + " = new " + BIG_INTEGER.getSimpleTypeName() + "(\"" + maxValue + "\");");
			} else {
				bodyBuilder.appendFormalLine("if (" + fieldName + " < " + minValue + suffix + " || " + fieldName + " > " + maxValue + suffix + ") {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine(fieldName + " = " + maxValue + suffix + ";");
			}

			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("}");
		}

		return bodyBuilder.getOutput();
	}

	/**
	 * @return the "modifyEntity(Entity):boolean" method (never returns null)
	 */
	public MethodMetadata getModifyMethod() {
		// Method definition to find or build
		JavaSymbolName methodName = new JavaSymbolName("modify" + entity.getSimpleTypeName());
		final JavaType parameterType = entity;
		List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("obj"));
		JavaType returnType = JavaType.BOOLEAN_PRIMITIVE;

		// Locate user-defined method
		MethodMetadata userMethod = getGovernorMethod(methodName, parameterType);
		if (userMethod != null) {
			Assert.isTrue(userMethod.getReturnType().equals(returnType), "Method '" + methodName + "' on '" + destination + "' must return '" + returnType.getNameIncludingTypeParameters() + "'");
			return userMethod;
		}

		// Create method
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return false;");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(parameterType), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}

	/**
	 * @return the "getRandomEntity():Entity" method (never returns null)
	 */
	public MethodMetadata getRandomPersistentEntityMethod() {
		// Method definition to find or build
		JavaSymbolName methodName = new JavaSymbolName("getRandom" + entity.getSimpleTypeName());
		final JavaType[] parameterTypes = {};
		List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

		// Locate user-defined method
		MethodMetadata userMethod = getGovernorMethod(methodName, parameterTypes);
		if (userMethod != null) {
			Assert.isTrue(userMethod.getReturnType().equals(entity), "Method '" + methodName + "' on '" + destination + "' must return '" + entity.getNameIncludingTypeParameters() + "'");
			return userMethod;
		}

		// Create method
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("init();");
		bodyBuilder.appendFormalLine(entity.getSimpleTypeName() + " obj = " + getDataField().getFieldName().getSymbolName() + ".get(" + getRndField().getFieldName().getSymbolName() + ".nextInt(" + getDataField().getFieldName().getSymbolName() + ".size()));");
		bodyBuilder.appendFormalLine(identifierType.getFullyQualifiedTypeName() + " id = " + "obj." + identifierAccessor.getMethodName().getSymbolName() + "();");
		bodyBuilder.appendFormalLine("return " + findMethod.getMethodCall() + ";");

		findMethod.copyAdditionsTo(builder, governorTypeDetails);
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, entity, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}

	/**
	 * @return the "getSpecificEntity(int):Entity" method (never returns null)
	 */
	public MethodMetadata getSpecificPersistentEntityMethod() {
		// Method definition to find or build
		JavaSymbolName methodName = new JavaSymbolName("getSpecific" + entity.getSimpleTypeName());
		final JavaType parameterType = JavaType.INT_PRIMITIVE;
		List<JavaSymbolName> parameterNames = Arrays.asList(INDEX);

		// Locate user-defined method
		MethodMetadata userMethod = getGovernorMethod(methodName, parameterType);
		if (userMethod != null) {
			Assert.isTrue(userMethod.getReturnType().equals(entity), "Method '" + methodName + "' on '" + destination + "' must return '" + entity.getNameIncludingTypeParameters() + "'");
			return userMethod;
		}

		// Create method
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("init();");
		bodyBuilder.appendFormalLine("if (index < 0) index = 0;");
		bodyBuilder.appendFormalLine("if (index > (" + getDataField().getFieldName().getSymbolName() + ".size() - 1)) index = " + getDataField().getFieldName().getSymbolName() + ".size() - 1;");
		bodyBuilder.appendFormalLine(entity.getSimpleTypeName() + " obj = " + getDataField().getFieldName().getSymbolName() + ".get(index);");
		bodyBuilder.appendFormalLine(identifierType.getFullyQualifiedTypeName() + " id = " + "obj." + identifierAccessor.getMethodName().getSymbolName() + "();");
		bodyBuilder.appendFormalLine("return " + findMethod.getMethodCall() + ";");

		findMethod.copyAdditionsTo(builder, governorTypeDetails);
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, entity, AnnotatedJavaType.convertFromJavaTypes(parameterType), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}

	/**
	 * Returns the DoD type's "void init()" method (existing or generated)
	 *
	 * @param findEntriesMethodAdditions (required)
	 * @param persistMethodAdditions (required)
	 * @param flushAdditions (required)
	 * @return never <code>null</code>
	 */
	private MethodMetadata getInitMethod(final MemberTypeAdditions findEntriesMethodAdditions, final MemberTypeAdditions persistMethodAdditions, final MemberTypeAdditions flushAdditions) {
		// Method definition to find or build
		final JavaSymbolName methodName = new JavaSymbolName("init");
		final JavaType[] parameterTypes = {};
		final List<JavaSymbolName> parameterNames = Collections.<JavaSymbolName> emptyList();
		final JavaType returnType = JavaType.VOID_PRIMITIVE;

		// Locate user-defined method
		final MethodMetadata userMethod = getGovernorMethod(methodName, parameterTypes);
		if (userMethod != null) {
			Assert.isTrue(userMethod.getReturnType().equals(returnType), "Method '" + methodName + "' on '" + destination + "' must return '" + returnType.getNameIncludingTypeParameters() + "'");
			return userMethod;
		}

		// Create the method body
		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(ARRAY_LIST);
		imports.addImport(ITERATOR);
		imports.addImport(CONSTRAINT_VIOLATION_EXCEPTION);
		imports.addImport(CONSTRAINT_VIOLATION);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		final String dataField = getDataField().getFieldName().getSymbolName();
		bodyBuilder.appendFormalLine("int from = 0;");
		bodyBuilder.appendFormalLine("int to = 10;");
		bodyBuilder.appendFormalLine(dataField + " = " + findEntriesMethodAdditions.getMethodCall() + ";");
		findEntriesMethodAdditions.copyAdditionsTo(builder, governorTypeDetails);
		bodyBuilder.appendFormalLine("if (data == null) throw new IllegalStateException(\"Find entries implementation for '" + entity.getSimpleTypeName() + "' illegally returned null\");");
		bodyBuilder.appendFormalLine("if (!" + dataField + ".isEmpty()) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return;");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("");
		bodyBuilder.appendFormalLine(dataField + " = new ArrayList<" + getDataField().getFieldType().getParameters().get(0).getNameIncludingTypeParameters() + ">();");
		bodyBuilder.appendFormalLine("for (int i = 0; i < " + annotationValues.getQuantity() + "; i++) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(entity.getSimpleTypeName() + " obj = " + getNewTransientEntityMethod().getMethodName() + "(i);");
		bodyBuilder.appendFormalLine("try {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(persistMethodAdditions.getMethodCall() + ";");
		persistMethodAdditions.copyAdditionsTo(builder, governorTypeDetails);
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("} catch (ConstraintViolationException e) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("StringBuilder msg = new StringBuilder();");
		bodyBuilder.appendFormalLine("for (Iterator<ConstraintViolation<?>> it = e.getConstraintViolations().iterator(); it.hasNext();) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("ConstraintViolation<?> cv = it.next();");
		bodyBuilder.appendFormalLine("msg.append(\"[\").append(cv.getConstraintDescriptor()).append(\":\").append(cv.getMessage()).append(\"=\").append(cv.getInvalidValue()).append(\"]\");");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("throw new RuntimeException(msg.toString(), e);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		if (flushAdditions != null) {
			bodyBuilder.appendFormalLine(flushAdditions.getMethodCall() + ";");
			flushAdditions.copyAdditionsTo(builder, governorTypeDetails);
		}
		bodyBuilder.appendFormalLine(dataField + ".add(obj);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");

		// Create the method
		return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder).build();
	}

	public boolean hasEmbeddedIdentifier() {
		return embeddedIdentifierHolder != null;
	}

	private void storeFieldInitializers() {
		for (Map.Entry<MethodMetadata, CollaboratingDataOnDemandMetadataHolder> entry : locatedMutators.entrySet()) {
			CollaboratingDataOnDemandMetadataHolder metadataHolder = entry.getValue();
			String initializer = getFieldInitializer(metadataHolder.getField(), metadataHolder.getDataOnDemandMetadata());
			fieldInitializers.put(entry.getKey(), initializer);
		}
	}

	private void storeEmbeddedFieldInitializers() {
		for (EmbeddedHolder embeddedHolder: embeddedHolders) {
			Map<FieldMetadata, String> initializers = new LinkedHashMap<FieldMetadata, String>();
			for (FieldMetadata field : embeddedHolder.getFields()) {
				initializers.put(field, getFieldInitializer(field, null));
			}
			embeddedFieldInitializers.put(embeddedHolder.getEmbeddedField(), initializers);
		}
	}

	private String getFieldInitializer(final FieldMetadata field, final DataOnDemandMetadata collaboratingMetadata) {
		final JavaType fieldType = field.getFieldType();
		final String fieldName = field.getFieldName().getSymbolName();
		String initializer = "null";
		String fieldInitializer = field.getFieldInitializer();
		Set<Object> fieldCustomDataKeys = field.getCustomData().keySet();
		ImportRegistrationResolver imports = builder.getImportRegistrationResolver();

		// Date fields included for DataNucleus (
		if (fieldType.equals(DATE)) {
			if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), PAST) != null) {
				imports.addImport(DATE);
				initializer = "new Date(new Date().getTime() - 10000000L)";
			} else if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), FUTURE) != null) {
				imports.addImport(DATE);
				initializer = "new Date(new Date().getTime() + 10000000L)";
			} else {
				imports.addImport(CALENDAR);
				imports.addImport(GREGORIAN_CALENDAR);
				initializer = "new GregorianCalendar(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.DAY_OF_MONTH), Calendar.getInstance().get(Calendar.HOUR_OF_DAY), Calendar.getInstance().get(Calendar.MINUTE), Calendar.getInstance().get(Calendar.SECOND) + new Double(Math.random() * 1000).intValue()).getTime()";
			}
		} else if (fieldType.equals(CALENDAR)) {
			imports.addImport(CALENDAR);
			imports.addImport(GREGORIAN_CALENDAR);

			String calendarString = "new GregorianCalendar(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.DAY_OF_MONTH)";
			if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), PAST) != null) {
				initializer = calendarString + " - 1)";
			} else if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), FUTURE) != null) {
				initializer = calendarString + " + 1)";
			} else {
				initializer = "Calendar.getInstance()";
			}
		} else if (fieldType.equals(STRING)) {
			if (fieldInitializer != null && fieldInitializer.contains("\"")) {
				int offset = fieldInitializer.indexOf("\"");
				initializer = fieldInitializer.substring(offset + 1, fieldInitializer.lastIndexOf("\""));
			} else {
				initializer = fieldName;
			}

			if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), VALIDATOR_CONSTRAINTS_EMAIL) != null || fieldName.toLowerCase().contains("email")) {
				initializer = "\"foo\" + index + \"@bar.com\"";
			} else {
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
						Assert.isTrue(maxLength >= minLength, "@Size attribute 'max' must be greater than 'min' for field '" + fieldName + "' in " + entity.getFullyQualifiedTypeName());
						if (initializer.length() + 2 < minLength) {
							initializer = String.format("%1$-" + (minLength - 2) + "s", initializer).replace(' ', 'x');
						}
					}
				} else {
					if (field.getCustomData().keySet().contains(CustomDataKeys.COLUMN_FIELD)) {
						@SuppressWarnings("unchecked") Map<String, Object> columnValues = (Map<String, Object>) field.getCustomData().get(CustomDataKeys.COLUMN_FIELD);
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
			}
		} else if (fieldType.equals(new JavaType(STRING.getFullyQualifiedTypeName(), 1, DataType.TYPE, null, null))) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "{ \"Y\", \"N\" }");
		} else if (fieldType.equals(JavaType.BOOLEAN_OBJECT)) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "Boolean.TRUE");
		} else if (fieldType.equals(JavaType.BOOLEAN_PRIMITIVE)) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "true");
		} else if (fieldType.equals(JavaType.INT_OBJECT)) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(index)");
		} else if (fieldType.equals(JavaType.INT_PRIMITIVE)) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "index");
		} else if (fieldType.equals(new JavaType(JavaType.INT_OBJECT.getFullyQualifiedTypeName(), 1, DataType.PRIMITIVE, null, null))) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "{ index, index }");
		} else if (fieldType.equals(JavaType.DOUBLE_OBJECT)) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(index).doubleValue()"); // Auto-boxed
		} else if (fieldType.equals(JavaType.DOUBLE_PRIMITIVE)) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(index).doubleValue()");
		} else if (fieldType.equals(new JavaType(JavaType.DOUBLE_OBJECT.getFullyQualifiedTypeName(), 1, DataType.PRIMITIVE, null, null))) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "{ new Integer(index).doubleValue(), new Integer(index).doubleValue() }");
		} else if (fieldType.equals(JavaType.FLOAT_OBJECT)) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(index).floatValue()"); // Auto-boxed
		} else if (fieldType.equals(JavaType.FLOAT_PRIMITIVE)) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(index).floatValue()");
		} else if (fieldType.equals(new JavaType(JavaType.FLOAT_OBJECT.getFullyQualifiedTypeName(), 1, DataType.PRIMITIVE, null, null))) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "{ new Integer(index).floatValue(), new Integer(index).floatValue() }");
		} else if (fieldType.equals(JavaType.LONG_OBJECT)) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(index).longValue()"); // Auto-boxed
		} else if (fieldType.equals(JavaType.LONG_PRIMITIVE)) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(index).longValue()");
		} else if (fieldType.equals(new JavaType(JavaType.LONG_OBJECT.getFullyQualifiedTypeName(), 1, DataType.PRIMITIVE, null, null))) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "{ new Integer(index).longValue(), new Integer(index).longValue() }");
		} else if (fieldType.equals(JavaType.SHORT_OBJECT)) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(index).shortValue()"); // Auto-boxed
		} else if (fieldType.equals(JavaType.SHORT_PRIMITIVE)) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(index).shortValue()");
		} else if (fieldType.equals(new JavaType(JavaType.SHORT_OBJECT.getFullyQualifiedTypeName(), 1, DataType.PRIMITIVE, null, null))) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "{ new Integer(index).shortValue(), new Integer(index).shortValue() }");
		} else if (fieldType.equals(JavaType.CHAR_OBJECT)) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Character('N')");
		} else if (fieldType.equals(JavaType.CHAR_PRIMITIVE)) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "'N'");
		} else if (fieldType.equals(new JavaType(JavaType.CHAR_OBJECT.getFullyQualifiedTypeName(), 1, DataType.PRIMITIVE, null, null))) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "{ 'Y', 'N' }");
		} else if (fieldType.equals(BIG_DECIMAL)) {
			imports.addImport(BIG_DECIMAL);
			initializer = BIG_DECIMAL.getSimpleTypeName() + ".valueOf(index)";
		} else if (fieldType.equals(BIG_INTEGER)) {
			imports.addImport(BIG_INTEGER);
			initializer = BIG_INTEGER.getSimpleTypeName() + ".valueOf(index)";
		} else if (fieldType.equals(JavaType.BYTE_OBJECT)) {
			initializer = "new Byte(" + StringUtils.defaultIfEmpty(fieldInitializer, "\"1\"") + ")";
		} else if (fieldType.equals(JavaType.BYTE_PRIMITIVE)) {
			initializer = "new Byte(" + StringUtils.defaultIfEmpty(fieldInitializer, "\"1\"") + ").byteValue()";
		} else if (fieldType.equals(JavaType.BYTE_ARRAY_PRIMITIVE)) {
			initializer = StringUtils.defaultIfEmpty(fieldInitializer, "String.valueOf(index).getBytes()");
		} else if (fieldType.equals(entity)) {
			// Avoid circular references (ROO-562)
			initializer = "obj";
		} else if (fieldCustomDataKeys.contains(CustomDataKeys.ENUMERATED_FIELD)) {
			imports.addImport(field.getFieldType());
			initializer = field.getFieldType().getSimpleTypeName() + ".class.getEnumConstants()[0]";
		} else if (collaboratingMetadata != null && collaboratingMetadata.getEntityType() != null) {
			requiredDataOnDemandCollaborators.add(field.getFieldType());

			String collaboratingFieldName = getCollaboratingFieldName(field.getFieldType()).getSymbolName();
			// Decide if we're dealing with a one-to-one and therefore should _try_ to keep the same id (ROO-568)
			if (fieldCustomDataKeys.contains(CustomDataKeys.ONE_TO_ONE_FIELD)) {
				initializer = collaboratingFieldName + "." + collaboratingMetadata.getSpecificPersistentEntityMethod().getMethodName().getSymbolName() + "(index)";
			} else {
				initializer = collaboratingFieldName + "." + collaboratingMetadata.getRandomPersistentEntityMethod().getMethodName().getSymbolName() + "()";
			}
		}

		return initializer;
	}

	private JavaSymbolName getCollaboratingFieldName(final JavaType entity) {
		return new JavaSymbolName(StringUtils.uncapitalize(getCollaboratingType(entity).getSimpleTypeName()));
	}

	private JavaType getCollaboratingType(final JavaType entity) {
		return new JavaType(entity.getFullyQualifiedTypeName() + "DataOnDemand");
	}

	private JavaSymbolName getEmbeddedIdMutatorMethodName() {
		List<JavaSymbolName> fieldNames = new ArrayList<JavaSymbolName>();
		for (MethodMetadata mutator : fieldInitializers.keySet()) {
			fieldNames.add(locatedMutators.get(mutator).getField().getFieldName());
		}

		int index = -1;
		JavaSymbolName embeddedIdField;
		while (true) {
			// Compute the required field name
			index++;
			embeddedIdField = new JavaSymbolName("embeddedIdClass" + StringUtils.repeat("_", index));
			if (!fieldNames.contains(embeddedIdField)) {
				// Found a usable name
				break;
			}
		}
		return new JavaSymbolName(embeddedIdField.getSymbolNameTurnedIntoMutatorMethodName());
	}

	public JavaType getEntityType() {
		return entity;
	}

	@Override
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

	public static String createIdentifier(final JavaType javaType, final ContextualPath path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static JavaType getJavaType(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static ContextualPath getPath(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
}