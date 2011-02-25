package org.springframework.roo.addon.gwt;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultPhysicalTypeMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.util.Assert;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	@Reference private GwtFileManager gwtFileManager;
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;
	@Reference private FileManager fileManager;
	@Reference private MetadataService metadataService;
	@Reference private GwtTypeNamingStrategy gwtTypeNamingStrategy;
	@Reference private MemberDetailsScanner memberDetailsScanner;
	@Reference private GwtTemplatingService gwtTemplateService;
	@Reference private GwtTypeService gwtTypeService;

	public MetadataItem get(String metadataIdentificationString) {
		// Abort early if we can't continue
		ProjectMetadata projectMetadata = getProjectMetadata();
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

		// Abort if this is for a .java file under any of the GWT-related directories
		for (GwtPath path : GwtPath.values()) {
			if (governorTypeName.getPackage().getFullyQualifiedPackageName().equals(path.packageName(projectMetadata))) {
				return null;
			}
		}

		// Obtain the governor's information
		PhysicalTypeMetadata governorPhysicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(governorTypeName, governorTypePath));
		if (governorPhysicalTypeMetadata == null || !governorPhysicalTypeMetadata.isValid() || !(governorPhysicalTypeMetadata.getMemberHoldingTypeDetails() instanceof ClassOrInterfaceTypeDetails)) {
			return null;
		}

		ClassOrInterfaceTypeDetails governorTypeDetails = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();

		if (Modifier.isAbstract(governorTypeDetails.getModifier())) {
			return null;
		}

		EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(governorTypeName, governorTypePath));

		//We are only interested in a certain types, we must verify that the MID passed in corresponds with such a type.
		if (!GwtUtils.isMappable(governorTypeDetails, entityMetadata)) {
			return null;
		}

		List<MemberHoldingTypeDetails> memberHoldingTypeDetails = memberDetailsScanner.getMemberDetails(GwtMetadataProviderImpl.class.getName(), governorTypeDetails).getDetails();
		Map<GwtType, JavaType> mirrorTypeMap = GwtUtils.getMirrorTypeMap(projectMetadata, governorTypeName);
		Map<JavaType, JavaType> gwtClientTypeMap = gwtTypeService.getClientTypeMap(governorTypeDetails);

		// Next let's obtain a handle to the "proxy" we'd want to produce/modify/delete as applicable for this governor
		JavaType keyTypeName = gwtTypeNamingStrategy.convertGovernorTypeNameIntoKeyTypeName(GwtType.PROXY, projectMetadata, governorTypeName);
		Path keyTypePath = Path.SRC_MAIN_JAVA;

		metadataDependencyRegistry.registerDependency(entityMetadata.getId(), metadataIdentificationString);

		// Lookup any existing proxy type and verify it is mapped to this governor MID (ie detect name collisions and abort if necessary)
		// We do this before deleting, to verify we don't get into a delete-create-delete type case if there are name collisions
		PhysicalTypeMetadata keyPhysicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(keyTypeName, keyTypePath));

		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.createIdentifier(keyTypeName, keyTypePath), metadataIdentificationString);

		if (keyPhysicalTypeMetadata != null && keyPhysicalTypeMetadata.isValid() && keyPhysicalTypeMetadata.getMemberHoldingTypeDetails() instanceof ClassOrInterfaceTypeDetails) {
			// The key presently exists, so do a sanity check
			ClassOrInterfaceTypeDetails cid = (ClassOrInterfaceTypeDetails) keyPhysicalTypeMetadata.getMemberHoldingTypeDetails();
			AnnotationMetadata rooGwtAnnotation = MemberFindingUtils.getAnnotationOfType(cid.getAnnotations(), new JavaType(RooGwtMirroredFrom.class.getName()));
			Assert.notNull(rooGwtAnnotation, "@" + RooGwtMirroredFrom.class.getSimpleName() + " removed from " + keyTypeName.getFullyQualifiedTypeName() + " unexpectedly");
			AnnotationAttributeValue<?> value = rooGwtAnnotation.getAttribute(new JavaSymbolName("value"));
			Assert.isInstanceOf(StringAttributeValue.class, value, "Expected string-based content in @" + RooGwtMirroredFrom.class.getSimpleName() + " on " + keyTypeName.getFullyQualifiedTypeName());
			JavaType actualValue = new JavaType(value.getValue().toString());

			// Now we've read the original type, let's verify it's the same as what we're expecting
			Assert.isTrue(actualValue.equals(governorTypeName), "Every mappable type in the project must have a unique simple type name (" + governorTypeName + " and " + actualValue + " both cannot map to single key " + keyTypeName + ")");
		}

		// Excellent, so we have uniqueness taken care of by now; let's get the some metadata so we can discover what fields are available (NB: this will return null for enums)
		// Handle the "governor is deleted/unavailable/not-suitable-for-mirroring" use case

		Map<JavaSymbolName, GwtProxyProperty> clientSideTypeMap = gwtTypeService.getClientSideTypeMap(governorTypeDetails);

		GwtMetadata gwtMetadata = new GwtMetadata(metadataIdentificationString, mirrorTypeMap, governorTypeDetails, keyTypePath, entityMetadata, clientSideTypeMap, gwtClientTypeMap);
		if (keyPhysicalTypeMetadata == null) {
			DefaultPhysicalTypeMetadata defaultPhysicalTypeMetadata = new DefaultPhysicalTypeMetadata(PhysicalTypeIdentifier.createIdentifier(keyTypeName, keyTypePath), keyTypePath.toString(), gwtMetadata.buildProxy());
			Set<JavaSymbolName> proxyFields = new HashSet<JavaSymbolName>();
			for (JavaSymbolName symbolName : clientSideTypeMap.keySet()) {
				if (clientSideTypeMap.get(symbolName).getValueType().equals(mirrorTypeMap.get(GwtType.PROXY))) {
					proxyFields.add(symbolName);
				}
			}
			for (JavaSymbolName symbolName : proxyFields) {
				GwtProxyProperty proxyProperty = clientSideTypeMap.get(symbolName);
				GwtProxyProperty newProxyProperty = new GwtProxyProperty(getProjectMetadata(), proxyProperty.getPropertyType(), defaultPhysicalTypeMetadata, proxyProperty.getName(), proxyProperty.getGetter());
				clientSideTypeMap.put(symbolName, newProxyProperty);
			}
		}

		GwtTemplateDataHolder templateDataHolder = gwtTemplateService.getMirrorTemplateTypeDetails(governorTypeDetails, clientSideTypeMap);
		Map<GwtType, List<ClassOrInterfaceTypeDetails>> typesToBeWritten = new HashMap<GwtType, List<ClassOrInterfaceTypeDetails>>();
		Map<String, String> xmlToBeWritten = new HashMap<String, String>();
		for (GwtType gwtType : mirrorTypeMap.keySet()) {
			if (!gwtType.isMirrorType()) {
				continue;
			}
			gwtType.dynamicallyResolveFieldsToWatch(clientSideTypeMap);
			gwtType.dynamicallyResolveMethodsToWatch(mirrorTypeMap.get(GwtType.PROXY), clientSideTypeMap, projectMetadata);

			List<MemberHoldingTypeDetails> extendsTypes = gwtTypeService.getExtendsTypes(templateDataHolder.getTemplateTypeDetailsMap().get(gwtType));

			typesToBeWritten.put(gwtType, gwtMetadata.buildType(gwtType, templateDataHolder.getTemplateTypeDetailsMap().get(gwtType), extendsTypes));

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

	public void notify(String upstreamDependency, String downstreamDependency) {

		ProjectMetadata projectMetadata = getProjectMetadata();
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

	private ProjectMetadata getProjectMetadata() {
		return (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
	}

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return GwtMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return GwtMetadata.getMetadataIdentifierType();
	}
}
