package org.springframework.roo.addon.web.mvc.controller.details;

import static org.springframework.roo.model.Jsr303JavaType.NOT_NULL;
import static org.springframework.roo.model.RooJavaType.ROO_WEB_SCAFFOLD;
import static org.springframework.roo.model.SpringJavaType.DATE_TIME_FORMAT;

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.finder.FinderMetadata;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldMetadata;
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
import org.springframework.roo.classpath.layers.LayerService;
import org.springframework.roo.classpath.layers.LayerType;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.layers.MethodParameter;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
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
	
	// Constants
	private static final Logger logger = HandlerUtils.getLogger(WebMetadataServiceImpl.class);
	private static final String COUNT_ALL_METHOD = PersistenceCustomDataKeys.COUNT_ALL_METHOD.name();
	private static final String DELETE_METHOD = PersistenceCustomDataKeys.REMOVE_METHOD.name();
	private static final String FIND_METHOD = PersistenceCustomDataKeys.FIND_METHOD.name();
	private static final String FIND_ALL_METHOD = PersistenceCustomDataKeys.FIND_ALL_METHOD.name();
	private static final String FIND_ENTRIES_METHOD = PersistenceCustomDataKeys.FIND_ENTRIES_METHOD.name();
	private static final String MERGE_METHOD = PersistenceCustomDataKeys.MERGE_METHOD.name();
	private static final String PERSIST_METHOD = PersistenceCustomDataKeys.PERSIST_METHOD.name();
	private static final int LAYER_POSITION = LayerType.HIGHEST.getPosition();

	// Fields
	@Reference private LayerService layerService;
	@Reference private MemberDetailsScanner memberDetailsScanner;
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;
	@Reference private MetadataService metadataService;
	@Reference private PersistenceMemberLocator persistenceMemberLocator;
	@Reference private TypeLocationService typeLocationService;
	
	public SortedMap<JavaType, JavaTypeMetadataDetails> getRelatedApplicationTypeMetadata(JavaType javaType, MemberDetails memberDetails, String metadataIdentificationString) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(memberDetails, "Member details required");
		Assert.isTrue(isApplicationType(javaType), "The supplied type " + javaType + " is not a type which is present in this application");
		
		MethodMetadata identifierAccessor = persistenceMemberLocator.getIdentifierAccessor(javaType);
		MethodMetadata versionAccessor = persistenceMemberLocator.getVersionAccessor(javaType);

		SortedMap<JavaType, JavaTypeMetadataDetails> specialTypes = new TreeMap<JavaType, JavaTypeMetadataDetails>();
		JavaTypeMetadataDetails javaTypeMetadataDetails = getJavaTypeMetadataDetails(javaType, memberDetails, metadataIdentificationString);
		specialTypes.put(javaType, javaTypeMetadataDetails);
		
		for (MethodMetadata method: MemberFindingUtils.getMethods(memberDetails)) {
			// Not interested in non accessor methods
			if (!BeanInfoUtils.isAccessorMethod(method)) {
				continue;
			}
			// Not interested in persistence identifiers and version fields
			if (isPersistenceIdentifierOrVersionMethod(method, identifierAccessor, versionAccessor)) {
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
		Assert.notNull(memberDetails, "Member details required");
		
		List<JavaTypeMetadataDetails> dependentTypes = new ArrayList<JavaTypeMetadataDetails>();
		for (MethodMetadata method : MemberFindingUtils.getMethods(memberDetails)) {
			JavaType type = method.getReturnType();
			if (BeanInfoUtils.isAccessorMethod(method) && isApplicationType(type)) {
				FieldMetadata field = BeanInfoUtils.getFieldForPropertyName(memberDetails, BeanInfoUtils.getPropertyNameForJavaBeanMethod(method));
				if (field != null && MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), NOT_NULL) != null) {
					MemberDetails typeMemberDetails = getMemberDetails(type);
					if (getJavaTypePersistenceMetadataDetails(type, typeMemberDetails, metadataIdentificationString) != null) {
						dependentTypes.add(getJavaTypeMetadataDetails(type, typeMemberDetails, metadataIdentificationString));
					}
				}
			}
		}
		return Collections.unmodifiableList(dependentTypes);
	}
	
	public List<FieldMetadata> getScaffoldEligibleFieldMetadata(JavaType javaType, MemberDetails memberDetails, String metadataIdentificationString) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(memberDetails, "Member details required");
		
		MethodMetadata identifierAccessor = persistenceMemberLocator.getIdentifierAccessor(javaType);
		MethodMetadata versionAccessor = persistenceMemberLocator.getVersionAccessor(javaType);

		Map<JavaSymbolName, FieldMetadata> fields = new LinkedHashMap<JavaSymbolName, FieldMetadata>();
		List<MethodMetadata> methods = MemberFindingUtils.getMethods(memberDetails);

		for (MethodMetadata method : methods) {
			// Only interested in accessors
			if (!BeanInfoUtils.isAccessorMethod(method)) {
				continue;
			}
			if (isPersistenceIdentifierOrVersionMethod(method, identifierAccessor, versionAccessor)) {
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
	public JavaTypePersistenceMetadataDetails getJavaTypePersistenceMetadataDetails(final JavaType javaType, final MemberDetails memberDetails, final String metadataIdentificationString) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(memberDetails, "Member details service required");
		Assert.hasText(metadataIdentificationString, "Metadata id required");
		
		List<FieldMetadata> idFields = persistenceMemberLocator.getIdentifierFields(javaType);
		if (idFields.isEmpty()) {
			return null;
		}
		FieldMetadata identifierField = idFields.get(0);
		JavaType identifierType = persistenceMemberLocator.getIdentifierType(javaType);
		if (identifierType == null) {
			return null;
		}
		registerDependency(identifierField.getDeclaredByMetadataId(), metadataIdentificationString);

		JavaSymbolName entityName = JavaSymbolName.getReservedWordSafeName(javaType);
		final MethodParameter entityParameter = new MethodParameter(javaType, entityName);
		final MethodParameter idParameter = new MethodParameter(identifierType, "id");
		final MethodParameter firstResultParameter = new MethodParameter(JavaType.INT_PRIMITIVE, "firstResult");
		final MethodParameter maxResultsParameter = new MethodParameter(JavaType.INT_PRIMITIVE, "sizeNo");

		MethodMetadata identifierAccessor = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.IDENTIFIER_ACCESSOR_METHOD);
		MethodMetadata versionAccessor = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.VERSION_ACCESSOR_METHOD);
		MemberTypeAdditions persistMethod = layerService.getMemberTypeAdditions(metadataIdentificationString, PERSIST_METHOD, javaType, identifierType, LAYER_POSITION, entityParameter);
		MemberTypeAdditions removeMethod = layerService.getMemberTypeAdditions(metadataIdentificationString, DELETE_METHOD, javaType, identifierType, LAYER_POSITION, entityParameter);
		MemberTypeAdditions mergeMethod = layerService.getMemberTypeAdditions(metadataIdentificationString, MERGE_METHOD, javaType, identifierType, LAYER_POSITION, entityParameter);
		MemberTypeAdditions findAllMethod = layerService.getMemberTypeAdditions(metadataIdentificationString, FIND_ALL_METHOD, javaType, identifierType, LAYER_POSITION);
		MemberTypeAdditions findMethod = layerService.getMemberTypeAdditions(metadataIdentificationString, FIND_METHOD, javaType, identifierType, LAYER_POSITION, idParameter);
		MemberTypeAdditions countMethod = layerService.getMemberTypeAdditions(metadataIdentificationString, COUNT_ALL_METHOD, javaType, identifierType, LAYER_POSITION);
		MemberTypeAdditions findEntriesMethod = layerService.getMemberTypeAdditions(metadataIdentificationString, FIND_ENTRIES_METHOD, javaType, identifierType, LAYER_POSITION, firstResultParameter, maxResultsParameter);
		List<String> dynamicFinderNames = new ArrayList<String>();
		for (MemberHoldingTypeDetails mhtd: memberDetails.getDetails()) {
			if (mhtd.getCustomData().keySet().contains(PersistenceCustomDataKeys.DYNAMIC_FINDER_NAMES)) {
				dynamicFinderNames = (List<String>) mhtd.getCustomData().get(PersistenceCustomDataKeys.DYNAMIC_FINDER_NAMES);
				break;
			}
		}
		
		JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataDetails = null;
		if (identifierAccessor != null) {
			registerDependency(identifierAccessor.getDeclaredByMetadataId(), metadataIdentificationString);
			javaTypePersistenceMetadataDetails = new JavaTypePersistenceMetadataDetails(identifierType, identifierField, identifierAccessor, versionAccessor, persistMethod, mergeMethod, removeMethod, findAllMethod, 
					findMethod, countMethod, findEntriesMethod, dynamicFinderNames, isRooIdentifier(javaType, memberDetails), persistenceMemberLocator.getEmbeddedIdentifierFields(javaType));
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
	
	private boolean isEnumType(JavaType javaType) {
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
//				if (MemberFindingUtils.getAnnotationOfType(details.getAnnotations(), ENUMERATED) != null) {
//					return true;
//				}
			}
		}
		return false;
	}
	
	public boolean isApplicationType(JavaType javaType) {
		Assert.notNull(javaType, "Java type required");
		return metadataService.get(PhysicalTypeIdentifier.createIdentifier(javaType)) != null;
	}
	
	public Map<JavaSymbolName, DateTimeFormatDetails> getDatePatterns(JavaType javaType, MemberDetails memberDetails, String metadataIdentificationString) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(memberDetails, "Member details required");
		
		MethodMetadata identifierAccessor = persistenceMemberLocator.getIdentifierAccessor(javaType);
		MethodMetadata versionAccessor = persistenceMemberLocator.getVersionAccessor(javaType);

		Map<JavaSymbolName, DateTimeFormatDetails> dates = new LinkedHashMap<JavaSymbolName, DateTimeFormatDetails>();
		JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataDetails = getJavaTypePersistenceMetadataDetails(javaType, memberDetails, metadataIdentificationString);

		for (MethodMetadata method : MemberFindingUtils.getMethods(memberDetails)) {
			// Only interested in accessors
			if (!BeanInfoUtils.isAccessorMethod(method)) {
				continue;
			}
			// Not interested in fields that are not exposed via a mutator and accessor and in identifiers and version fields
			if (isPersistenceIdentifierOrVersionMethod(method, identifierAccessor, versionAccessor)) {
				continue;
			}
			JavaType type = method.getReturnType();
			JavaSymbolName fieldName = BeanInfoUtils.getPropertyNameForJavaBeanMethod(method);
			FieldMetadata fieldMetadata = BeanInfoUtils.getFieldForPropertyName(memberDetails, fieldName);
			if (fieldMetadata == null || !BeanInfoUtils.hasAccessorAndMutator(fieldMetadata, memberDetails)) {
				continue;
			}
			if (!JdkJavaType.isDateField(type)) {
				continue;
			}
			AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(fieldMetadata.getAnnotations(), DATE_TIME_FORMAT);
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
				logger.warning("It is recommended to use @DateTimeFormat(style=\"M-\") on " + fieldMetadata.getFieldType().getFullyQualifiedTypeName() + "." + fieldMetadata.getFieldName() + " to use automatic date conversion in Spring MVC");
			}
		}
		return Collections.unmodifiableMap(dates);
	}
	
	public Set<FinderMetadataDetails> getDynamicFinderMethodsAndFields(JavaType javaType, MemberDetails memberDetails, String metadataIdentificationString) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(memberDetails, "Member details required");
		
		SortedSet<FinderMetadataDetails> finderMetadataDetails = new TreeSet<FinderMetadataDetails>();
		String finderMetadataKey = FinderMetadata.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
		registerDependency(finderMetadataKey, metadataIdentificationString);
		FinderMetadata finderMetadata = (FinderMetadata) metadataService.get(finderMetadataKey);
		if (finderMetadata != null) {
			for (MethodMetadata method: finderMetadata.getAllDynamicFinders()) {
				List<JavaSymbolName> parameterNames = method.getParameterNames();
				List<JavaType> parameterTypes = AnnotatedJavaType.convertFromAnnotatedJavaTypes(method.getParameterTypes());
				List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
				for (int i = 0; i < parameterTypes.size(); i++) {
					JavaSymbolName fieldName = null;
					if (parameterNames.get(i).getSymbolName().startsWith("max") || parameterNames.get(i).getSymbolName().startsWith("min")) {
						fieldName = new JavaSymbolName(Introspector.decapitalize(StringUtils.capitalize(parameterNames.get(i).getSymbolName().substring(3))));
					} else {
						fieldName = parameterNames.get(i);
					}
					FieldMetadata field = BeanInfoUtils.getFieldForPropertyName(memberDetails, fieldName);
					if (field != null) {
						FieldMetadataBuilder fieldMd = new FieldMetadataBuilder(field);
						fieldMd.setFieldName(parameterNames.get(i));
						fields.add(fieldMd.build());
					}
				}
				FinderMetadataDetails details = new FinderMetadataDetails(method.getMethodName().getSymbolName(), method, fields);
				finderMetadataDetails.add(details);
			}
		}
		return Collections.unmodifiableSortedSet(finderMetadataDetails);
	}
	
	private boolean isPersistenceIdentifierOrVersionMethod(MethodMetadata method, MethodMetadata idMethod, MethodMetadata versionMethod) {
		Assert.notNull(method, "Method metadata required");
		
		return (idMethod != null && method.getMethodName().equals(idMethod.getMethodName())) || (versionMethod != null && method.getMethodName().equals(versionMethod.getMethodName()));
	}
	
	public JavaTypeMetadataDetails getJavaTypeMetadataDetails(JavaType javaType, MemberDetails memberDetails, String metadataIdentificationString) {
		Assert.notNull(javaType, "Java type required");
		registerDependency(PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA), metadataIdentificationString);
		return new JavaTypeMetadataDetails(
				javaType, 
				getPlural(javaType, metadataIdentificationString),
				isEnumType(javaType), isApplicationType(javaType),
				getJavaTypePersistenceMetadataDetails(javaType, memberDetails, metadataIdentificationString),
				getControllerPathForType(javaType, metadataIdentificationString));
	}

	private HashMap<String, String> pathMap = new HashMap<String, String>();
	
	private String getControllerPathForType(JavaType type, String metadataIdentificationString) {
		if (pathMap.containsKey(type.getFullyQualifiedTypeName()) && !typeLocationService.hasTypeChanged(getClass().getName(), type)) {
			return pathMap.get(type.getFullyQualifiedTypeName());
		}
		String webScaffoldMetadataKey = null;
		WebScaffoldMetadata webScaffoldMetadata = null;
		for (ClassOrInterfaceTypeDetails coitd: typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(ROO_WEB_SCAFFOLD)) {
			for (AnnotationMetadata annotation: coitd.getAnnotations()) {
				if (annotation.getAnnotationType().equals(ROO_WEB_SCAFFOLD)) {
					AnnotationAttributeValue<?> formBackingObject = annotation.getAttribute(new JavaSymbolName("formBackingObject"));
					if (formBackingObject instanceof ClassAttributeValue) {
						ClassAttributeValue formBackingObjectValue = (ClassAttributeValue) formBackingObject;
						if (formBackingObjectValue.getValue().equals(type)) {
							AnnotationAttributeValue<String> path = annotation.getAttribute("path");
							if (path != null) {
								String pathString = path.getValue();
								pathMap.put(type.getFullyQualifiedTypeName(), pathString);
								return pathString;
							}
							webScaffoldMetadataKey = WebScaffoldMetadata.createIdentifier(coitd.getName(), Path.SRC_MAIN_JAVA);
							webScaffoldMetadata = (WebScaffoldMetadata) metadataService.get(webScaffoldMetadataKey);
							break;
						}
					}
				}
			}
		}
		if (webScaffoldMetadata != null) {
			registerDependency(webScaffoldMetadataKey, metadataIdentificationString);
			String path = webScaffoldMetadata.getAnnotationValues().getPath();
			pathMap.put(type.getFullyQualifiedTypeName(), path);
			return path;
		}
		return getPlural(type, metadataIdentificationString).toLowerCase();
	}
	
	private void registerDependency(String upstreamDependency, String downStreamDependency) {
		if (metadataDependencyRegistry != null && StringUtils.hasText(upstreamDependency) && StringUtils.hasText(downStreamDependency) && !upstreamDependency.equals(downStreamDependency) && !MetadataIdentificationUtils.getMetadataClass(downStreamDependency).equals(MetadataIdentificationUtils.getMetadataClass(upstreamDependency))) {
			metadataDependencyRegistry.registerDependency(upstreamDependency, downStreamDependency);
		}
	}
	
	public MemberDetails getMemberDetails(final JavaType javaType) {
		ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = typeLocationService.getClassOrInterface(javaType);
		Assert.notNull(classOrInterfaceTypeDetails, "Unable to obtain physical type metadata for type " + javaType.getFullyQualifiedTypeName());
		return memberDetailsScanner.getMemberDetails(WebMetadataServiceImpl.class.getName(), classOrInterfaceTypeDetails);
	}
	
	public Map<String, MemberTypeAdditions> getCrudAdditions(final JavaType domainType, String metadataIdentificationString) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.createIdentifier(domainType, Path.SRC_MAIN_JAVA), metadataIdentificationString);
		
		JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataDetails = getJavaTypePersistenceMetadataDetails(domainType, getMemberDetails(domainType), metadataIdentificationString);
		Map<String, MemberTypeAdditions> additions = new HashMap<String, MemberTypeAdditions>();
		if (javaTypePersistenceMetadataDetails == null) {
			return additions;
		}
		
		additions.put(COUNT_ALL_METHOD, javaTypePersistenceMetadataDetails.getCountMethod());
		additions.put(DELETE_METHOD, javaTypePersistenceMetadataDetails.getRemoveMethod());
		additions.put(FIND_METHOD, javaTypePersistenceMetadataDetails.getFindMethod());
		additions.put(FIND_ALL_METHOD, javaTypePersistenceMetadataDetails.getFindAllMethod());
		additions.put(FIND_ENTRIES_METHOD, javaTypePersistenceMetadataDetails.getFindEntriesMethod());
		additions.put(MERGE_METHOD, javaTypePersistenceMetadataDetails.getMergeMethod());
		additions.put(PERSIST_METHOD, javaTypePersistenceMetadataDetails.getPersistMethod());
		return Collections.unmodifiableMap(additions);
	}
}
