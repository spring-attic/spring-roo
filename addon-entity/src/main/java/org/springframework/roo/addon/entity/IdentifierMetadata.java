package org.springframework.roo.addon.entity;

import static org.springframework.roo.classpath.customdata.CustomDataKeys.IDENTIFIER_TYPE;
import static org.springframework.roo.model.JavaType.LONG_OBJECT;
import static org.springframework.roo.model.JdkJavaType.BIG_DECIMAL;
import static org.springframework.roo.model.JdkJavaType.DATE;
import static org.springframework.roo.model.JpaJavaType.COLUMN;
import static org.springframework.roo.model.JpaJavaType.EMBEDDABLE;
import static org.springframework.roo.model.JpaJavaType.TEMPORAL;
import static org.springframework.roo.model.JpaJavaType.TEMPORAL_TYPE;
import static org.springframework.roo.model.JpaJavaType.TRANSIENT;
import static org.springframework.roo.model.SpringJavaType.DATE_TIME_FORMAT;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.ContextualPath;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for {@link RooIdentifier}.
 *
 * @author Alan Stewart
 * @since 1.1
 */
public class IdentifierMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

	// Constants
	private static final String PROVIDES_TYPE_STRING = IdentifierMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);

	// Fields
	private boolean noArgConstructor;
	private boolean publicNoArgConstructor;
	private List<FieldMetadata> fields;
	// See {@link IdentifierService} for further information (populated via {@link IdentifierMetadataProviderImpl}); may be null
	private List<Identifier> identifierServiceResult;

	public IdentifierMetadata(final String identifier, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final boolean noArgConstructor, final List<Identifier> identifierServiceResult, final IdentifierAnnotationValues annotationValues) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");

		if (!isValid()) {
			return;
		}

		this.identifierServiceResult = identifierServiceResult;
		this.noArgConstructor = noArgConstructor;

		// Add @Embeddable annotation
		builder.addAnnotation(getEmbeddableAnnotation());

		// Add declared fields and accessors and mutators
		fields = getFields();
		for (FieldMetadata field : fields) {
			builder.addField(field);
		}

		// Obtain a parameterised constructor
		builder.addConstructor(getParameterizedConstructor());

		// Obtain a no-arg constructor, if one is appropriate to provide
		builder.addConstructor(getNoArgConstructor());

		if (annotationValues.isGettersByDefault()) {
			List<MethodMetadata> accessors = getAccessors();
			for (MethodMetadata accessor : accessors) {
				builder.addMethod(accessor);
			}
		}
		if (annotationValues.isSettersByDefault()) {
			List<MethodMetadata> mutators = getMutators();
			for (MethodMetadata mutator : mutators) {
				builder.addMethod(mutator);
			}
		}

		// Add custom data tag for Roo Identifier type
		builder.putCustomData(IDENTIFIER_TYPE, null);

		// Create a representation of the desired output ITD
		buildItd();
	}

	public AnnotationMetadata getEmbeddableAnnotation() {
		if (governorTypeDetails.getAnnotation(EMBEDDABLE) != null) {
			return null;
		}
		AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(EMBEDDABLE);
		return annotationBuilder.build();
	}

	/**
	 * Locates declared fields.
	 *
	 * <p>
	 * If no parent is defined, one will be located or created. All declared fields will be returned.
	 *
	 * @return fields (never returns null)
	 */
	public List<FieldMetadata> getFields() {
		// Locate all declared fields
		List<? extends FieldMetadata> declaredFields = governorTypeDetails.getDeclaredFields();

		// Add fields to ITD from annotation
		List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
		if (identifierServiceResult != null) {
			for (Identifier identifier : identifierServiceResult) {
				List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
				annotations.add(getColumnBuilder(identifier));
				if (identifier.getFieldType().equals(DATE)) {
					setDateAnnotations(identifier.getColumnDefinition(), annotations);
				}

				FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations, identifier.getFieldName(), identifier.getFieldType());
				FieldMetadata idField = fieldBuilder.build();

				// Only add field to ITD if not declared on governor
				if (!hasField(declaredFields, idField)) {
					fields.add(idField);
				}
			}
		}

		fields.addAll(declaredFields);

		// Remove fields with static and transient modifiers
		for (Iterator<FieldMetadata> iter = fields.iterator(); iter.hasNext();) {
			FieldMetadata field = iter.next();
			if (Modifier.isStatic(field.getModifier()) || Modifier.isTransient(field.getModifier())) {
				iter.remove();
			}
		}

		// Remove fields with the @Transient annotation
		List<FieldMetadata> transientAnnotatedFields = governorTypeDetails.getFieldsWithAnnotation(TRANSIENT);
		if (fields.containsAll(transientAnnotatedFields)) {
			fields.removeAll(transientAnnotatedFields);
		}

		if (!fields.isEmpty()) {
			return fields;
		}

		// We need to create a default identifier field
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

		// Compute the column name, as required
		AnnotationMetadataBuilder columnBuilder = new AnnotationMetadataBuilder(COLUMN);
		columnBuilder.addStringAttribute("name", "id");
		columnBuilder.addBooleanAttribute("nullable", false);
		annotations.add(columnBuilder);

		FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations, new JavaSymbolName("id"), LONG_OBJECT);
		fields.add(fieldBuilder.build());

		return fields;
	}

	private AnnotationMetadataBuilder getColumnBuilder(final Identifier identifier) {
		AnnotationMetadataBuilder columnBuilder = new AnnotationMetadataBuilder(COLUMN);
		columnBuilder.addStringAttribute("name", identifier.getColumnName());
		if (StringUtils.hasText(identifier.getColumnDefinition())) {
			columnBuilder.addStringAttribute("columnDefinition", identifier.getColumnDefinition());
		}
		columnBuilder.addBooleanAttribute("nullable", false);

		// Add length attribute for Strings
		if (identifier.getColumnSize() < 4000 && identifier.getFieldType().equals(JavaType.STRING)) {
			columnBuilder.addIntegerAttribute("length", identifier.getColumnSize());
		}

		// Add precision and scale attributes for numeric fields
		if (identifier.getScale() > 0 && (identifier.getFieldType().equals(JavaType.DOUBLE_OBJECT) || identifier.getFieldType().equals(JavaType.DOUBLE_PRIMITIVE) || identifier.getFieldType().equals(BIG_DECIMAL))) {
			columnBuilder.addIntegerAttribute("precision", identifier.getColumnSize());
			columnBuilder.addIntegerAttribute("scale", identifier.getScale());
		}

		return columnBuilder;
	}

	private void setDateAnnotations(final String columnDefinition, final List<AnnotationMetadataBuilder> annotations) {
		// Add JSR 220 @Temporal annotation to date fields
		String temporalType = StringUtils.defaultIfEmpty(StringUtils.toUpperCase(columnDefinition), "DATE");
		if ("DATETIME".equals(temporalType)) {
			temporalType = "TIMESTAMP"; // ROO-2606
		}
		AnnotationMetadataBuilder temporalBuilder = new AnnotationMetadataBuilder(TEMPORAL);
		temporalBuilder.addEnumAttribute("value", new EnumDetails(TEMPORAL_TYPE, new JavaSymbolName(temporalType)));
		annotations.add(temporalBuilder);

		AnnotationMetadataBuilder dateTimeFormatBuilder = new AnnotationMetadataBuilder(DATE_TIME_FORMAT);
		dateTimeFormatBuilder.addStringAttribute("style", "M-");
		annotations.add(dateTimeFormatBuilder);
	}

	private boolean hasField(final List<? extends FieldMetadata> declaredFields, final FieldMetadata idField) {
		for (FieldMetadata declaredField : declaredFields) {
			if (declaredField.getFieldName().equals(idField.getFieldName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Locates the accessor methods.
	 *
	 * <p>
	 * If {@link #getFields()} returns fields created by this ITD, public accessors will automatically be produced in the declaring class.
	 *
	 * @return the accessors (never returns null)
	 */
	public List<MethodMetadata> getAccessors() {
		Assert.notNull(fields, "Fields required");
		List<MethodMetadata> accessors = new ArrayList<MethodMetadata>();

		// Compute the names of the accessors that will be produced
		for (FieldMetadata field : fields) {
			JavaSymbolName requiredAccessorName = BeanInfoUtils.getAccessorMethodName(field);
			MethodMetadata accessor = getGovernorMethod(requiredAccessorName);
			if (accessor != null) {
				Assert.isTrue(Modifier.isPublic(accessor.getModifier()), "User provided field but failed to provide a public '" + requiredAccessorName.getSymbolName() + "()' method in '" + destination.getFullyQualifiedTypeName() + "'");
			} else {
				accessor = getAccessorMethod(field.getFieldName(), field.getFieldType());
			}
			accessors.add(accessor);
		}

		return accessors;
	}

	/**
	 * Locates the mutator methods.
	 *
	 * <p>
	 * If {@link #getFields()} returns fields created by this ITD, public mutators will automatically be produced in the declaring class.
	 *
	 * @return the mutators (never returns null)
	 */
	public List<MethodMetadata> getMutators() {
		Assert.notNull(fields, "Fields required");
		List<MethodMetadata> mutators = new ArrayList<MethodMetadata>();

		// Compute the names of the mutators that will be produced
		for (FieldMetadata field : fields) {
			JavaSymbolName requiredMutatorName = BeanInfoUtils.getMutatorMethodName(field);
			final JavaType parameterType = field.getFieldType();
			MethodMetadata mutator = getGovernorMethod(requiredMutatorName, parameterType);
			if (mutator == null) {
				mutator = getMutatorMethod(field.getFieldName(), field.getFieldType());
			} else {
				Assert.isTrue(Modifier.isPublic(mutator.getModifier()), "User provided field but failed to provide a public '" + requiredMutatorName + "(" + field.getFieldName().getSymbolName() + ")' method in '" + destination.getFullyQualifiedTypeName() + "'");
			}
			mutators.add(mutator);
		}

		return mutators;
	}

	/**
	 * Locates the parameterised constructor consisting of the id fields for this class.
	 *
	 * @return the constructor, never null.
	 */
	public ConstructorMetadata getParameterizedConstructor() {
		Assert.notNull(fields, "Fields required");
		// Search for an existing constructor
		List<JavaType> parameterTypes = new ArrayList<JavaType>();
		for (FieldMetadata field : fields) {
			parameterTypes.add(field.getFieldType());
		}

		ConstructorMetadata result = governorTypeDetails.getDeclaredConstructor(parameterTypes);
		if (result != null) {
			// Found an existing no-arg constructor on this class, so return it
			publicNoArgConstructor = true;
			return result;
		}

		List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("super();");
		for (FieldMetadata field : fields) {
			String fieldName = field.getFieldName().getSymbolName();
			bodyBuilder.appendFormalLine("this." + fieldName + " = " + fieldName + ";");
			parameterNames.add(field.getFieldName());
		}

		// Create the constructor
		ConstructorMetadataBuilder constructorBuilder = new ConstructorMetadataBuilder(getId());
		constructorBuilder.setModifier(Modifier.PUBLIC);
		constructorBuilder.setParameterTypes(AnnotatedJavaType.convertFromJavaTypes(parameterTypes));
		constructorBuilder.setParameterNames(parameterNames);
		constructorBuilder.setBodyBuilder(bodyBuilder);
		return constructorBuilder.build();
	}

	/**
	 * Locates the no-arg constructor for this class, if available.
	 *
	 * <p>
	 * If a class defines a no-arg constructor, it is returned (irrespective of access modifiers).
	 *
	 * <p>
	 * If a class does not define a no-arg constructor, one might be created. It
	 * will only be created if the {@link #noArgConstructor} is true AND there
	 * is at least one other constructor declared in the source file. If a
	 * constructor is created, it will have a private access modifier.
	 *
	 * @return the constructor (may return null if no constructor is to be produced)
	 */
	public ConstructorMetadata getNoArgConstructor() {
		// Search for an existing constructor
		List<JavaType> parameterTypes = new ArrayList<JavaType>();
		ConstructorMetadata result = governorTypeDetails.getDeclaredConstructor(parameterTypes);
		if (result != null) {
			// Found an existing no-arg constructor on this class, so return it
			return result;
		}

		// To get this far, the user did not define a no-arg constructor

		if (!noArgConstructor) {
			// This metadata instance is prohibited from making a no-arg constructor
			return null;
		}

		// Create the constructor
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("super();");

		ConstructorMetadataBuilder constructorBuilder = new ConstructorMetadataBuilder(getId());
		constructorBuilder.setModifier(publicNoArgConstructor ? Modifier.PUBLIC : Modifier.PRIVATE);
		constructorBuilder.setParameterTypes(AnnotatedJavaType.convertFromJavaTypes(parameterTypes));
		constructorBuilder.setBodyBuilder(bodyBuilder);
		return constructorBuilder.build();
	}

	public static String getMetadataIdentifierType() {
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
