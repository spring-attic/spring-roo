package org.springframework.roo.addon.layers.service;

import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.PUBLIC;
import static org.springframework.roo.classpath.PhysicalTypeCategory.CLASS;
import static org.springframework.roo.model.RooJavaType.ROO_PERMISSION_EVALUATOR;
import static org.springframework.roo.model.RooJavaType.ROO_SERVICE_PERMISSION;
import static org.springframework.roo.model.SpringJavaType.AUTHENTICATION;
import static org.springframework.roo.model.SpringJavaType.COMPONENT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.layers.LayerService;
import org.springframework.roo.classpath.layers.LayerType;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.layers.MethodParameter;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;

/**
 * Provides {@link ServiceClassMetadata} for building the ITD for the
 * implementation class of a user project's service.
 * 
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component(immediate = true)
@Service
public class ServiceClassMetadataProvider extends
        AbstractMemberDiscoveringItdMetadataProvider {
    private static final int LAYER_POSITION = LayerType.SERVICE.getPosition();

    @Reference private LayerService layerService;
    @Reference private TypeManagementService typeManagementService;
    @Reference private PathResolver pathResolver;

    private final Map<JavaType, String> managedEntityTypes = new HashMap<JavaType, String>();

    protected void activate(final ComponentContext context) {
        metadataDependencyRegistry.addNotificationListener(this);
        metadataDependencyRegistry.registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        setIgnoreTriggerAnnotations(true);
    }

    @Override
    protected String createLocalIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return ServiceClassMetadata.createIdentifier(javaType, path);
    }

    protected void deactivate(final ComponentContext context) {
        metadataDependencyRegistry.removeNotificationListener(this);
        metadataDependencyRegistry.deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
    }

    @Override
    protected String getGovernorPhysicalTypeIdentifier(
            final String metadataIdentificationString) {
        final JavaType javaType = ServiceClassMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath path = ServiceClassMetadata
                .getPath(metadataIdentificationString);
        return PhysicalTypeIdentifier.createIdentifier(javaType, path);
    }

    public String getItdUniquenessFilenameSuffix() {
        return "Service";
    }

    @Override
    protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
        // Determine the governor for this ITD, and whether any metadata is even
        // hoping to hear about changes to that JavaType and its ITDs
        final JavaType governor = itdTypeDetails.getName();
        final String localMid = managedEntityTypes.get(governor);
        if (localMid != null) {
            return localMid;
        }

        final MemberHoldingTypeDetails memberHoldingTypeDetails = typeLocationService
                .getTypeDetails(governor);
        if (memberHoldingTypeDetails != null) {
            for (final JavaType type : memberHoldingTypeDetails
                    .getLayerEntities()) {
                final String localMidType = managedEntityTypes.get(type);
                if (localMidType != null) {
                    return localMidType;
                }
            }
        }
        return null;
    }

    @Override
    protected ItdTypeDetailsProvidingMetadataItem getMetadata(
            final String metadataIdentificationString,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final String itdFilename) {
        final ClassOrInterfaceTypeDetails serviceClass = governorPhysicalTypeMetadata
                .getMemberHoldingTypeDetails();
        if (serviceClass == null) {
            return null;
        }

        ServiceInterfaceMetadata serviceInterfaceMetadata = null;
        ClassOrInterfaceTypeDetails serviceInterface = null;
        for (final JavaType implementedType : serviceClass.getImplementsTypes()) {
            final ClassOrInterfaceTypeDetails potentialServiceInterfaceTypeDetails = typeLocationService
                    .getTypeDetails(implementedType);
            if (potentialServiceInterfaceTypeDetails != null) {
                final LogicalPath path = PhysicalTypeIdentifier
                        .getPath(potentialServiceInterfaceTypeDetails
                                .getDeclaredByMetadataId());
                final String implementedTypeId = ServiceInterfaceMetadata
                        .createIdentifier(implementedType, path);
                if ((serviceInterfaceMetadata = (ServiceInterfaceMetadata) metadataService
                        .get(implementedTypeId)) != null) {
                    // Found the metadata for the service interface
                    serviceInterface = potentialServiceInterfaceTypeDetails;
                    break;
                }
            }
        }
        if (serviceInterface == null || serviceInterfaceMetadata == null
                || !serviceInterfaceMetadata.isValid()) {
            return null;
        }

        // Register this provider for changes to the service interface // TODO
        // move this down in case we return null early below?
        metadataDependencyRegistry.registerDependency(
                serviceInterfaceMetadata.getId(), metadataIdentificationString);

        final ServiceAnnotationValues serviceAnnotationValues = serviceInterfaceMetadata
                .getServiceAnnotationValues();
        final JavaType[] domainTypes = serviceAnnotationValues.getDomainTypes();
        if (domainTypes == null) {
            return null;
        }

        /*
         * For each domain type, collect (1) the plural and (2) the additions to
         * make to the service class for calling a lower layer when implementing
         * each service layer method. We use LinkedHashMaps for the latter
         * nested map to ensure repeatable order of code generation.
         */
        final Map<JavaType, String> domainTypePlurals = new HashMap<JavaType, String>();
        final Map<JavaType, JavaType> domainTypeToIdTypeMap = new HashMap<JavaType, JavaType>();
        // Collect the additions for each method for each supported domain type
        final Map<JavaType, Map<ServiceLayerMethod, MemberTypeAdditions>> allCrudAdditions = new LinkedHashMap<JavaType, Map<ServiceLayerMethod, MemberTypeAdditions>>();
        for (final JavaType domainType : domainTypes) {

            final JavaType idType = persistenceMemberLocator
                    .getIdentifierType(domainType);
            if (idType == null) {
                return null;
            }
            domainTypeToIdTypeMap.put(domainType, idType);
            // Collect the plural for this domain type

            final ClassOrInterfaceTypeDetails domainTypeDetails = typeLocationService
                    .getTypeDetails(domainType);
            if (domainTypeDetails == null) {
                return null;
            }
            final LogicalPath path = PhysicalTypeIdentifier
                    .getPath(domainTypeDetails.getDeclaredByMetadataId());
            final String pluralId = PluralMetadata.createIdentifier(domainType,
                    path);
            final PluralMetadata pluralMetadata = (PluralMetadata) metadataService
                    .get(pluralId);
            if (pluralMetadata == null) {
                return null;
            }
            domainTypePlurals.put(domainType, pluralMetadata.getPlural());

            // Maintain a list of entities that are being handled by this layer
            managedEntityTypes.put(domainType, metadataIdentificationString);

            // Collect the additions the service class needs in order to invoke
            // each service layer method
            final Map<ServiceLayerMethod, MemberTypeAdditions> methodAdditions = new LinkedHashMap<ServiceLayerMethod, MemberTypeAdditions>();
            for (final ServiceLayerMethod method : ServiceLayerMethod.values()) {
                final Collection<MethodParameter> methodParameters = MethodParameter
                        .asList(method.getParameters(domainType, idType));
                final MemberTypeAdditions memberTypeAdditions = layerService
                        .getMemberTypeAdditions(metadataIdentificationString,
                                method.getKey(), domainType, idType,
                                LAYER_POSITION, methodParameters);
                if (memberTypeAdditions != null) {
                    // A lower layer implements this method
                    methodAdditions.put(method, memberTypeAdditions);
                }
            }
            allCrudAdditions.put(domainType, methodAdditions);

            // Register this provider for changes to the domain type or its
            // plural
            metadataDependencyRegistry.registerDependency(
                    domainTypeDetails.getDeclaredByMetadataId(),
                    metadataIdentificationString);
            metadataDependencyRegistry.registerDependency(pluralId,
                    metadataIdentificationString);
        }

        if (serviceAnnotationValues.usePermissionEvaluator()) {
            createPermissions(serviceClass, serviceInterface,
                    serviceAnnotationValues, domainTypes);
        }

        final MemberDetails serviceClassDetails = memberDetailsScanner
                .getMemberDetails(getClass().getName(), serviceClass);
        return new ServiceClassMetadata(metadataIdentificationString,
                aspectName, governorPhysicalTypeMetadata, serviceClassDetails,
                serviceAnnotationValues, domainTypeToIdTypeMap,
                allCrudAdditions, domainTypePlurals, serviceInterface.getName()
                        .getSimpleTypeName());

    }

    public String getProvidesType() {
        return ServiceClassMetadata.getMetadataIdentiferType();
    }

    private void createPermissions(ClassOrInterfaceTypeDetails serviceClass,
            ClassOrInterfaceTypeDetails serviceInterface,
            ServiceAnnotationValues serviceAnnotationValues,
            JavaType[] domainTypes) {
        Set<ClassOrInterfaceTypeDetails> classes = typeLocationService
                .findClassesOrInterfaceDetailsWithAnnotation(ROO_PERMISSION_EVALUATOR);

        Validate.isTrue(classes.size() != 0,
                "Roo Permission Evaluator is required");
        Validate.isTrue(classes.size() == 1,
                "More than one Roo Permission Evaluator is has been found");

        ClassOrInterfaceTypeDetails permissionEvaluator = classes.iterator()
                .next();
        ClassOrInterfaceTypeDetails servicePermission = null;

        for (final ClassOrInterfaceTypeDetails cid : typeLocationService
                .findClassesOrInterfaceDetailsWithAnnotation(ROO_SERVICE_PERMISSION)) {
            final AnnotationMetadata annotationMetadata = cid
                    .getAnnotation(ROO_SERVICE_PERMISSION);
            if (annotationMetadata != null) {
                final AnnotationAttributeValue<String> attributeValue = annotationMetadata
                        .getAttribute("value");
                final String value = attributeValue.getValue();
                if (serviceClass.getName().getFullyQualifiedTypeName()
                        .equals(value)) {
                    servicePermission = cid;
                }
            }
        }

        if (servicePermission == null) {
            final String packageName = permissionEvaluator.getName()
                    .getPackage().getFullyQualifiedPackageName();
            final JavaType classType = new JavaType(packageName + "."
                    + serviceInterface.getName().getSimpleTypeName()
                    + "Permissions");
            final String classIdentifier = pathResolver
                    .getFocusedCanonicalPath(Path.SRC_MAIN_JAVA, classType);
            final String classMid = PhysicalTypeIdentifier.createIdentifier(
                    classType, pathResolver.getPath(classIdentifier));
            final ClassOrInterfaceTypeDetailsBuilder classTypeBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                    classMid, PUBLIC, classType, CLASS);

            final AnnotationMetadataBuilder annotationMetadata = new AnnotationMetadataBuilder(
                    ROO_SERVICE_PERMISSION);
            annotationMetadata.addStringAttribute("value", serviceInterface
                    .getName().getFullyQualifiedTypeName());
            classTypeBuilder.addAnnotation(annotationMetadata.build());

            final InvocableMemberBodyBuilder publicMethodBodyBuilder = new InvocableMemberBodyBuilder();

            for (JavaType domainType : domainTypes) {
                for (final ServiceLayerMethod method : ServiceLayerMethod
                        .values()) {
                    List<JavaType> parameterTypes = new ArrayList<JavaType>();
                    parameterTypes.add(AUTHENTICATION);

                    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
                    parameterNames.add(new JavaSymbolName("authentication"));

                    JavaType idType = persistenceMemberLocator
                            .getIdentifierType(domainType);
                    List<JavaSymbolName> methodParameterNames = method
                            .getParameterNames(domainType, idType);

                    if (methodParameterNames.size() > 0) {
                        parameterTypes.add(JavaType.OBJECT);
                        parameterNames.add(methodParameterNames.get(0));
                    }

                    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
                    bodyBuilder.append("return true;");
                    final ClassOrInterfaceTypeDetails domainTypeDetails = typeLocationService
                            .getTypeDetails(domainType);
                    if (domainTypeDetails == null) {
                        return;
                    }
                    final LogicalPath path = PhysicalTypeIdentifier
                            .getPath(domainTypeDetails
                                    .getDeclaredByMetadataId());
                    final String pluralId = PluralMetadata.createIdentifier(
                            domainType, path);
                    final PluralMetadata pluralMetadata = (PluralMetadata) metadataService
                            .get(pluralId);
                    if (pluralMetadata == null) {
                        return;
                    }
                    JavaSymbolName methodName = method.getSymbolPermissionName(
                            serviceAnnotationValues, domainType,
                            pluralMetadata.getPlural());
                    MethodMetadataBuilder methodMetadataBuilder = new MethodMetadataBuilder(
                            classMid, PRIVATE, methodName,
                            JavaType.BOOLEAN_PRIMITIVE,
                            AnnotatedJavaType
                                    .convertFromJavaTypes(parameterTypes),
                            parameterNames, bodyBuilder);
                    classTypeBuilder.addMethod(methodMetadataBuilder.build());

                    publicMethodBodyBuilder.append("if(permission.equals(\""
                            + methodName + "\"))\n");
                    publicMethodBodyBuilder.append("return " + methodName
                            + "(authentication");

                    if (parameterNames.size() > 1) {
                        publicMethodBodyBuilder.append(", ");
                        publicMethodBodyBuilder.append("targetObject");
                    }

                    publicMethodBodyBuilder.append(");\n\n");
                }

            }

            publicMethodBodyBuilder.append("return false;");

            List<JavaType> publicMethodParameterTypes = new ArrayList<JavaType>();
            publicMethodParameterTypes.add(AUTHENTICATION);
            publicMethodParameterTypes.add(JavaType.OBJECT);
            publicMethodParameterTypes.add(JavaType.OBJECT);

            List<JavaSymbolName> publicMethodParameterNames = new ArrayList<JavaSymbolName>();
            publicMethodParameterNames
                    .add(new JavaSymbolName("authentication"));
            publicMethodParameterNames.add(new JavaSymbolName("targetObject"));
            publicMethodParameterNames.add(new JavaSymbolName("permission"));

            MethodMetadataBuilder publicMethodMetadataBuilder = new MethodMetadataBuilder(
                    classMid, PUBLIC, new JavaSymbolName("isAllowed"),
                    JavaType.BOOLEAN_PRIMITIVE,
                    AnnotatedJavaType
                            .convertFromJavaTypes(publicMethodParameterTypes),
                    publicMethodParameterNames, publicMethodBodyBuilder);

            classTypeBuilder.addMethod(publicMethodMetadataBuilder.build());

            classTypeBuilder.addAnnotation(new AnnotationMetadataBuilder(
                    COMPONENT).build());

            typeManagementService.createOrUpdateTypeOnDisk(classTypeBuilder
                    .build());
        }
    }
}
