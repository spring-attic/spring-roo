package org.springframework.roo.classpath;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultPhysicalTypeMetadata;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsBuilder;
import org.springframework.roo.classpath.scanner.MemberDetailsDecorator;
import org.springframework.roo.file.monitor.event.FileEvent;
import org.springframework.roo.file.monitor.event.FileEventListener;
import org.springframework.roo.file.monitor.event.FileOperation;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;

/**
 * Monitors for *.java files and produces a {@link PhysicalTypeMetadata} for each,
 * also providing type creation and deleting methods.
 *
 * <p>
 * This implementation does not support {@link org.springframework.roo.project.ClasspathProvidingProjectMetadata}. Whilst the
 * project metadata may implement this interface, the {@link #findIdentifier(JavaType)} will ignore
 * such paths in the current release.
 *
 * Prior to 1.2.0 the default implementation of PhysicalTypeMetadataProvider was JavaParserMetadataProvider.
 *
 * @author Ben Alex
 * @author James Tyrrell
 * @since 1.2.0
 */
@Component(immediate = true)
@Service
@References(
	value = {
		@Reference(name = "memberHoldingDecorator", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = MemberDetailsDecorator.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
	}
)
public class DefaultPhysicalTypeMetadataProvider implements PhysicalTypeMetadataProvider, FileEventListener {

	// Fields
	@Reference private TypeParsingService typeParsingService;
	@Reference private FileManager fileManager;
	@Reference private MetadataService metadataService;
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;
	@Reference private ProjectOperations projectOperations;
	@Reference private TypeLocationService typeLocationService;

	// Mutex
	private final Object lock = new Object();

	private final SortedSet<MemberDetailsDecorator> decorators = new TreeSet<MemberDetailsDecorator>(new Comparator<MemberDetailsDecorator>() {
		public int compare(final MemberDetailsDecorator o1, final MemberDetailsDecorator o2) {
			return o1.getClass().getName().compareTo(o2.getClass().getName());
		}
	});

	protected void bindMemberHoldingDecorator(final MemberDetailsDecorator decorator) {
		synchronized (lock) {
			decorators.add(decorator);
		}
	}

	protected void unbindMemberHoldingDecorator(final MemberDetailsDecorator decorator) {
		synchronized (lock) {
			decorators.remove(decorator);
		}
	}

	public String getProvidesType() {
		return PhysicalTypeIdentifier.getMetadataIdentiferType();
	}

	public void onFileEvent(final FileEvent fileEvent) {
		String fileIdentifier = fileEvent.getFileDetails().getCanonicalPath();

		// Check to see if file is of interest
		if (fileIdentifier.endsWith(".java") && fileEvent.getOperation() != FileOperation.MONITORING_FINISH && !fileIdentifier.endsWith("package-info.java")) {
			// Figure out the PhysicalTypeIdentifier
			String id = typeLocationService.findIdentifier(fileIdentifier);

			if (id == null) {
				return;
			}

			// Now we've worked out the id, we can publish the event in case others were interested
			metadataService.evict(id);
			metadataDependencyRegistry.notifyDownstream(id);
		}
	}

	public MetadataItem get(final String metadataIdentificationString) {
		Assert.isTrue(PhysicalTypeIdentifier.isValid(metadataIdentificationString), "Metadata identification string '" + metadataIdentificationString + "' is not valid for this metadata provider");
		String fileIdentifier = typeLocationService.getPhysicalTypeCanonicalPath(metadataIdentificationString);
		metadataDependencyRegistry.deregisterDependencies(metadataIdentificationString);
		if (!fileManager.exists(fileIdentifier)) {
			// Couldn't find the file, so return null to distinguish from a file that was found but could not be parsed
			return null;
		}
		JavaType typeName = PhysicalTypeIdentifier.getJavaType(metadataIdentificationString);
		ClassOrInterfaceTypeDetails typeDetails = typeParsingService.getTypeAtLocation(fileIdentifier, metadataIdentificationString, typeName);
		if (typeDetails == null) {
			return null;
		}
		DefaultPhysicalTypeMetadata result = new DefaultPhysicalTypeMetadata(metadataIdentificationString, fileIdentifier, typeDetails);
		if (result.getMemberHoldingTypeDetails() != null && result.getMemberHoldingTypeDetails() instanceof ClassOrInterfaceTypeDetails) {
			ClassOrInterfaceTypeDetails details = (ClassOrInterfaceTypeDetails) result.getMemberHoldingTypeDetails();
			if (details.getPhysicalTypeCategory() == PhysicalTypeCategory.CLASS && details.getExtendsTypes().size() == 1) {
				// This is a class, and it extends another class
				if (details.getSuperclass() != null) {
					// We have a dependency on the superclass, and there is metadata available for the superclass
					// We won't implement the full MetadataNotificationListener here, but rely on MetadataService's fallback
					// (which is to evict from cache and call get again given JavaParserMetadataProvider doesn't implement MetadataNotificationListener, then notify everyone we've changed)
					String superclassId = details.getSuperclass().getDeclaredByMetadataId();
					metadataDependencyRegistry.registerDependency(superclassId, result.getId());
				} else {
					// We have a dependency on the superclass, but no metadata is available
					// We're left with no choice but to register for every physical type change, in the hope we discover our parent someday (sad, isn't it? :-) )
					for (Path sourcePath : projectOperations.getPathResolver().getSourcePaths()) {
						String possibleSuperclass = PhysicalTypeIdentifier.createIdentifier(details.getExtendsTypes().get(0), sourcePath);
						metadataDependencyRegistry.registerDependency(possibleSuperclass, result.getId());
					}
				}
			}
		}
		List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList = Collections.singletonList(result.getMemberHoldingTypeDetails());
		MemberDetails memberDetails = new MemberDetailsBuilder(memberHoldingTypeDetailsList).build();
		// Loop until such time as we complete a full loop where no changes are made to the result
		boolean additionalLoopRequired = true;
		while (additionalLoopRequired) {
			additionalLoopRequired = false;
			for (MemberDetailsDecorator decorator : decorators) {
				MemberDetails newResult = decorator.decorateTypes(DefaultPhysicalTypeMetadataProvider.class.getName(), memberDetails);
				Assert.isTrue(newResult != null, "Decorator '" + decorator.getClass().getName() + "' returned an illegal result");
				if (newResult != null && !newResult.equals(memberDetails)) {
					additionalLoopRequired = true;
					memberDetails = newResult;
				}
			}
		}

		return new DefaultPhysicalTypeMetadata(metadataIdentificationString, fileIdentifier, memberDetails.getDetails().get(0));
	}
}

