package org.springframework.roo.addon.web.mvc.controller.converter;

import static org.springframework.roo.model.RooJavaType.ROO_CONVERSION_SERVICE;
import static org.springframework.roo.model.RooJavaType.ROO_WEB_SCAFFOLD;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.json.CustomDataJsonTags;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldAnnotationValues;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.layers.LayerService;
import org.springframework.roo.classpath.layers.LayerType;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.layers.MethodParameter;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Implementation of {@link ConversionServiceMetadataProvider}.
 * 
 * @author Rossen Stoyanchev
 * @author Stefan Schmidt
 * @since 1.1.1
 */
@Component(immediate = true)
@Service
public class ConversionServiceMetadataProviderImpl extends
        AbstractItdMetadataProvider implements
        ConversionServiceMetadataProvider {

    // Stores the MID (as accepted by this
    // ConversionServiceMetadataProviderImpl) for the one (and only one)
    // application-wide conversion service
    private String applicationConversionServiceFactoryBeanMid;

    @Reference private LayerService layerService;

    protected void activate(final ComponentContext context) {
        metadataDependencyRegistry.registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        metadataDependencyRegistry.registerDependency(
                WebScaffoldMetadata.getMetadataIdentiferType(),
                getProvidesType());
        addMetadataTrigger(ROO_CONVERSION_SERVICE);
    }

    @Override
    protected String createLocalIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return PhysicalTypeIdentifierNamingUtils.createIdentifier(
                ConversionServiceMetadata.class.getName(), javaType, path);
    }

    protected void deactivate(final ComponentContext context) {
        metadataDependencyRegistry.deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        metadataDependencyRegistry.deregisterDependency(
                WebScaffoldMetadata.getMetadataIdentiferType(),
                getProvidesType());
        removeMetadataTrigger(ROO_CONVERSION_SERVICE);
    }

    @Override
    protected String getGovernorPhysicalTypeIdentifier(final String metadataId) {
        final JavaType javaType = PhysicalTypeIdentifierNamingUtils
                .getJavaType(ConversionServiceMetadata.class.getName(),
                        metadataId);
        final LogicalPath path = PhysicalTypeIdentifierNamingUtils.getPath(
                ConversionServiceMetadata.class.getName(), metadataId);
        return PhysicalTypeIdentifier.createIdentifier(javaType, path);
    }

    public String getItdUniquenessFilenameSuffix() {
        return "ConversionService";
    }

    @Override
    protected ItdTypeDetailsProvidingMetadataItem getMetadata(
            final String metadataIdentificationString,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final String itdFilename) {
        applicationConversionServiceFactoryBeanMid = metadataIdentificationString;

        // To get here we know the governor is the
        // ApplicationConversionServiceFactoryBean so let's go ahead and create
        // its ITD
        final Map<JavaType, Map<Object, JavaSymbolName>> compositePrimaryKeyTypes = new HashMap<JavaType, Map<Object, JavaSymbolName>>();
        final Set<JavaType> relevantDomainTypes = new LinkedHashSet<JavaType>();
        final Map<JavaType, MemberTypeAdditions> findMethods = new HashMap<JavaType, MemberTypeAdditions>();
        final Map<JavaType, JavaType> idTypes = new HashMap<JavaType, JavaType>();
        final Map<JavaType, List<MethodMetadata>> toStringMethods = new HashMap<JavaType, List<MethodMetadata>>();

        for (final ClassOrInterfaceTypeDetails controllerTypeDetails : typeLocationService
                .findClassesOrInterfaceDetailsWithAnnotation(ROO_WEB_SCAFFOLD)) {
            metadataDependencyRegistry.registerDependency(
                    controllerTypeDetails.getDeclaredByMetadataId(),
                    metadataIdentificationString);

            final WebScaffoldAnnotationValues webScaffoldAnnotationValues = new WebScaffoldAnnotationValues(
                    controllerTypeDetails);
            final JavaType formBackingObject = webScaffoldAnnotationValues
                    .getFormBackingObject();
            if (formBackingObject == null) {
                continue;
            }
            final MemberDetails memberDetails = getMemberDetails(formBackingObject);

            // Find composite primary key types requiring a converter
            final List<FieldMetadata> embeddedIdFields = MemberFindingUtils
                    .getFieldsWithTag(memberDetails,
                            CustomDataKeys.EMBEDDED_ID_FIELD);
            if (embeddedIdFields.size() > 1) {
                throw new IllegalStateException(
                        "Found multiple embedded ID fields in "
                                + formBackingObject.getFullyQualifiedTypeName()
                                + " type. Only one is allowed.");
            }
            else if (embeddedIdFields.size() == 1) {
                final Map<Object, JavaSymbolName> jsonMethodNames = new LinkedHashMap<Object, JavaSymbolName>();
                final MemberDetails fieldMemberDetails = getMemberDetails(embeddedIdFields
                        .get(0).getFieldType());
                final MethodMetadata fromJsonMethod = MemberFindingUtils
                        .getMostConcreteMethodWithTag(fieldMemberDetails,
                                CustomDataJsonTags.FROM_JSON_METHOD);
                if (fromJsonMethod != null) {
                    jsonMethodNames.put(CustomDataJsonTags.FROM_JSON_METHOD,
                            fromJsonMethod.getMethodName());
                    final MethodMetadata toJsonMethod = MemberFindingUtils
                            .getMostConcreteMethodWithTag(fieldMemberDetails,
                                    CustomDataJsonTags.TO_JSON_METHOD);
                    if (toJsonMethod != null) {
                        jsonMethodNames.put(CustomDataJsonTags.TO_JSON_METHOD,
                                toJsonMethod.getMethodName());
                        compositePrimaryKeyTypes.put(embeddedIdFields.get(0)
                                .getFieldType(), jsonMethodNames);
                    }
                }
            }

            final JavaType identifierType = persistenceMemberLocator
                    .getIdentifierType(formBackingObject);
            if (identifierType == null) {
                // This type either has no ID field (e.g. an embedded type) or
                // it's ID type is unknown right now;
                // don't generate a converter for it; this will happen later if
                // and when the ID field becomes known.
                continue;
            }

            relevantDomainTypes.add(formBackingObject);
            idTypes.put(formBackingObject, identifierType);
            final MemberTypeAdditions findMethod = layerService
                    .getMemberTypeAdditions(metadataIdentificationString,
                            CustomDataKeys.FIND_METHOD.name(),
                            formBackingObject, identifierType,
                            LayerType.HIGHEST.getPosition(),
                            new MethodParameter(identifierType, "id"));
            findMethods.put(formBackingObject, findMethod);
            toStringMethods.put(
                    formBackingObject,
                    getToStringMethods(memberDetails,
                            metadataIdentificationString));
        }

        return new ConversionServiceMetadata(metadataIdentificationString,
                aspectName, governorPhysicalTypeMetadata, findMethods, idTypes,
                relevantDomainTypes, compositePrimaryKeyTypes, toStringMethods);
    }

    public String getProvidesType() {
        return MetadataIdentificationUtils
                .create(ConversionServiceMetadata.class.getName());
    }

    private List<MethodMetadata> getToStringMethods(
            final MemberDetails memberDetails,
            final String metadataIdentificationString) {
        final List<MethodMetadata> toStringMethods = new ArrayList<MethodMetadata>();

        int counter = 0;
        for (final MethodMetadata method : memberDetails.getMethods()) {
            // Track any changes to that method (eg it goes away)
            metadataDependencyRegistry.registerDependency(
                    method.getDeclaredByMetadataId(),
                    metadataIdentificationString);

            if (counter < 4 && isMethodOfInterest(method, memberDetails)) {
                counter++;
                toStringMethods.add(method);
            }
        }

        return toStringMethods;
    }

    private boolean isMethodOfInterest(final MethodMetadata method,
            final MemberDetails memberDetails) {
        if (!BeanInfoUtils.isAccessorMethod(method)) {
            return false; // Only interested in accessors
        }
        if (method.getCustomData().keySet()
                .contains(CustomDataKeys.IDENTIFIER_ACCESSOR_METHOD)
                || method.getCustomData().keySet()
                        .contains(CustomDataKeys.VERSION_ACCESSOR_METHOD)) {
            return false; // Only interested in methods which are not accessors
                          // for persistence id or version fields
        }

        final FieldMetadata field = BeanInfoUtils.getFieldForJavaBeanMethod(
                memberDetails, method);
        if (field == null) {
            return false;
        }
        final JavaType fieldType = field.getFieldType();
        if (fieldType.isCommonCollectionType()
                || fieldType.isArray() // Exclude collections and arrays
                || typeLocationService.isInProject(fieldType) // Exclude
                                                              // references to
                                                              // other domain
                                                              // objects as they
                                                              // are too verbose
                || fieldType.equals(JavaType.BOOLEAN_PRIMITIVE)
                || fieldType.equals(JavaType.BOOLEAN_OBJECT) // Exclude boolean
                                                             // values as they
                                                             // would not be
                                                             // meaningful in
                                                             // this
                                                             // presentation
                || field.getCustomData().keySet()
                        .contains(CustomDataKeys.EMBEDDED_FIELD) /*
                                                                  * Not
                                                                  * interested
                                                                  * in embedded
                                                                  * types
                                                                  */) {
            return false;
        }
        return true;
    }

    @Override
    protected String resolveDownstreamDependencyIdentifier(
            final String upstreamDependency) {
        if (MetadataIdentificationUtils.getMetadataClass(upstreamDependency)
                .equals(MetadataIdentificationUtils
                        .getMetadataClass(WebScaffoldMetadata
                                .getMetadataIdentiferType()))) {
            // A WebScaffoldMetadata upstream MID has changed or become
            // available for the first time
            // It's OK to return null if we don't yet know the MID because its
            // JavaType has never been found
            return applicationConversionServiceFactoryBeanMid;
        }

        // It wasn't a WebScaffoldMetadata, so we can let the superclass handle
        // it
        // (it's expected it would be a PhysicalTypeIdentifier notification, as
        // that's the only other thing we registered to receive)
        return super.resolveDownstreamDependencyIdentifier(upstreamDependency);
    }
}