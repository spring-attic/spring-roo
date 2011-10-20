package org.springframework.roo.addon.gwt.scaffold;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.gwt.GwtFileManager;
import org.springframework.roo.addon.gwt.GwtProxyProperty;
import org.springframework.roo.addon.gwt.GwtTemplateDataHolder;
import org.springframework.roo.addon.gwt.GwtTemplateService;
import org.springframework.roo.addon.gwt.GwtType;
import org.springframework.roo.addon.gwt.GwtTypeService;
import org.springframework.roo.addon.gwt.GwtUtils;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.ContextualPath;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Monitors Java types and if necessary creates/updates/deletes the GWT files maintained for each mirror-compatible object.
 * You can find a list of mirror-compatible objects in {@link org.springframework.roo.addon.gwt.GwtType}.
 * <p/>
 * <p/>
 * For now only @RooJpaEntity instances will be mirror-compatible.
 * <p/>
 * <p/>
 * Like all Roo add-ons, this provider aims to expose potentially-useful contents of the above files via {@link GwtScaffoldMetadata}.
 * It also attempts to avoiding writing to disk unless actually necessary.
 * <p/>
 * <p/>
 * A separate type monitors the creation/deletion of the aforementioned files to maintain "global indexes".
 *
 * @author Ben Alex
 * @author Alan Stewart
 * @author Ray Cromwell
 * @author Amit Manjhi
 * @since 1.1
 */
@Component(immediate = true)
@Service
public class GwtScaffoldMetadataProviderImpl implements GwtScaffoldMetadataProvider {

	// Fields
	@Reference protected GwtFileManager gwtFileManager;
	@Reference protected GwtTemplateService gwtTemplateService;
	@Reference protected GwtTypeService gwtTypeService;
	@Reference protected MetadataService metadataService;
	@Reference protected MetadataDependencyRegistry metadataDependencyRegistry;
	@Reference protected ProjectOperations projectOperations;
	@Reference protected TypeLocationService typeLocationService;

	protected void activate(final ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
	}

	protected void deactivate(final ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
	}

