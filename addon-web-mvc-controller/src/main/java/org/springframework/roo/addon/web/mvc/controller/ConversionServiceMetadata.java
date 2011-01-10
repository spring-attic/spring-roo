package org.springframework.roo.addon.web.mvc.controller;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.roo.addon.beaninfo.BeanInfoUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
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
	
	private Set<JavaType> domainTypes;
	private Set<JavaType> domainEnumTypes;

	ConversionServiceMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		// For testing
	}

	public ConversionServiceMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, Set<JavaTypeMetadataHolder> domainJavaTypes) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		domainTypes = new HashSet<JavaType>();
		domainEnumTypes = new HashSet<JavaType>();
		if (!isValid()) {
			return;
		}
		for (JavaTypeMetadataHolder domainJavaType: domainJavaTypes) {
			domainTypes.add(domainJavaType.getType());
			if (domainJavaType.isEnumType()) {
				domainEnumTypes.add(domainJavaType.getType());
			}
		}

		MethodMetadataBuilder installMethodBuilder = getInstallMethodBuilder();
		for (JavaTypeMetadataHolder domainJavaType : domainJavaTypes) {
			String converterMethodName = "get" + domainJavaType.getType().getSimpleTypeName() + "Converter";
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

	private MethodMetadata getConverterMethod(JavaTypeMetadataHolder rooJavaType, String converterMethodName) {
		List<JavaType> params = new ArrayList<JavaType>();
		params.add(rooJavaType.getType());
		params.add(JavaType.STRING_OBJECT);
		JavaType converterJavaType = new JavaType("org.springframework.core.convert.converter.Converter", 0, DataType.TYPE, null, params);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return new " + converterJavaType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "() {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("public String convert(" + rooJavaType.getType().getSimpleTypeName() + " source) {");
		bodyBuilder.indent();
		
		StringBuilder sb = new StringBuilder("return new StringBuilder()");
		List<MethodMetadata> labelMethods = getLabelMethodsForDomainType(rooJavaType);
		for (int i=0; i < labelMethods.size(); i++) {
			if (i > 0) {
				sb.append(".append(\" \")");
			}
			sb.append(".append(source." + labelMethods.get(i).getMethodName().getSymbolName() + "()");
			
			if (domainEnumTypes.contains(labelMethods.get(i).getReturnType())) {
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
	
	private MethodMetadataBuilder getInstallMethodBuilder() {
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

	private MethodMetadata getAfterPropertiesSetMethod(MethodMetadata installConvertersMethod) {
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

	private MethodMetadata getGovernorMethod(String methodName, List<AnnotatedJavaType> parameters) {
		return MemberFindingUtils.getDeclaredMethod(governorTypeDetails, new JavaSymbolName(methodName), AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameters));
	}
	
	/**
	 * Selects up to 3 methods that can be used to build up a label that represents the 
	 * domain object. Only methods returning non-domain types, non-collections, and non-arrays
	 * are considered. Accessors for the id and the version fields are also excluded.
	 * @param memberDetailsScanner 
	 * 
	 * @return a list containing between 1 and 3 methods. 
	 *		If no methods could be selected the toString() method is added.
	 */
	private List<MethodMetadata> getLabelMethodsForDomainType(JavaTypeMetadataHolder javaTypeMetadataHolder) {
		int fieldCount = 0;
		List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
		for (MethodMetadata accessor : javaTypeMetadataHolder.getBeanInfoMetadata().getPublicAccessors()) {
			if (accessor.getMethodName().equals(javaTypeMetadataHolder.getEntityMetadata().getIdentifierAccessor().getMethodName())) {
				continue;
			}
			MethodMetadata versionAccessor = javaTypeMetadataHolder.getEntityMetadata().getVersionAccessor();
			if (versionAccessor != null && accessor.getMethodName().equals(versionAccessor.getMethodName())) {
				continue;
			}
			FieldMetadata field = javaTypeMetadataHolder.getBeanInfoMetadata().getFieldForPropertyName(BeanInfoUtils.getPropertyNameForJavaBeanMethod(accessor));
			if (field != null // Should not happen
					&& !field.getFieldType().isCommonCollectionType() && !field.getFieldType().isArray() // Exclude collections and arrays
					&& !domainTypes.contains(accessor.getReturnType()) // Exclude references to other domain objects as they are too verbose
					&& !field.getFieldType().equals(JavaType.BOOLEAN_PRIMITIVE) 
					&& !field.getFieldType().equals(JavaType.BOOLEAN_OBJECT) /* Exclude boolean values as they would not be meaningful in this presentation */ ) {

				methods.add(accessor);
				fieldCount++;
				if (fieldCount == 3) {
					break;
				}
			}
		}
		if (methods.size() == 0) {
			methods.add(new MethodMetadataBuilder(javaTypeMetadataHolder.getBeanInfoMetadata().getId(), Modifier.PUBLIC, new JavaSymbolName("toString"), 
					new JavaType("java.lang.String"), null, null, new InvocableMemberBodyBuilder()).build());
		}
		return Collections.unmodifiableList(methods);
	}
}