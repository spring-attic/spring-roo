package org.springframework.roo.addon.web.mvc.controller;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Logger;

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
import org.springframework.roo.support.logging.HandlerUtils;

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

	private static final Logger logger = HandlerUtils.getLogger(ConversionServiceMetadata.class);

	ConversionServiceMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		// For testing
	}

	public ConversionServiceMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, LinkedHashSet<RooJavaType> domainJavaTypes) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		if (!isValid()) {
			return;
		}

		MethodMetadataBuilder installMethodBuilder = getInstallMethodBuilder();
		for (RooJavaType domainJavaType : getLabelConverterTypes(domainJavaTypes)) {
			String converterMethodName = "get" + domainJavaType.getSimpleTypeName() + "Converter";
			if (getGovernorMethod(converterMethodName, new ArrayList<AnnotatedJavaType>()) == null) {
				builder.addMethod(getConverterMethod(domainJavaType, converterMethodName));
			}
			installMethodBuilder.getBodyBuilder().appendFormalLine("registry.addConverter(" + converterMethodName + "());");
		}
		MethodMetadata installMethod = installMethodBuilder.build();
		builder.addMethod(installMethod);
		builder.addMethod(getAfterPropertiesSetMethod(installMethod));
		
		itdTypeDetails = builder.build();
		
		new ItdSourceFileComposer(itdTypeDetails);
	}

	/* Private class methods */
	
	LinkedHashSet<RooJavaType> getLabelConverterTypes(LinkedHashSet<RooJavaType> domainJavaTypes) {
		LinkedHashSet<RooJavaType> allTypes = new LinkedHashSet<RooJavaType>(domainJavaTypes);
		for (RooJavaType t : domainJavaTypes) {
			allTypes.addAll(t.getRelatedRooTypes());
		}
		LinkedHashSet<RooJavaType> labelConverterTypes = new LinkedHashSet<RooJavaType>(domainJavaTypes);
		for (RooJavaType t : allTypes) {
			if (t.getBeanInfoMetadata() == null) {
				logger.finer("No BeanInfoMetadata found for " + t.toString() + ". A Converter will not be created for this type.");
				continue;
			} 
			labelConverterTypes.add(t);
		}
		return labelConverterTypes;
	}
	
	MethodMetadata getConverterMethod(RooJavaType rooJavaType, String converterMethodName) {
		List<JavaType> params = new ArrayList<JavaType>();
		params.add(rooJavaType.getJavaType());
		params.add(JavaType.STRING_OBJECT);
		JavaType converterJavaType = new JavaType("org.springframework.core.convert.converter.Converter", 0, DataType.TYPE, null, params);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return new " + converterJavaType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "() {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("public String convert(" + rooJavaType.getSimpleTypeName() + " " + "source" + ") {");
		bodyBuilder.indent();
		
		StringBuilder sb = new StringBuilder("return new StringBuilder()");
		List<MethodMetadata> labelMethods = rooJavaType.getMethodsForLabel();
		for (int i=0; i < labelMethods.size(); i++) {
			if (i > 0) {
				sb.append(".append(\" \")");
			}
			sb.append(".append(source." + labelMethods.get(i).getMethodName().getSymbolName() + "()");
			RooJavaType returnType = new RooJavaType(labelMethods.get(i).getReturnType(), rooJavaType.getMetadataService());
			if (returnType.isEnumType()) {
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
	
	MethodMetadataBuilder getInstallMethodBuilder() {
		JavaSymbolName methodName = new JavaSymbolName("installLabelConverters");
		List<AnnotatedJavaType> parameters = new ArrayList<AnnotatedJavaType>();
		parameters.add(new AnnotatedJavaType(new JavaType("org.springframework.format.FormatterRegistry"), null));
		if (getGovernorMethod(methodName.getSymbolName(), parameters) != null) {
			throw new IllegalStateException("Did not expect to find installLabelConverters method in " + governorTypeDetails.getName());
		}
		List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
		parameterNames.add(new JavaSymbolName("registry"));
		return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, parameters, parameterNames, new InvocableMemberBodyBuilder());
	}

	MethodMetadata getAfterPropertiesSetMethod(MethodMetadata installConvertersMethod) {
		JavaSymbolName methodName = new JavaSymbolName("afterPropertiesSet");
		
		List<AnnotatedJavaType> parameters = Collections.emptyList();
		List<JavaSymbolName> parameterNames = Collections.emptyList();

		if (getGovernorMethod(methodName.getSymbolName(), parameters) != null) {
			return null;
		}

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("super.afterPropertiesSet();");
		bodyBuilder.appendFormalLine(installConvertersMethod.getMethodName().getSymbolName() + "(getObject());");

		return (new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, parameters, parameterNames, bodyBuilder)).build();
	}

	MethodMetadata getGovernorMethod(String methodName, List<AnnotatedJavaType> parameters) {
		return MemberFindingUtils.getDeclaredMethod(governorTypeDetails, new JavaSymbolName(methodName), AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameters));
	}

}