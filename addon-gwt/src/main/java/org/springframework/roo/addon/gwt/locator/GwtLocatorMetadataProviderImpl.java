package org.springframework.roo.addon.gwt.locator;

import static org.springframework.roo.addon.gwt.GwtJavaType.LOCATOR;
import static org.springframework.roo.model.JavaType.CLASS;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.gwt.GwtTypeService;
import org.springframework.roo.addon.gwt.GwtUtils;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.layers.LayerService;
import org.springframework.roo.classpath.layers.LayerType;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.layers.MethodParameter;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectOperations;

@Component(immediate = true)
@Service
public class GwtLocatorMetadataProviderImpl implements
        GwtLocatorMetadataProvider {

    private static final int LAYER_POSITION = LayerType.HIGHEST.getPosition();

    @Reference GwtTypeService gwtTypeService;
    @Reference LayerService layerService;
    @Reference MetadataDependencyRegistry metadataDependencyRegistry;
    @Reference MetadataService metadataService;
    @Reference PersistenceMemberLocator persistenceMemberLocator;
    @Reference ProjectOperations projectOperations;
    @Reference TypeLocationService typeLocationService;
    @Reference TypeManagementService typeManagementService;

    protected void activate(final ComponentContext context) {
        metadataDependencyRegistry.registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
    }

    protected void deactivate(final ComponentContext context) {
        metadataDependencyRegistry.deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
    }

    public MetadataItem get(final String metadataIdentificationString) {
        final ClassOrInterfaceTypeDetails proxy = getGovernor(metadataIdentificationString);
        if (proxy == null) {
            return null;
        }

        final AnnotationMetadata proxyAnnotation = GwtUtils.getFirstAnnotation(
                proxy, GwtUtils.PROXY_ANNOTATIONS);
        if (proxyAnnotation == null) {
            return null;
        }

        final String locatorType = GwtUtils.getStringValue(proxyAnnotation
                .getAttribute("locator"));
        if (StringUtils.isBlank(locatorType)) {
            return null;
        }

        final ClassOrInterfaceTypeDetails entityType = gwtTypeService
                .lookupEntityFromProxy(proxy);
        if (entityType == null || Modifier.isAbstract(entityType.getModifier())) {
            return null;
        }

        final JavaType entity = entityType.getName();
        final MethodMetadata identifierAccessor = persistenceMemberLocator
                .getIdentifierAccessor(entity);
        final MethodMetadata versionAccessor = persistenceMemberLocator
                .getVersionAccessor(entity);
        if (identifierAccessor == null || versionAccessor == null) {
            return null;
        }

        final JavaType identifierType = GwtUtils.convertPrimitiveType(
                identifierAccessor.getReturnType(), true);
        final String locatorPhysicalTypeId = PhysicalTypeIdentifier
                .createIdentifier(new JavaType(locatorType),
                        PhysicalTypeIdentifier.getPath(proxy
                                .getDeclaredByMetadataId()));
        final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                locatorPhysicalTypeId);
        final AnnotationMetadataBuilder annotationMetadataBuilder = new AnnotationMetadataBuilder(
                RooJavaType.ROO_GWT_LOCATOR);
        annotationMetadataBuilder.addStringAttribute("value",
                entity.getFullyQualifiedTypeName());
        cidBuilder.addAnnotation(annotationMetadataBuilder);

        cidBuilder.addAnnotation(new AnnotationMetadataBuilder(
                SpringJavaType.COMPONENT));
        cidBuilder.setName(new JavaType(locatorType));
        cidBuilder.setModifier(Modifier.PUBLIC);
        cidBuilder.setPhysicalTypeCategory(PhysicalTypeCategory.CLASS);
        cidBuilder.addExtendsTypes(new JavaType(LOCATOR
                .getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays
                .asList(entity, identifierType)));
        cidBuilder.addMethod(getCreateMethod(locatorPhysicalTypeId, entity));

        final MemberTypeAdditions findMethodAdditions = layerService
                .getMemberTypeAdditions(locatorPhysicalTypeId,
                        CustomDataKeys.FIND_METHOD.name(), entity,
                        identifierType, LAYER_POSITION, new MethodParameter(
                                identifierType, "id"));
        Validate.notNull(
                findMethodAdditions,
                "Find method not available for entity '"
                        + entity.getFullyQualifiedTypeName() + "'");
        cidBuilder.addMethod(getFindMethod(findMethodAdditions, cidBuilder,
                locatorPhysicalTypeId, entity, identifierType));

        cidBuilder
                .addMethod(getDomainTypeMethod(locatorPhysicalTypeId, entity));
        cidBuilder.addMethod(getIdMethod(locatorPhysicalTypeId, entity,
                identifierAccessor));
        cidBuilder.addMethod(getIdTypeMethod(locatorPhysicalTypeId, entity,
                identifierType));
        cidBuilder.addMethod(getVersionMethod(locatorPhysicalTypeId, entity,
                versionAccessor));

        typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
        return null;
    }

    private MethodMetadataBuilder getCreateMethod(final String declaredById,
            final JavaType targetType) {
        final InvocableMemberBodyBuilder invocableMemberBodyBuilder = InvocableMemberBodyBuilder
                .getInstance();
        invocableMemberBodyBuilder.append("return new "
                + targetType.getSimpleTypeName() + "();");
        final MethodMetadataBuilder createMethodBuilder = new MethodMetadataBuilder(
                declaredById, Modifier.PUBLIC, new JavaSymbolName("create"),
                targetType, invocableMemberBodyBuilder);
        final JavaType wildEntityType = new JavaType(
                targetType.getFullyQualifiedTypeName(), 0, DataType.VARIABLE,
                JavaType.WILDCARD_EXTENDS, null);
        final JavaType classParameterType = new JavaType(
                JavaType.CLASS.getFullyQualifiedTypeName(), 0, DataType.TYPE,
                null, Arrays.asList(wildEntityType));
        createMethodBuilder.addParameter("clazz", classParameterType);
        return createMethodBuilder;
    }

    private MethodMetadataBuilder getDomainTypeMethod(
            final String declaredById, final JavaType targetType) {
        final InvocableMemberBodyBuilder invocableMemberBodyBuilder = InvocableMemberBodyBuilder
                .getInstance();
        invocableMemberBodyBuilder.append("return "
                + targetType.getSimpleTypeName() + ".class;");
        final JavaType returnType = new JavaType(
                CLASS.getFullyQualifiedTypeName(), 0, DataType.TYPE, null,
                Arrays.asList(targetType));
        return new MethodMetadataBuilder(declaredById, Modifier.PUBLIC,
                new JavaSymbolName("getDomainType"), returnType,
                invocableMemberBodyBuilder);
    }

    private MethodMetadataBuilder getFindMethod(
            final MemberTypeAdditions findMethodAdditions,
            final ClassOrInterfaceTypeDetailsBuilder locatorBuilder,
            final String declaredById, final JavaType targetType,
            final JavaType idType) {
        final InvocableMemberBodyBuilder invocableMemberBodyBuilder = InvocableMemberBodyBuilder
                .getInstance();
        invocableMemberBodyBuilder.append("return ")
                .append(findMethodAdditions.getMethodCall()).append(";");
        findMethodAdditions.copyAdditionsTo(locatorBuilder,
                locatorBuilder.build());
        final MethodMetadataBuilder findMethodBuilder = new MethodMetadataBuilder(
                declaredById, Modifier.PUBLIC, new JavaSymbolName("find"),
                targetType, invocableMemberBodyBuilder);
        final JavaType wildEntityType = new JavaType(
                targetType.getFullyQualifiedTypeName(), 0, DataType.VARIABLE,
                JavaType.WILDCARD_EXTENDS, null);
        final JavaType classParameterType = new JavaType(
                JavaType.CLASS.getFullyQualifiedTypeName(), 0, DataType.TYPE,
                null, Arrays.asList(wildEntityType));
        findMethodBuilder.addParameter("clazz", classParameterType);
        findMethodBuilder.addParameter("id", idType);
        return findMethodBuilder;
    }

    private ClassOrInterfaceTypeDetails getGovernor(
            final String metadataIdentificationString) {
        final JavaType governorTypeName = GwtLocatorMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath governorTypePath = GwtLocatorMetadata
                .getPath(metadataIdentificationString);
        final String physicalTypeId = PhysicalTypeIdentifier.createIdentifier(
                governorTypeName, governorTypePath);
        return typeLocationService.getTypeDetails(physicalTypeId);
    }

    private MethodMetadataBuilder getIdMethod(final String declaredById,
            final JavaType targetType, final MethodMetadata idAccessor) {
        final InvocableMemberBodyBuilder invocableMemberBodyBuilder = InvocableMemberBodyBuilder
                .getInstance();
        invocableMemberBodyBuilder.append("return "
                + StringUtils.uncapitalize(targetType.getSimpleTypeName())
                + "." + idAccessor.getMethodName() + "();");
        final MethodMetadataBuilder getIdMethod = new MethodMetadataBuilder(
                declaredById,
                Modifier.PUBLIC,
                new JavaSymbolName("getId"),
                GwtUtils.convertPrimitiveType(idAccessor.getReturnType(), true),
                invocableMemberBodyBuilder);
        getIdMethod.addParameter(
                StringUtils.uncapitalize(targetType.getSimpleTypeName()),
                targetType);
        return getIdMethod;
    }

    private MethodMetadataBuilder getIdTypeMethod(final String declaredById,
            final JavaType targetType, final JavaType idType) {
        final InvocableMemberBodyBuilder invocableMemberBodyBuilder = InvocableMemberBodyBuilder
                .getInstance();
        invocableMemberBodyBuilder.append("return "
                + idType.getSimpleTypeName() + ".class;");
        final JavaType returnType = new JavaType(
                JavaType.CLASS.getFullyQualifiedTypeName(), 0, DataType.TYPE,
                null, Arrays.asList(idType));
        return new MethodMetadataBuilder(declaredById, Modifier.PUBLIC,
                new JavaSymbolName("getIdType"), returnType,
                invocableMemberBodyBuilder);
    }

    public String getProvidesType() {
        return GwtLocatorMetadata.getMetadataIdentifierType();
    }

    private MethodMetadataBuilder getVersionMethod(final String declaredById,
            final JavaType targetType, final MethodMetadata versionAccessor) {
        final InvocableMemberBodyBuilder invocableMemberBodyBuilder = InvocableMemberBodyBuilder
                .getInstance();
        invocableMemberBodyBuilder.append("return "
                + StringUtils.uncapitalize(targetType.getSimpleTypeName())
                + "." + versionAccessor.getMethodName() + "();");
        final MethodMetadataBuilder getIdMethodBuilder = new MethodMetadataBuilder(
                declaredById, Modifier.PUBLIC,
                new JavaSymbolName("getVersion"), JavaType.OBJECT,
                invocableMemberBodyBuilder);
        getIdMethodBuilder.addParameter(
                StringUtils.uncapitalize(targetType.getSimpleTypeName()),
                targetType);
        return getIdMethodBuilder;
    }

    public void notify(final String upstreamDependency,
            String downstreamDependency) {
        if (MetadataIdentificationUtils
                .isIdentifyingClass(downstreamDependency)) {
            Validate.isTrue(
                    MetadataIdentificationUtils.getMetadataClass(
                            upstreamDependency).equals(
                            MetadataIdentificationUtils
                                    .getMetadataClass(PhysicalTypeIdentifier
                                            .getMetadataIdentiferType())),
                    "Expected class-level notifications only for PhysicalTypeIdentifier (not '"
                            + upstreamDependency + "')");

            final ClassOrInterfaceTypeDetails cid = typeLocationService
                    .getTypeDetails(upstreamDependency);
            if (cid == null) {
                return;
            }
            boolean processed = false;
            if (cid.getAnnotation(RooJavaType.ROO_GWT_REQUEST) != null) {
                final ClassOrInterfaceTypeDetails proxy = gwtTypeService
                        .lookupProxyFromRequest(cid);
                if (proxy != null) {
                    final JavaType typeName = PhysicalTypeIdentifier
                            .getJavaType(proxy.getDeclaredByMetadataId());
                    final LogicalPath typePath = PhysicalTypeIdentifier
                            .getPath(proxy.getDeclaredByMetadataId());
                    downstreamDependency = GwtLocatorMetadata.createIdentifier(
                            typeName, typePath);
                    processed = true;
                }
            }
            if (!processed
                    && cid.getAnnotation(RooJavaType.ROO_GWT_PROXY) == null) {
                boolean found = false;
                for (final ClassOrInterfaceTypeDetails proxyCid : typeLocationService
                        .findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_GWT_PROXY)) {
                    final AnnotationMetadata annotationMetadata = GwtUtils
                            .getFirstAnnotation(proxyCid,
                                    GwtUtils.ROO_PROXY_REQUEST_ANNOTATIONS);
                    if (annotationMetadata != null) {
                        final AnnotationAttributeValue<?> attributeValue = annotationMetadata
                                .getAttribute("value");
                        if (attributeValue != null) {
                            final String mirrorName = GwtUtils
                                    .getStringValue(attributeValue);
                            if (mirrorName != null
                                    && cid.getName()
                                            .getFullyQualifiedTypeName()
                                            .equals(attributeValue.getValue())) {
                                found = true;
                                final JavaType typeName = PhysicalTypeIdentifier
                                        .getJavaType(proxyCid
                                                .getDeclaredByMetadataId());
                                final LogicalPath typePath = PhysicalTypeIdentifier
                                        .getPath(proxyCid
                                                .getDeclaredByMetadataId());
                                downstreamDependency = GwtLocatorMetadata
                                        .createIdentifier(typeName, typePath);
                                break;
                            }
                        }
                    }
                }
                if (!found) {
                    return;
                }
            }
            else if (!processed) {
                // A physical Java type has changed, and determine what the
                // corresponding local metadata identification string would have
                // been
                final JavaType typeName = PhysicalTypeIdentifier
                        .getJavaType(upstreamDependency);
                final LogicalPath typePath = PhysicalTypeIdentifier
                        .getPath(upstreamDependency);
                downstreamDependency = GwtLocatorMetadata.createIdentifier(
                        typeName, typePath);
            }

            // We only need to proceed if the downstream dependency relationship
            // is not already registered
            // (if it's already registered, the event will be delivered directly
            // later on)
            if (metadataDependencyRegistry.getDownstream(upstreamDependency)
                    .contains(downstreamDependency)) {
                return;
            }
        }

        // We should now have an instance-specific "downstream dependency" that
        // can be processed by this class
        Validate.isTrue(
                MetadataIdentificationUtils.getMetadataClass(
                        downstreamDependency).equals(
                        MetadataIdentificationUtils
                                .getMetadataClass(getProvidesType())),
                "Unexpected downstream notification for '"
                        + downstreamDependency
                        + "' to this provider (which uses '"
                        + getProvidesType() + "'");

        metadataService.evictAndGet(downstreamDependency);
    }
}
