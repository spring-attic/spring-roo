package org.springframework.roo.addon.web.mvc.controller.converter;

import static org.springframework.roo.model.SpringJavaType.CONFIGURABLE;
import static org.springframework.roo.model.SpringJavaType.FORMATTER_REGISTRY;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.roo.addon.json.CustomDataJsonTags;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
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

	// Constants
	private static final JavaType BASE_64 = new JavaType("org.apache.commons.codec.binary.Base64");

	/**
	 * Constructor for testing
	 *
	 * @param identifier
	 * @param aspectName
	 * @param governorPhysicalTypeMetadata
	 */
	ConversionServiceMetadata(final String identifier, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
	}

	/**
	 * Production constructor
	 *
	 * @param identifier
	 * @param aspectName
	 * @param governorPhysicalTypeMetadata
	 * @param findMethods
	 * @param idTypes the ID types of the domain types for which to generate converters (required); must be one for each domain type
	 * @param relevantDomainTypes the types for which to generate converters (required)
	 * @param compositePrimaryKeyTypes (required)
	 */
	public ConversionServiceMetadata(final String identifier, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final Map<JavaType, MemberTypeAdditions> findMethods, final Map<JavaType, JavaType> idTypes, final Map<JavaType, String> relevantDomainTypes, final Map<JavaType, Map<Object, JavaSymbolName>> compositePrimaryKeyTypes) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.notNull(relevantDomainTypes, "List of domain types required");
		Assert.notNull(compositePrimaryKeyTypes, "List of PK types required");
		Assert.notNull(idTypes, "List of ID types required");
		Assert.isTrue(relevantDomainTypes.size() == idTypes.size(), "Expected " + relevantDomainTypes.size() + " ID types, but was " + idTypes.size());

		if (!isValid() || (relevantDomainTypes.isEmpty() && compositePrimaryKeyTypes.isEmpty())) {
			valid = false;
			return;
		}

		builder.addAnnotation(getTypeAnnotation(CONFIGURABLE));

		final MethodMetadataBuilder installMethodBuilder = new MethodMetadataBuilder(getInstallMethod());
		final Set<String> methodNames = new HashSet<String>();

		for (final Map.Entry<JavaType, String> entry : relevantDomainTypes.entrySet()) {
			JavaType formBackingObject = entry.getKey();
			String displayNameMethod = entry.getValue();
			String simpleName = formBackingObject.getSimpleTypeName();
			while (methodNames.contains(simpleName)) {
				simpleName += "_";
			}
			methodNames.add(simpleName);
			JavaSymbolName toIdMethodName = new JavaSymbolName("get" + simpleName + "ToStringConverter");
			MethodMetadata toIdMethod = getToStringConverterMethod(formBackingObject, toIdMethodName, displayNameMethod);
			if (toIdMethod != null) {
				builder.addMethod(toIdMethod);
				installMethodBuilder.getBodyBuilder().appendFormalLine("registry.addConverter(" + toIdMethodName.getSymbolName() + "());");
			}

			JavaSymbolName toTypeMethodName = new JavaSymbolName("getIdTo" + simpleName + "Converter");
			MethodMetadata toTypeMethod = getToTypeConverterMethod(formBackingObject, toTypeMethodName, findMethods.get(formBackingObject), idTypes.get(formBackingObject));
			if (toTypeMethod != null) {
				builder.addMethod(toTypeMethod);
				installMethodBuilder.getBodyBuilder().appendFormalLine("registry.addConverter(" + toTypeMethodName.getSymbolName() + "());");
			}

			// Only allow conversion if ID type is not String already.
			if (!idTypes.get(formBackingObject).equals(JavaType.STRING)) {
				JavaSymbolName stringToTypeMethodName = new JavaSymbolName("getStringTo" + simpleName + "Converter");
				MethodMetadata stringToTypeMethod = getStringToTypeConverterMethod(formBackingObject, stringToTypeMethodName, idTypes.get(formBackingObject));
				if (stringToTypeMethod != null) {
					builder.addMethod(stringToTypeMethod);
					installMethodBuilder.getBodyBuilder().appendFormalLine("registry.addConverter(" + stringToTypeMethodName.getSymbolName() + "());");
				}
			}
		}

		for (final Entry<JavaType, Map<Object, JavaSymbolName>> entry : compositePrimaryKeyTypes.entrySet()) {
			final JavaType type = entry.getKey();
			for (final MethodMetadata converterMethod : getCompositePkConverters(type, entry.getValue())) {
				builder.addMethod(converterMethod);
				installMethodBuilder.getBodyBuilder().appendFormalLine("registry.addConverter(" + converterMethod.getMethodName().getSymbolName() + "());");
			}
		}

		MethodMetadata installMethod = installMethodBuilder.build();
		if (getGovernorMethod(installMethod.getMethodName(), AnnotatedJavaType.convertFromAnnotatedJavaTypes(installMethod.getParameterTypes())) == null) {
			builder.addMethod(installMethod);
		}
		builder.addMethod(getAfterPropertiesSetMethod(installMethod));

		itdTypeDetails = builder.build();
	}

	private List<MethodMetadata> getCompositePkConverters(final JavaType targetType, final Map<Object, JavaSymbolName> jsonMethodNames) {
		List<MethodMetadata> converterMethods = new ArrayList<MethodMetadata>();

		JavaSymbolName methodName = new JavaSymbolName("getJsonTo" + targetType.getSimpleTypeName() + "Converter");
		JavaType base64 = BASE_64;
		String base64Name = base64.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		String typeName = targetType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());

		if (MemberFindingUtils.getDeclaredMethod(governorTypeDetails, methodName, new ArrayList<JavaType>()) == null) {
			final JavaType converterJavaType = SpringJavaType.getConverterType(JavaType.STRING, targetType);
			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine("return new " + converterJavaType.getNameIncludingTypeParameters() + "() {");
			bodyBuilder.indent();
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
			final JavaType converterJavaType = SpringJavaType.getConverterType(targetType, JavaType.STRING);
			String targetTypeName = StringUtils.uncapitalize(targetType.getSimpleTypeName());
			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine("return new " + converterJavaType.getNameIncludingTypeParameters() + "() {");
			bodyBuilder.indent();
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

	private MethodMetadata getToStringConverterMethod(final JavaType targetType, final JavaSymbolName methodName, final String displayNameMethod) {
		if (getGovernorMethod(methodName) != null) {
			return null;
		}

		final JavaType converterJavaType = SpringJavaType.getConverterType(targetType, JavaType.STRING);
		String targetTypeName = StringUtils.uncapitalize(targetType.getSimpleTypeName());
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return new " + converterJavaType.getNameIncludingTypeParameters() + "() {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("public String convert(" + targetType.getSimpleTypeName() + " " + targetTypeName + ") {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return " + targetTypeName + "." + displayNameMethod + ";");
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
		final JavaType converterJavaType = SpringJavaType.getConverterType(JavaType.STRING, targetType);
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		final String idTypeName = idType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		bodyBuilder.appendFormalLine("return new " + converterJavaType.getNameIncludingTypeParameters() + "() {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("public " + targetType.getFullyQualifiedTypeName() + " convert(String id) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return getObject().convert(getObject().convert(id, " + idTypeName + ".class), " + targetType.getSimpleTypeName() + ".class);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("};");

		return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, converterJavaType, bodyBuilder).build();
	}

	private MethodMetadata getToTypeConverterMethod(final JavaType targetType, final JavaSymbolName methodName, final MemberTypeAdditions findMethod, final JavaType idType) {
		if (findMethod == null || MemberFindingUtils.getDeclaredMethod(governorTypeDetails, methodName, new ArrayList<JavaType>()) != null) {
			return null;
		}
		final JavaType converterJavaType = SpringJavaType.getConverterType(idType, targetType);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return new " + converterJavaType.getNameIncludingTypeParameters() + "() {");
		bodyBuilder.indent();
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

	private MethodMetadata getInstallMethod() {
		JavaSymbolName methodName = new JavaSymbolName("installLabelConverters");
		final JavaType parameterType = FORMATTER_REGISTRY;
		MethodMetadata method = getGovernorMethod(methodName, parameterType);
		if (method != null) {
			return method;
		}

		List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("registry"));

		return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterType), parameterNames, new InvocableMemberBodyBuilder()).build();
	}

	private MethodMetadata getAfterPropertiesSetMethod(final MethodMetadata installConvertersMethod) {
		JavaSymbolName methodName = new JavaSymbolName("afterPropertiesSet");
		final JavaType[] parameterTypes = {};
		MethodMetadata method = getGovernorMethod(methodName, parameterTypes);
		if (method != null) {
			return method;
		}

		List<JavaSymbolName> parameterNames = Collections.emptyList();

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("super.afterPropertiesSet();");
		bodyBuilder.appendFormalLine(installConvertersMethod.getMethodName().getSymbolName() + "(getObject());");

		return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder).build();
	}
}