package org.springframework.roo.addon.gwt;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Monitors Java types and if necessary creates/updates/deletes the GWT files maintained for each mirror-compatible object.
 * You can find a list of mirror-compatible objects in {@link GwtType}.
 * <p/>
 * <p/>
 * For now only @RooEntity instances will be mirror-compatible.
 * <p/>
 * <p/>
 * Like all Roo add-ons, this provider aims to expose potentially-useful contents of the above files via {@link GwtMetadata}.
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
public class GwtMetadataProviderImpl implements GwtMetadataProvider {
	@Reference private FileManager fileManager;
	@Reference private GwtFileManager gwtFileManager;
	@Reference private GwtTemplateService gwtTemplateService;
	@Reference private GwtTypeService gwtTypeService;
	@Reference private MetadataService metadataService;
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;
	@Reference private ProjectOperations projectOperations;
	private Map<String, Set<String>> dependsOn = new HashMap<String, Set<String>>();

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
	}

	public MetadataItem get(String metadataIdentificationString) {
		// Abort early if we can't continue
		ProjectMetadata projectMetadata = projectOperations.getProjectMetadata();
		if (projectMetadata == null) {
			return null;
		}

		if (!fileManager.exists(GwtPath.MANAGED_REQUEST.canonicalFileSystemPath(projectMetadata))) {
			return null;
		}

		// Start by converting the MID into something more useful (we still use the concept of a governor, given the MID refers to the governor)
		JavaType governorTypeName = GwtMetadata.getJavaType(metadataIdentificationString);
		Path governorTypePath = GwtMetadata.getPath(metadataIdentificationString);
		// Abort if this is for a .java file that is not on the main source path
		if (!governorTypePath.equals(Path.SRC_MAIN_JAVA)) {
			return null;
		}

		// We are only interested in source that is not in the client package
		if (governorTypeName.getPackage().getFullyQualifiedPackageName().startsWith(GwtPath.CLIENT.packageName(projectMetadata))) {
			return null;
		}

		String physicalTypeId = PhysicalTypeIdentifier.createIdentifier(governorTypeName, governorTypePath);
		// Obtain the governor's information
		PhysicalTypeMetadata governorPhysicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(physicalTypeId);
		if (governorPhysicalTypeMetadata == null || !governorPhysicalTypeMetadata.isValid() || !(governorPhysicalTypeMetadata.getMemberHoldingTypeDetails() instanceof ClassOrInterfaceTypeDetails)) {
			return null;
		}

		ClassOrInterfaceTypeDetails governorTypeDetails = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
		EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(governorTypeName, governorTypePath));

		// We are only interested in a certain types, we must verify that the MID passed in corresponds with such a type.
		if (!GwtUtils.isMappable(governorTypeDetails, entityMetadata)) {
			return null;
		}

		List<MethodMetadata> proxyMethods = gwtTypeService.getProxyMethods(governorTypeDetails);
		List<MethodMetadata> convertedProxyMethods = new LinkedList<MethodMetadata>();
		boolean dependsOnSomething = false;
		List<String> sourcePaths = gwtTypeService.getSourcePaths();
		for (MethodMetadata method : proxyMethods) {
			JavaType returnType = method.getReturnType().isCommonCollectionType() && method.getReturnType().getParameters().size() != 0 ? method.getReturnType().getParameters().get(0) : method.getReturnType();
			if (gwtTypeService.isDomainObject(returnType) && !method.getReturnType().equals(governorTypeName)) {
				JavaType proxyType = GwtUtils.convertGovernorTypeNameIntoKeyTypeName(GwtType.PROXY, projectMetadata, returnType);
				PhysicalTypeMetadata ptmd = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(proxyType, Path.SRC_MAIN_JAVA));
				if (ptmd == null) {
					Set<String> set = new HashSet<String>();
					if (dependsOn.containsKey(createLocalIdentifier(returnType, Path.SRC_MAIN_JAVA))) {
						set = dependsOn.get(createLocalIdentifier(returnType, Path.SRC_MAIN_JAVA));
					}
					set.add(metadataIdentificationString);
					dependsOn.put(createLocalIdentifier(returnType, Path.SRC_MAIN_JAVA), set);
					dependsOnSomething = true;
					break;
				}
			}
			JavaType gwtType = gwtTypeService.getGwtSideLeafType(method.getReturnType(), projectOperations.getProjectMetadata(), governorTypeDetails.getName(), false);
			MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(method);
			methodBuilder.setReturnType(gwtType);
			MethodMetadata convertedMethod = methodBuilder.build();
			if (gwtTypeService.isMethodReturnTypesInSourcePath(convertedMethod, governorTypeDetails, sourcePaths)) {
				convertedProxyMethods.add(methodBuilder.build());
			}
		}

		List<MethodMetadata> requestMethods = gwtTypeService.getRequestMethods(governorTypeDetails);

		GwtMetadata gwtMetadata = new GwtMetadata(metadataIdentificationString, governorTypeDetails, projectMetadata, convertedProxyMethods, requestMethods, entityMetadata);
		gwtFileManager.write(gwtMetadata.buildProxy(), true);
		gwtFileManager.write(gwtMetadata.buildRequest(), true);

		if (dependsOnSomething) {
			return null;
		}

		if (dependsOn.containsKey(metadataIdentificationString)) {
			Set<String> toRemove = new HashSet<String>();
			Set<String> toAlert = dependsOn.get(metadataIdentificationString);
			for (String toGet : toAlert) {
				metadataService.get(toGet);
				toRemove.add(toGet);
			}
			toAlert.removeAll(toRemove);
		}

		buildType(GwtType.APP_ENTITY_TYPES_PROCESSOR);
		buildType(GwtType.APP_REQUEST_FACTORY);
		buildType(GwtType.LIST_PLACE_RENDERER);
		buildType(GwtType.MASTER_ACTIVITIES);
		buildType(GwtType.LIST_PLACE_RENDERER);
		buildType(GwtType.DETAILS_ACTIVITIES);
		buildType(GwtType.MOBILE_ACTIVITIES);

		Map<JavaSymbolName, GwtProxyProperty> clientSideTypeMap = new LinkedHashMap<JavaSymbolName, GwtProxyProperty>();

		for (MethodMetadata proxyMethod : convertedProxyMethods) {
			JavaSymbolName propertyName = new JavaSymbolName(StringUtils.uncapitalize(BeanInfoUtils.getPropertyNameForJavaBeanMethod(proxyMethod).getSymbolName()));
			JavaType propertyType = proxyMethod.getReturnType();
			PhysicalTypeMetadata ptmd = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(propertyType, Path.SRC_MAIN_JAVA));

			if (propertyType.isCommonCollectionType()) {
				ptmd = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(propertyType.getParameters().get(0), Path.SRC_MAIN_JAVA));
			}

			GwtProxyProperty gwtProxyProperty = new GwtProxyProperty(projectOperations.getProjectMetadata(), propertyType, ptmd, propertyName.getSymbolName(), proxyMethod.getMethodName().getSymbolName());
			clientSideTypeMap.put(propertyName, gwtProxyProperty);
		}

		GwtTemplateDataHolder templateDataHolder = gwtTemplateService.getMirrorTemplateTypeDetails(governorTypeDetails, clientSideTypeMap);
		Map<GwtType, List<ClassOrInterfaceTypeDetails>> typesToBeWritten = new HashMap<GwtType, List<ClassOrInterfaceTypeDetails>>();
		Map<String, String> xmlToBeWritten = new HashMap<String, String>();
		Map<GwtType, JavaType> mirrorTypeMap = GwtUtils.getMirrorTypeMap(projectMetadata, governorTypeName);

		for (GwtType gwtType : mirrorTypeMap.keySet()) {
			if (!gwtType.isMirrorType() || gwtType.equals(GwtType.PROXY) || gwtType.equals(GwtType.REQUEST)) {
				continue;
			}
			gwtType.dynamicallyResolveFieldsToWatch(clientSideTypeMap);
			gwtType.dynamicallyResolveMethodsToWatch(mirrorTypeMap.get(GwtType.PROXY), clientSideTypeMap, projectMetadata);

			List<MemberHoldingTypeDetails> extendsTypes = gwtTypeService.getExtendsTypes(templateDataHolder.getTemplateTypeDetailsMap().get(gwtType));

			typesToBeWritten.put(gwtType, gwtTypeService.buildType(gwtType, templateDataHolder.getTemplateTypeDetailsMap().get(gwtType), extendsTypes));

			if (gwtType.isCreateUiXml()) {
				String destFile = gwtType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + mirrorTypeMap.get(gwtType).getSimpleTypeName() + ".ui.xml";
				String contents = templateDataHolder.getXmlTemplates().get(gwtType);
				if (fileManager.exists(destFile)) {
					contents = gwtMetadata.buildUiXml(templateDataHolder.getXmlTemplates().get(gwtType), destFile);
				}
				xmlToBeWritten.put(destFile, contents);
			}
		}

		// Our general strategy is to instantiate GwtMetadata, which offers a conceptual representation of what should go into the 4 key-specific types; after that we do comparisons and write to disk if needed
		for (GwtType type : typesToBeWritten.keySet()) {
			gwtFileManager.write(typesToBeWritten.get(type), type.isOverwriteConcrete());
		}
		for (ClassOrInterfaceTypeDetails type : templateDataHolder.getTypeList()) {
			gwtFileManager.write(type, false);
		}
		for (String destFile : xmlToBeWritten.keySet()) {
			gwtFileManager.write(destFile, xmlToBeWritten.get(destFile));
		}
		for (String destFile : templateDataHolder.getXmlMap().keySet()) {
			gwtFileManager.write(destFile, templateDataHolder.getXmlMap().get(destFile));
		}

		return gwtMetadata;
	}

	private void buildType(GwtType type) {
		if (GwtType.LIST_PLACE_RENDERER.equals(type)) {
			ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
			HashMap<JavaSymbolName, List<JavaType>> watchedMethods = new HashMap<JavaSymbolName, List<JavaType>>();
			watchedMethods.put(new JavaSymbolName("render"), Collections.singletonList(new JavaType(projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName() + ".client.scaffold.place.ProxyListPlace")));
			type.setWatchedMethods(watchedMethods);
		} else {
			type.resolveMethodsToWatch(type);
		}

		type.resolveWatchedFieldNames(type);
		List<ClassOrInterfaceTypeDetails> templateTypeDetails = gwtTemplateService.getStaticTemplateTypeDetails(type);
		List<ClassOrInterfaceTypeDetails> typesToBeWritten = new ArrayList<ClassOrInterfaceTypeDetails>();
		for (ClassOrInterfaceTypeDetails templateTypeDetail : templateTypeDetails) {
			typesToBeWritten.addAll(gwtTypeService.buildType(type, templateTypeDetail, gwtTypeService.getExtendsTypes(templateTypeDetail)));
		}
		gwtFileManager.write(typesToBeWritten, type.isOverwriteConcrete());
	}

	public void notify(String upstreamDependency, String downstreamDependency) {
		ProjectMetadata projectMetadata = projectOperations.getProjectMetadata();
		if (projectMetadata == null) {
			return;
		}

		if (MetadataIdentificationUtils.isIdentifyingClass(downstreamDependency)) {
			Assert.isTrue(MetadataIdentificationUtils.getMetadataClass(upstreamDependency).equals(MetadataIdentificationUtils.getMetadataClass(PhysicalTypeIdentifier.getMetadataIdentiferType())), "Expected class-level notifications only for PhysicalTypeIdentifier (not '" + upstreamDependency + "')");

			// A physical Java type has changed, and determine what the corresponding local metadata identification string would have been
			JavaType typeName = PhysicalTypeIdentifier.getJavaType(upstreamDependency);
			Path typePath = PhysicalTypeIdentifier.getPath(upstreamDependency);
			downstreamDependency = createLocalIdentifier(typeName, typePath);

			// We only need to proceed if the downstream dependency relationship is not already registered
			// (if it's already registered, the event will be delivered directly later on)
			if (metadataDependencyRegistry.getDownstream(upstreamDependency).contains(downstreamDependency)) {
				return;
			}
		}

		// We should now have an instance-specific "downstream dependency" that can be processed by this class
		Assert.isTrue(MetadataIdentificationUtils.getMetadataClass(downstreamDependency).equals(MetadataIdentificationUtils.getMetadataClass(getProvidesType())), "Unexpected downstream notification for '" + downstreamDependency + "' to this provider (which uses '" + getProvidesType() + "'");

		metadataService.evict(downstreamDependency);
		if (get(downstreamDependency) != null) {
			metadataDependencyRegistry.notifyDownstream(downstreamDependency);
		}
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return GwtMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return GwtMetadata.getMetadataIdentifierType();
	}
}
