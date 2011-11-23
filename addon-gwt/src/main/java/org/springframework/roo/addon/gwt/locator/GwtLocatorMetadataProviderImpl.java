package org.springframework.roo.addon.gwt.locator;

import static org.springframework.roo.addon.gwt.GwtJavaType.LOCATOR;
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
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

@Component(immediate = true)
@Service
public class GwtLocatorMetadataProviderImpl implements GwtLocatorMetadataProvider {

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

		AnnotationMetadata proxyAnnotation = GwtUtils.getFirstAnnotation(proxy, GwtUtils.PROXY_ANNOTATIONS);
		if (proxyAnnotation == null) {
			return null;
		}

		final String locatorType = GwtUtils.getStringValue(proxyAnnotation.getAttribute("locator"));
		if (StringUtils.isBlank(locatorType)) {
			return null;
		}

		ClassOrInterfaceTypeDetails entityType = gwtTypeService.lookupEntityFromProxy(proxy);
		if (entityType == null || Modifier.isAbstract(entityType.getModifier())) {
			return null;
		}

		final JavaType entity = entityType.getName();
		MethodMetadata identifierAccessor = persistenceMemberLocator.getIdentifierAccessor(entity);
		MethodMetadata versionAccessor = persistenceMemberLocator.getVersionAccessor(entity);
		if (identifierAccessor == null || versionAccessor == null) {
			return null;
		}

		final JavaType identifierType = GwtUtils.convertPrimitiveType(identifierAccessor.getReturnType(), true);
		final String locatorPhysicalTypeId = PhysicalTypeIdentifier.createIdentifier(new JavaType(locatorType), PhysicalTypeIdentifier.getPath(proxy.getDeclaredByMetadataId()));
		ClassOrInterfaceTypeDetailsBuilder locatorBuilder = new ClassOrInterfaceTypeDetailsBuilder(locatorPhysicalTypeId);
		AnnotationMetadataBuilder annotationMetadataBuilder = new AnnotationMetadataBuilder(RooJavaType.ROO_GWT_LOCATOR);
		annotationMetadataBuilder.addStringAttribute("value", entity.getFullyQualifiedTypeName());
		locatorBuilder.addAnnotation(annotationMetadataBuilder);

