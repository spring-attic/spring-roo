package org.springframework.roo.addon.gwt.request;

import static org.springframework.roo.addon.gwt.GwtJavaType.INSTANCE_REQUEST;
import static org.springframework.roo.addon.gwt.GwtJavaType.OLD_REQUEST_CONTEXT;
import static org.springframework.roo.addon.gwt.GwtJavaType.REQUEST;
import static org.springframework.roo.addon.gwt.GwtJavaType.REQUEST_CONTEXT;
import static org.springframework.roo.model.RooJavaType.ROO_GWT_PROXY;
import static org.springframework.roo.model.RooJavaType.ROO_GWT_REQUEST;
import static org.springframework.roo.model.RooJavaType.ROO_SERVICE;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.AbstractHashCodeTrackingMetadataNotifier;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

@Component(immediate = true)
@Service
public class GwtRequestMetadataProviderImpl extends AbstractHashCodeTrackingMetadataNotifier implements GwtRequestMetadataProvider {

	// Fields
	@Reference protected GwtFileManager gwtFileManager;
	@Reference protected GwtTypeService gwtTypeService;
	@Reference protected MemberDetailsScanner memberDetailsScanner;
	@Reference protected ProjectOperations projectOperations;
	@Reference protected TypeLocationService typeLocationService;

	protected void activate(final ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
	}

	protected void deactivate(final ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
	}

	public String getProvidesType() {
		return GwtRequestMetadata.getMetadataIdentifierType();
	}

	public MetadataItem get(final String requestMetadataId) {
		// Abort early if we can't continue
		final ProjectMetadata projectMetadata = projectOperations.getProjectMetadata(PhysicalTypeIdentifierNamingUtils.getPath(requestMetadataId).getModule());
		if (projectMetadata == null) {
			return null;
		}

		final ClassOrInterfaceTypeDetails request = getGovernor(requestMetadataId);
		if (request == null || request.getAnnotation(ROO_GWT_REQUEST) == null) {
			return null;
		}

		// Target type can be an Active Record entity, a service, etc.
		final JavaType targetType = GwtUtils.lookupRequestTargetType(request);
		if (targetType == null) {
			return null;
		}

		final ClassOrInterfaceTypeDetails target = typeLocationService.getTypeDetails(targetType);
		if (target == null || Modifier.isAbstract(target.getModifier())) {
			return null;
		}

		final MemberDetails targetDetails = memberDetailsScanner.getMemberDetails(getClass().getName(), target);
		if (targetDetails == null) {
			return null;
		}

		final List<MethodMetadata> requestMethods = getRequestMethods(getMethodExclusions(request), target.getType(), targetDetails);
		
		final GwtRequestMetadata gwtRequestMetadata = new GwtRequestMetadata(requestMetadataId, writeRequestInterface(request, requestMethods));
		notifyIfRequired(gwtRequestMetadata);
		return gwtRequestMetadata;
	}

	private List<MethodMetadata> getRequestMethods(final List<String> excludedMethods, final JavaType targetType, final MemberDetails targetDetails) {
		final List<MethodMetadata> requestMethods = new ArrayList<MethodMetadata>();
		for (final MethodMetadata methodMetadata : targetDetails.getMethods()) {
			if (Modifier.isPublic(methodMetadata.getModifier()) && !excludedMethods.contains(methodMetadata.getMethodName().getSymbolName())) {
				final JavaType returnType = gwtTypeService.getGwtSideLeafType(methodMetadata.getReturnType(), targetType, true, true);
				if (returnType != null) {
					final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(methodMetadata);
					methodBuilder.setReturnType(returnType);
					methodBuilder.setBodyBuilder(null);
					requestMethods.add(methodBuilder.build());
				}
			}
		}
		return requestMethods;
	}

	private List<String> getMethodExclusions(final ClassOrInterfaceTypeDetails request) {
		final List<String> exclusionList = GwtUtils.getAnnotationValues(request, ROO_GWT_REQUEST, "exclude");

		final ClassOrInterfaceTypeDetails proxy = gwtTypeService.lookupProxyFromRequest(request);
		if (proxy != null) {
			final Boolean ignoreProxyExclusions = GwtUtils.getBooleanAnnotationValue(request, ROO_GWT_REQUEST, "ignoreProxyExclusions", false);
			if (!ignoreProxyExclusions) {
				for (final String exclusion : GwtUtils.getAnnotationValues(proxy, ROO_GWT_PROXY, "exclude")) {
					exclusionList.add("set" + StringUtils.capitalize(exclusion));
					exclusionList.add("get" + StringUtils.capitalize(exclusion));
				}
				exclusionList.addAll(GwtUtils.getAnnotationValues(proxy, ROO_GWT_PROXY, "exclude"));
			}
			final Boolean ignoreProxyReadOnly = GwtUtils.getBooleanAnnotationValue(request, ROO_GWT_REQUEST, "ignoreProxyReadOnly", false);
			if (!ignoreProxyReadOnly) {
				for (final String exclusion : GwtUtils.getAnnotationValues(proxy, ROO_GWT_PROXY, "readOnly")) {
					exclusionList.add("set" + StringUtils.capitalize(exclusion));
				}
			}
			final Boolean dontIncludeProxyMethods = GwtUtils.getBooleanAnnotationValue(proxy, ROO_GWT_REQUEST, "ignoreProxyReadOnly", true);
			if (dontIncludeProxyMethods) {
				for (final MethodMetadata methodMetadata : proxy.getDeclaredMethods())  {
					exclusionList.add(methodMetadata.getMethodName().getSymbolName());
				}
			}
		}
		return exclusionList;
	}

