package org.springframework.roo.addon.web.mvc.controller;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

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
 * @since 1.1.1
 */
public class ConversionServiceMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

	public ConversionServiceMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, LinkedHashSet<DomainJavaType> domainJavaTypes) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		if (!isValid()) {
			return;
		}

		MethodMetadataBuilder installConvertersMethodBuilder = getInstallLabelConvertersMethodBuilder();
		for (DomainJavaType domainJavaType : getLabelConverterTypes(domainJavaTypes)) {
			String converterMethodName = "get" + domainJavaType.getSimpleTypeName() + "Converter";
			if (getMethod(converterMethodName, new ArrayList<AnnotatedJavaType>()) == null) {
				MethodMetadata converterMethod = getConverterMethod(domainJavaType, converterMethodName);
				builder.addMethod(converterMethod);
			}
			String line = "registry.addConverter(" + converterMethodName + "());";
			installConvertersMethodBuilder.getBodyBuilder().appendFormalLine(line);
		}		
		
		MethodMetadata installConvertersMethod = installConvertersMethodBuilder.build();
		builder.addMethod(installConvertersMethod);
		builder.addMethod(getAfterPropertiesSetMethod(installConvertersMethod));

		itdTypeDetails = builder.build();
		
		new ItdSourceFileComposer(itdTypeDetails);
	}

	/* Private class methods */
	
	LinkedHashSet<DomainJavaType> getLabelConverterTypes(LinkedHashSet<DomainJavaType> domainJavaTypes) {
		LinkedHashSet<DomainJavaType> allTypes = new LinkedHashSet<DomainJavaType>(domainJavaTypes);
		for (DomainJavaType t : domainJavaTypes) {
			allTypes.addAll(t.getRelatedDomainTypes());
		}
		LinkedHashSet<DomainJavaType> labelConverterTypes = new LinkedHashSet<DomainJavaType>(domainJavaTypes);
		for (DomainJavaType t : allTypes) {
			if (t.getBeanInfoMetadata() != null) {
				labelConverterTypes.add(t);
			}
		}
		return labelConverterTypes;
	}
	
	MethodMetadata getConverterMethod(DomainJavaType domainType, String converterMethodName) {
		List<JavaType> params = new ArrayList<JavaType>();
		params.add(domainType.getJavaType());
		params.add(JavaType.STRING_OBJECT);
		JavaType converterJavaType = new JavaType("org.springframework.core.convert.converter.Converter", 0, DataType.TYPE, null, params);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return new " + converterJavaType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "() {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("public String convert(" + domainType.getSimpleTypeName() + " " + "source" + ") {");
		bodyBuilder.indent();
		
		StringBuilder sb = new StringBuilder("return new StringBuilder()");
		List<MethodMetadata> labelMethods = domainType.getMethodsForLabel();
		for (int i=0; i < labelMethods.size(); i++) {
			if (i > 0) {
				sb.append(".append(\" \")");
			}
			sb.append(".append(source." + labelMethods.get(i).getMethodName().getSymbolName() + "()");
			if (labelMethods.get(i).getClass().isEnum()) {
				sb.append(".name()");
			}
			sb.append(")");
		}
		sb.append(".toString();");
		
		bodyBuilder.appendFormalLine(sb.toString()); 
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("};");
		
		return (new MethodMetadataBuilder(getId(), 0, new JavaSymbolName(converterMethodName), converterJavaType, bodyBuilder)).build();
	}
	
	MethodMetadataBuilder getInstallLabelConvertersMethodBuilder() {
		JavaSymbolName methodName = new JavaSymbolName("installLabelConverters");
		List<AnnotatedJavaType> parameters = new ArrayList<AnnotatedJavaType>();
		parameters.add(new AnnotatedJavaType(new JavaType("org.springframework.format.FormatterRegistry"), null));
		if (getMethod(methodName.getSymbolName(), parameters) != null) {
			throw new IllegalStateException("Did not expect to find installLabelConverters method");
		}
		List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
		parameterNames.add(new JavaSymbolName("registry"));
		return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, parameters, parameterNames, new InvocableMemberBodyBuilder());
	}

	MethodMetadata getAfterPropertiesSetMethod(MethodMetadata installConvertersMethod) {
		JavaSymbolName methodName = new JavaSymbolName("afterPropertiesSet");
		
		List<AnnotatedJavaType> parameters = Collections.emptyList();
		List<JavaSymbolName> parameterNames = Collections.emptyList();

		if (getMethod(methodName.getSymbolName(), parameters) != null) {
			return null;
		}

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("super.afterPropertiesSet();");
		bodyBuilder.appendFormalLine(installConvertersMethod.getMethodName().getSymbolName() + "(getObject());");

		return (new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, parameters, parameterNames, bodyBuilder)).build();
	}

	MethodMetadata getMethod(String methodName, List<AnnotatedJavaType> parameters) {
		return MemberFindingUtils.getDeclaredMethod(governorTypeDetails, new JavaSymbolName(methodName), AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameters));
	}

}