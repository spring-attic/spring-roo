package org.springframework.roo.addon.web.mvc.controller.converter;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.springframework.roo.addon.json.CustomDataJsonTags;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
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

	public ConversionServiceMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, Map<JavaType, List<MethodMetadata>> relevantDomainTypes, Map<JavaType, Map<Object, JavaSymbolName>> compositePrimaryKeyTypes) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.notNull(relevantDomainTypes, "List of domain types required");
		Assert.notNull(compositePrimaryKeyTypes, "List of PK types required");
		
		if (!isValid()) {
			return;
		}
		
		if (relevantDomainTypes.isEmpty() && compositePrimaryKeyTypes.isEmpty()) { 
			return;
		}

		MethodMetadataBuilder installMethodBuilder = getInstallMethodBuilder();
		//loading the keyset of the domain type map into a TreeSet to create a consistent ordering of the generated methods across shell restarts
		for (JavaType type: new TreeSet<JavaType>(relevantDomainTypes.keySet())) {
			JavaType converterName = new JavaType(type.getSimpleTypeName() + "Converter");
			ClassOrInterfaceTypeDetails converter = getConverter(type, converterName, relevantDomainTypes.get(type));
			if (converter != null) {
				builder.addInnerType(converter);
				installMethodBuilder.getBodyBuilder().appendFormalLine("registry.addConverter(new " + converterName + "());");
			}
		}
		for (JavaType type: compositePrimaryKeyTypes.keySet()) {
			for (ClassOrInterfaceTypeDetails converter: getCompositePkConverters(type, compositePrimaryKeyTypes.get(type))) {
				builder.addInnerType(converter);
				installMethodBuilder.getBodyBuilder().appendFormalLine("registry.addConverter(new " + converter.getName().getSimpleTypeName() + "());");
			}
		}
		MethodMetadata installMethod = installMethodBuilder.build();
		builder.addMethod(installMethod);
		builder.addMethod(getAfterPropertiesSetMethod(installMethod));
		
		itdTypeDetails = builder.build();
	}
	
	private List<ClassOrInterfaceTypeDetails> getCompositePkConverters(JavaType targetType, Map<Object, JavaSymbolName> jsonMethodNames) {
		List<ClassOrInterfaceTypeDetails> converters = new LinkedList<ClassOrInterfaceTypeDetails>();
		
		JavaType innerType = new JavaType("JsonTo" + targetType.getSimpleTypeName() + "Converter");
		JavaType base64 = new JavaType("org.apache.commons.codec.binary.Base64");
		String base64Name = base64.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		String typeName = targetType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		
		if (MemberFindingUtils.getDeclaredInnerType(governorTypeDetails, innerType) == null) {
			List<JavaType> parameters = new ArrayList<JavaType>();
			parameters.add(JavaType.STRING_OBJECT);
			parameters.add(targetType);
			JavaType converterJavaType = new JavaType("org.springframework.core.convert.converter.Converter", 0, DataType.TYPE, null, parameters);
			
			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine("return " + typeName + "." + jsonMethodNames.get(CustomDataJsonTags.FROM_JSON_METHOD).getSymbolName() + "(new String(" + base64Name + ".decodeBase64(encodedJson)));");
			
			MethodMetadataBuilder convert = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, new JavaSymbolName("convert"), targetType, bodyBuilder);
			convert.addParameterType(new AnnotatedJavaType(JavaType.STRING_OBJECT, null));
			convert.addParameterName(new JavaSymbolName("encodedJson"));
			ClassOrInterfaceTypeDetailsBuilder converterBuilder = new ClassOrInterfaceTypeDetailsBuilder(getId(), Modifier.STATIC, innerType, PhysicalTypeCategory.CLASS);
			converterBuilder.addImplementsType(converterJavaType);
			converterBuilder.addMethod(convert);
			converters.add(converterBuilder.build());
		}
		
		innerType = new JavaType(targetType.getSimpleTypeName() + "ToJsonConverter");
		if (MemberFindingUtils.getDeclaredInnerType(governorTypeDetails, innerType) == null) {
			List<JavaType> parameters = new ArrayList<JavaType>();
			parameters.add(targetType);
			parameters.add(JavaType.STRING_OBJECT);
			JavaType converterJavaType = new JavaType("org.springframework.core.convert.converter.Converter", 0, DataType.TYPE, null, parameters);
			
			InvocableMemberBodyBuilder bodyBuilder2 = new InvocableMemberBodyBuilder();
			bodyBuilder2.appendFormalLine("return " + base64Name + ".encodeBase64URLSafeString(" + StringUtils.uncapitalize(targetType.getSimpleTypeName()) + "." + jsonMethodNames.get(CustomDataJsonTags.TO_JSON_METHOD).getSymbolName() + "().getBytes());");
			
			MethodMetadataBuilder convert = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, new JavaSymbolName("convert"), JavaType.STRING_OBJECT, bodyBuilder2);
			convert.addParameterType(new AnnotatedJavaType(targetType, null));
			convert.addParameterName(new JavaSymbolName(StringUtils.uncapitalize(targetType.getSimpleTypeName())));
			ClassOrInterfaceTypeDetailsBuilder converterBuilder = new ClassOrInterfaceTypeDetailsBuilder(getId(), Modifier.STATIC, innerType, PhysicalTypeCategory.CLASS);
			converterBuilder.addImplementsType(converterJavaType);
			converterBuilder.addMethod(convert);
			converters.add(converterBuilder.build());
		}
		return converters;
	}
	
	private ClassOrInterfaceTypeDetails getConverter(JavaType targetType, JavaType innerType, List<MethodMetadata> methods) {
		if (MemberFindingUtils.getDeclaredInnerType(governorTypeDetails, innerType) != null) {
			return null;
		}
		List<JavaType> parameters = new ArrayList<JavaType>();
		parameters.add(targetType);
		parameters.add(JavaType.STRING_OBJECT);
		
		JavaType converterJavaType = new JavaType("org.springframework.core.convert.converter.Converter", 0, DataType.TYPE, null, parameters);
		String targetTypeName = StringUtils.uncapitalize(targetType.getSimpleTypeName());
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.indent();
		
		StringBuilder sb = new StringBuilder("return new StringBuilder()");
		for (int i=0; i < methods.size(); i++) {
			if (i > 0) {
				sb.append(".append(\" \")");
			}
			sb.append(".append(").append(targetTypeName).append(".").append(methods.get(i).getMethodName().getSymbolName()).append("())");
		}
		sb.append(".toString();");
		
		bodyBuilder.appendFormalLine(sb.toString()); 
		bodyBuilder.indentRemove();

		MethodMetadataBuilder convert = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, new JavaSymbolName("convert"), JavaType.STRING_OBJECT, bodyBuilder);
		convert.addParameterType(new AnnotatedJavaType(targetType, null));
		convert.addParameterName(new JavaSymbolName(targetTypeName));
		ClassOrInterfaceTypeDetailsBuilder classBuilder = new ClassOrInterfaceTypeDetailsBuilder(getId(), Modifier.STATIC, innerType, PhysicalTypeCategory.CLASS);
		classBuilder.addImplementsType(converterJavaType);
		classBuilder.addMethod(convert);
		return classBuilder.build();
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