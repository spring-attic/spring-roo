package org.springframework.roo.addon.web.mvc.controller.converter;

import static org.springframework.roo.model.RooJavaType.ROO_CONVERSION_SERVICE;
import static org.springframework.roo.model.RooJavaType.ROO_WEB_SCAFFOLD;

import java.util.HashMap;
import java.util.LinkedHashMap;
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
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldAnnotationValues;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.layers.LayerService;
import org.springframework.roo.classpath.layers.LayerType;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.layers.MethodParameter;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;

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
	
	// Fields
	@Reference private LayerService layerService;
	@Reference private PersistenceMemberLocator persistenceMemberLocator;
	@Reference private TypeLocationService typeLocationService;
	
	// Stores the MID (as accepted by this ConversionServiceMetadataProvider) for the one (and only one) application-wide conversion service
	private String applicationConversionServiceFactoryBeanMid;
	
	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		metadataDependencyRegistry.registerDependency(WebScaffoldMetadata.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(ROO_CONVERSION_SERVICE);
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		metadataDependencyRegistry.deregisterDependency(WebScaffoldMetadata.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(ROO_CONVERSION_SERVICE);
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
		Set<JavaType> controllers = typeLocationService.findTypesWithAnnotation(ROO_WEB_SCAFFOLD);
//		Map<JavaType, String> relevantDomainTypes = findDomainTypesRequiringAConverter(metadataIdentificationString, controllers);
		Map<JavaType, Map<Object, JavaSymbolName>> compositePrimaryKeyTypes = findCompositePrimaryKeyTypesRequiringAConverter(controllers);
		Map<JavaType, MemberTypeAdditions> findMethods = new HashMap<JavaType, MemberTypeAdditions>();
		final Map<JavaType, JavaType> idTypes = new HashMap<JavaType, JavaType>();
		
		
		Map<JavaType, String> relevantDomainTypes = new LinkedHashMap<JavaType, String>();
		for (JavaType controller : controllers) {
			PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(controller, Path.SRC_MAIN_JAVA));
			Assert.notNull(physicalTypeMetadata, "Unable to obtain physical type metadata for type " + controller.getFullyQualifiedTypeName());
			WebScaffoldAnnotationValues webScaffoldAnnotationValues = new WebScaffoldAnnotationValues(physicalTypeMetadata);
			final JavaType formBackingObject = webScaffoldAnnotationValues.getFormBackingObject();
			if (formBackingObject == null) {
				continue;
			}
			final JavaType identifierType = persistenceMemberLocator.getIdentifierType(formBackingObject);
			if (identifierType == null) {
				// This type either has no ID field (e.g. an embedded type) or it's ID type is unknown right now;
				// don't generate a converter for it; this will happen later if and when the ID field becomes known.
				continue;
			}

			relevantDomainTypes.put(formBackingObject, getDisplayMethod(formBackingObject));
			idTypes.put(formBackingObject, identifierType);
			final MemberTypeAdditions findMethod = layerService.getMemberTypeAdditions(metadataIdentificationString, CustomDataKeys.FIND_METHOD.name(), formBackingObject, identifierType, LayerType.HIGHEST.getPosition(), new MethodParameter(identifierType, "id"));
			findMethods.put(formBackingObject, findMethod);
		}
		
		return new ConversionServiceMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, findMethods, idTypes, relevantDomainTypes, compositePrimaryKeyTypes);
	}
	
	private Map<JavaType, Map<Object, JavaSymbolName>> findCompositePrimaryKeyTypesRequiringAConverter(Set<JavaType> controllers) {
		Map<JavaType, Map<Object, JavaSymbolName>> types = new TreeMap<JavaType, Map<Object,JavaSymbolName>>();
		for (JavaType controller : controllers) {
			PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(controller, Path.SRC_MAIN_JAVA));
			Assert.notNull(physicalTypeMetadata, "Unable to obtain physical type metadata for type " + controller.getFullyQualifiedTypeName());
			WebScaffoldAnnotationValues webScaffoldAnnotationValues = new WebScaffoldAnnotationValues(physicalTypeMetadata);
			JavaType formBackingObject = webScaffoldAnnotationValues.getFormBackingObject();
			if (formBackingObject == null) {
				continue;
			}
			final MemberDetails memberDetails = getMemberDetails(formBackingObject);
			final List<FieldMetadata> embeddedIdFields = MemberFindingUtils.getFieldsWithTag(memberDetails, CustomDataKeys.EMBEDDED_ID_FIELD);
			if (embeddedIdFields.size() > 1) {
				throw new IllegalStateException("Found multiple embedded ID fields in " + formBackingObject.getFullyQualifiedTypeName() + " type. Only one is allowed.");
			} else if (embeddedIdFields.size() == 1) {
				final Map<Object, JavaSymbolName> jsonMethodNames = new LinkedHashMap<Object, JavaSymbolName>();
				final MemberDetails fieldMemberDetails = getMemberDetails(embeddedIdFields.get(0).getFieldType());
				final MethodMetadata fromJsonMethod = MemberFindingUtils.getMostConcreteMethodWithTag(fieldMemberDetails, CustomDataJsonTags.FROM_JSON_METHOD);
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

	private String getDisplayMethod(final JavaType formBackingObject) {
		final MemberDetails memberDetails = getMemberDetails(formBackingObject);
		String displayMethod = "toString()";

		final MethodMetadata displayNameMethod = memberDetails.getMostConcreteMethodWithTag(CustomDataKeys.DISPLAY_NAME_METHOD);
		if (displayNameMethod != null) {
			displayMethod = displayNameMethod.getMethodName().getSymbolName() + "()";
		} else {
			final JavaSymbolName methodName = new JavaSymbolName("getDisplayName");
			MethodMetadata method = memberDetails.getMethod(methodName);
			if (method != null) {
				displayMethod = methodName.getSymbolName() + "()";
			} else {
				final MethodMetadata identifierAccessor = persistenceMemberLocator.getIdentifierAccessor(formBackingObject);
				if (identifierAccessor != null) {
					displayMethod = identifierAccessor.getMethodName().getSymbolName() + "()." + displayMethod;
				}
			}
		}
		return displayMethod;
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