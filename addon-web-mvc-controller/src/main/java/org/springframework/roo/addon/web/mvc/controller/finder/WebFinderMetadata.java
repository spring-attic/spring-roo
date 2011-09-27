package org.springframework.roo.addon.web.mvc.controller.finder;

import static org.springframework.roo.model.SpringJavaType.DATE_TIME_FORMAT;
import static org.springframework.roo.model.SpringJavaType.MODEL;
import static org.springframework.roo.model.SpringJavaType.REQUEST_MAPPING;
import static org.springframework.roo.model.SpringJavaType.REQUEST_METHOD;
import static org.springframework.roo.model.SpringJavaType.REQUEST_PARAM;

import java.beans.Introspector;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.springframework.roo.addon.web.mvc.controller.RooWebScaffold;
import org.springframework.roo.addon.web.mvc.controller.details.FinderMetadataDetails;
import org.springframework.roo.addon.web.mvc.controller.details.JavaTypeMetadataDetails;
import org.springframework.roo.addon.web.mvc.controller.details.JavaTypePersistenceMetadataDetails;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldAnnotationValues;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.BooleanAttributeValue;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for finder functionality provided via {@link RooWebScaffold}.
 * 
 * @author Stefan Schmidt
 * @since 1.1.3
 */
public class WebFinderMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	
	// Constants
	private static final String PROVIDES_TYPE_STRING = WebFinderMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);

	// Fields
	private JavaType formBackingType;
	private JavaTypeMetadataDetails javaTypeMetadataHolder;
	private Map<JavaType, JavaTypeMetadataDetails> specialDomainTypes;
	private MemberDetails memberDetails;
	private String controllerPath;
	private WebScaffoldAnnotationValues annotationValues;

	/**
	 * Constructor
	 *
	 * @param identifier
	 * @param aspectName
	 * @param governorPhysicalTypeMetadata
	 * @param annotationValues
	 * @param memberDetails
	 * @param specialDomainTypes
	 * @param dynamicFinderMethods
	 */
	public WebFinderMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, WebScaffoldAnnotationValues annotationValues, MemberDetails memberDetails, SortedMap<JavaType, JavaTypeMetadataDetails> specialDomainTypes, Set<FinderMetadataDetails> dynamicFinderMethods) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.notNull(specialDomainTypes, "Special domain type map required");
		Assert.notNull(dynamicFinderMethods, "Dynamoic finder methods required");
		Assert.notNull(memberDetails, "Member details required");

		if (!isValid()) {
			return;
		}
		
		this.annotationValues = annotationValues;
		this.controllerPath = annotationValues.getPath();
		this.formBackingType = annotationValues.getFormBackingObject();
		this.specialDomainTypes = specialDomainTypes;
		this.memberDetails = memberDetails;
		
		if (dynamicFinderMethods.isEmpty()) {
			valid = false;
			return;
		}

		javaTypeMetadataHolder = specialDomainTypes.get(formBackingType);
		Assert.notNull(javaTypeMetadataHolder, "Metadata holder required for form backing type: " + formBackingType);
		
		for (FinderMetadataDetails finder : dynamicFinderMethods) {
			builder.addMethod(getFinderFormMethod(finder));
			builder.addMethod(getFinderMethod(finder));
		}

		itdTypeDetails = builder.build();
	}

	public WebScaffoldAnnotationValues getAnnotationValues() {
		return annotationValues;
	}

	private MethodMetadata getFinderFormMethod(FinderMetadataDetails finder) {
		Assert.notNull(finder, "Method metadata required for finder");
		JavaSymbolName finderFormMethodName = new JavaSymbolName(finder.getFinderMethodMetadata().getMethodName().getSymbolName() + "Form");

		List<JavaType> parameterTypes = new ArrayList<JavaType>();
		List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
		List<JavaType> types = AnnotatedJavaType.convertFromAnnotatedJavaTypes(finder.getFinderMethodMetadata().getParameterTypes());

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

		boolean needmodel = false;
		for (JavaType javaType : types) {
			JavaTypeMetadataDetails typeMd = specialDomainTypes.get(javaType);
			JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataHolder = null;
			if (javaType.isCommonCollectionType()) {
				JavaTypeMetadataDetails paramTypeMd = specialDomainTypes.get(javaType.getParameters().get(0));
				if (paramTypeMd != null && paramTypeMd.isApplicationType()) {
					javaTypePersistenceMetadataHolder = paramTypeMd.getPersistenceDetails();
				}
			} else if (typeMd != null && typeMd.isEnumType()) {
				bodyBuilder.appendFormalLine("uiModel.addAttribute(\"" + typeMd.getPlural().toLowerCase() + "\", java.util.Arrays.asList(" + javaType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".class.getEnumConstants()));");
			} else if (typeMd != null && typeMd.isApplicationType()) {
				javaTypePersistenceMetadataHolder = typeMd.getPersistenceDetails();
			}
			if (typeMd != null && javaTypePersistenceMetadataHolder != null && javaTypePersistenceMetadataHolder.getFindAllMethod() != null) {
				bodyBuilder.appendFormalLine("uiModel.addAttribute(\"" + typeMd.getPlural().toLowerCase() + "\", " + javaTypePersistenceMetadataHolder.getFindAllMethod().getMethodCall() + ");");
			}
			needmodel = true;
		}
		if (types.contains(new JavaType(Date.class.getName())) || types.contains(new JavaType(Calendar.class.getName()))) {
			bodyBuilder.appendFormalLine("addDateTimeFormatPatterns(uiModel);");
		}
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/" + finder.getFinderMethodMetadata().getMethodName().getSymbolName() + "\";");

		if (needmodel) {
			parameterTypes.add(MODEL);
			parameterNames.add(new JavaSymbolName("uiModel"));
		}
		
		if (methodExists(finderFormMethodName, parameterTypes)) {
			return null;
		}
		
		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		List<StringAttributeValue> arrayValues = new ArrayList<StringAttributeValue>();
		arrayValues.add(new StringAttributeValue(new JavaSymbolName("value"), "find=" + finder.getFinderMethodMetadata().getMethodName().getSymbolName().replaceFirst("find" + javaTypeMetadataHolder.getPlural(), "")));
		arrayValues.add(new StringAttributeValue(new JavaSymbolName("value"), "form"));
		requestMappingAttributes.add(new ArrayAttributeValue<StringAttributeValue>(new JavaSymbolName("params"), arrayValues));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(REQUEST_METHOD, new JavaSymbolName("GET"))));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(REQUEST_MAPPING, requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, finderFormMethodName, JavaType.STRING, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getFinderMethod(FinderMetadataDetails finderMetadataDetails) {
		Assert.notNull(finderMetadataDetails, "Method metadata required for finder");
		JavaSymbolName finderMethodName = new JavaSymbolName(finderMetadataDetails.getFinderMethodMetadata().getMethodName().getSymbolName());

		List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
		List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		StringBuilder methodParams = new StringBuilder();

		boolean dateFieldPresent = false;
		for (FieldMetadata field: finderMetadataDetails.getFinderMethodParamFields()) {
			JavaSymbolName fieldName = field.getFieldName();
			List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
			List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
			attributes.add(new StringAttributeValue(new JavaSymbolName("value"), uncapitalize(fieldName.getSymbolName())));
			if (field.getFieldType().equals(JavaType.BOOLEAN_PRIMITIVE) || field.getFieldType().equals(JavaType.BOOLEAN_OBJECT)) {
				attributes.add(new BooleanAttributeValue(new JavaSymbolName("required"), false));
			}
			AnnotationMetadataBuilder requestParamAnnotation = new AnnotationMetadataBuilder(REQUEST_PARAM, attributes);
			annotations.add(requestParamAnnotation.build());
			if (field.getFieldType().equals(new JavaType(Date.class.getName())) || field.getFieldType().equals(new JavaType(Calendar.class.getName()))) {
				dateFieldPresent = true;
				AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), DATE_TIME_FORMAT);
				if (annotation != null) {
					DATE_TIME_FORMAT.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
					annotations.add(annotation);
				}
			}
			parameterNames.add(fieldName);
			parameterTypes.add(new AnnotatedJavaType(field.getFieldType(), annotations));

			if (field.getFieldType().equals(JavaType.BOOLEAN_OBJECT)) {
				methodParams.append(fieldName + " == null ? Boolean.FALSE : " + fieldName + ", ");
			} else {
				methodParams.append(fieldName + ", ");
			}
		}

		if (methodParams.length() > 0) {
			methodParams.delete(methodParams.length() - 2, methodParams.length());
		}

		parameterTypes.add(new AnnotatedJavaType(MODEL));
		if (methodExists(finderMethodName, AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes))) {
			return null;
		}
		
		List<JavaSymbolName> newParamNames = new ArrayList<JavaSymbolName>();
		newParamNames.addAll(parameterNames);
		newParamNames.add(new JavaSymbolName("uiModel"));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("params"), "find=" + finderMetadataDetails.getFinderMethodMetadata().getMethodName().getSymbolName().replaceFirst("find" + javaTypeMetadataHolder.getPlural(), "")));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(REQUEST_METHOD, new JavaSymbolName("GET"))));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(REQUEST_MAPPING, requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);
		
		bodyBuilder.appendFormalLine("uiModel.addAttribute(\"" + javaTypeMetadataHolder.getPlural().toLowerCase() + "\", " + formBackingType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "." + finderMetadataDetails.getFinderMethodMetadata().getMethodName().getSymbolName() + "(" + methodParams.toString() + ").getResultList());");
		if (dateFieldPresent) {
			bodyBuilder.appendFormalLine("addDateTimeFormatPatterns(uiModel);");
		}
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/list\";");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, finderMethodName, JavaType.STRING, parameterTypes, newParamNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private boolean methodExists(JavaSymbolName methodName, List<JavaType> parameterTypes) {
		return memberDetails.getMethod(methodName, parameterTypes, getId()) != null;
	}
	
	private String uncapitalize(String term) {
		// [ROO-1790] this is needed to adhere to the JavaBean naming conventions (see JavaBean spec section 8.8)
		return Introspector.decapitalize(StringUtils.capitalize(term));
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

	public static String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}

	public static String createIdentifier(JavaType javaType, Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static JavaType getJavaType(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static Path getPath(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
}
