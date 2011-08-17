package org.springframework.roo.addon.web.mvc.controller.converter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.json.CustomDataJsonTags;
import org.springframework.roo.addon.web.mvc.controller.RooConversionService;
import org.springframework.roo.addon.web.mvc.controller.RooWebScaffold;
import org.springframework.roo.addon.web.mvc.controller.details.WebMetadataService;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldAnnotationValues;
import org.springframework.roo.addon.web.mvc.controller.scaffold.mvc.WebScaffoldMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.layers.LayerService;
import org.springframework.roo.classpath.layers.LayerType;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.Pair;

/**
 * Metadata provider for {@link ConversionServiceMetadata}. Monitors
 * notifications for {@link RooConversionService} and {@link RooWebScaffold}
 * annotated types. Also listens for changes to the scaffolded domain types and
 * their associated domain types.
 * 
 * @author Rossen Stoyanchev
 * @author Stefan Schmidt
 * @since 1.1.1
 */
@Component(immediate = true)
@Service
public final class ConversionServiceMetadataProvider extends AbstractItdMetadataProvider {
	@Reference private TypeLocationService typeLocationService;
	@Reference private WebMetadataService webMetadataService;
	@Reference private PersistenceMemberLocator persistenceMemberLocator;
	@Reference private LayerService layerService;
	
