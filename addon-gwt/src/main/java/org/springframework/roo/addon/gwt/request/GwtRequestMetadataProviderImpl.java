package org.springframework.roo.addon.gwt.request;

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
import org.springframework.roo.classpath.layers.LayerCustomDataKeys;
import org.springframework.roo.classpath.layers.LayerService;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.AbstractHashCodeTrackingMetadataNotifier;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

@Component(immediate = true)
@Service
public class GwtRequestMetadataProviderImpl extends AbstractHashCodeTrackingMetadataNotifier implements GwtRequestMetadataProvider{

	@Reference protected GwtTypeService gwtTypeService;
	@Reference protected LayerService layerService;
	@Reference protected MemberDetailsScanner memberDetailsScanner;
	@Reference protected ProjectOperations projectOperations;
	@Reference protected TypeLocationService typeLocationService;
	@Reference protected GwtFileManager gwtFileManager;

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
	}

	public String getProvidesType() {
		return GwtRequestMetadata.getMetadataIdentifierType();
	}

	public MetadataItem get(String metadataIdentificationString) {
		// Abort early if we can't continue
		ProjectMetadata projectMetadata = projectOperations.getProjectMetadata();
		if (projectMetadata == null) {
			return null;
		}

		ClassOrInterfaceTypeDetails request = getGovernor(metadataIdentificationString);
		if (request == null) {
			return null;
		}

		AnnotationMetadata mirrorAnnotation = MemberFindingUtils.getAnnotationOfType(request.getAnnotations(), RooJavaType.ROO_GWT_REQUEST);
		if (mirrorAnnotation == null) {
			return null;
		}

		JavaType targetType = GwtUtils.lookupRequestTargetType(request);
		if (targetType == null) {
			return null;
		}

		ClassOrInterfaceTypeDetails target = typeLocationService.findClassOrInterface(targetType);
		if (target == null || Modifier.isAbstract(target.getModifier())) {
			return null;
		}

 		MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(getClass().getName(), target);
 		if (memberDetails == null) {
 			return null;
 		}

		List<String> exclusionsList = getMethodExclusions(request);

		List<MethodMetadata> requestMethods = new ArrayList<MethodMetadata>();
		for (MethodMetadata methodMetadata : MemberFindingUtils.getMethods(memberDetails)) {
			if (Modifier.isPublic(methodMetadata.getModifier()) && !exclusionsList.contains(methodMetadata.getMethodName().getSymbolName())) {
				JavaType returnType = gwtTypeService.getGwtSideLeafType(methodMetadata.getReturnType(), projectMetadata, target.getName(), true, true);
				if (returnType == null) {
					continue;
				}
				MethodMetadataBuilder methodMetadataBuilder = new MethodMetadataBuilder(methodMetadata);
				methodMetadataBuilder.setReturnType(returnType);
				methodMetadataBuilder.setBodyBuilder(null);
				requestMethods.add(methodMetadataBuilder.build());
			}
		}
		GwtRequestMetadata metadata = new GwtRequestMetadata(request.getName(), updateRequest(request, requestMethods));
		notifyIfRequired(metadata);
		return metadata;
	}

	private List<String> getMethodExclusions(ClassOrInterfaceTypeDetails request) {
		List<String> exclusionList = GwtUtils.getAnnotationValues(request, RooJavaType.ROO_GWT_REQUEST, "exclude");
		ClassOrInterfaceTypeDetails proxy = gwtTypeService.lookupProxyFromRequest(request);
		if (proxy != null) {
			Boolean ignoreProxyExclusions = GwtUtils.getBooleanAnnotationValue(request, RooJavaType.ROO_GWT_REQUEST, "ignoreProxyExclusions", false);
			if (!ignoreProxyExclusions) {
				for (String exclusion : GwtUtils.getAnnotationValues(proxy, RooJavaType.ROO_GWT_PROXY, "exclude")) {
					exclusionList.add("set" + StringUtils.capitalize(exclusion));
					exclusionList.add("get" + StringUtils.capitalize(exclusion));
				}
				exclusionList.addAll(GwtUtils.getAnnotationValues(proxy, RooJavaType.ROO_GWT_PROXY, "exclude"));
			}
			Boolean ignoreProxyReadOnly = GwtUtils.getBooleanAnnotationValue(request, RooJavaType.ROO_GWT_REQUEST, "ignoreProxyReadOnly", false);
			if (!ignoreProxyReadOnly) {
				for (String exclusion : GwtUtils.getAnnotationValues(proxy, RooJavaType.ROO_GWT_PROXY, "readOnly")) {
					exclusionList.add("set" + StringUtils.capitalize(exclusion));
				}
			}
			Boolean dontIncludeProxyMethods = GwtUtils.getBooleanAnnotationValue(proxy, RooJavaType.ROO_GWT_REQUEST, "ignoreProxyReadOnly", true);
			if (dontIncludeProxyMethods) {
				for (MethodMetadata methodMetadata : proxy.getDeclaredMethods())  {
					exclusionList.add(methodMetadata.getMethodName().getSymbolName());
				}
			}
		}
		return exclusionList;
	}

	private ClassOrInterfaceTypeDetails getGovernor(String metadataIdentificationString) {
		JavaType governorTypeName = GwtRequestMetadata.getJavaType(metadataIdentificationString);
		Path governorTypePath = GwtRequestMetadata.getPath(metadataIdentificationString);

		String physicalTypeId = PhysicalTypeIdentifier.createIdentifier(governorTypeName, governorTypePath);
		return typeLocationService.getTypeForIdentifier(physicalTypeId);
	}

	public String updateRequest(ClassOrInterfaceTypeDetails request, List<MethodMetadata> requestMethods) {
		List<MethodMetadataBuilder> methods = new ArrayList<MethodMetadataBuilder>();
		for (MethodMetadata method : requestMethods) {
			methods.add(getRequestMethod(request, method));
		}

		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(request);
		ClassOrInterfaceTypeDetails entity = gwtTypeService.lookupEntityFromRequest(request);
		AnnotationMetadata annotationMetadata = GwtUtils.getFirstAnnotation(request, GwtUtils.REQUEST_ANNOTATIONS);
		if (annotationMetadata != null) {
			AnnotationMetadataBuilder annotationMetadataBuilder = new AnnotationMetadataBuilder(annotationMetadata);
			annotationMetadataBuilder.addStringAttribute("value", entity.getName().getFullyQualifiedTypeName());
			annotationMetadataBuilder.removeAttribute("locator");
			Set<ClassOrInterfaceTypeDetails> services = typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_SERVICE);
			for (ClassOrInterfaceTypeDetails serviceLayer : services) {
				AnnotationMetadata serviceAnnotation = MemberFindingUtils.getTypeAnnotation(serviceLayer, RooJavaType.ROO_SERVICE);
				AnnotationAttributeValue<List<ClassAttributeValue>> domainTypesAnnotation = serviceAnnotation.getAttribute("domainTypes");
				for (ClassAttributeValue classAttributeValue : domainTypesAnnotation.getValue()) {
					if (classAttributeValue.getValue().equals(entity.getName())) {
						annotationMetadataBuilder.addStringAttribute("value", serviceLayer.getName().getFullyQualifiedTypeName());
						annotationMetadataBuilder.addStringAttribute("locator", projectOperations.getProjectMetadata().getTopLevelPackage() + ".server.locator.GwtServiceLocator");
					}
				}
			}
			typeDetailsBuilder.removeAnnotation(annotationMetadata.getAnnotationType());
			typeDetailsBuilder.updateTypeAnnotation(annotationMetadataBuilder);
		}
		// Only inherit from RequestContext if extension is not already defined
		if (!typeDetailsBuilder.getExtendsTypes().contains(GwtUtils.REQUEST_CONTEXT)) {
			typeDetailsBuilder.addExtendsTypes(GwtUtils.REQUEST_CONTEXT);
		}
		typeDetailsBuilder.setDeclaredMethods(methods);
		return gwtFileManager.write(typeDetailsBuilder.build(), GwtUtils.PROXY_REQUEST_WARNING);
	}

	private MethodMetadataBuilder getRequestMethod(ClassOrInterfaceTypeDetails request, MethodMetadata methodMetadata) {

		ClassOrInterfaceTypeDetails proxy = gwtTypeService.lookupProxyFromRequest(request);
		ClassOrInterfaceTypeDetails service = gwtTypeService.lookupTargetServiceFromRequest(request);
		if (proxy == null || service == null) {
			return null;
		}
		ClassOrInterfaceTypeDetails entity = gwtTypeService.lookupEntityFromProxy(proxy);

		if (entity.getName().equals(service.getName()) && !Modifier.isStatic(methodMetadata.getModifier())) {
			List<JavaType> methodReturnTypeArgs = Arrays.asList(proxy.getName(), methodMetadata.getReturnType());
			JavaType methodReturnType = new JavaType(GwtUtils.INSTANCE_REQUEST.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, methodReturnTypeArgs);
			return getRequestMethod(request, methodMetadata, methodReturnType);
		}

		List<JavaType> methodReturnTypeArgs = Collections.singletonList(methodMetadata.getReturnType());
		JavaType methodReturnType = new JavaType(GwtUtils.REQUEST.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, methodReturnTypeArgs);
		return getRequestMethod(request, methodMetadata, methodReturnType);
	}

	private MethodMetadataBuilder getRequestMethod(ClassOrInterfaceTypeDetails request, MethodMetadata methodMetaData, JavaType methodReturnType) {
		List<AnnotatedJavaType> paramaterTypes = new ArrayList<AnnotatedJavaType>();
		ClassOrInterfaceTypeDetails mirroredTypeDetails = gwtTypeService.lookupEntityFromRequest(request);
		for (AnnotatedJavaType parameterType : methodMetaData.getParameterTypes()) {
			paramaterTypes.add(new AnnotatedJavaType(gwtTypeService.getGwtSideLeafType(parameterType.getJavaType(), projectOperations.getProjectMetadata(), mirroredTypeDetails.getName(), true, false)));
		}
		return new MethodMetadataBuilder(request.getDeclaredByMetadataId(), Modifier.ABSTRACT, methodMetaData.getMethodName(), methodReturnType, paramaterTypes, methodMetaData.getParameterNames(), new InvocableMemberBodyBuilder());
	}

	public void notify(String upstreamDependency, String downstreamDependency) {
		ProjectMetadata projectMetadata = projectOperations.getProjectMetadata();
		if (projectMetadata == null) {
			return;
		}

		if (MetadataIdentificationUtils.isIdentifyingClass(downstreamDependency)) {
			Assert.isTrue(MetadataIdentificationUtils.getMetadataClass(upstreamDependency).equals(MetadataIdentificationUtils.getMetadataClass(PhysicalTypeIdentifier.getMetadataIdentiferType())), "Expected class-level notifications only for PhysicalTypeIdentifier (not '" + upstreamDependency + "')");
			
			ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeForIdentifier(upstreamDependency);
			boolean processed = false;
			if (cid.getCustomData().get(LayerCustomDataKeys.LAYER_TYPE) != null) {
				@SuppressWarnings("unchecked")
				List<JavaType> layerTypes = (List<JavaType>) cid.getCustomData().get(LayerCustomDataKeys.LAYER_TYPE);
				for (ClassOrInterfaceTypeDetails request : typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_GWT_REQUEST)) {
					ClassOrInterfaceTypeDetails entity = gwtTypeService.lookupEntityFromRequest(request);
					if (layerTypes.contains(entity.getName())) {
						JavaType typeName = PhysicalTypeIdentifier.getJavaType(request.getDeclaredByMetadataId());
						Path typePath = PhysicalTypeIdentifier.getPath(request.getDeclaredByMetadataId());
						downstreamDependency = GwtRequestMetadata.createIdentifier(typeName, typePath);
						processed = true;
						break;
					}
				}
			}
			if (!processed && MemberFindingUtils.getAnnotationOfType(cid.getAnnotations(), RooJavaType.ROO_GWT_REQUEST) == null) {
				boolean found = false;
				for (ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails : typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_GWT_REQUEST)) {
					AnnotationMetadata annotationMetadata = GwtUtils.getFirstAnnotation(classOrInterfaceTypeDetails, GwtUtils.REQUEST_ANNOTATIONS);
					if (annotationMetadata != null) {
						AnnotationAttributeValue<?> attributeValue = annotationMetadata.getAttribute("value");
						if (attributeValue != null) {
							String mirrorName = GwtUtils.getStringValue(attributeValue);
							if (mirrorName != null && cid.getName().getFullyQualifiedTypeName().equals(mirrorName)) {
								found = true;
								JavaType typeName = PhysicalTypeIdentifier.getJavaType(classOrInterfaceTypeDetails.getDeclaredByMetadataId());
								Path typePath = PhysicalTypeIdentifier.getPath(classOrInterfaceTypeDetails.getDeclaredByMetadataId());
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
				JavaType typeName = PhysicalTypeIdentifier.getJavaType(upstreamDependency);
				Path typePath = PhysicalTypeIdentifier.getPath(upstreamDependency);
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

		metadataService.get(downstreamDependency, true);
	}
}
