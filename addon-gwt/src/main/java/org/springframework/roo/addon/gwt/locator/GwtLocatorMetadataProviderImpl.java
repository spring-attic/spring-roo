package org.springframework.roo.addon.gwt.locator;

import static org.springframework.roo.model.JavaType.CLASS;

import java.lang.reflect.Modifier;
import java.util.Arrays;

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
import org.springframework.roo.classpath.details.MemberFindingUtils;
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
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

@Component(immediate = true)
@Service
public class GwtLocatorMetadataProviderImpl implements GwtLocatorMetadataProvider{

	// Constants
	private static final int LAYER_POSITION = LayerType.HIGHEST.getPosition();

	// Fields
	@Reference GwtTypeService gwtTypeService;
	@Reference LayerService layerService;
	@Reference MetadataDependencyRegistry metadataDependencyRegistry;
	@Reference MetadataService metadataService;
	@Reference PersistenceMemberLocator persistenceMemberLocator;
	@Reference ProjectOperations projectOperations;
	@Reference TypeLocationService typeLocationService;
	@Reference TypeManagementService typeManagementService;

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
	}

	public MetadataItem get(String metadataIdentificationString) {
		ProjectMetadata projectMetadata = projectOperations.getProjectMetadata();
		if (projectMetadata == null) {
			return null;
		}

		ClassOrInterfaceTypeDetails proxy = getGovernor(metadataIdentificationString);
		if (proxy == null) {
			return null;
		}

		AnnotationMetadata proxyAnnotation = GwtUtils.getFirstAnnotation(proxy, GwtUtils.PROXY_ANNOTATIONS);
		if (proxyAnnotation == null) {
			return null;
		}

		String locatorType = GwtUtils.getStringValue(proxyAnnotation.getAttribute("locator"));
		if (!StringUtils.hasText(locatorType)) {
			return null;
		}

		ClassOrInterfaceTypeDetails entity = gwtTypeService.lookupEntityFromProxy(proxy);
		if (entity == null) {
			return null;
		}

		MethodMetadata identifierAccessor = persistenceMemberLocator.getIdentifierAccessor(entity.getName());
		MethodMetadata versionAccessor = persistenceMemberLocator.getVersionAccessor(entity.getName());
		if (identifierAccessor == null || versionAccessor == null) {
			return null;
		}
		
		final JavaType identifierType = GwtUtils.convertPrimitiveType(identifierAccessor.getReturnType(), true);
		String locatorIdentifier = PhysicalTypeIdentifier.createIdentifier(new JavaType(locatorType));
		ClassOrInterfaceTypeDetailsBuilder locatorBuilder = new ClassOrInterfaceTypeDetailsBuilder(locatorIdentifier);
		AnnotationMetadataBuilder annotationMetadataBuilder = new AnnotationMetadataBuilder(RooJavaType.ROO_GWT_LOCATOR);
		annotationMetadataBuilder.addStringAttribute("value", entity.getName().getFullyQualifiedTypeName());
		locatorBuilder.addAnnotation(annotationMetadataBuilder);
		annotationMetadataBuilder = new AnnotationMetadataBuilder(SpringJavaType.COMPONENT);
		locatorBuilder.addAnnotation(annotationMetadataBuilder);
		locatorBuilder.setName(new JavaType(locatorType));
		locatorBuilder.setModifier(Modifier.PUBLIC);
		locatorBuilder.setPhysicalTypeCategory(PhysicalTypeCategory.CLASS);
		locatorBuilder.addExtendsTypes(new JavaType(GwtUtils.LOCATOR.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(entity.getName(), identifierType)));
		locatorBuilder.addMethod(getCreateMethod(locatorIdentifier, entity.getName()));
		locatorBuilder.addMethod(getFindMethod(locatorBuilder, locatorIdentifier, entity.getName(), identifierType));
		locatorBuilder.addMethod(getDomainTypeMethod(locatorIdentifier, entity.getName()));
		locatorBuilder.addMethod(getIdMethod(locatorIdentifier, entity.getName(), identifierAccessor));
		locatorBuilder.addMethod(getIdTypeMethod(locatorIdentifier, entity.getName(), identifierType));
		locatorBuilder.addMethod(getVersionMethod(locatorIdentifier, entity.getName(), versionAccessor));
		
		typeManagementService.createOrUpdateTypeOnDisk(locatorBuilder.build());
		return null;
	}

	private MethodMetadataBuilder getDomainTypeMethod(String declaredById, JavaType targetType) {
		InvocableMemberBodyBuilder invocableMemberBodyBuilder = InvocableMemberBodyBuilder.getInstance();
		invocableMemberBodyBuilder.append("return " + targetType.getSimpleTypeName() + ".class;");
		JavaType returnType = new JavaType(CLASS.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(targetType));
		return new MethodMetadataBuilder(declaredById, Modifier.PUBLIC, new JavaSymbolName("getDomainType"), returnType, invocableMemberBodyBuilder);
	}

	private MethodMetadataBuilder getIdMethod(String declaredById, JavaType targetType, MethodMetadata idAccessor) {
		InvocableMemberBodyBuilder invocableMemberBodyBuilder = InvocableMemberBodyBuilder.getInstance();
		invocableMemberBodyBuilder.append("return " + StringUtils.uncapitalize(targetType.getSimpleTypeName()) + "." + idAccessor.getMethodName() + "();");
		MethodMetadataBuilder getIdMethod = new MethodMetadataBuilder(declaredById, Modifier.PUBLIC, new JavaSymbolName("getId"), GwtUtils.convertPrimitiveType(idAccessor.getReturnType(), true), invocableMemberBodyBuilder);
		getIdMethod.addParameter(StringUtils.uncapitalize(targetType.getSimpleTypeName()), targetType);
		return getIdMethod;
	}

	private MethodMetadataBuilder getVersionMethod(String declaredById, JavaType targetType, MethodMetadata versionAccessor) {
		InvocableMemberBodyBuilder invocableMemberBodyBuilder = InvocableMemberBodyBuilder.getInstance();
		invocableMemberBodyBuilder.append("return " + StringUtils.uncapitalize(targetType.getSimpleTypeName()) + "." + versionAccessor.getMethodName() + "();");
		MethodMetadataBuilder getIdMethodBuilder = new MethodMetadataBuilder(declaredById, Modifier.PUBLIC, new JavaSymbolName("getVersion"), JavaType.OBJECT, invocableMemberBodyBuilder);
		getIdMethodBuilder.addParameter(StringUtils.uncapitalize(targetType.getSimpleTypeName()), targetType);
		return getIdMethodBuilder;
	}

	private MethodMetadataBuilder getIdTypeMethod(String declaredById, JavaType targetType, JavaType idType) {
		InvocableMemberBodyBuilder invocableMemberBodyBuilder = InvocableMemberBodyBuilder.getInstance();
		invocableMemberBodyBuilder.append("return " + idType.getSimpleTypeName() + ".class;");
		JavaType returnType = new JavaType(JavaType.CLASS.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(idType));
		return new MethodMetadataBuilder(declaredById, Modifier.PUBLIC, new JavaSymbolName("getIdType"), returnType, invocableMemberBodyBuilder);
	}

	private MethodMetadataBuilder getFindMethod(ClassOrInterfaceTypeDetailsBuilder locatorBuilder, String declaredById, JavaType targetType, JavaType idType) {
		MemberTypeAdditions findMethodAdditions = layerService.getMemberTypeAdditions(declaredById, CustomDataKeys.FIND_METHOD.name(), targetType, idType, LAYER_POSITION, new MethodParameter(idType, "id"));
		InvocableMemberBodyBuilder invocableMemberBodyBuilder = InvocableMemberBodyBuilder.getInstance();
		invocableMemberBodyBuilder.append("return ").append(findMethodAdditions.getMethodCall()).append(";");
		findMethodAdditions.copyAdditionsTo(locatorBuilder, locatorBuilder.build());
		MethodMetadataBuilder findMethodBuilder = new MethodMetadataBuilder(declaredById, Modifier.PUBLIC, new JavaSymbolName("find"), targetType, invocableMemberBodyBuilder);
		JavaType wildEntityType = new JavaType(targetType.getFullyQualifiedTypeName(), 0, DataType.VARIABLE, JavaType.WILDCARD_EXTENDS, null);
		JavaType classParameterType = new JavaType(JavaType.CLASS.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(wildEntityType));
		findMethodBuilder.addParameter("clazz", classParameterType);
		findMethodBuilder.addParameter("id", idType);
		return findMethodBuilder;
	}

	private MethodMetadataBuilder getCreateMethod(String declaredById, JavaType targetType) {
		InvocableMemberBodyBuilder invocableMemberBodyBuilder = InvocableMemberBodyBuilder.getInstance();
		invocableMemberBodyBuilder.append("return new " + targetType.getSimpleTypeName() + "();");
		MethodMetadataBuilder createMethodBuilder = new MethodMetadataBuilder(declaredById, Modifier.PUBLIC, new JavaSymbolName("create"), targetType, invocableMemberBodyBuilder);
		JavaType wildEntityType = new JavaType(targetType.getFullyQualifiedTypeName(), 0, DataType.VARIABLE, JavaType.WILDCARD_EXTENDS, null);
		JavaType classParameterType = new JavaType(JavaType.CLASS.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(wildEntityType));
		createMethodBuilder.addParameter("clazz", classParameterType);
		return createMethodBuilder;
	}

	public void notify(String upstreamDependency, String downstreamDependency) {
		ProjectMetadata projectMetadata = projectOperations.getProjectMetadata();
		if (projectMetadata == null) {
			return;
		}

		if (MetadataIdentificationUtils.isIdentifyingClass(downstreamDependency)) {
			Assert.isTrue(MetadataIdentificationUtils.getMetadataClass(upstreamDependency).equals(MetadataIdentificationUtils.getMetadataClass(PhysicalTypeIdentifier.getMetadataIdentiferType())), "Expected class-level notifications only for PhysicalTypeIdentifier (not '" + upstreamDependency + "')");

			ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeForIdentifier(upstreamDependency);
			if (cid == null) {
				return;
			}
			boolean processed = false;
			if (MemberFindingUtils.getAnnotationOfType(cid.getAnnotations(), RooJavaType.ROO_GWT_REQUEST) != null) {
				ClassOrInterfaceTypeDetails proxy = gwtTypeService.lookupProxyFromRequest(cid);
				if (proxy != null) {
					JavaType typeName = PhysicalTypeIdentifier.getJavaType(proxy.getDeclaredByMetadataId());
					Path typePath = PhysicalTypeIdentifier.getPath(proxy.getDeclaredByMetadataId());
					downstreamDependency = GwtLocatorMetadata.createIdentifier(typeName, typePath);
					processed = true;
				}
			}
			if (!processed && MemberFindingUtils.getAnnotationOfType(cid.getAnnotations(), RooJavaType.ROO_GWT_PROXY) == null) {
				boolean found = false;
				for (ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails : typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_GWT_PROXY)) {
					AnnotationMetadata annotationMetadata = GwtUtils.getFirstAnnotation(classOrInterfaceTypeDetails, GwtUtils.ROO_PROXY_REQUEST_ANNOTATIONS);
					if (annotationMetadata != null) {
						AnnotationAttributeValue<?> attributeValue = annotationMetadata.getAttribute("value");
						if (attributeValue != null) {
							String mirrorName = GwtUtils.getStringValue(attributeValue);
							if (mirrorName != null && cid.getName().getFullyQualifiedTypeName().equals(attributeValue.getValue())) {
								found = true;
								JavaType typeName = PhysicalTypeIdentifier.getJavaType(classOrInterfaceTypeDetails.getDeclaredByMetadataId());
								Path typePath = PhysicalTypeIdentifier.getPath(classOrInterfaceTypeDetails.getDeclaredByMetadataId());
								downstreamDependency = GwtLocatorMetadata.createIdentifier(typeName, typePath);
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
				downstreamDependency = GwtLocatorMetadata.createIdentifier(typeName, typePath);
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

	public String getProvidesType() {
		return GwtLocatorMetadata.getMetadataIdentifierType();
	}

	private ClassOrInterfaceTypeDetails getGovernor(String metadataIdentificationString) {
		JavaType governorTypeName = GwtLocatorMetadata.getJavaType(metadataIdentificationString);
		Path governorTypePath = GwtLocatorMetadata.getPath(metadataIdentificationString);

		String physicalTypeId = PhysicalTypeIdentifier.createIdentifier(governorTypeName, governorTypePath);
		return typeLocationService.getTypeForIdentifier(physicalTypeId);
	}
}