	// Stores the MID (as accepted by this ConversionServiceMetadataProvider) for the one (and only one) application-wide conversion service
	private String applicationConversionServiceFactoryBeanMid;
	
	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		metadataDependencyRegistry.registerDependency(WebScaffoldMetadata.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(new JavaType(RooConversionService.class.getName()));
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		metadataDependencyRegistry.deregisterDependency(WebScaffoldMetadata.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(new JavaType(RooConversionService.class.getName()));
	}

	@Override
	protected String resolveDownstreamDependencyIdentifier(String upstreamDependency) {
		if (MetadataIdentificationUtils.getMetadataClass(upstreamDependency).equals(MetadataIdentificationUtils.getMetadataClass(WebScaffoldMetadata.getMetadataIdentiferType()))) {
			// A WebScaffoldMetadata upstream MID has changed or become available for the first time
			// It's OK to return null if we don't yet know the MID because its JavaType has never been found
			return applicationConversionServiceFactoryBeanMid;
		}

		// It wasn't a WebScaffoldMetadata, so we can let the superclass handle it
		// (it's expected it would be a PhysicalTypeIdentifier notification, as that's the only other thing we registered to receive)
		return super.resolveDownstreamDependencyIdentifier(upstreamDependency);
	}

	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		applicationConversionServiceFactoryBeanMid = metadataIdentificationString;
		
		// To get here we know the governor is the ApplicationConversionServiceFactoryBean so let's go ahead and create its ITD
		Set<JavaType> controllers = typeLocationService.findTypesWithAnnotation(new JavaType(RooWebScaffold.class.getName()));
		Map<JavaType, List<MethodMetadata>> relevantDomainTypes = findDomainTypesRequiringAConverter(metadataIdentificationString, controllers);
		Map<JavaType, Map<Object, JavaSymbolName>> compositePrimaryKeyTypes = findCompositePrimaryKeyTypesRequiringAConverter(metadataIdentificationString, controllers);
		Map<JavaType, MemberTypeAdditions> findMethods = new HashMap<JavaType, MemberTypeAdditions>();
		final Map<JavaType, JavaType> idTypes = new HashMap<JavaType, JavaType>();
		
		for (final Iterator<JavaType> types = relevantDomainTypes.keySet().iterator(); types.hasNext();) {
			final JavaType type = types.next();
			final JavaType idType = persistenceMemberLocator.getIdentifierType(type);
			if (idType == null) {
				// This type either has no ID field (e.g. an embedded type) or it's ID type is unknown right now;
				// don't generate a converter for it; this will happen later if and when the ID field becomes known.
				types.remove();
			} else {
				idTypes.put(type, idType);
				@SuppressWarnings("unchecked")
				final MemberTypeAdditions findMethod = layerService.getMemberTypeAdditions(metadataIdentificationString, PersistenceCustomDataKeys.FIND_METHOD.name(), type, idType, LayerType.HIGHEST.getPosition(), new Pair<JavaType, JavaSymbolName>(idType, new JavaSymbolName("id")));
				findMethods.put(type, findMethod);
			}
		}
		
		return new ConversionServiceMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, findMethods, idTypes, relevantDomainTypes, compositePrimaryKeyTypes);
	}
	
	private Map<JavaType, List<MethodMetadata>> findDomainTypesRequiringAConverter(String metadataIdentificationString, Set<JavaType> controllers) {
		Map<JavaType, List<MethodMetadata>> relevantDomainTypes = new LinkedHashMap<JavaType, List<MethodMetadata>>();
		for (JavaType controller : controllers) {
			PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(controller, Path.SRC_MAIN_JAVA));
			Assert.notNull(physicalTypeMetadata, "Unable to obtain physical type metadata for type " + controller.getFullyQualifiedTypeName());
			WebScaffoldAnnotationValues webScaffoldAnnotationValues = new WebScaffoldAnnotationValues(physicalTypeMetadata);
			if (webScaffoldAnnotationValues.getFormBackingObject() == null) {
				continue;
			}
			Map<JavaType, List<MethodMetadata>> relevantTypes = findRelevantTypes(webScaffoldAnnotationValues.getFormBackingObject(), metadataIdentificationString);
			relevantDomainTypes.putAll(relevantTypes);
		}
		return relevantDomainTypes;
	}
	
	private Map<JavaType, Map<Object, JavaSymbolName>> findCompositePrimaryKeyTypesRequiringAConverter(String metadataIdentificationString, Set<JavaType> controllers) {
		Map<JavaType, Map<Object, JavaSymbolName>> types = new TreeMap<JavaType, Map<Object,JavaSymbolName>>();
		for (JavaType controller : controllers) {
			PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(controller, Path.SRC_MAIN_JAVA));
			Assert.notNull(physicalTypeMetadata, "Unable to obtain physical type metadata for type " + controller.getFullyQualifiedTypeName());
			WebScaffoldAnnotationValues webScaffoldAnnotationValues = new WebScaffoldAnnotationValues(physicalTypeMetadata);
			JavaType formBackingObject = webScaffoldAnnotationValues.getFormBackingObject();
			if (formBackingObject == null) {
				continue;
			}
			MemberDetails memberDetails = getMemberDetails(formBackingObject);
			List<FieldMetadata> embeddedIdFields = MemberFindingUtils.getFieldsWithTag(memberDetails, PersistenceCustomDataKeys.EMBEDDED_ID_FIELD);
			if (embeddedIdFields.size() > 1) {
				throw new IllegalStateException("Found multiple embedded ID fields in " + formBackingObject.getFullyQualifiedTypeName() + " type. Only one is allowed.");
			} else if (embeddedIdFields.size() == 1) {
				Map<Object, JavaSymbolName> jsonMethodNames = new LinkedHashMap<Object, JavaSymbolName>();
				MemberDetails fieldMemberDetails = getMemberDetails(embeddedIdFields.get(0).getFieldType());
				MethodMetadata fromJsonMethod = MemberFindingUtils.getMostConcreteMethodWithTag(fieldMemberDetails, CustomDataJsonTags.FROM_JSON_METHOD);
				if (fromJsonMethod != null) {
					jsonMethodNames.put(CustomDataJsonTags.FROM_JSON_METHOD, fromJsonMethod.getMethodName());
					MethodMetadata toJsonMethod = MemberFindingUtils.getMostConcreteMethodWithTag(fieldMemberDetails, CustomDataJsonTags.TO_JSON_METHOD);
					if (toJsonMethod != null) {
						jsonMethodNames.put(CustomDataJsonTags.TO_JSON_METHOD, toJsonMethod.getMethodName());
						types.put(embeddedIdFields.get(0).getFieldType(), jsonMethodNames);
					}
				}
			}
		}
		return types;
	}

	private Map<JavaType, List<MethodMetadata>> findRelevantTypes(JavaType type, String metadataIdentificationString) {
		final MemberDetails memberDetails = getMemberDetails(type);
		if (memberDetails == null) {
			return Collections.emptyMap();
		}
		
		Map<JavaType, List<MethodMetadata>> types = new LinkedHashMap<JavaType, List<MethodMetadata>>();
		List<MethodMetadata> locatedAccessors = new LinkedList<MethodMetadata>();
		
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.createIdentifier(type, Path.SRC_MAIN_JAVA), metadataIdentificationString);
		int counter = 0;
		for (MethodMetadata method : MemberFindingUtils.getMethods(memberDetails)) {
			// Track any changes to that method (eg it goes away)
			metadataDependencyRegistry.registerDependency(method.getDeclaredByMetadataId(), metadataIdentificationString);

			if (counter < 4 && isMethodOfInterest(method, memberDetails)) {
				counter++;
				locatedAccessors.add(method);
			} 
		}

		if (!locatedAccessors.isEmpty()) {
			types.put(type, locatedAccessors);
		}
		return types;
	}
	
	private boolean isMethodOfInterest(MethodMetadata method, MemberDetails memberDetails) {
		if (!BeanInfoUtils.isAccessorMethod(method)) {
			return false; // Only interested in accessors
		}
		if (method.getCustomData().keySet().contains(PersistenceCustomDataKeys.IDENTIFIER_ACCESSOR_METHOD) || method.getCustomData().keySet().contains(PersistenceCustomDataKeys.VERSION_ACCESSOR_METHOD)) {
			return false; // Only interested in methods which are not accessors for persistence version or id fields
		}
		FieldMetadata field = BeanInfoUtils.getFieldForPropertyName(memberDetails, BeanInfoUtils.getPropertyNameForJavaBeanMethod(method));
		if (field == null) {
			return false;
		}
		JavaType fieldType = field.getFieldType();
		if (fieldType.isCommonCollectionType() || fieldType.isArray() // Exclude collections and arrays
				|| webMetadataService.isApplicationType(fieldType) // Exclude references to other domain objects as they are too verbose
				|| fieldType.equals(JavaType.BOOLEAN_PRIMITIVE) || fieldType.equals(JavaType.BOOLEAN_OBJECT) // Exclude boolean values as they would not be meaningful in this presentation
				|| field.getCustomData().keySet().contains(PersistenceCustomDataKeys.EMBEDDED_FIELD) /* Not interested in embedded types */) {
			return false;
		}
		return true;
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "ConversionService";
	}

	public String getProvidesType() {
		return MetadataIdentificationUtils.create(ConversionServiceMetadata.class.getName());
	}

	@Override
	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(ConversionServiceMetadata.class.getName(), javaType, path);
	}

	@Override
	protected String getGovernorPhysicalTypeIdentifier(String metadataId) {
		JavaType javaType = PhysicalTypeIdentifierNamingUtils.getJavaType(ConversionServiceMetadata.class.getName(), metadataId);
		Path path = PhysicalTypeIdentifierNamingUtils.getPath(ConversionServiceMetadata.class.getName(), metadataId);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}
}