package org.springframework.roo.addon.gwt.proxy;

import static org.springframework.roo.addon.gwt.GwtJavaType.ENTITY_PROXY;
import static org.springframework.roo.addon.gwt.GwtJavaType.OLD_ENTITY_PROXY;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

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
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

@Component(immediate = true)
@Service
public class GwtProxyMetadataProviderImpl extends AbstractHashCodeTrackingMetadataNotifier implements GwtProxyMetadataProvider {

	// Fields
	@Reference protected GwtFileManager gwtFileManager;
	@Reference protected GwtTypeService gwtTypeService;
	@Reference protected ProjectOperations projectOperations;
	@Reference protected TypeLocationService typeLocationService;

	protected void activate(final ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
	}

	protected void deactivate(final ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
	}

	public MetadataItem get(final String metadataIdentificationString) {
		ClassOrInterfaceTypeDetails proxy = getGovernor(metadataIdentificationString);
		if (proxy == null) {
			return null;
		}

		AnnotationMetadata mirrorAnnotation = MemberFindingUtils.getAnnotationOfType(proxy.getAnnotations(), RooJavaType.ROO_GWT_PROXY);
		if (mirrorAnnotation == null) {
			return null;
		}

		JavaType mirroredType = GwtUtils.lookupProxyTargetType(proxy);
		if (mirroredType == null) {
			return null;
		}

		List<String> exclusionList = new ArrayList<String>();
		AnnotationAttributeValue<?> excludeAttribute = mirrorAnnotation.getAttribute("exclude");
		if (excludeAttribute != null && excludeAttribute instanceof ArrayAttributeValue) {
			@SuppressWarnings("unchecked")
			ArrayAttributeValue<StringAttributeValue> excludeArrayAttribute = (ArrayAttributeValue<StringAttributeValue>) excludeAttribute;
			for (StringAttributeValue attributeValue : excludeArrayAttribute.getValue()) {
				exclusionList.add(attributeValue.getValue());
			}
		} else if (excludeAttribute != null && excludeAttribute instanceof StringAttributeValue) {
			StringAttributeValue excludeStringAttribute = (StringAttributeValue) excludeAttribute;
			exclusionList.add(excludeStringAttribute.getValue());
		}

		List<String> readOnlyList = new ArrayList<String>();
		AnnotationAttributeValue<?> readOnlyAttribute = mirrorAnnotation.getAttribute("readOnly");
		if (readOnlyAttribute != null && readOnlyAttribute instanceof ArrayAttributeValue) {
			@SuppressWarnings("unchecked")
			ArrayAttributeValue<StringAttributeValue> readOnlyArrayAttribute = (ArrayAttributeValue<StringAttributeValue>) readOnlyAttribute;
			for (StringAttributeValue attributeValue : readOnlyArrayAttribute.getValue()) {
				readOnlyList.add(attributeValue.getValue());
			}
		} else if (readOnlyAttribute != null && readOnlyAttribute instanceof StringAttributeValue) {
			StringAttributeValue readOnlyStringAttribute = (StringAttributeValue) readOnlyAttribute;
			readOnlyList.add(readOnlyStringAttribute.getValue());
		}

		ClassOrInterfaceTypeDetails mirroredDetails = typeLocationService.getTypeDetails(mirroredType);
		if (mirroredDetails == null || Modifier.isAbstract(mirroredDetails.getModifier())) {
			return null;
		}

		final String moduleName = PhysicalTypeIdentifier.getPath(proxy.getDeclaredByMetadataId()).getModule();
		List<MethodMetadata> proxyMethods = gwtTypeService.getProxyMethods(mirroredDetails);
		List<MethodMetadata> convertedProxyMethods = new ArrayList<MethodMetadata>();
		Set<String> sourcePaths = gwtTypeService.getSourcePaths(moduleName);
		for (MethodMetadata method : proxyMethods) {
			JavaType gwtType = gwtTypeService.getGwtSideLeafType(method.getReturnType(), mirroredDetails.getName(), false, true);
			MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(method);
			methodBuilder.setReturnType(gwtType);
			MethodMetadata convertedMethod = methodBuilder.build();
			if (gwtTypeService.isMethodReturnTypesInSourcePath(convertedMethod, mirroredDetails, sourcePaths)) {
				convertedProxyMethods.add(methodBuilder.build());
			}
		}
		GwtProxyMetadata metadata = new GwtProxyMetadata(metadataIdentificationString, updateProxy(proxy, convertedProxyMethods, exclusionList, readOnlyList));
		notifyIfRequired(metadata);
		return metadata;
	}

	private ClassOrInterfaceTypeDetails getGovernor(final String metadataIdentificationString) {
		JavaType governorTypeName = GwtProxyMetadata.getJavaType(metadataIdentificationString);
		LogicalPath governorTypePath = GwtProxyMetadata.getPath(metadataIdentificationString);

		String physicalTypeId = PhysicalTypeIdentifier.createIdentifier(governorTypeName, governorTypePath);
		return typeLocationService.getTypeDetails(physicalTypeId);
	}

