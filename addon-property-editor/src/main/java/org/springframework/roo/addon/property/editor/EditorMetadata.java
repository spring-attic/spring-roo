package org.springframework.roo.addon.property.editor;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.DefaultFieldMetadata;
import org.springframework.roo.classpath.details.DefaultMethodMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Metadata for {@link RooEditor}.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
public class EditorMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

	private static final String PROVIDES_TYPE_STRING = EditorMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);

	private BeanInfoMetadata beanInfoMetadata;
	private EntityMetadata entityMetadata;

	public EditorMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, BeanInfoMetadata beanInfoMetadata, EntityMetadata entityMetadata) {
		super(identifier, aspectName, governorPhysicalTypeMetadata, getJavaType(identifier));
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(beanInfoMetadata, "Bean info metadata required");
		Assert.notNull(entityMetadata, "Entity metadata required");
		
		if (!isValid()) {
			return;
		}

		// Process values from the annotation, if present
		AnnotationMetadata annotation = MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, new JavaType(RooEditor.class.getName()));
		if (annotation != null) {
			AutoPopulationUtils.populate(this, annotation);
		}		
		
		this.beanInfoMetadata = beanInfoMetadata;
		this.entityMetadata = entityMetadata;
		
		builder.addImplementsType(new JavaType("java.beans.PropertyEditorSupport"));
		
		JavaType typeConverter = new JavaType("org.springframework.beans.SimpleTypeConverter");
		builder.addField(new DefaultFieldMetadata(getId(), 0, new JavaSymbolName("typeConverter"), typeConverter, typeConverter, null));

		builder.addMethod(getGetAsTextMethod());		
		builder.addMethod(getSetAsTextMethod());
		
		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	private MethodMetadata getGetAsTextMethod() {
		
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("Object obj = getValue();");
		bodyBuilder.appendFormalLine("if (obj == null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return null;");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return (String) typeConverter.convertIfNecessary(((" + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName() + ") obj)." + entityMetadata.getIdentifierAccessor().getMethodName() + "() , String.class);");

		List<AnnotatedJavaType> types = new ArrayList<AnnotatedJavaType>();			
		List<JavaSymbolName> names = new ArrayList<JavaSymbolName>();
		return new DefaultMethodMetadata(getId(), Modifier.PUBLIC, new JavaSymbolName("getAsText"), new JavaType(String.class.getName()), types, names, null, bodyBuilder.getOutput());
	}
 
	private MethodMetadata getSetAsTextMethod() {
		
		String identifierTypeName = entityMetadata.getIdentifierField().getFieldType().getFullyQualifiedTypeName();
		
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("if (text == null || \"\".equals(text)) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("setValue(null);");
		bodyBuilder.appendFormalLine("return;");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.newLine();
		bodyBuilder.appendFormalLine (identifierTypeName + " identifier = (" + identifierTypeName + ") typeConverter.convertIfNecessary(text, " + identifierTypeName + ".class);");
		bodyBuilder.appendFormalLine("if (identifier == null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("setValue(null);");
		bodyBuilder.appendFormalLine("return;");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.newLine();
		bodyBuilder.appendFormalLine("setValue(" + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName() + "." + entityMetadata.getFindMethod().getMethodName() + "(identifier));");
		
		List<AnnotatedJavaType> types = new ArrayList<AnnotatedJavaType>();		
		types.add(new AnnotatedJavaType(new JavaType(String.class.getName()), null));
		
		List<JavaSymbolName> names = new ArrayList<JavaSymbolName>();
		names.add(new JavaSymbolName("text"));
		
		return new DefaultMethodMetadata(getId(), Modifier.PUBLIC, new JavaSymbolName("setAsText"), JavaType.VOID_PRIMITIVE, types, names, null, bodyBuilder.getOutput());
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