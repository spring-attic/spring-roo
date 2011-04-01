package org.springframework.roo.addon.web.mvc.controller.converter;

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
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.itd.ItdSourceFileComposer;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
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

	public ConversionServiceMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, Map<JavaType, List<MethodMetadata>> domainJavaTypes, Map<JavaType, Map<Object, JavaSymbolName>> compositePkTypes) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.notNull(domainJavaTypes, "List of domain types required");
		Assert.notNull(compositePkTypes, "List of PK types required");
		
		if (!isValid()) {
			return;
		}

		MethodMetadataBuilder installMethodBuilder = getInstallMethodBuilder();
		//loading the keyset of the domain type map into a TreeSet to create a consistent ordering of the generated methods across shell restarts
		for (JavaType type: new TreeSet<JavaType>(domainJavaTypes.keySet())) {
			JavaSymbolName converterMethodName = new JavaSymbolName("get" + type.getSimpleTypeName() + "Converter");
			builder.addMethod(getConverterMethod(type, domainJavaTypes.get(type), converterMethodName));
			installMethodBuilder.getBodyBuilder().appendFormalLine("registry.addConverter(" + converterMethodName + "());");
		}
		for (JavaType type: compositePkTypes.keySet()) {
			for (MethodMetadata method: getCompositePkConverters(type, compositePkTypes.get(type))) {
				builder.addMethod(method);
				installMethodBuilder.getBodyBuilder().appendFormalLine("registry.addConverter(" + method.getMethodName().getSymbolName() + "());");
			}
		}
		MethodMetadata installMethod = installMethodBuilder.build();
		builder.addMethod(installMethod);
		builder.addMethod(getAfterPropertiesSetMethod(installMethod));
		
		itdTypeDetails = builder.build();
		
		new ItdSourceFileComposer(itdTypeDetails);
	}
	
	private List<MethodMetadata> getCompositePkConverters(JavaType type, Map<Object, JavaSymbolName> jsonMethodNames) {
		List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
		
		JavaType base64 = new JavaType("org.apache.commons.codec.binary.Base64");
		String base64Name = base64.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		String typeName = type.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
				
		JavaSymbolName stringToPkTypeMethod = new JavaSymbolName("getStringTo" + type.getSimpleTypeName() + "Converter");
		if (getGovernorMethod(stringToPkTypeMethod, new ArrayList<AnnotatedJavaType>()) == null) {
			List<JavaType> parameters = new ArrayList<JavaType>();
			parameters.add(JavaType.STRING_OBJECT);
			parameters.add(type);
			JavaType converterJavaType = new JavaType("org.springframework.core.convert.converter.Converter", 0, DataType.TYPE, null, parameters);
			String converterTypeName = converterJavaType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
			
			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine("return new " + converterTypeName + "() {");
			bodyBuilder.indent();
			bodyBuilder.appendFormalLine("public " + typeName + " convert(String encodedJson) {");
			bodyBuilder.indent();
			bodyBuilder.appendFormalLine("return " + typeName + "." + jsonMethodNames.get(CustomDataJsonTags.FROM_JSON_METHOD).getSymbolName() + "(new String(" + base64Name + ".decodeBase64(encodedJson)));");
			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("}");
			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("};");
			
			methods.add(new MethodMetadataBuilder(getId(), 0, stringToPkTypeMethod, converterJavaType, bodyBuilder).build());
		}
		JavaSymbolName pkTypeToStringMethod = new JavaSymbolName("get" + type.getSimpleTypeName() + "ToStringConverter");
		if (getGovernorMethod(pkTypeToStringMethod, new ArrayList<AnnotatedJavaType>()) == null) {
			List<JavaType> parameters = new ArrayList<JavaType>();
			parameters.add(type);
			parameters.add(JavaType.STRING_OBJECT);
			JavaType converterJavaType = new JavaType("org.springframework.core.convert.converter.Converter", 0, DataType.TYPE, null, parameters);
			String converterTypeName = converterJavaType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());

			InvocableMemberBodyBuilder bodyBuilder2 = new InvocableMemberBodyBuilder();
			bodyBuilder2.appendFormalLine("return new " + converterTypeName + "() {");
			bodyBuilder2.indent();
			bodyBuilder2.appendFormalLine("public String convert(" + typeName + " " + StringUtils.uncapitalize(type.getSimpleTypeName()) + ") {");
			bodyBuilder2.indent();
			bodyBuilder2.appendFormalLine("return " + base64Name + ".encodeBase64URLSafeString(" + StringUtils.uncapitalize(type.getSimpleTypeName()) + "." + jsonMethodNames.get(CustomDataJsonTags.TO_JSON_METHOD).getSymbolName() + "().getBytes());");
			bodyBuilder2.indentRemove();
			bodyBuilder2.appendFormalLine("}");
			bodyBuilder2.indentRemove();
			bodyBuilder2.appendFormalLine("};");
			
			methods.add(new MethodMetadataBuilder(getId(), 0, pkTypeToStringMethod, converterJavaType, bodyBuilder2).build());
		}
		return methods;
	}
	
	private MethodMetadata getConverterMethod(JavaType type, List<MethodMetadata> methods, JavaSymbolName methodName) {
		if (getGovernorMethod(methodName, new ArrayList<AnnotatedJavaType>()) != null) {
			return null;
		}
		List<JavaType> parameters = new ArrayList<JavaType>();
		parameters.add(type);
		parameters.add(JavaType.STRING_OBJECT);
		
		JavaType converterJavaType = new JavaType("org.springframework.core.convert.converter.Converter", 0, DataType.TYPE, null, parameters);
		
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return new " + converterJavaType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "() {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("public String convert(" + type.getSimpleTypeName() + " " + type.getSimpleTypeName().toLowerCase() + ") {");
		bodyBuilder.indent();
		
		StringBuilder sb = new StringBuilder("return new StringBuilder()");
		for (int i=0; i < methods.size(); i++) {
			if (i > 0) {
				sb.append(".append(\" \")");
			}
			sb.append(".append(" + type.getSimpleTypeName().toLowerCase() + "." + methods.get(i).getMethodName().getSymbolName() + "()");
			
//			if (domainEnumTypes.contains(methods.get(i).getReturnType())) {
//				sb.append(".name()");
//			}
			sb.append(")");
		}
		sb.append(".toString();");
		
		bodyBuilder.appendFormalLine(sb.toString()); 
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("};");
		
		return (new MethodMetadataBuilder(getId(), 0, methodName, converterJavaType, bodyBuilder)).build();
	}
	
	private MethodMetadataBuilder getInstallMethodBuilder() {
		JavaSymbolName methodName = new JavaSymbolName("installLabelConverters");
		List<AnnotatedJavaType> parameters = new ArrayList<AnnotatedJavaType>();
		parameters.add(new AnnotatedJavaType(new JavaType("org.springframework.format.FormatterRegistry"), null));
		
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