	private String updateProxy(final ClassOrInterfaceTypeDetails proxy, final List<MethodMetadata> proxyMethods, final List<String> exclusionList, final List<String> readOnlyList) {
		// Create a new ClassOrInterfaceTypeDetailsBuilder for the Proxy, will be overridden if the Proxy has already been created
		ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(proxy);

		// Only inherit from EntityProxy if extension is not already defined
		if (!cidBuilder.getExtendsTypes().contains(OLD_ENTITY_PROXY) && !cidBuilder.getExtendsTypes().contains(ENTITY_PROXY)) {
			cidBuilder.addExtendsTypes(ENTITY_PROXY);
		}

		if (!cidBuilder.getExtendsTypes().contains(ENTITY_PROXY)) {
			cidBuilder.addExtendsTypes(ENTITY_PROXY);
		}

		String destinationMetadataId = proxy.getDeclaredByMetadataId();
		List<MethodMetadataBuilder> methods = new ArrayList<MethodMetadataBuilder>();
		for (MethodMetadata method : proxyMethods) {
			if (exclusionList.contains(method.getMethodName().getSymbolName())) {
				continue;
			}
			String propertyName = StringUtils.uncapitalize(BeanInfoUtils.getPropertyNameForJavaBeanMethod(method).getSymbolName());
			if (exclusionList.contains(propertyName)) {
				continue;
			}

			MethodMetadataBuilder abstractAccessorMethodBuilder = new MethodMetadataBuilder(destinationMetadataId, method);
			abstractAccessorMethodBuilder.setBodyBuilder(new InvocableMemberBodyBuilder());
			abstractAccessorMethodBuilder.setModifier(Modifier.ABSTRACT);
			methods.add(abstractAccessorMethodBuilder);

			if (readOnlyList.contains(propertyName)) {
				continue;
			}
			MethodMetadataBuilder abstractMutatorMethodBuilder = new MethodMetadataBuilder(destinationMetadataId, method);
			abstractMutatorMethodBuilder.setBodyBuilder(new InvocableMemberBodyBuilder());
			abstractMutatorMethodBuilder.setModifier(Modifier.ABSTRACT);
			abstractMutatorMethodBuilder.setReturnType(JavaType.VOID_PRIMITIVE);
			abstractMutatorMethodBuilder.setParameterTypes(AnnotatedJavaType.convertFromJavaTypes(Arrays.asList(method.getReturnType())));
			abstractMutatorMethodBuilder.setParameterNames(Arrays.asList(new JavaSymbolName(StringUtils.uncapitalize(propertyName))));
			abstractMutatorMethodBuilder.setMethodName(new JavaSymbolName(method.getMethodName().getSymbolName().replaceFirst("get", "set")));
			methods.add(abstractMutatorMethodBuilder);
		}

		cidBuilder.setDeclaredMethods(methods);
		return gwtFileManager.write(cidBuilder.build(), GwtUtils.PROXY_REQUEST_WARNING);
	}

	public void notify(final String upstreamDependency, String downstreamDependency) {
		if (MetadataIdentificationUtils.isIdentifyingClass(downstreamDependency)) {
			Assert.isTrue(MetadataIdentificationUtils.getMetadataClass(upstreamDependency).equals(MetadataIdentificationUtils.getMetadataClass(PhysicalTypeIdentifier.getMetadataIdentiferType())), "Expected class-level notifications only for PhysicalTypeIdentifier (not '" + upstreamDependency + "')");

			ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(upstreamDependency);
			if (cid == null) {
				return;
			}
			if (MemberFindingUtils.getAnnotationOfType(cid.getAnnotations(), RooJavaType.ROO_GWT_PROXY) == null) {
				boolean found = false;
				for (ClassOrInterfaceTypeDetails proxyCid : typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_GWT_PROXY)) {
					AnnotationMetadata annotationMetadata = GwtUtils.getFirstAnnotation(proxyCid, GwtUtils.ROO_PROXY_REQUEST_ANNOTATIONS);
					if (annotationMetadata != null) {
						AnnotationAttributeValue<?> attributeValue = annotationMetadata.getAttribute("value");
						if (attributeValue != null) {
							String mirrorName = GwtUtils.getStringValue(attributeValue);
							if (mirrorName != null && cid.getName().getFullyQualifiedTypeName().equals(attributeValue.getValue())) {
								found = true;
								JavaType typeName = PhysicalTypeIdentifier.getJavaType(proxyCid.getDeclaredByMetadataId());
								LogicalPath typePath = PhysicalTypeIdentifier.getPath(proxyCid.getDeclaredByMetadataId());
								downstreamDependency = GwtProxyMetadata.createIdentifier(typeName, typePath);
								break;
							}
						}
					}
				}
				if (!found) {
					return;
				}
			} else {
				// A physical Java type has changed, and determine what the corresponding local metadata identification string would have been
				JavaType typeName = PhysicalTypeIdentifier.getJavaType(upstreamDependency);
				LogicalPath typePath = PhysicalTypeIdentifier.getPath(upstreamDependency);
				downstreamDependency = GwtProxyMetadata.createIdentifier(typeName, typePath);
			}

			// We only need to proceed if the downstream dependency relationship is not already registered
			// (if it's already registered, the event will be delivered directly later on)
			if (metadataDependencyRegistry.getDownstream(upstreamDependency).contains(downstreamDependency)) {
				return;
			}
		}

		// We should now have an instance-specific "downstream dependency" that can be processed by this class
		Assert.isTrue(MetadataIdentificationUtils.getMetadataClass(downstreamDependency).equals(MetadataIdentificationUtils.getMetadataClass(getProvidesType())), "Unexpected downstream notification for '" + downstreamDependency + "' to this provider (which uses '" + getProvidesType() + "'");

		metadataService.evictAndGet(downstreamDependency);
	}

	public String getProvidesType() {
		return GwtProxyMetadata.getMetadataIdentifierType();
	}
}
