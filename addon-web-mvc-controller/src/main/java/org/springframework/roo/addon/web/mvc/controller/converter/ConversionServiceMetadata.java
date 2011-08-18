package org.springframework.roo.addon.web.mvc.controller.converter;

import static org.springframework.roo.model.SpringJavaType.CONFIGURABLE;
import static org.springframework.roo.model.SpringJavaType.FORMATTER_REGISTRY;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.springframework.roo.addon.json.CustomDataJsonTags;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Represents metadata for the application-wide conversion service. Generates the following ITD methods:
 * <ul>
 * 	<li>afterPropertiesSet() - overrides InitializingBean lifecycle parent method</li>
 * 	<li>installLabelConverters(FormatterRegistry registry) - registers all converter methods</li>
 * 	<li>a converter method for all scaffolded domain types as well their associations</li>
 * </ul>
 * 
 * @author Rossen Stoyanchev
 * @author Stefan Schmidt
 * @since 1.1.1
 */
public class ConversionServiceMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

	ConversionServiceMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		// For testing
	}

	/**
	 * Constructor
	 *
	 * @param identifier
	 * @param aspectName
	 * @param governorPhysicalTypeMetadata
	 * @param findMethods
	 * @param idTypes the ID types of the domain types for which to generate converters (required); must be one for each domain type
	 * @param relevantDomainTypes the types for which to generate converters (required)
	 * @param compositePrimaryKeyTypes (required)
	 */
	public ConversionServiceMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, Map<JavaType, MemberTypeAdditions> findMethods, Map<JavaType, JavaType> idTypes, Map<JavaType, List<MethodMetadata>> relevantDomainTypes, Map<JavaType, Map<Object, JavaSymbolName>> compositePrimaryKeyTypes) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.notNull(relevantDomainTypes, "List of domain types required");
		Assert.notNull(compositePrimaryKeyTypes, "List of PK types required");
		Assert.notNull(idTypes, "List of ID types required");
		Assert.isTrue(relevantDomainTypes.size() == idTypes.size(), "Expected " + relevantDomainTypes.size() + " ID types, but was " + idTypes.size());
		
		if (!isValid() || (relevantDomainTypes.isEmpty() && compositePrimaryKeyTypes.isEmpty())) { 
			return;
		}

		MethodMetadataBuilder installMethodBuilder = getInstallMethodBuilder();
		//loading the keyset of the domain type map into a TreeSet to create a consistent ordering of the generated methods across shell restarts
		for (final JavaType type : new TreeSet<JavaType>(relevantDomainTypes.keySet())) {
			JavaSymbolName toIdMethodName = new JavaSymbolName("get" + type.getSimpleTypeName() + "ToStringConverter");
			MethodMetadata toIdMethod = getToStringConverterMethod(type, toIdMethodName, relevantDomainTypes.get(type));
			if (toIdMethod != null) {
				builder.addMethod(toIdMethod);
				installMethodBuilder.getBodyBuilder().appendFormalLine("registry.addConverter(" + toIdMethodName.getSymbolName() + "());");
			}
			
			JavaSymbolName toTypeMethodName = new JavaSymbolName("getIdTo" + type.getSimpleTypeName() + "Converter");
			MethodMetadata toTypeMethod = getToTypeConverterMethod(type, toTypeMethodName, findMethods.get(type), idTypes.get(type));
			if (toTypeMethod != null) {
				builder.addMethod(toTypeMethod);
				installMethodBuilder.getBodyBuilder().appendFormalLine("registry.addConverter(" + toTypeMethodName.getSymbolName() + "());");
			}
			
			// Only allow conversion if ID type is not String already.
			if (!idTypes.get(type).equals(JavaType.STRING_OBJECT)) {
				JavaSymbolName stringToTypeMethodName = new JavaSymbolName("getStringTo" + type.getSimpleTypeName() + "Converter");
				MethodMetadata stringToTypeMethod = getStringToTypeConverterMethod(type, stringToTypeMethodName, idTypes.get(type));
				if (stringToTypeMethod != null) {
					builder.addMethod(stringToTypeMethod);
					installMethodBuilder.getBodyBuilder().appendFormalLine("registry.addConverter(" + stringToTypeMethodName.getSymbolName() + "());");
				}
			}
		}
		for (JavaType type: compositePrimaryKeyTypes.keySet()) {
			for (MethodMetadata converterMethod: getCompositePkConverters(type, compositePrimaryKeyTypes.get(type))) {
				builder.addMethod(converterMethod);
				installMethodBuilder.getBodyBuilder().appendFormalLine("registry.addConverter(" + converterMethod.getMethodName().getSymbolName() + "());");
			}
		}
		
		MethodMetadata installMethod = installMethodBuilder.build();
		if (MemberFindingUtils.getMethod(governorTypeDetails, installMethod.getMethodName(), AnnotatedJavaType.convertFromAnnotatedJavaTypes(installMethod.getParameterTypes())) == null) {
			builder.addMethod(installMethod);
		}
		builder.addMethod(getAfterPropertiesSetMethod(installMethod));
		
		AnnotationMetadata configurable = new DefaultAnnotationMetadata(CONFIGURABLE, new ArrayList<AnnotationAttributeValue<?>>());
		boolean configurablePresent = false;
		for (AnnotationMetadata annotation : governorTypeDetails.getAnnotations()) {
			if (annotation.getAnnotationType().equals(configurable.getAnnotationType())) {
				configurablePresent = true;
				break;
			}
		}
		if(!configurablePresent) {
			builder.addAnnotation(configurable);
		}
		
		itdTypeDetails = builder.build();
	}
	
	private List<MethodMetadata> getCompositePkConverters(JavaType targetType, Map<Object, JavaSymbolName> jsonMethodNames) {
		List<MethodMetadata> converterMethods = new ArrayList<MethodMetadata>();
		
		JavaSymbolName methodName = new JavaSymbolName("getJsonTo" + targetType.getSimpleTypeName() + "Converter");
		JavaType base64 = new JavaType("org.apache.commons.codec.binary.Base64");
		String base64Name = base64.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		String typeName = targetType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		
		if (MemberFindingUtils.getDeclaredMethod(governorTypeDetails, methodName, new ArrayList<JavaType>()) == null) {
			final JavaType converterJavaType = SpringJavaType.getConverterType(JavaType.STRING_OBJECT, targetType);
			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine("return new " + converterJavaType.getNameIncludingTypeParameters() + "() {");
			bodyBuilder.indent();
			bodyBuilder.appendFormalLine("@Override");
			bodyBuilder.appendFormalLine("public " + targetType.getSimpleTypeName() + " convert(String encodedJson) {");
			bodyBuilder.indent();
			bodyBuilder.appendFormalLine("return " + typeName + "." + jsonMethodNames.get(CustomDataJsonTags.FROM_JSON_METHOD).getSymbolName() + "(new String(" + base64Name + ".decodeBase64(encodedJson)));");
			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("}");
			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("};");
			
			converterMethods.add(new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, converterJavaType, bodyBuilder).build());
		}
		
		methodName = new JavaSymbolName("get" + targetType.getSimpleTypeName() + "ToJsonConverter");
		if (MemberFindingUtils.getDeclaredMethod(governorTypeDetails, methodName, new ArrayList<JavaType>()) == null) {
			final JavaType converterJavaType = SpringJavaType.getConverterType(targetType, JavaType.STRING_OBJECT);
			String targetTypeName = StringUtils.uncapitalize(targetType.getSimpleTypeName());
			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine("return new " + converterJavaType.getNameIncludingTypeParameters() + "() {");
			bodyBuilder.indent();
			bodyBuilder.appendFormalLine("@Override");
			bodyBuilder.appendFormalLine("public String convert(" + targetType.getSimpleTypeName() + " " + targetTypeName + ") {");
			bodyBuilder.indent();
			bodyBuilder.appendFormalLine("return " + base64Name + ".encodeBase64URLSafeString(" + targetTypeName + "." + jsonMethodNames.get(CustomDataJsonTags.TO_JSON_METHOD).getSymbolName() + "().getBytes());");
			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("}");
			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("};");
			
			converterMethods.add(new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, converterJavaType, bodyBuilder).build());
		}
		return converterMethods;
	}
	
	private MethodMetadata getToStringConverterMethod(JavaType targetType, JavaSymbolName methodName, List<MethodMetadata> methods) {
		if (MemberFindingUtils.getDeclaredMethod(governorTypeDetails, methodName, new ArrayList<JavaType>()) != null) {
			return null;
		}
		final JavaType converterJavaType = SpringJavaType.getConverterType(targetType, JavaType.STRING_OBJECT);
		String targetTypeName = StringUtils.uncapitalize(targetType.getSimpleTypeName());
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();	
		bodyBuilder.appendFormalLine("return new " + converterJavaType.getNameIncludingTypeParameters() + "() {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("@Override");
		bodyBuilder.appendFormalLine("public String convert(" + targetType.getSimpleTypeName() + " " + targetTypeName + ") {");
		StringBuilder sb = new StringBuilder("return new StringBuilder()");
		for (int i=0; i < methods.size(); i++) {
			if (i > 0) {
				sb.append(".append(\" \")");
			}
			sb.append(".append(").append(targetTypeName).append(".").append(methods.get(i).getMethodName().getSymbolName()).append("())");
		}
		sb.append(".toString();");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(sb.toString()); 
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("};");

		return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, converterJavaType, bodyBuilder).build();
	}
	
	/**
	 * Returns the "string to type" converter method to be generated, if any
	 * 
	 * @param targetType the type being converted into (required)
	 * @param methodName the name of the method to generate if necessary (required)
	 * @param idType the ID type of the given target type (required)
	 * @return <code>null</code> if none is to be generated
	 */
	private MethodMetadata getStringToTypeConverterMethod(final JavaType targetType, final JavaSymbolName methodName, final JavaType idType) {
		Assert.notNull(methodName, "Method name is required");
		if (MemberFindingUtils.getDeclaredMethod(governorTypeDetails, methodName, new ArrayList<JavaType>()) != null) {
			return null;
		}
		Assert.notNull(targetType, "Target type is required for method = " + methodName);
		Assert.notNull(idType, "ID type is required for " + targetType);
		final JavaType converterJavaType = SpringJavaType.getConverterType(JavaType.STRING_OBJECT, targetType);
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();	
		bodyBuilder.appendFormalLine("return new " + converterJavaType.getNameIncludingTypeParameters() + "() {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("@Override");
		bodyBuilder.appendFormalLine("public " + targetType.getFullyQualifiedTypeName() + " convert(String id) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return getObject().convert(getObject().convert(id, " + idType.getSimpleTypeName() + ".class), " + targetType.getSimpleTypeName() + ".class);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("};");
		return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, converterJavaType, bodyBuilder).build();
	}
	
	private MethodMetadata getToTypeConverterMethod(JavaType targetType, JavaSymbolName methodName, MemberTypeAdditions findMethod, JavaType idType) {
		if (findMethod == null || MemberFindingUtils.getDeclaredMethod(governorTypeDetails, methodName, new ArrayList<JavaType>()) != null) {
			return null;
		}
		final JavaType converterJavaType = SpringJavaType.getConverterType(idType, targetType);
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();	
		bodyBuilder.appendFormalLine("return new " + converterJavaType.getNameIncludingTypeParameters() + "() {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("@Override");
		bodyBuilder.appendFormalLine("public " + targetType.getFullyQualifiedTypeName() + " convert(" + idType + " id) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return " + findMethod.getMethodCall() + ";");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("};");
		findMethod.copyAdditionsTo(builder, governorTypeDetails);
		return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, converterJavaType, bodyBuilder).build();
	}
	
	private MethodMetadataBuilder getInstallMethodBuilder() {
		JavaSymbolName methodName = new JavaSymbolName("installLabelConverters");
		List<AnnotatedJavaType> parameters = new ArrayList<AnnotatedJavaType>();
		parameters.add(new AnnotatedJavaType(FORMATTER_REGISTRY));
		
		MethodMetadata method = getGovernorMethod(methodName, parameters);
		if (getGovernorMethod(methodName, parameters) != null) {
			return new MethodMetadataBuilder(method);
		}
		
		List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
		parameterNames.add(new JavaSymbolName("registry"));
		return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, parameters, parameterNames, new InvocableMemberBodyBuilder());
	}

	private MethodMetadataBuilder getAfterPropertiesSetMethod(MethodMetadata installConvertersMethod) {
		JavaSymbolName methodName = new JavaSymbolName("afterPropertiesSet");
		
		List<AnnotatedJavaType> parameters = Collections.emptyList();
		List<JavaSymbolName> parameterNames = Collections.emptyList();

		MethodMetadata method = getGovernorMethod(methodName, parameters);
		if (method != null) {
			return new MethodMetadataBuilder(method);
		}

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("super.afterPropertiesSet();");
		bodyBuilder.appendFormalLine(installConvertersMethod.getMethodName().getSymbolName() + "(getObject());");

		return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, parameters, parameterNames, bodyBuilder);
	}

	private MethodMetadata getGovernorMethod(JavaSymbolName methodName, List<AnnotatedJavaType> parameters) {
		return MemberFindingUtils.getDeclaredMethod(governorTypeDetails, methodName, AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameters));
	}
}