package org.springframework.roo.addon.serializable;

import static org.springframework.roo.model.RooJavaType.ROO_SERIALIZABLE;

import java.lang.reflect.Modifier;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Metadata for {@link RooSerializable}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class SerializableMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	
	// Constants
	private static final String PROVIDES_TYPE_STRING = SerializableMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private static final JavaType SERIALIZABLE = new JavaType("java.io.Serializable");

	// From annotation
	@AutoPopulate private String serialVersionUIDField = "serialVersionUID";

	public SerializableMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");

		if (!isValid()) {
			return;
		}

		// Process values from the annotation, if present
		AnnotationMetadata annotation = MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, ROO_SERIALIZABLE);
		if (annotation != null) {
			AutoPopulationUtils.populate(this, annotation);
		}
		
		// Generate "implements Serializable"
		if (!isJavaSerializableInterfaceIntroduced()) {
			builder.addImplementsType(SERIALIZABLE);
		}

		// Generate the serialVersionUID field
		builder.addField(getSerialVersionUIDField());

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	/**
	 * @return true if the ITD will be introducing a java.io.Serializable interface (false means the class already was Serializable)
	 */
	private boolean isJavaSerializableInterfaceIntroduced() {
		return isImplementing(governorTypeDetails, SERIALIZABLE);
	}

	/**
	 * Obtains the "serialVersionUID" field for this type, if available.
	 * 
	 * <p>
	 * If the user provided a "serialVersionUID" field, that field will be returned.
	 * 
	 * @return the "serialVersionUID" field declared on this type or that will be introduced (or null if undeclared and not introduced)
	 */
	private FieldMetadata getSerialVersionUIDField() {
		if (isJavaSerializableInterfaceIntroduced()) {
			return null;
		}
		
		// Compute the relevant toString method name
		JavaSymbolName fieldName = new JavaSymbolName("serialVersionUID");
		if (!this.serialVersionUIDField.equals("")) {
			fieldName = new JavaSymbolName(this.serialVersionUIDField);
		}
	
		// See if the type itself declared the field
		FieldMetadata result = MemberFindingUtils.getDeclaredField(governorTypeDetails, fieldName);
		if (result != null) {
			FieldMetadataBuilder field = new FieldMetadataBuilder(result);
			field.putCustomData(CustomDataSerializableTags.SERIAL_VERSION_UUID_FIELD.name(), null);
			return field.build();
		}
		
		FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL, fieldName, JavaType.LONG_PRIMITIVE, "1L");
		fieldBuilder.putCustomData(CustomDataSerializableTags.SERIAL_VERSION_UUID_FIELD.name(), null);
		return fieldBuilder.build();
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
