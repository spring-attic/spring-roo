package org.springframework.roo.addon.entity;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for {@link RooIdentifier}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class IdentifierMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	private static final String PROVIDES_TYPE_STRING = IdentifierMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private static final JavaType EMBEDDABLE = new JavaType("javax.persistence.Embeddable");
	private static final JavaType COLUMN = new JavaType("javax.persistence.Column");

	private boolean noArgConstructor;
	private boolean publicNoArgConstructor = false;
	private List<FieldMetadata> fields;

	// From annotation
	@AutoPopulate private boolean gettersByDefault = true;
	@AutoPopulate private boolean settersByDefault = false;

	/** See {@link IdentifierService} for further information (populated via {@link IdentifierMetadataProviderImpl}); may be null */
	private List<Identifier> identifierServiceResult;

	public IdentifierMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, boolean noArgConstructor, List<Identifier> identifierServiceResult) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");

		if (!isValid()) {
			return;
		}

		this.noArgConstructor = noArgConstructor;
		this.identifierServiceResult = identifierServiceResult;

		// Process values from the annotation, if present
		AnnotationMetadata annotation = MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, new JavaType(RooIdentifier.class.getName()));
		if (annotation != null) {
			AutoPopulationUtils.populate(this, annotation);
		}
		
		// Add @Embeddable annotation
		builder.addAnnotation(getEmbeddableAnnotation());
		
		// Add declared fields and accessors and mutators
		fields = getFields();
		for (FieldMetadata field : fields) {
			builder.addField(field);
		}
		
		// Obtain an parameterised constructor,
		builder.addConstructor(getParameterizedConstructor());

		// Obtain a no-arg constructor, if one is appropriate to provide
		builder.addConstructor(getNoArgConstructor());

		if (gettersByDefault) {
			List<MethodMetadata> accessors = getAccessors();
			for (MethodMetadata accessor : accessors) {
				builder.addMethod(accessor);
			}
		}
		if (settersByDefault) {
			List<MethodMetadata> mutators = getMutators();
			for (MethodMetadata mutator : mutators) {
				builder.addMethod(mutator);
			}
		}

		// Add equals and hashCode methods
		builder.addMethod(getEqualsMethod());
		builder.addMethod(getHashCodeMethod());
		
		// Add custom data tag for Roo Identifier type
		builder.putCustomData(PersistenceCustomDataKeys.IDENTIFIER_TYPE, null);

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	public AnnotationMetadata getEmbeddableAnnotation() {
		if (MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, EMBEDDABLE) != null) {
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
				setDateAnnotations(identifier, annotations);
				
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
		List<FieldMetadata> transientAnnotatedFields = MemberFindingUtils.getFieldsWithAnnotation(governorTypeDetails, new JavaType("javax.persistence.Transient"));
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

		FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations, new JavaSymbolName("id"), new JavaType(Long.class.getName()));
		fields.add(fieldBuilder.build());
		
		return fields;
	}
	
	private AnnotationMetadataBuilder getColumnBuilder(Identifier identifier) {
		AnnotationMetadataBuilder columnBuilder = new AnnotationMetadataBuilder(COLUMN);
		columnBuilder.addStringAttribute("name", identifier.getColumnName());
		if (StringUtils.hasText(identifier.getColumnDefinition())) {
			columnBuilder.addStringAttribute("columnDefinition", identifier.getColumnDefinition());
		}
		columnBuilder.addBooleanAttribute("nullable", false);

		// Add length attribute for Strings
		if (identifier.getColumnSize() < 4000 && identifier.getFieldType().equals(JavaType.STRING_OBJECT)) {
			columnBuilder.addIntegerAttribute("length", identifier.getColumnSize());
		}

		// Add precision and scale attributes for numeric fields
		if (identifier.getScale() > 0 && (identifier.getFieldType().equals(JavaType.DOUBLE_OBJECT) || identifier.getFieldType().equals(JavaType.DOUBLE_PRIMITIVE) || identifier.getFieldType().equals(new JavaType("java.math.BigDecimal")))) {
			columnBuilder.addIntegerAttribute("precision", identifier.getColumnSize());
			columnBuilder.addIntegerAttribute("scale", identifier.getScale());
		}

		return columnBuilder;
	}
	
	private void setDateAnnotations(Identifier identifier, List<AnnotationMetadataBuilder> annotations) {
		// Add JSR 220 @Temporal annotation to date fields
		if (identifier.getFieldType().equals(new JavaType("java.util.Date"))) {
			String temporalType = StringUtils.defaultIfEmpty(StringUtils.toUpperCase(identifier.getColumnDefinition()), "DATE");
			AnnotationMetadataBuilder temporalBuilder = new AnnotationMetadataBuilder(new JavaType("javax.persistence.Temporal"));
			temporalBuilder.addEnumAttribute("value", new EnumDetails(new JavaType("javax.persistence.TemporalType"), new JavaSymbolName(temporalType)));
			annotations.add(temporalBuilder);

			AnnotationMetadataBuilder dateTimeFormatBuilder = new AnnotationMetadataBuilder(new JavaType("org.springframework.format.annotation.DateTimeFormat"));
			dateTimeFormatBuilder.addStringAttribute("style", "S-");
			annotations.add(dateTimeFormatBuilder);
		}
	}

	private boolean hasField(List<? extends FieldMetadata> declaredFields, FieldMetadata idField) {
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
		List<MethodMetadata> accessors = new LinkedList<MethodMetadata>();

		// Compute the names of the accessors that will be produced
		for (FieldMetadata field : fields) {
			String requiredAccessorName = getRequiredAccessorName(field);
			MethodMetadata accessor = MemberFindingUtils.getMethod(governorTypeDetails, new JavaSymbolName(requiredAccessorName), new ArrayList<JavaType>());
			if (accessor != null) {
				Assert.isTrue(Modifier.isPublic(accessor.getModifier()), "User provided field but failed to provide a public '" + requiredAccessorName + "()' method in '" + destination.getFullyQualifiedTypeName() + "'");
			} else {
				accessor = getAccessor(field);
			}
			accessors.add(accessor);
		}

		return accessors;
	}

	private String getRequiredAccessorName(FieldMetadata field) {
		return "get" + StringUtils.capitalize(field.getFieldName().getSymbolName());
	}

	private MethodMetadata getAccessor(FieldMetadata field) {
		String requiredAccessorName = getRequiredAccessorName(field);
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return this." + field.getFieldName().getSymbolName() + ";");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, new JavaSymbolName(requiredAccessorName), field.getFieldType(), bodyBuilder);
		return methodBuilder.build();
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
		List<MethodMetadata> mutators = new LinkedList<MethodMetadata>();

		// Compute the names of the mutators that will be produced
		for (FieldMetadata field : fields) {
			String requiredMutatorName = getRequiredMutatorName(field);

			List<JavaType> paramTypes = new ArrayList<JavaType>();
			paramTypes.add(field.getFieldType());
			MethodMetadata mutator = MemberFindingUtils.getMethod(governorTypeDetails, new JavaSymbolName(requiredMutatorName), paramTypes);
			if (mutator != null) {
				Assert.isTrue(Modifier.isPublic(mutator.getModifier()), "User provided field but failed to provide a public '" + requiredMutatorName + "(" + field.getFieldName().getSymbolName() + ")' method in '" + destination.getFullyQualifiedTypeName() + "'");
			} else {
				mutator = getMutator(field);
			}
			mutators.add(mutator);
		}

		return mutators;
	}

	private String getRequiredMutatorName(FieldMetadata field) {
		return "set" + StringUtils.capitalize(field.getFieldName().getSymbolName());
	}

	private MethodMetadata getMutator(FieldMetadata field) {
		String requiredMutatorName = getRequiredMutatorName(field);

		List<JavaType> paramTypes = new ArrayList<JavaType>();
		paramTypes.add(field.getFieldType());
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(field.getFieldName());

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("this." + field.getFieldName().getSymbolName() + " = " + field.getFieldName().getSymbolName() + ";");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, new JavaSymbolName(requiredMutatorName), JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, bodyBuilder);
		return methodBuilder.build();
	}

	/**
	 * Locates the parameterised constructor consisting of the id fields for this class.
	 *  
	 * @return the constructor, never null.
	 */
	public ConstructorMetadata getParameterizedConstructor() {
		Assert.notNull(fields, "Fields required");
		// Search for an existing constructor
		List<JavaType> paramTypes = new LinkedList<JavaType>();
		for (FieldMetadata field : fields) {
			paramTypes.add(field.getFieldType());
		}
		
		ConstructorMetadata result = MemberFindingUtils.getDeclaredConstructor(governorTypeDetails, paramTypes);
		if (result != null) {
			// Found an existing no-arg constructor on this class, so return it
			publicNoArgConstructor = true;
			return result;
		}

		// Create the constructor
		List<JavaSymbolName> paramNames = new LinkedList<JavaSymbolName>();
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("super();");
		for (FieldMetadata field : fields) {
			bodyBuilder.appendFormalLine("this." + field.getFieldName().getSymbolName() + " = " + field.getFieldName().getSymbolName() + ";");
			paramNames.add(field.getFieldName());
		}

		ConstructorMetadataBuilder constructorBuilder = new ConstructorMetadataBuilder(getId());
		constructorBuilder.setModifier(Modifier.PUBLIC);
		constructorBuilder.setParameterTypes(AnnotatedJavaType.convertFromJavaTypes(paramTypes));
		constructorBuilder.setParameterNames(paramNames);
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
	 * If a class does not define a no-arg constructor, one might be created. It will only be created if the {@link #noArgConstructor} is true AND there is at least one other constructor declared in
	 * the source file. If a constructor is created, it will have a private access modifier.
	 * 
	 * @return the constructor (may return null if no constructor is to be produced)
	 */
	public ConstructorMetadata getNoArgConstructor() {
		// Search for an existing constructor
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		ConstructorMetadata result = MemberFindingUtils.getDeclaredConstructor(governorTypeDetails, paramTypes);
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
		constructorBuilder.setParameterTypes(AnnotatedJavaType.convertFromJavaTypes(paramTypes));
		constructorBuilder.setBodyBuilder(bodyBuilder);
		return constructorBuilder.build();
	}

	public MethodMetadata getEqualsMethod() {
		Assert.notNull(fields, "Fields required");
		// See if the user provided the equals method
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		paramTypes.add(new JavaType("java.lang.Object"));
		MethodMetadata equalsMethod = MemberFindingUtils.getMethod(governorTypeDetails, new JavaSymbolName("equals"), paramTypes);
		if (equalsMethod != null) {
			return equalsMethod;
		}

		if (fields.isEmpty()) {
			return null;
		}

		String typeName = destination.getSimpleTypeName();

		// Create the method
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("if (this == obj) return true;");
		bodyBuilder.appendFormalLine("if (obj == null) return false;");
		bodyBuilder.appendFormalLine("if (!(obj instanceof " + typeName + ")) return false;");
		bodyBuilder.appendFormalLine(typeName + " other = (" + typeName + ") obj;");

		for (FieldMetadata field : fields) {
			String fieldName = field.getFieldName().getSymbolName();
			JavaType fieldType = field.getFieldType();
			if (fieldType.equals(JavaType.BOOLEAN_PRIMITIVE) || fieldType.equals(JavaType.INT_PRIMITIVE) || fieldType.equals(JavaType.LONG_PRIMITIVE) || fieldType.equals(JavaType.SHORT_PRIMITIVE) || fieldType.equals(JavaType.CHAR_PRIMITIVE)) {
				bodyBuilder.appendFormalLine("if (" + fieldName + " != other." + fieldName + ") return false;");
			} else if (fieldType.equals(JavaType.DOUBLE_PRIMITIVE)) {
				bodyBuilder.appendFormalLine("if (Double.doubleToLongBits(" + fieldName + ") != Double.doubleToLongBits(other." + fieldName + ")) return false;");
			} else if (fieldType.equals(JavaType.FLOAT_PRIMITIVE)) {
				bodyBuilder.appendFormalLine("if (Float.floatToIntBits(" + fieldName + ") != Float.floatToIntBits(other." + fieldName + ")) return false;");
			} else {
				bodyBuilder.appendFormalLine("if (" + fieldName + " == null) {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine("if (other." + fieldName + " != null) return false;");
				bodyBuilder.indentRemove();
				bodyBuilder.appendFormalLine("} else if (!" + fieldName + ".equals(other." + fieldName + ")) return false;");
			}
		}
		bodyBuilder.appendFormalLine("return true;");

		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("obj"));

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, new JavaSymbolName("equals"), JavaType.BOOLEAN_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, bodyBuilder);
		return methodBuilder.build();
	}

	public MethodMetadata getHashCodeMethod() {
		Assert.notNull(fields, "Fields required");
		// See if the user provided the hashCode method
		MethodMetadata hashCodeMethod = MemberFindingUtils.getMethod(governorTypeDetails, new JavaSymbolName("hashCode"), new ArrayList<JavaType>());
		if (hashCodeMethod != null) {
			return hashCodeMethod;
		}

		if (fields.isEmpty()) {
			return null;
		}

		// Create the method
		String primeStr = "prime";
		String resultStr = "result";
		
		// Check field names for the same variable names used in the hashCode method
		for (FieldMetadata field : fields) {
			if (primeStr.equals(field.getFieldName().getSymbolName())) {
				primeStr = "_" + primeStr;
				break;
			}
		}
		for (FieldMetadata field : fields) {
			if (resultStr.equals(field.getFieldName().getSymbolName())) {
				resultStr = "_" + resultStr;
				break;
			}
		}
		
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("final int " + primeStr + " = 31;");
		bodyBuilder.appendFormalLine("int " + resultStr + " = 17;");

		String header = resultStr + " = " + primeStr + " * " + resultStr + " + ";
		for (FieldMetadata field : fields) {
			String fieldName = field.getFieldName().getSymbolName();
			JavaType fieldType = field.getFieldType();
			if (fieldType.equals(JavaType.BOOLEAN_PRIMITIVE)) {
				bodyBuilder.appendFormalLine(header + "(" + fieldName + " ? 1231 : 1237);");
			} else if (fieldType.equals(JavaType.INT_PRIMITIVE) || fieldType.equals(JavaType.SHORT_PRIMITIVE) || fieldType.equals(JavaType.CHAR_PRIMITIVE)) {
				bodyBuilder.appendFormalLine(header + fieldName + ";");
			} else if (fieldType.equals(JavaType.LONG_PRIMITIVE)) {
				bodyBuilder.appendFormalLine(header + "(int) (" + fieldName + " ^ (" + fieldName + " >>> 32));");
			} else if (fieldType.equals(JavaType.DOUBLE_PRIMITIVE)) {
				bodyBuilder.appendFormalLine(header + "(int) (Double.doubleToLongBits(" + fieldName + ") ^ (Double.doubleToLongBits(" + fieldName + ") >>> 32));");
			} else if (fieldType.equals(JavaType.FLOAT_PRIMITIVE)) {
				bodyBuilder.appendFormalLine(header + "Float.floatToIntBits(" + fieldName + ");");
			} else {
				bodyBuilder.appendFormalLine(header + "(" + field.getFieldName().getSymbolName() + " == null ? 0 : " + fieldName + ".hashCode());");
			}
		}
		bodyBuilder.appendFormalLine("return " + resultStr + ";");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, new JavaSymbolName("hashCode"), JavaType.INT_PRIMITIVE, bodyBuilder);
		return methodBuilder.build();
	}

	public static String getMetadataIdentifierType() {
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
