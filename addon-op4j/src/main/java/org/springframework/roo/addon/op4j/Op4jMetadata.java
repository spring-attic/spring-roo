package org.springframework.roo.addon.op4j;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Metadata to be triggered by {@link RooOp4j} annotation
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public class Op4jMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	private static final String PROVIDES_TYPE_STRING = Op4jMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);

	public Op4jMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");

		if (!isValid()) {
			return;
		}

		builder.addInnerType(getInnerType());

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	private ClassOrInterfaceTypeDetails getInnerType() {
		List<FieldMetadataBuilder> fields = new ArrayList<FieldMetadataBuilder>();
		int fieldModifier = Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL;
		String targetName = super.destination.getSimpleTypeName();

		ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(new JavaType("org.op4j.functions.Get"));
		imports.addImport(new JavaType("org.javaruntype.type.Types"));
		String initializer = "Get.attrOf(Types.forClass(" + targetName + ".class),\"" + targetName.toLowerCase() + "\")";

		List<JavaType> parameters = new ArrayList<JavaType>();
		parameters.add(new JavaType(Object.class.getName()));
		parameters.add(super.destination);

		JavaType function = new JavaType("org.op4j.functions.Function", 0, DataType.TYPE, null, parameters);

		FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), fieldModifier, new JavaSymbolName(targetName.toUpperCase()), function, initializer);
		fields.add(fieldBuilder);

		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(getId(), Modifier.PUBLIC | Modifier.STATIC, new JavaType("Keys"), PhysicalTypeCategory.CLASS);
		typeDetailsBuilder.setDeclaredFields(fields);
		return typeDetailsBuilder.build();
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
