package org.springframework.roo.addon.javabean;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.DefaultMethodMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
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
 * Metadata for {@link RooJavaBean}.
 * 
 * <p>
 * Any getter or setter produced by this metadata is automatically included in the
 * {@link BeanInfoMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class JavaBeanMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

	private static final String PROVIDES_TYPE_STRING = JavaBeanMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	
	// From annotation
	@AutoPopulate private boolean gettersByDefault = true;
	@AutoPopulate private boolean settersByDefault = true;
	
	public JavaBeanMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(identifier, aspectName, governorPhysicalTypeMetadata, getJavaType(identifier));
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		
		if (!isValid()) {
			return;
		}

		// Process values from the annotation, if present
		AnnotationMetadata annotation = MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, new JavaType(RooJavaBean.class.getName()));
		if (annotation != null) {
			AutoPopulationUtils.populate(this, annotation);
		}

		// Add getters and setters
		for (FieldMetadata field : governorTypeDetails.getDeclaredFields()) {
			builder.addMethod(getDeclaredGetter(field));
			builder.addMethod(getDeclaredSetter(field));
		}
		
		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}
	
	/**
	 * Obtains the specific accessor method that is either contained within the normal Java compilation unit or will
	 * be introduced by this add-on via an ITD.
	 * 
	 * @param field that already exists on the type either directly or via introduction (required; must be declared by this type to be located)
	 * @return the method corresponding to an accessor, or null if not found
	 */
	public MethodMetadata getDeclaredGetter(FieldMetadata field) {
		Assert.notNull(field, "Field required");
		
		// Compute the accessor method name
		JavaSymbolName methodName;
		// TODO: This is a temporary workaround to support web data binding approaches; to be reviewed more thoroughly in future
//		if (field.getFieldType().equals(JavaType.BOOLEAN_PRIMITIVE)) {
//			methodName = new JavaSymbolName("is" + StringUtils.capitalize(field.getFieldName().getSymbolName()));
//		} else {
			methodName = new JavaSymbolName("get" + StringUtils.capitalize(field.getFieldName().getSymbolName()));
//		}
		
		// See if the type itself declared the accessor
		MethodMetadata result = MemberFindingUtils.getDeclaredMethod(governorTypeDetails, methodName, null);
		if (result != null) {
			return result;
		}
		
		// Decide whether we need to produce the accessor method
		if (this.gettersByDefault && !Modifier.isTransient(field.getModifier())) {
			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine("return this." + field.getFieldName().getSymbolName() + ";");
			result = new DefaultMethodMetadata(getId(), Modifier.PUBLIC, methodName, field.getFieldType(), new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), new ArrayList<AnnotationMetadata>(), bodyBuilder.getOutput());
		}
		
		return result;
	}
	
	/**
	 * Obtains the specific mutator method that is either contained within the normal Java compilation unit or will
	 * be introduced by this add-on via an ITD.
	 * 
	 * @param field that already exists on the type either directly or via introduction (required; must be declared by this type to be located)
	 * @return the method corresponding to a mutator, or null if not found
	 */
	public MethodMetadata getDeclaredSetter(FieldMetadata field) {
		Assert.notNull(field, "Field required");
		
		// Compute the mutator method name
		JavaSymbolName methodName = new JavaSymbolName("set" + StringUtils.capitalize(field.getFieldName().getSymbolName()));

		// Compute the mutator method parameters
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		paramTypes.add(field.getFieldType());
		
		// See if the type itself declared the mutator
		MethodMetadata result = MemberFindingUtils.getDeclaredMethod(governorTypeDetails, methodName, paramTypes);
		if (result != null) {
			return result;
		}
		
		// Compute the mutator method parameter names
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(field.getFieldName());
		
		// Decide whether we need to produce the mutator method
		if (this.settersByDefault && !Modifier.isTransient(field.getModifier())) {
			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine("this." + field.getFieldName().getSymbolName() + " = " + field.getFieldName().getSymbolName() + ";");
			result = new DefaultMethodMetadata(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, new ArrayList<AnnotationMetadata>(), bodyBuilder.getOutput());
		}
		
		return result;
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