		locatorBuilder.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.COMPONENT));
		locatorBuilder.setName(new JavaType(locatorType));
		locatorBuilder.setModifier(Modifier.PUBLIC);
		locatorBuilder.setPhysicalTypeCategory(PhysicalTypeCategory.CLASS);
		locatorBuilder.addExtendsTypes(new JavaType(LOCATOR.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(entity, identifierType)));
		locatorBuilder.addMethod(getCreateMethod(locatorPhysicalTypeId, entity));
		
		MemberTypeAdditions findMethodAdditions = layerService.getMemberTypeAdditions(locatorPhysicalTypeId, CustomDataKeys.FIND_METHOD.name(), entity, identifierType, LAYER_POSITION, new MethodParameter(identifierType, "id"));
		Assert.notNull(findMethodAdditions, "Find method not available for entity '" + entity.getFullyQualifiedTypeName() + "'");
		locatorBuilder.addMethod(getFindMethod(findMethodAdditions, locatorBuilder, locatorPhysicalTypeId, entity, identifierType));
		
		locatorBuilder.addMethod(getDomainTypeMethod(locatorPhysicalTypeId, entity));
		locatorBuilder.addMethod(getIdMethod(locatorPhysicalTypeId, entity, identifierAccessor));
		locatorBuilder.addMethod(getIdTypeMethod(locatorPhysicalTypeId, entity, identifierType));
		locatorBuilder.addMethod(getVersionMethod(locatorPhysicalTypeId, entity, versionAccessor));

		typeManagementService.createOrUpdateTypeOnDisk(locatorBuilder.build());
		return null;
	}

	private MethodMetadataBuilder getDomainTypeMethod(final String declaredById, final JavaType targetType) {
		InvocableMemberBodyBuilder invocableMemberBodyBuilder = InvocableMemberBodyBuilder.getInstance();
		invocableMemberBodyBuilder.append("return " + targetType.getSimpleTypeName() + ".class;");
		JavaType returnType = new JavaType(CLASS.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(targetType));
		return new MethodMetadataBuilder(declaredById, Modifier.PUBLIC, new JavaSymbolName("getDomainType"), returnType, invocableMemberBodyBuilder);
	}

	private MethodMetadataBuilder getIdMethod(final String declaredById, final JavaType targetType, final MethodMetadata idAccessor) {
		InvocableMemberBodyBuilder invocableMemberBodyBuilder = InvocableMemberBodyBuilder.getInstance();
		invocableMemberBodyBuilder.append("return " + StringUtils.uncapitalize(targetType.getSimpleTypeName()) + "." + idAccessor.getMethodName() + "();");
		MethodMetadataBuilder getIdMethod = new MethodMetadataBuilder(declaredById, Modifier.PUBLIC, new JavaSymbolName("getId"), GwtUtils.convertPrimitiveType(idAccessor.getReturnType(), true), invocableMemberBodyBuilder);
		getIdMethod.addParameter(StringUtils.uncapitalize(targetType.getSimpleTypeName()), targetType);
		return getIdMethod;
	}

	private MethodMetadataBuilder getVersionMethod(final String declaredById, final JavaType targetType, final MethodMetadata versionAccessor) {
		InvocableMemberBodyBuilder invocableMemberBodyBuilder = InvocableMemberBodyBuilder.getInstance();
		invocableMemberBodyBuilder.append("return " + StringUtils.uncapitalize(targetType.getSimpleTypeName()) + "." + versionAccessor.getMethodName() + "();");
		MethodMetadataBuilder getIdMethodBuilder = new MethodMetadataBuilder(declaredById, Modifier.PUBLIC, new JavaSymbolName("getVersion"), JavaType.OBJECT, invocableMemberBodyBuilder);
		getIdMethodBuilder.addParameter(StringUtils.uncapitalize(targetType.getSimpleTypeName()), targetType);
		return getIdMethodBuilder;
	}

	private MethodMetadataBuilder getIdTypeMethod(final String declaredById, final JavaType targetType, final JavaType idType) {
		InvocableMemberBodyBuilder invocableMemberBodyBuilder = InvocableMemberBodyBuilder.getInstance();
		invocableMemberBodyBuilder.append("return " + idType.getSimpleTypeName() + ".class;");
		JavaType returnType = new JavaType(JavaType.CLASS.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(idType));
		return new MethodMetadataBuilder(declaredById, Modifier.PUBLIC, new JavaSymbolName("getIdType"), returnType, invocableMemberBodyBuilder);
	}

	private MethodMetadataBuilder getFindMethod(final MemberTypeAdditions findMethodAdditions, final ClassOrInterfaceTypeDetailsBuilder locatorBuilder, final String declaredById, final JavaType targetType, final JavaType idType) {
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

	private MethodMetadataBuilder getCreateMethod(final String declaredById, final JavaType targetType) {
		InvocableMemberBodyBuilder invocableMemberBodyBuilder = InvocableMemberBodyBuilder.getInstance();
		invocableMemberBodyBuilder.append("return new " + targetType.getSimpleTypeName() + "();");
		MethodMetadataBuilder createMethodBuilder = new MethodMetadataBuilder(declaredById, Modifier.PUBLIC, new JavaSymbolName("create"), targetType, invocableMemberBodyBuilder);
		JavaType wildEntityType = new JavaType(targetType.getFullyQualifiedTypeName(), 0, DataType.VARIABLE, JavaType.WILDCARD_EXTENDS, null);
		JavaType classParameterType = new JavaType(JavaType.CLASS.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(wildEntityType));
		createMethodBuilder.addParameter("clazz", classParameterType);
		return createMethodBuilder;
	}

	public void notify(final String upstreamDependency, String downstreamDependency) {
		if (MetadataIdentificationUtils.isIdentifyingClass(downstreamDependency)) {
			Assert.isTrue(MetadataIdentificationUtils.getMetadataClass(upstreamDependency).equals(MetadataIdentificationUtils.getMetadataClass(PhysicalTypeIdentifier.getMetadataIdentiferType())), "Expected class-level notifications only for PhysicalTypeIdentifier (not '" + upstreamDependency + "')");

			ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(upstreamDependency);
			if (cid == null) {
				return;
			}
			boolean processed = false;
			if (MemberFindingUtils.getAnnotationOfType(cid.getAnnotations(), RooJavaType.ROO_GWT_REQUEST) != null) {
				ClassOrInterfaceTypeDetails proxy = gwtTypeService.lookupProxyFromRequest(cid);
				if (proxy != null) {
					JavaType typeName = PhysicalTypeIdentifier.getJavaType(proxy.getDeclaredByMetadataId());
					LogicalPath typePath = PhysicalTypeIdentifier.getPath(proxy.getDeclaredByMetadataId());
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
								LogicalPath typePath = PhysicalTypeIdentifier.getPath(classOrInterfaceTypeDetails.getDeclaredByMetadataId());
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
				LogicalPath typePath = PhysicalTypeIdentifier.getPath(upstreamDependency);
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

		metadataService.evictAndGet(downstreamDependency);
	}

	public String getProvidesType() {
		return GwtLocatorMetadata.getMetadataIdentifierType();
	}

	private ClassOrInterfaceTypeDetails getGovernor(final String metadataIdentificationString) {
		JavaType governorTypeName = GwtLocatorMetadata.getJavaType(metadataIdentificationString);
		LogicalPath governorTypePath = GwtLocatorMetadata.getPath(metadataIdentificationString);
		String physicalTypeId = PhysicalTypeIdentifier.createIdentifier(governorTypeName, governorTypePath);
		return typeLocationService.getTypeDetails(physicalTypeId);
	}
}