	public MetadataItem get(String metadataIdentificationString) {
		// Obtain the governor's information
		ClassOrInterfaceTypeDetails mirroredType = getGovernor(metadataIdentificationString);
		if (mirroredType == null || Modifier.isAbstract(mirroredType.getModifier())) {
			return null;
		}

		ClassOrInterfaceTypeDetails proxy = gwtTypeService.lookupProxyFromEntity(mirroredType);
		if (proxy == null ) {
			return null;
		}

		ClassOrInterfaceTypeDetails request = gwtTypeService.lookupRequestFromEntity(mirroredType);
		if (request == null) {
			return null;
		}

		if (proxy.getDeclaredMethods().isEmpty()) {
			return null;
		}

		Boolean scaffold = GwtUtils.getBooleanAnnotationValue(proxy, RooJavaType.ROO_GWT_PROXY, "scaffold", false);
		if (!scaffold) {
			return null;
		}

		String moduleName = PhysicalTypeIdentifier.getPath(proxy.getDeclaredByMetadataId()).getModule();
		ProjectMetadata projectMetadata = projectOperations.getProjectMetadata(moduleName);

		buildType(GwtType.APP_ENTITY_TYPES_PROCESSOR, projectMetadata);
		buildType(GwtType.APP_REQUEST_FACTORY, projectMetadata);
		buildType(GwtType.LIST_PLACE_RENDERER, projectMetadata);
		buildType(GwtType.MASTER_ACTIVITIES, projectMetadata);
		buildType(GwtType.LIST_PLACE_RENDERER, projectMetadata);
		buildType(GwtType.DETAILS_ACTIVITIES, projectMetadata);
		buildType(GwtType.MOBILE_ACTIVITIES, projectMetadata);

		GwtScaffoldMetadata gwtScaffoldMetadata = new GwtScaffoldMetadata(metadataIdentificationString);

		Map<JavaSymbolName, GwtProxyProperty> clientSideTypeMap = new LinkedHashMap<JavaSymbolName, GwtProxyProperty>();
		for (MethodMetadata proxyMethod : proxy.getDeclaredMethods()) {
			if (!proxyMethod.getMethodName().getSymbolName().startsWith("get")) {
				continue;
			}
			JavaSymbolName propertyName = new JavaSymbolName(StringUtils.uncapitalize(BeanInfoUtils.getPropertyNameForJavaBeanMethod(proxyMethod).getSymbolName()));
			JavaType propertyType = proxyMethod.getReturnType();
			ClassOrInterfaceTypeDetails ptmd = typeLocationService.getTypeDetails(propertyType);
			if (propertyType.isCommonCollectionType() && !propertyType.getParameters().isEmpty()) {
				ptmd = typeLocationService.getTypeDetails(propertyType.getParameters().get(0));
			}

			FieldMetadata field = proxy.getDeclaredField(propertyName);
			List<AnnotationMetadata> annotations = field != null ? field.getAnnotations() : Collections.<AnnotationMetadata> emptyList();

			GwtProxyProperty gwtProxyProperty = new GwtProxyProperty(projectOperations.getTopLevelPackage(moduleName), ptmd, propertyType, propertyName.getSymbolName(), annotations, proxyMethod.getMethodName().getSymbolName());
			clientSideTypeMap.put(propertyName, gwtProxyProperty);
		}

		GwtTemplateDataHolder templateDataHolder = gwtTemplateService.getMirrorTemplateTypeDetails(mirroredType, clientSideTypeMap, projectOperations.getProjectMetadata(moduleName));
		Map<GwtType, List<ClassOrInterfaceTypeDetails>> typesToBeWritten = new HashMap<GwtType, List<ClassOrInterfaceTypeDetails>>();
		Map<String, String> xmlToBeWritten = new HashMap<String, String>();

		Map<GwtType, JavaType> mirrorTypeMap = GwtUtils.getMirrorTypeMap(projectOperations, mirroredType.getName());
	   	mirrorTypeMap.put(GwtType.PROXY, proxy.getName());
		mirrorTypeMap.put(GwtType.REQUEST, request.getName());

		for (Map.Entry<GwtType, JavaType> entry : mirrorTypeMap.entrySet()) {
			GwtType gwtType = entry.getKey();
			JavaType javaType = entry.getValue();
			if (!gwtType.isMirrorType() || gwtType.equals(GwtType.PROXY) || gwtType.equals(GwtType.REQUEST)) {
				continue;
			}
			gwtType.dynamicallyResolveFieldsToWatch(clientSideTypeMap);
			gwtType.dynamicallyResolveMethodsToWatch(proxy.getName(), clientSideTypeMap, projectOperations);

			List<MemberHoldingTypeDetails> extendsTypes = gwtTypeService.getExtendsTypes(templateDataHolder.getTemplateTypeDetailsMap().get(gwtType));

			typesToBeWritten.put(gwtType, gwtTypeService.buildType(gwtType, templateDataHolder.getTemplateTypeDetailsMap().get(gwtType), extendsTypes));

			if (gwtType.isCreateUiXml()) {
				String destFile = gwtType.getPath().canonicalFileSystemPath(projectOperations) + File.separatorChar + javaType.getSimpleTypeName() + ".ui.xml";
				String contents = gwtTemplateService.buildUiXml(templateDataHolder.getXmlTemplates().get(gwtType), destFile, new ArrayList<MethodMetadata>(proxy.getDeclaredMethods()));
				xmlToBeWritten.put(destFile, contents);
			}
		}

		// Our general strategy is to instantiate GwtScaffoldMetadata, which offers a conceptual representation of what should go into the 4 key-specific types; after that we do comparisons and write to disk if needed
		for (Map.Entry<GwtType, List<ClassOrInterfaceTypeDetails>> entry : typesToBeWritten.entrySet()) {
			gwtFileManager.write(typesToBeWritten.get(entry.getKey()), entry.getKey().isOverwriteConcrete());
		}
		for (ClassOrInterfaceTypeDetails type : templateDataHolder.getTypeList()) {
			gwtFileManager.write(type, false);
		}
		for (Map.Entry<String, String> entry : xmlToBeWritten.entrySet()) {
			gwtFileManager.write(entry.getKey(), entry.getValue());
		}
		for (Map.Entry<String, String> entry : templateDataHolder.getXmlMap().entrySet()) {
			gwtFileManager.write(entry.getKey(), entry.getValue());
		}

		return gwtScaffoldMetadata;
	}