	private ClassOrInterfaceTypeDetails getGovernor(final String metadataIdentificationString) {
		final JavaType governorTypeName = GwtRequestMetadata.getJavaType(metadataIdentificationString);
		final LogicalPath governorTypePath = GwtRequestMetadata.getPath(metadataIdentificationString);
		final String physicalTypeId = PhysicalTypeIdentifier.createIdentifier(governorTypeName, governorTypePath);
		return typeLocationService.getTypeDetails(physicalTypeId);
	}

	/**
	 * Creates or updates the entity-specific request interface with
	 * 
	 * @param request
	 * @param requestMethods the methods to declare in the interface (required)
	 * @return the Java source code for the request interface
	 */
	private String writeRequestInterface(final ClassOrInterfaceTypeDetails request, final List<MethodMetadata> requestMethods) {
		final List<MethodMetadataBuilder> methods = new ArrayList<MethodMetadataBuilder>();
		for (final MethodMetadata method : requestMethods) {
			methods.add(getRequestMethod(request, method));
		}

		final ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(request);

		// Only inherit from RequestContext if extension is not already defined
		if (!typeDetailsBuilder.getExtendsTypes().contains(OLD_REQUEST_CONTEXT) && !typeDetailsBuilder.getExtendsTypes().contains(REQUEST_CONTEXT)) {
			typeDetailsBuilder.addExtendsTypes(REQUEST_CONTEXT);
		}

		if (!typeDetailsBuilder.getExtendsTypes().contains(REQUEST_CONTEXT)) {
			typeDetailsBuilder.addExtendsTypes(REQUEST_CONTEXT);
		}

		final ClassOrInterfaceTypeDetails entity = gwtTypeService.lookupEntityFromRequest(request);
		final AnnotationMetadata annotationMetadata = GwtUtils.getFirstAnnotation(request, GwtUtils.REQUEST_ANNOTATIONS);
		if (annotationMetadata != null && entity != null) {
			final AnnotationMetadataBuilder annotationMetadataBuilder = new AnnotationMetadataBuilder(annotationMetadata);
			annotationMetadataBuilder.addStringAttribute("value", entity.getName().getFullyQualifiedTypeName());
			annotationMetadataBuilder.removeAttribute("locator");
			final Set<ClassOrInterfaceTypeDetails> services = typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(ROO_SERVICE);
			for (final ClassOrInterfaceTypeDetails serviceLayer : services) {
				final AnnotationMetadata serviceAnnotation = serviceLayer.getTypeAnnotation(ROO_SERVICE);
				final AnnotationAttributeValue<List<ClassAttributeValue>> domainTypesAnnotation = serviceAnnotation.getAttribute("domainTypes");
				for (final ClassAttributeValue classAttributeValue : domainTypesAnnotation.getValue()) {
					if (classAttributeValue.getValue().equals(entity.getName())) {
						annotationMetadataBuilder.addStringAttribute("value", serviceLayer.getName().getFullyQualifiedTypeName());
						final LogicalPath path = PhysicalTypeIdentifier.getPath(request.getDeclaredByMetadataId());
						annotationMetadataBuilder.addStringAttribute("locator", projectOperations.getTopLevelPackage(path.getModule()) + ".server.locator.GwtServiceLocator");
					}
				}
			}
			typeDetailsBuilder.removeAnnotation(annotationMetadata.getAnnotationType());
			typeDetailsBuilder.updateTypeAnnotation(annotationMetadataBuilder);
		}
		typeDetailsBuilder.setDeclaredMethods(methods);
		return gwtFileManager.write(typeDetailsBuilder.build(), GwtUtils.PROXY_REQUEST_WARNING);
	}

