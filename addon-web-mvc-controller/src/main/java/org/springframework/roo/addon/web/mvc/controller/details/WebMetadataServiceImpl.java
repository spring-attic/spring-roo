package org.springframework.roo.addon.web.mvc.controller.details;

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.finder.FinderMetadata;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.addon.serializable.CustomDataSerializableTags;
import org.springframework.roo.addon.web.mvc.controller.RooWebScaffold;
import org.springframework.roo.addon.web.mvc.controller.scaffold.mvc.WebScaffoldMetadata;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Implementation of {@link WebMetadataService} to retrieve various metadata information for use by Web scaffolding add-ons.
 * 
 * @author Stefan Schmidt
 * @since 1.1.2
 */
@Component
@Service
public class WebMetadataServiceImpl implements WebMetadataService {
	private static final Logger logger = HandlerUtils.getLogger(WebMetadataServiceImpl.class);
	@Reference private MemberDetailsScanner memberDetailsScanner;
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;
	@Reference private MetadataService metadataService;
	@Reference private TypeLocationService typeLocationService;
	
	public SortedMap<JavaType, JavaTypeMetadataDetails> getRelatedApplicationTypeMetadata(JavaType javaType, MemberDetails memberDetails, String metadataIdentificationString) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(memberDetails, "Member details required");
		Assert.isTrue(isApplicationType(javaType), "The supplied type " + javaType + " is not a type which is present in this application");
		
		SortedMap<JavaType, JavaTypeMetadataDetails> specialTypes = new TreeMap<JavaType, JavaTypeMetadataDetails>();
		JavaTypeMetadataDetails javaTypeMetadataDetails = getJavaTypeMetadataDetails(javaType, memberDetails, metadataIdentificationString);
		JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataDetails = javaTypeMetadataDetails.getPersistenceDetails();
		specialTypes.put(javaType, javaTypeMetadataDetails);
		
