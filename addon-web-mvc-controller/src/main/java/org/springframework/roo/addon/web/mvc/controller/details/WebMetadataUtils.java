package org.springframework.roo.addon.web.mvc.controller.details;

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.entity.IdentifierMetadata;
import org.springframework.roo.addon.finder.FinderMetadata;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.addon.web.mvc.controller.RooWebScaffold;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldMetadata;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Utils to retrieve various metadata information for use by Web scaffolding add-ons.
 * 
 * @author Stefan Schmidt
 * @since 1.1.2
 */
public abstract class WebMetadataUtils {
	
	private static Logger log = Logger.getLogger(WebMetadataUtils.class.getName());
	
	public static SortedMap<JavaType, JavaTypeMetadataDetails> getRelatedApplicationTypeMetadata(JavaType javaType, MemberDetails memberDetails, MetadataService metadataService, TypeLocationService typeLocationService, String metadataIdentificationString, MetadataDependencyRegistry metadataDependencyRegistry) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(memberDetails, "Member details required");
		Assert.isTrue(isApplicationType(javaType, metadataService), "The supplied type " + javaType + " is not a type which is present in this application");
		
		SortedMap<JavaType, JavaTypeMetadataDetails> specialTypes = new TreeMap<JavaType, JavaTypeMetadataDetails>();
		JavaTypeMetadataDetails javaTypeMetadataDetails = getJavaTypeMetadataDetails(javaType, metadataService, typeLocationService, metadataIdentificationString, metadataDependencyRegistry);
		JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataDetails = javaTypeMetadataDetails.getPersistenceDetails();
		specialTypes.put(javaType, javaTypeMetadataDetails);
		