	private MethodMetadataBuilder getRequestMethod(final ClassOrInterfaceTypeDetails request, final MethodMetadata methodMetadata) {
		final ClassOrInterfaceTypeDetails proxy = gwtTypeService.lookupProxyFromRequest(request);
		final ClassOrInterfaceTypeDetails service = gwtTypeService.lookupTargetServiceFromRequest(request);
		if (proxy == null || service == null) {
			return null;
		}
		final ClassOrInterfaceTypeDetails entity = gwtTypeService.lookupEntityFromProxy(proxy);
		if (entity == null) {
			return null;
		}

		List<JavaType> methodReturnTypeArgs;
		JavaType methodReturnType;
		if (entity.getName().equals(service.getName()) && !Modifier.isStatic(methodMetadata.getModifier())) {
			methodReturnTypeArgs = Arrays.asList(proxy.getName(), methodMetadata.getReturnType());
			methodReturnType = new JavaType(INSTANCE_REQUEST.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, methodReturnTypeArgs);
		} else {
			methodReturnTypeArgs = Collections.singletonList(methodMetadata.getReturnType());
			methodReturnType = new JavaType(REQUEST.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, methodReturnTypeArgs);
		}
		return getRequestMethod(request, methodMetadata, methodReturnType);
	}

	private MethodMetadataBuilder getRequestMethod(final ClassOrInterfaceTypeDetails request, final MethodMetadata methodMetadata, final JavaType methodReturnType) {
		final List<AnnotatedJavaType> paramaterTypes = new ArrayList<AnnotatedJavaType>();
		final ClassOrInterfaceTypeDetails mirroredTypeDetails = gwtTypeService.lookupEntityFromRequest(request);
		if (mirroredTypeDetails == null) {
			return null;
		}
		for (final AnnotatedJavaType parameterType : methodMetadata.getParameterTypes()) {
			paramaterTypes.add(new AnnotatedJavaType(gwtTypeService.getGwtSideLeafType(parameterType.getJavaType(), mirroredTypeDetails.getName(), true, false)));
		}
		return new MethodMetadataBuilder(request.getDeclaredByMetadataId(), Modifier.ABSTRACT, methodMetadata.getMethodName(), methodReturnType, paramaterTypes, methodMetadata.getParameterNames(), new InvocableMemberBodyBuilder());
	}

	public void notify(final String upstreamDependency, String downstreamDependency) {
		if (MetadataIdentificationUtils.isIdentifyingClass(downstreamDependency)) {
			Assert.isTrue(MetadataIdentificationUtils.getMetadataClass(upstreamDependency).equals(MetadataIdentificationUtils.getMetadataClass(PhysicalTypeIdentifier.getMetadataIdentiferType())), "Expected class-level notifications only for PhysicalTypeIdentifier (not '" + upstreamDependency + "')");
			
			final ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(upstreamDependency);
			if (cid == null) {
				return;
			}
			boolean processed = false;
			final List<JavaType> layerTypes = cid.getLayerEntities();
			if (!layerTypes.isEmpty()) {
				for (final ClassOrInterfaceTypeDetails request : typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(ROO_GWT_REQUEST)) {
					final ClassOrInterfaceTypeDetails entity = gwtTypeService.lookupEntityFromRequest(request);
					if (entity != null && layerTypes.contains(entity.getName())) {
						final JavaType typeName = PhysicalTypeIdentifier.getJavaType(request.getDeclaredByMetadataId());
						final LogicalPath typePath = PhysicalTypeIdentifier.getPath(request.getDeclaredByMetadataId());
						downstreamDependency = GwtRequestMetadata.createIdentifier(typeName, typePath);
						processed = true;
						break;
					}
				}
			}
			if (!processed && MemberFindingUtils.getAnnotationOfType(cid.getAnnotations(), ROO_GWT_REQUEST) == null) {
				boolean found = false;
				for (final ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails : typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(ROO_GWT_REQUEST)) {
					final AnnotationMetadata annotationMetadata = GwtUtils.getFirstAnnotation(classOrInterfaceTypeDetails, GwtUtils.REQUEST_ANNOTATIONS);
					if (annotationMetadata != null) {
						final AnnotationAttributeValue<?> attributeValue = annotationMetadata.getAttribute("value");
						if (attributeValue != null) {
							final String mirrorName = GwtUtils.getStringValue(attributeValue);
							if (mirrorName != null && cid.getName().getFullyQualifiedTypeName().equals(mirrorName)) {
								found = true;
								final JavaType typeName = PhysicalTypeIdentifier.getJavaType(classOrInterfaceTypeDetails.getDeclaredByMetadataId());
								final LogicalPath typePath = PhysicalTypeIdentifier.getPath(classOrInterfaceTypeDetails.getDeclaredByMetadataId());
								downstreamDependency = GwtRequestMetadata.createIdentifier(typeName, typePath);
								break;
							}
						}
					}
				}
				if (!found) {
					return;
				}
			} else if (!processed) {
				// A physical Java type has changed, and determine what the corresponding local metadata identification string would have been
				final JavaType typeName = PhysicalTypeIdentifier.getJavaType(upstreamDependency);
				final LogicalPath typePath = PhysicalTypeIdentifier.getPath(upstreamDependency);
				downstreamDependency = GwtRequestMetadata.createIdentifier(typeName, typePath);
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
}
