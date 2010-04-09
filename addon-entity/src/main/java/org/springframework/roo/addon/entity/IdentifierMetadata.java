package org.springframework.roo.addon.entity;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.DefaultConstructorMetadata;
import org.springframework.roo.classpath.details.DefaultFieldMetadata;
import org.springframework.roo.classpath.details.DefaultMethodMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for {@link RooIdentifier}.
 * 
 * <p>
 * Any getter produced by this metadata is automatically included in the {@link BeanInfoMetadata}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class IdentifierMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

	private static final String PROVIDES_TYPE_STRING = IdentifierMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private static final JavaType ID = new JavaType("javax.persistence.Id");
	private static final JavaType EMBEDDABLE = new JavaType("javax.persistence.Embeddable");

	private boolean noArgConstructor;
	private IdentifierMetadata parent;
	
	public IdentifierMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, IdentifierMetadata parent, boolean noArgConstructor) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");

		if (!isValid()) {
			return;
		}

		this.noArgConstructor = noArgConstructor;
		this.parent = parent;

		// Process values from the annotation, if present
		AnnotationMetadata annotation = MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, new JavaType(RooIdentifier.class.getName()));
		if (annotation != null) {
			AutoPopulationUtils.populate(this, annotation);
		}

		// Add @java.persistence.Embeddable annotation
		builder.addTypeAnnotation(getEmbeddableAnnotation());

		// Obtain a no-arg constructor, if one is appropriate to provide
		builder.addConstructor(getNoArgConstructor());

		// Add identifier fields and accessors (no mutators)
		List<FieldMetadata> fields = getIdentifierFields();
		for (FieldMetadata field : fields) {
			builder.addField(field);
		}
		List<MethodMetadata> accessors = getIdentifierAccessors();
		for (MethodMetadata accessor : accessors) {
			builder.addMethod(accessor);
		}

		// Add a constructor with the id fields required
		builder.addConstructor(getConstructor());

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	public AnnotationMetadata getEmbeddableAnnotation() {
		if (MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, EMBEDDABLE) == null) {
			return new DefaultAnnotationMetadata(EMBEDDABLE, new ArrayList<AnnotationAttributeValue<?>>());
		}
		return MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, EMBEDDABLE);
	}

	/**
	 * Locates the identifier fields.
	 * 
	 * <p>
	 * If no parent is defined, one will be located or created. Any declared or inherited field which has the
	 * @javax.persistence.Id annotation will be taken as the identifier and returned.
	 * 
	 * @return the identifiers (never returns null)
	 */
	public List<FieldMetadata> getIdentifierFields() {
		if (parent != null) {
			return parent.getIdentifierFields();
		}

		// Try to locate existing fields with @javax.persistence.Id
		List<FieldMetadata> foundIds = MemberFindingUtils.getFieldsWithAnnotation(governorTypeDetails, ID);
		if (foundIds.size() > 0) {
			return foundIds;
		}

		// Ensure there isn't already a field called "id"; if so, compute a unique name (it's not really a fatal situation at the end of the day)
		int index = -1;
		JavaSymbolName idField = null;
		while (true) {
			// Compute the required field name
			index++;
			String fieldName = "";
			for (int i = 0; i < index; i++) {
				fieldName = fieldName + "_";
			}
			fieldName = fieldName + "id";

			idField = new JavaSymbolName(fieldName);
			if (MemberFindingUtils.getField(governorTypeDetails, idField) == null) {
				// Found a usable field name
				break;
			}
		}

		// We need to create one
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		JavaType annotationType = ID;
		AnnotationMetadata identifierAnnotation = new DefaultAnnotationMetadata(annotationType, new ArrayList<AnnotationAttributeValue<?>>());
		annotations.add(identifierAnnotation);

		// Compute the column name, as required
		String columnName = idField.getSymbolName();
		List<AnnotationAttributeValue<?>> columnAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		columnAttributes.add(new StringAttributeValue(new JavaSymbolName("name"), columnName));
		AnnotationMetadata columnAnnotation = new DefaultAnnotationMetadata(new JavaType("javax.persistence.Column"), columnAttributes);
		annotations.add(columnAnnotation);

		List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
		fields.add(new DefaultFieldMetadata(getId(), Modifier.PRIVATE, idField, new JavaType(Long.class.getName()), null, annotations));
		return fields;
	}

	/**
	 * Locates the identifier accessor methods.
	 * 
	 * <p>
	 * If {@link #getIdentifierFields()} returns fields created by this ITD, public accessors will automatically be produced in the declaring class. 
	 * 
	 * @return the accessors (never returns null)
	 */
	public List<MethodMetadata> getIdentifierAccessors() {
		if (parent != null) {
			return parent.getIdentifierAccessors();
		}

		List<MethodMetadata> accessors = new ArrayList<MethodMetadata>();

		// Locate the identifier fields, and compute the names of the accessors that will be produced
		List<FieldMetadata> ids = getIdentifierFields();
		for (FieldMetadata id : ids) {
			String requiredAccessorName = getRequiredAccessorName(id);

			// See if the user provided the field and the accessor method
			if (!getId().equals(id.getDeclaredByMetadataId())) {
				MethodMetadata accessor = MemberFindingUtils.getMethod(governorTypeDetails, new JavaSymbolName(requiredAccessorName), new ArrayList<JavaType>());
				if (accessor != null) {
					Assert.isTrue(Modifier.isPublic(accessor.getModifier()), "User provided @javax.persistence.Id field but failed to provide a public '" + requiredAccessorName + "()' method in '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "'");
					accessors.add(accessor);
				} else {
					accessors.add(getAccessor(id));
				}
			}
		}

		if (!accessors.isEmpty()) {
			return accessors;
		}

		// No accessor declared in governor so produce a public accessor for the default id field
		FieldMetadata id = ids.get(0);
		accessors.add(getAccessor(id));

		return new ArrayList<MethodMetadata>(accessors);
	}
	
	private MethodMetadata getAccessor(FieldMetadata id) {
		String requiredAccessorName = getRequiredAccessorName(id);
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return this." + id.getFieldName().getSymbolName() + ";");
		return new DefaultMethodMetadata(getId(), Modifier.PUBLIC, new JavaSymbolName(requiredAccessorName), id.getFieldType(), new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), new ArrayList<AnnotationMetadata>(), new ArrayList<JavaType>(), bodyBuilder.getOutput());
	}

	private String getRequiredAccessorName(FieldMetadata id) {
		return "get" + StringUtils.capitalize(id.getFieldName().getSymbolName());
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
		return new DefaultConstructorMetadata(getId(), Modifier.PRIVATE, AnnotatedJavaType.convertFromJavaTypes(paramTypes), new ArrayList<JavaSymbolName>(), new ArrayList<AnnotationMetadata>(), bodyBuilder.getOutput());
	}

	/**
	 * Locates the parameterised constructor for this class with the specified id fields, if available.
	 * 
	 * <p>
	 * If a class defines the parameterised constructor with the specified id fields, it is returned (irrespective of access modifiers).
	 * 
	 * <p>
	 * If a class does not define an parameterised constructor, one might be created.
	 * 
	 * @return the constructor
	 */
	public ConstructorMetadata getConstructor() {
		// Locate the identifier fields
		List<FieldMetadata> ids = getIdentifierFields();

		// Compute the constructor parameters
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		for (FieldMetadata id : ids) {
			paramTypes.add(id.getFieldType());
		}

		// Search for an existing constructor
		ConstructorMetadata result = MemberFindingUtils.getDeclaredConstructor(governorTypeDetails, paramTypes);
		if (result != null) {
			// Found an existing parameterised constructor on this class, so return it
			return result;
		}

		// Create the constructor
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		for (FieldMetadata id : ids) {
			paramNames.add(id.getFieldName());
		}

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("super();");
		for (FieldMetadata id : ids) {
			bodyBuilder.appendFormalLine("this." + id.getFieldName().getSymbolName() + " = " + id.getFieldName().getSymbolName() + ";");
		}

		return new DefaultConstructorMetadata(getId(), Modifier.PUBLIC, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, new ArrayList<AnnotationMetadata>(), bodyBuilder.getOutput());
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
