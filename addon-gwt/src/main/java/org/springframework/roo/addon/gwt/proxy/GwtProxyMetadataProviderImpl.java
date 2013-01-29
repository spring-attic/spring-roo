package org.springframework.roo.addon.gwt.proxy;

import static org.springframework.roo.addon.gwt.GwtJavaType.ENTITY_PROXY;
import static org.springframework.roo.addon.gwt.GwtJavaType.OLD_ENTITY_PROXY;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.gwt.GwtFileManager;
import org.springframework.roo.addon.gwt.GwtTypeService;
import org.springframework.roo.addon.gwt.GwtUtils;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.AbstractHashCodeTrackingMetadataNotifier;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectOperations;

@Component(immediate = true)
@Service
public class GwtProxyMetadataProviderImpl extends
        AbstractHashCodeTrackingMetadataNotifier implements
        GwtProxyMetadataProvider {

    @Reference protected GwtFileManager gwtFileManager;
    @Reference protected GwtTypeService gwtTypeService;
    @Reference protected ProjectOperations projectOperations;
    @Reference protected TypeLocationService typeLocationService;

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

        final AnnotationMetadata mirrorAnnotation = MemberFindingUtils
                .getAnnotationOfType(proxy.getAnnotations(),
                        RooJavaType.ROO_GWT_PROXY);
        if (mirrorAnnotation == null) {
            return null;
        }

        final JavaType mirroredType = GwtUtils.lookupProxyTargetType(proxy);
        if (mirroredType == null) {
            return null;
        }

        final List<String> exclusionList = new ArrayList<String>();
        final AnnotationAttributeValue<?> excludeAttribute = mirrorAnnotation
                .getAttribute("exclude");
        if (excludeAttribute != null
                && excludeAttribute instanceof ArrayAttributeValue) {
            @SuppressWarnings("unchecked")
            final ArrayAttributeValue<StringAttributeValue> excludeArrayAttribute = (ArrayAttributeValue<StringAttributeValue>) excludeAttribute;
            for (final StringAttributeValue attributeValue : excludeArrayAttribute
                    .getValue()) {
                exclusionList.add(attributeValue.getValue());
            }
        }
        else if (excludeAttribute != null
                && excludeAttribute instanceof StringAttributeValue) {
            final StringAttributeValue excludeStringAttribute = (StringAttributeValue) excludeAttribute;
            exclusionList.add(excludeStringAttribute.getValue());
        }

        final List<String> readOnlyList = new ArrayList<String>();
        final AnnotationAttributeValue<?> readOnlyAttribute = mirrorAnnotation
                .getAttribute("readOnly");
        if (readOnlyAttribute != null
                && readOnlyAttribute instanceof ArrayAttributeValue) {
            @SuppressWarnings("unchecked")
            final ArrayAttributeValue<StringAttributeValue> readOnlyArrayAttribute = (ArrayAttributeValue<StringAttributeValue>) readOnlyAttribute;
            for (final StringAttributeValue attributeValue : readOnlyArrayAttribute
                    .getValue()) {
                readOnlyList.add(attributeValue.getValue());
            }
        }
        else if (readOnlyAttribute != null
                && readOnlyAttribute instanceof StringAttributeValue) {
            final StringAttributeValue readOnlyStringAttribute = (StringAttributeValue) readOnlyAttribute;
            readOnlyList.add(readOnlyStringAttribute.getValue());
        }

        final ClassOrInterfaceTypeDetails mirroredDetails = typeLocationService
                .getTypeDetails(mirroredType);
        if (mirroredDetails == null
                || Modifier.isAbstract(mirroredDetails.getModifier())) {
            return null;
        }

        final String moduleName = PhysicalTypeIdentifier.getPath(
                proxy.getDeclaredByMetadataId()).getModule();
        final List<MethodMetadata> proxyMethods = gwtTypeService
                .getProxyMethods(mirroredDetails);
        final List<MethodMetadata> convertedProxyMethods = new ArrayList<MethodMetadata>();
        final Collection<JavaPackage> sourcePackages = gwtTypeService
                .getSourcePackages(moduleName);
        for (final MethodMetadata method : proxyMethods) {
            final JavaType gwtType = gwtTypeService.getGwtSideLeafType(
                    method.getReturnType(), mirroredDetails.getName(), false,
                    true);
            final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                    method);
            methodBuilder.setReturnType(gwtType);
            final MethodMetadata convertedMethod = methodBuilder.build();
            if (gwtTypeService.isMethodReturnTypeInSourcePath(convertedMethod,
                    mirroredDetails, sourcePackages)) {
                convertedProxyMethods.add(methodBuilder.build());
            }
        }
        final GwtProxyMetadata metadata = new GwtProxyMetadata(
                metadataIdentificationString, updateProxy(proxy,
                        convertedProxyMethods, exclusionList, readOnlyList));
        notifyIfRequired(metadata);
        return metadata;
    }

    private ClassOrInterfaceTypeDetails getGovernor(
            final String metadataIdentificationString) {
        final JavaType governorTypeName = GwtProxyMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath governorTypePath = GwtProxyMetadata
                .getPath(metadataIdentificationString);

        final String physicalTypeId = PhysicalTypeIdentifier.createIdentifier(
                governorTypeName, governorTypePath);
        return typeLocationService.getTypeDetails(physicalTypeId);
    }

    public String getProvidesType() {
        return GwtProxyMetadata.getMetadataIdentifierType();
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
                    "Expected class-level notifications only for PhysicalTypeIdentifier (not '%s')",
                    upstreamDependency);

            final ClassOrInterfaceTypeDetails cid = typeLocationService
                    .getTypeDetails(upstreamDependency);
            if (cid == null) {
                return;
            }
            if (MemberFindingUtils.getAnnotationOfType(cid.getAnnotations(),
                    RooJavaType.ROO_GWT_PROXY) == null) {
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
                                downstreamDependency = GwtProxyMetadata
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
            else {
                // A physical Java type has changed, and determine what the
                // corresponding local metadata identification string would have
                // been
                final JavaType typeName = PhysicalTypeIdentifier
                        .getJavaType(upstreamDependency);
                final LogicalPath typePath = PhysicalTypeIdentifier
                        .getPath(upstreamDependency);
                downstreamDependency = GwtProxyMetadata.createIdentifier(
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
                "Unexpected downstream notification for '%s' to this provider (which uses '%s')",
                downstreamDependency, getProvidesType());

        metadataService.evictAndGet(downstreamDependency);
    }

    private String updateProxy(final ClassOrInterfaceTypeDetails proxy,
            final List<MethodMetadata> proxyMethods,
            final List<String> exclusionList, final List<String> readOnlyList) {
        // Create a new ClassOrInterfaceTypeDetailsBuilder for the Proxy, will
        // be overridden if the Proxy has already been created
        final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                proxy);

        // Only inherit from EntityProxy if extension is not already defined
        if (!cidBuilder.getExtendsTypes().contains(OLD_ENTITY_PROXY)
                && !cidBuilder.getExtendsTypes().contains(ENTITY_PROXY)) {
            cidBuilder.addExtendsTypes(ENTITY_PROXY);
        }

        if (!cidBuilder.getExtendsTypes().contains(ENTITY_PROXY)) {
            cidBuilder.addExtendsTypes(ENTITY_PROXY);
        }

        final String destinationMetadataId = proxy.getDeclaredByMetadataId();
        final List<MethodMetadataBuilder> methods = new ArrayList<MethodMetadataBuilder>();
        for (final MethodMetadata method : proxyMethods) {
            if (exclusionList.contains(method.getMethodName().getSymbolName())) {
                continue;
            }
            final String propertyName = StringUtils.uncapitalize(BeanInfoUtils
                    .getPropertyNameForJavaBeanMethod(method).getSymbolName());
            if (exclusionList.contains(propertyName)) {
                continue;
            }

            final MethodMetadataBuilder abstractAccessorMethodBuilder = new MethodMetadataBuilder(
                    destinationMetadataId, method);
            abstractAccessorMethodBuilder
                    .setBodyBuilder(new InvocableMemberBodyBuilder());
            abstractAccessorMethodBuilder.setModifier(Modifier.ABSTRACT);
            methods.add(abstractAccessorMethodBuilder);

            if (readOnlyList.contains(propertyName)) {
                continue;
            }
            final MethodMetadataBuilder abstractMutatorMethodBuilder = new MethodMetadataBuilder(
                    destinationMetadataId, method);
            abstractMutatorMethodBuilder
                    .setBodyBuilder(new InvocableMemberBodyBuilder());
            abstractMutatorMethodBuilder.setModifier(Modifier.ABSTRACT);
            abstractMutatorMethodBuilder.setReturnType(JavaType.VOID_PRIMITIVE);
            abstractMutatorMethodBuilder
                    .setParameterTypes(AnnotatedJavaType
                            .convertFromJavaTypes(Arrays.asList(method
                                    .getReturnType())));
            abstractMutatorMethodBuilder.setParameterNames(Arrays
                    .asList(new JavaSymbolName(StringUtils
                            .uncapitalize(propertyName))));
            abstractMutatorMethodBuilder.setMethodName(new JavaSymbolName(
                    method.getMethodName().getSymbolName()
                            .replaceFirst("get", "set")));
            methods.add(abstractMutatorMethodBuilder);
        }

        cidBuilder.setDeclaredMethods(methods);
        return gwtFileManager.write(cidBuilder.build(),
                GwtUtils.PROXY_REQUEST_WARNING);
    }
}
