package org.springframework.roo.addon.gwt;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.classpath.MutablePhysicalTypeMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultPhysicalTypeMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.operations.ClasspathOperations;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataNotificationListener;
import org.springframework.roo.metadata.MetadataProvider;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.util.Assert;

/**
 * Monitors Java types and if necessary creates/updates/deletes the GWT files maintained for each mirror-compatible object.
 * You can find a list of mirror-compatible objects in {@link MirrorType}. 
 * 
 * <p>
 * For now only @RooEntity instances will be mirror-compatible.
 * 
 * <p>
 * Like all Roo add-ons, this provider aims to expose potentially-useful contents of the above files via {@link GwtMetadata}.
 * It also attempts to avoiding writing to disk unless actually necessary.
 * 
 * <p>
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
public final class GwtMetadataProvider implements MetadataNotificationListener, MetadataProvider {
	@Reference protected MetadataDependencyRegistry metadataDependencyRegistry;
	@Reference protected FileManager fileManager;
	@Reference protected MetadataService metadataService;
	@Reference protected MirrorTypeNamingStrategy mirrorTypeNamingStrategy;
	@Reference protected ClasspathOperations classpathOperations;
	@Reference private MutablePhysicalTypeMetadataProvider physicalTypeMetadataProvider;

	public MetadataItem get(String metadataIdentificationString) {
		// Abort early if we can't continue
		ProjectMetadata projectMetadata = getProjectMetadata();
		if (projectMetadata == null) {
			return null;
		}
		if (!fileManager.exists(GwtPath.GWT_REQUEST.canonicalFileSystemPath(projectMetadata))) {
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
		if (governorPhysicalTypeMetadata == null || !governorPhysicalTypeMetadata.isValid() || !(governorPhysicalTypeMetadata.getPhysicalTypeDetails() instanceof ClassOrInterfaceTypeDetails)) {
			return null;
		}
		ClassOrInterfaceTypeDetails governorTypeDetails = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getPhysicalTypeDetails();

		// Next let's obtain a handle to the "proxy" we'd want to produce/modify/delete as applicable for this governor
		JavaType keyTypeName = mirrorTypeNamingStrategy.convertGovernorTypeNameIntoKeyTypeName(MirrorType.PROXY, projectMetadata, governorTypeName);
		Path keyTypePath = Path.SRC_MAIN_JAVA;

		// Lookup any existing proxy type and verify it is mapped to this governor MID (ie detect name collisions and abort if necessary)
		// We do this before deleting, to verify we don't get into a delete-create-delete type case if there are name collisions
		PhysicalTypeMetadata keyPhysicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(keyTypeName, keyTypePath));
		if (keyPhysicalTypeMetadata != null && keyPhysicalTypeMetadata.isValid() && keyPhysicalTypeMetadata.getPhysicalTypeDetails() instanceof ClassOrInterfaceTypeDetails) {
			// The key presently exists, so do a sanity check
			ClassOrInterfaceTypeDetails cid = (ClassOrInterfaceTypeDetails) keyPhysicalTypeMetadata.getPhysicalTypeDetails();
			AnnotationMetadata rooGwtAnnotation = MemberFindingUtils.getAnnotationOfType(cid.getAnnotations(), new JavaType(RooGwtMirroredFrom.class.getName()));
			Assert.notNull(rooGwtAnnotation, "@" + RooGwtMirroredFrom.class.getSimpleName() + " removed from " + keyTypeName.getFullyQualifiedTypeName() + " unexpectedly");
			AnnotationAttributeValue<?> value = rooGwtAnnotation.getAttribute(new JavaSymbolName("value"));
			Assert.isInstanceOf(ClassAttributeValue.class, value, "Expected class-based content in @" + RooGwtMirroredFrom.class.getSimpleName() + " on " + keyTypeName.getFullyQualifiedTypeName());
			JavaType actualValue = ((ClassAttributeValue) value).getValue();

			// Now we've read the original type, let's verify it's the same as what we're expecting
			Assert.isTrue(actualValue.equals(governorTypeName), "Every mappable type in the project must have a unique simple type name (" + governorTypeName + " and " + actualValue + " both cannot map to single key " + keyTypeName + ")");
		}

		// Excellent, so we have uniqueness taken care of by now; let's get the some metadata so we can discover what fields are available (NB: this will return null for enums)
		BeanInfoMetadata beanInfoMetadata = (BeanInfoMetadata) metadataService.get(BeanInfoMetadata.createIdentifier(governorTypeName, governorTypePath));
		EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(governorTypeName, governorTypePath));

		// Handle the "governor is deleted/unavailable/not-suitable-for-mirroring" use case
		if (!isMappable(governorTypeDetails, entityMetadata)) {
			// Delete the key; this will trigger the listener to delete the rest
			String keyPath = classpathOperations.getPhysicalLocationCanonicalPath(PhysicalTypeIdentifier.createIdentifier(keyTypeName, keyTypePath));
			if (fileManager.exists(keyPath)) {
				fileManager.delete(keyPath);
			}
			return null;
		}

		// Our general strategy is to instantiate GwtMetadata, which offers a conceptual representation of what should go into the 4 key-specific types; after that we do comparisons and write to disk if needed
		GwtMetadata gwtMetadata = new GwtMetadata(metadataIdentificationString, mirrorTypeNamingStrategy, projectMetadata, governorTypeDetails, keyTypePath, beanInfoMetadata, entityMetadata, fileManager,
                    metadataService);

		// Output each type that was provided in the details
		for (ClassOrInterfaceTypeDetails details : gwtMetadata.getAllTypes()) {
			// Determine the canonical filename
			String physicalLocationCanonicalPath = classpathOperations.getPhysicalLocationCanonicalPath(details.getDeclaredByMetadataId());

			// Create (or modify the .java file)
			PhysicalTypeMetadata toCreate = new DefaultPhysicalTypeMetadata(details.getDeclaredByMetadataId(), physicalLocationCanonicalPath, details);
			physicalTypeMetadataProvider.createPhysicalType(toCreate);
		}

		return gwtMetadata;
	}

	private boolean isMappable(ClassOrInterfaceTypeDetails governorTypeDetails, EntityMetadata entityMetadata) {
		if (governorTypeDetails.getPhysicalTypeCategory() == PhysicalTypeCategory.CLASS) {
			return entityMetadata != null && entityMetadata.isValid() && entityMetadata.getFindAllMethod() != null;
		}
		if (governorTypeDetails.getPhysicalTypeCategory() == PhysicalTypeCategory.ENUMERATION) {
			return false;
			// We should really support enums, but for now we won't because the bikeshed sample doesn't show how
			// return true;
		}
		return false;
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
		return GwtMetadata.getMetadataIdentiferType();
	}
}