		for (MethodMetadata method: MemberFindingUtils.getMethods(memberDetails)) {
			// not interested in non accessor methods
			if (!BeanInfoUtils.isAccessorMethod(method)) {
				continue;
			}
			// Not interested in persitence identifiers and version fields
			if (javaTypePersistenceMetadataDetails != null && isPersistenceIdentifierOrVersionMethod(method, javaTypePersistenceMetadataDetails)) {
				continue;
			}
			// Not interested in fields that are JPA transient fields or immutable fields
			FieldMetadata fieldMetadata = BeanInfoUtils.getFieldForPropertyName(memberDetails, BeanInfoUtils.getPropertyNameForJavaBeanMethod(method));
			if (fieldMetadata == null || isTransientFieldType(fieldMetadata) || !BeanInfoUtils.hasAccessorAndMutator(fieldMetadata, memberDetails)) {
				continue;
			}
			JavaType type = method.getReturnType();
			if (type.isCommonCollectionType()) {
				for (JavaType genericType: type.getParameters()) {
					if (isApplicationType(genericType, metadataService)) {
						specialTypes.put(genericType, getJavaTypeMetadataDetails(genericType, metadataService, typeLocationService, metadataIdentificationString, metadataDependencyRegistry));
					}
				}
			} else {
				if (isApplicationType(type, metadataService) && !isEmbeddedFieldType(fieldMetadata)) {
					specialTypes.put(type, getJavaTypeMetadataDetails(type, metadataService, typeLocationService, metadataIdentificationString, metadataDependencyRegistry));
				}
			}
		}
		return Collections.unmodifiableSortedMap(specialTypes);
	}
	
	public static List<JavaTypeMetadataDetails> getDependentApplicationTypeMetadata(JavaType javaType, MemberDetails memberDetails, MetadataService metadataService, TypeLocationService typeLocationService, String metadataIdentificationString, MetadataDependencyRegistry metadataDependencyRegistry) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(memberDetails, "Member details required");
		
		List<JavaTypeMetadataDetails> dependentTypes = new ArrayList<JavaTypeMetadataDetails>();
		for (MethodMetadata method : MemberFindingUtils.getMethods(memberDetails)) {
			JavaType type = method.getReturnType();
			if (BeanInfoUtils.isAccessorMethod(method) && isApplicationType(type, metadataService)) {
				FieldMetadata field = BeanInfoUtils.getFieldForPropertyName(memberDetails, BeanInfoUtils.getPropertyNameForJavaBeanMethod(method));
				if (null != field && null != MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.NotNull"))) {
					JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataHolder = getJavaTypePersistenceMetadataDetails(type, metadataService, metadataIdentificationString, metadataDependencyRegistry);
					if (javaTypePersistenceMetadataHolder != null) {
						dependentTypes.add(getJavaTypeMetadataDetails(type, metadataService, typeLocationService, metadataIdentificationString, metadataDependencyRegistry));
					}
				}
			}
		}
		return Collections.unmodifiableList(dependentTypes);
	}
	
	public static List<FieldMetadata> getScaffoldElegibleFieldMetadata(JavaType javaType, MemberDetails memberDetails, MetadataService metadataService , String metadataIdentificationString, MetadataDependencyRegistry metadataDependencyRegistry) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(memberDetails, "Member details required");
		
		Map<JavaSymbolName, FieldMetadata> fields = new HashMap<JavaSymbolName, FieldMetadata>();
		for (MethodMetadata method : MemberFindingUtils.getMethods(memberDetails)) {
			// Only interested in accessors
			if (!BeanInfoUtils.isAccessorMethod(method)) {
				continue;
			}
			if (isPersistenceIdentifierOrVersionMethod(method, getJavaTypePersistenceMetadataDetails(javaType, metadataService, metadataIdentificationString, metadataDependencyRegistry))) {
				continue;
			}
			JavaSymbolName propertyName = BeanInfoUtils.getPropertyNameForJavaBeanMethod(method);
			FieldMetadata field = BeanInfoUtils.getFieldForPropertyName(memberDetails, propertyName);
			if (field == null || !BeanInfoUtils.hasAccessorAndMutator(field, memberDetails)) {
				continue;
			}
			registerDependency(metadataDependencyRegistry, method.getDeclaredByMetadataId(), metadataIdentificationString);
			if (!fields.containsKey(propertyName)) {
				fields.put(propertyName, field);
			}
		}
		return Collections.unmodifiableList(new ArrayList<FieldMetadata>(fields.values()));
	}
	
	public static JavaTypePersistenceMetadataDetails getJavaTypePersistenceMetadataDetails(JavaType javaType, MetadataService metadataService, String metadataIdentificationString, MetadataDependencyRegistry metadataDependencyRegistry) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(metadataService, "Metadata service required");
		
		JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataDetails = null;
		String entityMetadataKey = EntityMetadata.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
		EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(entityMetadataKey);
		List<FieldMetadata> rooIdentifierFields = new ArrayList<FieldMetadata>();
		String identifierMetadataKey = IdentifierMetadata.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
		IdentifierMetadata identifierMetadata = (IdentifierMetadata) metadataService.get(identifierMetadataKey);
		if (identifierMetadata != null) {
			registerDependency(metadataDependencyRegistry, identifierMetadataKey, metadataIdentificationString);
			rooIdentifierFields = identifierMetadata.getFields();
		}
		if (entityMetadata != null) {
			registerDependency(metadataDependencyRegistry, entityMetadataKey, metadataIdentificationString);
			javaTypePersistenceMetadataDetails = new JavaTypePersistenceMetadataDetails(entityMetadata.getIdentifierField(), entityMetadata.getIdentifierAccessor(), entityMetadata.getVersionAccessor(), entityMetadata.getPersistMethod(), 
					entityMetadata.getMergeMethod(), entityMetadata.getRemoveMethod(), entityMetadata.getFindAllMethod(), entityMetadata.getFindMethod(), entityMetadata.getCountMethod(), entityMetadata.getFindEntriesMethod(), 
					entityMetadata.getDynamicFinders(), isRooIdentifier(javaType, metadataService), rooIdentifierFields);
		}	
		return javaTypePersistenceMetadataDetails;
	}
	
	private static String getPlural(JavaType javaType, MetadataService metadataService, String metadataIdentificationString, MetadataDependencyRegistry metadataDependencyRegistry) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(metadataService, "Metadata service required");
		
		String pluralMetadataKey = PluralMetadata.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
		PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(pluralMetadataKey);
		if (pluralMetadata != null) {
			registerDependency(metadataDependencyRegistry, pluralMetadataKey, metadataIdentificationString);
			return pluralMetadata.getPlural();
		} else {
			return javaType.getSimpleTypeName() + "s";
		}
	}
	
	public static boolean isRooIdentifier(JavaType javaType, MetadataService metadataService) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(metadataService, "Metadata service required");
		
		IdentifierMetadata identifierMetadata = (IdentifierMetadata) metadataService.get(IdentifierMetadata.createIdentifier(javaType, Path.SRC_MAIN_JAVA));
		if (identifierMetadata != null) {
			return true;
		}
		return false;
	}
	
	private static boolean isEnumType(JavaType javaType, MetadataService metadataService) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(metadataService, "Metadata service required");
		
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA));
		if (physicalTypeMetadata != null) {
			ClassOrInterfaceTypeDetails details = (ClassOrInterfaceTypeDetails) physicalTypeMetadata.getMemberHoldingTypeDetails();
			if (details != null) {
				if (details.getPhysicalTypeCategory().equals(PhysicalTypeCategory.ENUMERATION)) {
					return true;
				}
				if (MemberFindingUtils.getAnnotationOfType(details.getAnnotations(), new JavaType("javax.persistence.Enumerated")) != null) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static boolean isApplicationType(JavaType javaType, MetadataService metadataService) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(metadataService, "Metadata service required");
		
		return (metadataService.get(PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA)) != null);
	}
	
	public static Map<JavaSymbolName, DateTimeFormatDetails> getDatePatterns(JavaType javaType, MemberDetails memberDetails, MetadataService metadataService, String metadataIdentificationString, MetadataDependencyRegistry metadataDependencyRegistry) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(memberDetails, "Member details required");
		
		Map<JavaSymbolName, DateTimeFormatDetails> dates = new HashMap<JavaSymbolName, DateTimeFormatDetails>();
		JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataDetails = getJavaTypePersistenceMetadataDetails(javaType, metadataService, metadataIdentificationString, metadataDependencyRegistry);
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
					registerDependency(metadataDependencyRegistry, fieldMetadata.getDeclaredByMetadataId(), metadataIdentificationString);
					dates.put(fieldMetadata.getFieldName(), dateTimeFormat);
					for (String finder : javaTypePersistenceMetadataDetails.getFinderNames()) {
						if (finder.contains(StringUtils.capitalize(fieldMetadata.getFieldName().getSymbolName()) + "Between")) {
							dates.put(new JavaSymbolName("min" + StringUtils.capitalize(fieldMetadata.getFieldName().getSymbolName())), dateTimeFormat);
							dates.put(new JavaSymbolName("max" + StringUtils.capitalize(fieldMetadata.getFieldName().getSymbolName())), dateTimeFormat);
						}
					}
				} else {
					log.warning("It is recommended to use @DateTimeFormat(style=\"S-\") on " + fieldMetadata.getFieldName() + " to use automatic date conversion in Spring MVC");
				}
			}
		}
		return Collections.unmodifiableMap(dates);
	}
	
	public static Set<FinderMetadataDetails> getDynamicFinderMethodsAndFields(JavaType javaType, MemberDetails memberDetails, MetadataService metadataService, String metadataIdentificationString, MetadataDependencyRegistry metadataDependencyRegistry) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(memberDetails, "Member details required");
		
		Set<FinderMetadataDetails> finderMetadataDetails = new HashSet<FinderMetadataDetails>();
		String finderMetadataKey = FinderMetadata.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
		FinderMetadata finderMetadata = (FinderMetadata) metadataService.get(finderMetadataKey);
		if (finderMetadata != null) {
			registerDependency(metadataDependencyRegistry, finderMetadataKey, metadataIdentificationString);
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
	
	private static boolean isTransientFieldType(FieldMetadata field) {
		Assert.notNull(field, "Field metadata required");
		
		return MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.Transient")) != null;
	}
	
	public static boolean isEmbeddedFieldType(FieldMetadata field) {
		Assert.notNull(field, "Field metadata required");
		
		return MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.Embedded")) != null;
	}
	
	private static boolean isPersistenceIdentifierOrVersionMethod(MethodMetadata method, JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataDetails) {
		Assert.notNull(method, "Method metadata required");
		
		return javaTypePersistenceMetadataDetails != null
						&& (method.getMethodName().equals(javaTypePersistenceMetadataDetails.getIdentifierAccessorMethod().getMethodName()) 
						|| (javaTypePersistenceMetadataDetails.getVersionAccessorMethod() != null && method.getMethodName().equals(javaTypePersistenceMetadataDetails.getVersionAccessorMethod().getMethodName())));
	}
	
	public static JavaTypeMetadataDetails getJavaTypeMetadataDetails(JavaType javaType, MetadataService metadataService, TypeLocationService typeLocationService, String metadataIdentificationString, MetadataDependencyRegistry metadataDependencyRegistry) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(metadataService, "Metadata service required");
		registerDependency(metadataDependencyRegistry, PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA), metadataIdentificationString);
		return new JavaTypeMetadataDetails(
				javaType, 
				getPlural(javaType, metadataService, metadataIdentificationString, metadataDependencyRegistry), 
				isEnumType(javaType, metadataService), isApplicationType(javaType, metadataService), 
				getJavaTypePersistenceMetadataDetails(javaType, metadataService, metadataIdentificationString, metadataDependencyRegistry), 
				getControllerPathForType(javaType, metadataService, typeLocationService, metadataIdentificationString, metadataDependencyRegistry));
	}
	
	private static String getControllerPathForType(JavaType type, MetadataService metadataService, TypeLocationService typeLocationService, String metadataIdentificationString, MetadataDependencyRegistry metadataDependencyRegistry) {
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
			registerDependency(metadataDependencyRegistry, webScaffoldMetadataKey, metadataIdentificationString);
			return webScaffoldMetadata.getAnnotationValues().getPath();
		} else {
			return getPlural(type, metadataService, metadataIdentificationString, metadataDependencyRegistry).toLowerCase();
		}
	}
	
	private static void registerDependency(MetadataDependencyRegistry registry, String upstream, String downStream) {
		if (registry != null && StringUtils.hasText(upstream) && StringUtils.hasText(downStream) && !upstream.equals(downStream) && !MetadataIdentificationUtils.getMetadataClass(downStream).equals(MetadataIdentificationUtils.getMetadataClass(upstream))) {
			registry.registerDependency(upstream, downStream);
		}
	}
}
