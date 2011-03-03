package org.springframework.roo.addon.web.mvc.controller.converter;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

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

	public ConversionServiceMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, Map<JavaType, List<MethodMetadata>> domainJavaTypes) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		
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
		MethodMetadata installMethod = installMethodBuilder.build();
		builder.addMethod(installMethod);
		builder.addMethod(getAfterPropertiesSetMethod(installMethod));
		
		itdTypeDetails = builder.build();
		
		new ItdSourceFileComposer(itdTypeDetails);
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