	private ClassOrInterfaceTypeDetails getGovernor(final String metadataIdentificationString) {
		JavaType governorTypeName = GwtScaffoldMetadata.getJavaType(metadataIdentificationString);
		ContextualPath governorTypePath = GwtScaffoldMetadata.getPath(metadataIdentificationString);

		String physicalTypeId = PhysicalTypeIdentifier.createIdentifier(governorTypeName, governorTypePath);
		return typeLocationService.getTypeDetails(physicalTypeId);
	}

	private void buildType(GwtType type, ProjectMetadata projectMetadata) {
		gwtTypeService.buildType(type, gwtTemplateService.getStaticTemplateTypeDetails(type, projectMetadata), projectMetadata.getModuleName());
	}

	public void notify(String upstreamDependency, String downstreamDependency) {

		if (MetadataIdentificationUtils.isIdentifyingClass(downstreamDependency)) {
			Assert.isTrue(MetadataIdentificationUtils.getMetadataClass(upstreamDependency).equals(MetadataIdentificationUtils.getMetadataClass(PhysicalTypeIdentifier.getMetadataIdentiferType())), "Expected class-level notifications only for PhysicalTypeIdentifier (not '" + upstreamDependency + "')");
			ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(upstreamDependency);
			if (cid == null) {
				return;
			}

			if (MemberFindingUtils.getAnnotationOfType(cid.getAnnotations(), RooJavaType.ROO_GWT_PROXY) != null) {
				ClassOrInterfaceTypeDetails entity = gwtTypeService.lookupEntityFromProxy(cid);
				if (entity != null) {
					upstreamDependency = entity.getDeclaredByMetadataId();
				}
			} else if (MemberFindingUtils.getAnnotationOfType(cid.getAnnotations(), RooJavaType.ROO_GWT_REQUEST) != null) {
				ClassOrInterfaceTypeDetails entity = gwtTypeService.lookupEntityFromRequest(cid);
				if (entity != null) {
					upstreamDependency = entity.getDeclaredByMetadataId();
				}
			} else if (MemberFindingUtils.getAnnotationOfType(cid.getAnnotations(), RooJavaType.ROO_GWT_LOCATOR) != null) {
				ClassOrInterfaceTypeDetails entity = gwtTypeService.lookupEntityFromLocator(cid);
				if (entity != null) {
					upstreamDependency = entity.getDeclaredByMetadataId();
				}
			}
			// A physical Java type has changed, and determine what the corresponding local metadata identification string would have been
			JavaType typeName = PhysicalTypeIdentifier.getJavaType(upstreamDependency);
			ContextualPath typePath = PhysicalTypeIdentifier.getPath(upstreamDependency);
			downstreamDependency = createLocalIdentifier(typeName, typePath);

		}

		// We only need to proceed if the downstream dependency relationship is not already registered
		// (if it's already registered, the event will be delivered directly later on)
		if (metadataDependencyRegistry.getDownstream(upstreamDependency).contains(downstreamDependency)) {
			return;
		}

		// We should now have an instance-specific "downstream dependency" that can be processed by this class
		Assert.isTrue(MetadataIdentificationUtils.getMetadataClass(downstreamDependency).equals(MetadataIdentificationUtils.getMetadataClass(getProvidesType())), "Unexpected downstream notification for '" + downstreamDependency + "' to this provider (which uses '" + getProvidesType() + "'");

		metadataService.get(downstreamDependency, true);
	}

	private String createLocalIdentifier(final JavaType javaType, final ContextualPath path) {
		return GwtScaffoldMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return GwtScaffoldMetadata.getMetadataIdentifierType();
	}
}