		for (MethodMetadata method: MemberFindingUtils.getMethods(memberDetails)) {
			// Not interested in non accessor methods
			if (!BeanInfoUtils.isAccessorMethod(method)) {
				continue;
			}
			// Not interested in persitence identifiers and version fields
			if (javaTypePersistenceMetadataDetails != null && isPersistenceIdentifierOrVersionMethod(method, javaTypePersistenceMetadataDetails)) {
				continue;
			}
			// Not interested in fields that are JPA transient fields or immutable fields
			FieldMetadata fieldMetadata = BeanInfoUtils.getFieldForPropertyName(memberDetails, BeanInfoUtils.getPropertyNameForJavaBeanMethod(method));
			if (fieldMetadata == null || fieldMetadata.getCustomData().keySet().contains(PersistenceCustomDataKeys.TRANSIENT_FIELD) || !BeanInfoUtils.hasAccessorAndMutator(fieldMetadata, memberDetails)) {
				continue;
			}
			JavaType type = method.getReturnType();
			if (type.isCommonCollectionType()) {
				for (JavaType genericType: type.getParameters()) {
					if (isApplicationType(genericType)) {
						MemberDetails genericTypeMemberDetails = getMemberDetails(genericType);
						specialTypes.put(genericType, getJavaTypeMetadataDetails(genericType, genericTypeMemberDetails, metadataIdentificationString));
					}
				}
			} else {
				if (isApplicationType(type) && !fieldMetadata.getCustomData().keySet().contains(PersistenceCustomDataKeys.EMBEDDED_FIELD)) {
					MemberDetails typeMemberDetails = getMemberDetails(type);
					specialTypes.put(type, getJavaTypeMetadataDetails(type, typeMemberDetails, metadataIdentificationString));
				}
			}
		}
		return Collections.unmodifiableSortedMap(specialTypes);
	}
	
	public List<JavaTypeMetadataDetails> getDependentApplicationTypeMetadata(JavaType javaType, MemberDetails memberDetails, String metadataIdentificationString) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(memberDetails, "Member details required");
		
		List<JavaTypeMetadataDetails> dependentTypes = new ArrayList<JavaTypeMetadataDetails>();
		for (MethodMetadata method : MemberFindingUtils.getMethods(memberDetails)) {
			JavaType type = method.getReturnType();
			if (BeanInfoUtils.isAccessorMethod(method) && isApplicationType(type)) {
				FieldMetadata field = BeanInfoUtils.getFieldForPropertyName(memberDetails, BeanInfoUtils.getPropertyNameForJavaBeanMethod(method));
				if (null != field && null != MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.NotNull"))) {
					MemberDetails typeMemberDetails = getMemberDetails(type);
					JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataHolder = getJavaTypePersistenceMetadataDetails(type, typeMemberDetails, metadataIdentificationString);
					if (javaTypePersistenceMetadataHolder != null) {
						dependentTypes.add(getJavaTypeMetadataDetails(type, typeMemberDetails, metadataIdentificationString));
					}
				}
			}
		}
		return Collections.unmodifiableList(dependentTypes);
	}
	
	public List<FieldMetadata> getScaffoldEligibleFieldMetadata(JavaType javaType, MemberDetails memberDetails, String metadataIdentificationString) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(memberDetails, "Member details required");
		
		Map<JavaSymbolName, FieldMetadata> fields = new LinkedHashMap<JavaSymbolName, FieldMetadata>();
		for (MethodMetadata method : MemberFindingUtils.getMethods(memberDetails)) {
			// Only interested in accessors
			if (!BeanInfoUtils.isAccessorMethod(method)) {
				continue;
			}
			if (isPersistenceIdentifierOrVersionMethod(method, getJavaTypePersistenceMetadataDetails(javaType, memberDetails, metadataIdentificationString))) {
				continue;
			}
			JavaSymbolName propertyName = BeanInfoUtils.getPropertyNameForJavaBeanMethod(method);
			FieldMetadata field = BeanInfoUtils.getFieldForPropertyName(memberDetails, propertyName);
			if (field == null || !BeanInfoUtils.hasAccessorAndMutator(field, memberDetails)) {
				continue;
			}
			registerDependency(method.getDeclaredByMetadataId(), metadataIdentificationString);
			if (!fields.containsKey(propertyName)) {
				fields.put(propertyName, field);
			}
		}
		return Collections.unmodifiableList(new ArrayList<FieldMetadata>(fields.values()));
	}
	
	@SuppressWarnings("unchecked") 
	public JavaTypePersistenceMetadataDetails getJavaTypePersistenceMetadataDetails(JavaType javaType, MemberDetails memberDetails, String metadataIdentificationString) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(memberDetails, "Member details service required");
		Assert.notNull(metadataService, "Metadata service required");
		
		JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataDetails = null;
		List<FieldMetadata> idFields = MemberFindingUtils.getFieldsWithTag(memberDetails, PersistenceCustomDataKeys.IDENTIFIER_FIELD);
		List<FieldMetadata> compositePkFields = new LinkedList<FieldMetadata>();
		if (idFields.size() == 0) {
			idFields = MemberFindingUtils.getFieldsWithTag(memberDetails, PersistenceCustomDataKeys.EMBEDDED_ID_FIELD);
			if (idFields.size() != 1) {
				return null;
			}
			for (FieldMetadata field: MemberFindingUtils.getFields(getMemberDetails(idFields.get(0).getFieldType()))) {
				if (!field.getCustomData().keySet().contains(CustomDataSerializableTags.SERIAL_VERSION_UUID_FIELD)) {
					compositePkFields.add(field);
				}
			}
		}
		FieldMetadata identifierField = idFields.get(0);
		registerDependency(identifierField.getDeclaredByMetadataId(), metadataIdentificationString);
		
		MethodMetadata identifierAccessor = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.IDENTIFIER_ACCESSOR_METHOD);
		MethodMetadata versionAccessor = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.VERSION_ACCESSOR_METHOD);
		MethodMetadata persistMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.PERSIST_METHOD);
		MethodMetadata removeMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.REMOVE_METHOD);
		MethodMetadata mergeMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.MERGE_METHOD);
		MethodMetadata findAllMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.FIND_ALL_METHOD);
		MethodMetadata findMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.FIND_METHOD);
		MethodMetadata countMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.COUNT_ALL_METHOD);
		MethodMetadata findEntriesMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.FIND_ENTRIES_METHOD);
		List<String> dynamicFinderNames = new ArrayList<String>();
		
		for (MemberHoldingTypeDetails mhtd: memberDetails.getDetails()) {
			if (mhtd.getCustomData().keySet().contains(PersistenceCustomDataKeys.DYNAMIC_FINDER_NAMES)) {
				dynamicFinderNames = (List<String>) mhtd.getCustomData().get(PersistenceCustomDataKeys.DYNAMIC_FINDER_NAMES);
			}
		}
		
		if (identifierAccessor != null) {
			registerDependency(identifierAccessor.getDeclaredByMetadataId(), metadataIdentificationString);
			javaTypePersistenceMetadataDetails = new JavaTypePersistenceMetadataDetails(identifierField, identifierAccessor, versionAccessor, persistMethod, mergeMethod, removeMethod, findAllMethod, 
					findMethod, countMethod, findEntriesMethod, dynamicFinderNames, isRooIdentifier(javaType, memberDetails), compositePkFields);
		}	
		return javaTypePersistenceMetadataDetails;
	}
	
	private String getPlural(JavaType javaType, String metadataIdentificationString) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(metadataService, "Metadata service required");
		
		String pluralMetadataKey = PluralMetadata.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
		PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(pluralMetadataKey);
		if (pluralMetadata != null) {
			registerDependency(pluralMetadataKey, metadataIdentificationString);
			return pluralMetadata.getPlural();
		}
		return javaType.getSimpleTypeName() + "s";
	}
	
	public boolean isRooIdentifier(JavaType javaType, MemberDetails memberDetails) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(memberDetails, "Member details required");
		return MemberFindingUtils.getMemberHoldingTypeDetailsWithTag(memberDetails, PersistenceCustomDataKeys.IDENTIFIER_TYPE).size() > 0;
	}
	
	private boolean isEnumType(JavaType javaType, MetadataService metadataService) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(metadataService, "Metadata service required");
		
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA));
		if (physicalTypeMetadata != null) {
			ClassOrInterfaceTypeDetails details = (ClassOrInterfaceTypeDetails) physicalTypeMetadata.getMemberHoldingTypeDetails();
			if (details != null) {
				if (details.getPhysicalTypeCategory().equals(PhysicalTypeCategory.ENUMERATION)) {
					return true;
				}
//				@Enumerated has a target of Method or Field
//				if (MemberFindingUtils.getAnnotationOfType(details.getAnnotations(), new JavaType("javax.persistence.Enumerated")) != null) {
//					return true;
//				}
			}
		}
		return false;
	}
	
	public boolean isApplicationType(JavaType javaType) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(metadataService, "Metadata service required");
		
		return (metadataService.get(PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA)) != null);
	}
	
	public Map<JavaSymbolName, DateTimeFormatDetails> getDatePatterns(JavaType javaType, MemberDetails memberDetails, String metadataIdentificationString) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(memberDetails, "Member details required");
		
		Map<JavaSymbolName, DateTimeFormatDetails> dates = new LinkedHashMap<JavaSymbolName, DateTimeFormatDetails>();
		JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataDetails = getJavaTypePersistenceMetadataDetails(javaType, memberDetails, metadataIdentificationString);
		for (MethodMetadata method : MemberFindingUtils.getMethods(memberDetails)) {
			// Only interested in accessors
			if (!BeanInfoUtils.isAccessorMethod(method)) {
				continue;
			}
			// Not interested in fields that are not exposed via a mutator and accessor and in identifiers and version fields
			if (isPersistenceIdentifierOrVersionMethod(method, javaTypePersistenceMetadataDetails)) {
				continue;
			}
			JavaType type = method.getReturnType();
			JavaSymbolName fieldName = BeanInfoUtils.getPropertyNameForJavaBeanMethod(method);
			FieldMetadata fieldMetadata = BeanInfoUtils.getFieldForPropertyName(memberDetails, fieldName);
			if (fieldMetadata == null || !BeanInfoUtils.hasAccessorAndMutator(fieldMetadata, memberDetails)) {
				continue;
			}
			if (type.getFullyQualifiedTypeName().equals(Date.class.getName()) || type.getFullyQualifiedTypeName().equals(Calendar.class.getName())) {
				AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(fieldMetadata.getAnnotations(), new JavaType("org.springframework.format.annotation.DateTimeFormat"));
				JavaSymbolName patternSymbol = new JavaSymbolName("pattern");
				JavaSymbolName styleSymbol = new JavaSymbolName("style");
				DateTimeFormatDetails dateTimeFormat = null;
				if (annotation != null) {
					if (annotation.getAttributeNames().contains(styleSymbol)) {
						dateTimeFormat = DateTimeFormatDetails.withStyle(annotation.getAttribute(styleSymbol).getValue().toString());
					} else if (annotation.getAttributeNames().contains(patternSymbol)) {
						dateTimeFormat = DateTimeFormatDetails.withPattern(annotation.getAttribute(patternSymbol).getValue().toString());
					}
				}
				if (dateTimeFormat != null) {
					registerDependency(fieldMetadata.getDeclaredByMetadataId(), metadataIdentificationString);
					dates.put(fieldMetadata.getFieldName(), dateTimeFormat);
					if (javaTypePersistenceMetadataDetails != null) {
						for (String finder : javaTypePersistenceMetadataDetails.getFinderNames()) {
							if (finder.contains(StringUtils.capitalize(fieldMetadata.getFieldName().getSymbolName()) + "Between")) {
								dates.put(new JavaSymbolName("min" + StringUtils.capitalize(fieldMetadata.getFieldName().getSymbolName())), dateTimeFormat);
								dates.put(new JavaSymbolName("max" + StringUtils.capitalize(fieldMetadata.getFieldName().getSymbolName())), dateTimeFormat);
							}
						}
					}
				} else {
					logger.warning("It is recommended to use @DateTimeFormat(style=\"S-\") on " + fieldMetadata.getFieldType().getFullyQualifiedTypeName() + "." + fieldMetadata.getFieldName() + " to use automatic date conversion in Spring MVC");
				}
			}
		}
		return Collections.unmodifiableMap(dates);
	}
	
	public Set<FinderMetadataDetails> getDynamicFinderMethodsAndFields(JavaType javaType, MemberDetails memberDetails, String metadataIdentificationString) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(memberDetails, "Member details required");
		
		Set<FinderMetadataDetails> finderMetadataDetails = new HashSet<FinderMetadataDetails>();
		String finderMetadataKey = FinderMetadata.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
		FinderMetadata finderMetadata = (FinderMetadata) metadataService.get(finderMetadataKey);
		if (finderMetadata != null) {
			registerDependency(finderMetadataKey, metadataIdentificationString);
			for (MethodMetadata method: finderMetadata.getAllDynamicFinders()) {
				List<JavaSymbolName> paramNames = method.getParameterNames();
				List<JavaType> paramTypes = AnnotatedJavaType.convertFromAnnotatedJavaTypes(method.getParameterTypes());
				List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
				for (int i = 0; i < paramTypes.size(); i++) {
					JavaSymbolName fieldName = null;
					if (paramNames.get(i).getSymbolName().startsWith("max") || paramNames.get(i).getSymbolName().startsWith("min")) {
						fieldName = new JavaSymbolName(Introspector.decapitalize(StringUtils.capitalize(paramNames.get(i).getSymbolName().substring(3))));
					} else {
						fieldName = paramNames.get(i);
					}
					FieldMetadata field = BeanInfoUtils.getFieldForPropertyName(memberDetails, fieldName);
					if (field != null) {
						FieldMetadataBuilder fieldMd = new FieldMetadataBuilder(field);
						fieldMd.setFieldName(paramNames.get(i));
						fields.add(fieldMd.build());
					}
				}
				FinderMetadataDetails details = new FinderMetadataDetails(method.getMethodName().getSymbolName(), method, fields);
				finderMetadataDetails.add(details);
			}
		}
		return Collections.unmodifiableSet(finderMetadataDetails);
	}
	
	private boolean isPersistenceIdentifierOrVersionMethod(MethodMetadata method, JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataDetails) {
		Assert.notNull(method, "Method metadata required");
		
		return javaTypePersistenceMetadataDetails != null
						&& (method.getMethodName().equals(javaTypePersistenceMetadataDetails.getIdentifierAccessorMethod().getMethodName()) 
						|| (javaTypePersistenceMetadataDetails.getVersionAccessorMethod() != null && method.getMethodName().equals(javaTypePersistenceMetadataDetails.getVersionAccessorMethod().getMethodName())));
	}
	
	public JavaTypeMetadataDetails getJavaTypeMetadataDetails(JavaType javaType, MemberDetails memberDetails, String metadataIdentificationString) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(metadataService, "Metadata service required");
		registerDependency(PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA), metadataIdentificationString);
		return new JavaTypeMetadataDetails(
				javaType, 
				getPlural(javaType, metadataIdentificationString),
				isEnumType(javaType, metadataService), isApplicationType(javaType),
				getJavaTypePersistenceMetadataDetails(javaType, memberDetails, metadataIdentificationString),
				getControllerPathForType(javaType, metadataIdentificationString));
	}
	
	private String getControllerPathForType(JavaType type, String metadataIdentificationString) {
		String webScaffoldMetadataKey = null;
		WebScaffoldMetadata webScaffoldMetadata = null;
		JavaType rooWebScaffold = new JavaType(RooWebScaffold.class.getName());
		for (ClassOrInterfaceTypeDetails coitd: typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(rooWebScaffold)) {
			for (AnnotationMetadata annotation: coitd.getAnnotations()) {
				if (annotation.getAnnotationType().equals(rooWebScaffold)) {
					AnnotationAttributeValue<?> formBackingObject = annotation.getAttribute(new JavaSymbolName("formBackingObject"));
					if (formBackingObject instanceof ClassAttributeValue) {
						ClassAttributeValue formBackingObjectValue = (ClassAttributeValue) formBackingObject;
						if (formBackingObjectValue.getValue().equals(type)) {
							webScaffoldMetadataKey = WebScaffoldMetadata.createIdentifier(coitd.getName(), Path.SRC_MAIN_JAVA);
							webScaffoldMetadata = (WebScaffoldMetadata) metadataService.get(webScaffoldMetadataKey);
						}
					}
				}
			}
		}
		if (webScaffoldMetadata != null) {
			registerDependency(webScaffoldMetadataKey, metadataIdentificationString);
			return webScaffoldMetadata.getAnnotationValues().getPath();
		}
		return getPlural(type, metadataIdentificationString).toLowerCase();
	}
	
	private void registerDependency(String upstreamDependency, String downStreamDependency) {
		if (metadataDependencyRegistry != null && StringUtils.hasText(upstreamDependency) && StringUtils.hasText(downStreamDependency) && !upstreamDependency.equals(downStreamDependency) && !MetadataIdentificationUtils.getMetadataClass(downStreamDependency).equals(MetadataIdentificationUtils.getMetadataClass(upstreamDependency))) {
			metadataDependencyRegistry.registerDependency(upstreamDependency, downStreamDependency);
		}
	}
	
	public MemberDetails getMemberDetails(JavaType javaType) {
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA));
		Assert.notNull(physicalTypeMetadata, "Unable to obtain physical type metadata for type " + javaType.getFullyQualifiedTypeName());
		ClassOrInterfaceTypeDetails classOrInterfaceDetails = (ClassOrInterfaceTypeDetails) physicalTypeMetadata.getMemberHoldingTypeDetails();
		return memberDetailsScanner.getMemberDetails(WebMetadataServiceImpl.class.getName(), classOrInterfaceDetails);
	}
}
