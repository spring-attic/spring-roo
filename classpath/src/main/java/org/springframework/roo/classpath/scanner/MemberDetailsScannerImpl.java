package org.springframework.roo.classpath.scanner;

import java.util.Comparator;
import java.util.LinkedList;
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
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.itd.ItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataProvider;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.support.util.Assert;

/**
 * Default implementation of {@link MemberDetailsScanner}.
 * 
 * <p>
 * Automatically detects all {@link MemberDetailsDecorator} instances in the OSGi container and will delegate to them
 * during execution of the {@link #getMemberDetails(String, ClassOrInterfaceTypeDetails)} method.
 * 
 * <p>
 * While internally this implementation will visit {@link MetadataProvider}s and {@link MemberDetailsDecorator}s in the order
 * of their type name, it is essential an add-on developer does not rely on this behaviour. Correct use of the metadata
 * infrastructure does not require special type naming approaches to be employed. The ordering behaviour exists solely
 * to simplify debugging for add-on developers and log comparison between invocations.
 * 
 * @author Ben Alex
 * @since 1.1
 */
@Component(immediate = true)
@Service
@References(
	value = { 
		@Reference(name = "memberHoldingDecorator", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = MemberDetailsDecorator.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE), 
		@Reference(name = "metadataProvider", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = MetadataProvider.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE) 
	}
)
public final class MemberDetailsScannerImpl implements MemberDetailsScanner {
	@Reference protected MetadataService metadataService;

	// Mutex
	private final Object lock = new Object();

	private SortedSet<MetadataProvider> providers = new TreeSet<MetadataProvider>(new Comparator<MetadataProvider>() {
		public int compare(MetadataProvider o1, MetadataProvider o2) {
			return o1.getClass().getName().compareTo(o2.getClass().getName());
		}
	});
	
	private SortedSet<MemberDetailsDecorator> decorators = new TreeSet<MemberDetailsDecorator>(new Comparator<MemberDetailsDecorator>() {
		public int compare(MemberDetailsDecorator o1, MemberDetailsDecorator o2) {
			return o1.getClass().getName().compareTo(o2.getClass().getName());
		}
	});
	
	protected void bindMemberHoldingDecorator(MemberDetailsDecorator decorator) {
		synchronized (lock) {
			decorators.add(decorator);
		}
	}

	protected void unbindMemberHoldingDecorator(MemberDetailsDecorator decorator) {
		synchronized (lock) {
			decorators.remove(decorator);
		}
	}

	protected void bindMetadataProvider(MetadataProvider mp) {
		synchronized (lock) {
			Assert.notNull(mp, "Metadata provider required");
			String mid = mp.getProvidesType();
			Assert.isTrue(MetadataIdentificationUtils.isIdentifyingClass(mid), "Metadata provider '" + mp + "' violated interface contract by returning '" + mid + "'");
			providers.add(mp);
		}
	}
	
	protected void unbindMetadataProvider(MetadataProvider mp) {
		synchronized (lock) {
			Assert.notNull(mp, "Metadata provider required");
			providers.remove(mp);
		}
	}

	protected void deactivate(ComponentContext componentContext) {
		synchronized (lock) {}
	}

	public final MemberDetails getMemberDetails(String requestingClass, ClassOrInterfaceTypeDetails cid) {
		synchronized (lock) {
			// Create a list of discovered members
			List<MemberHoldingTypeDetails> memberHoldingTypeDetails = new LinkedList<MemberHoldingTypeDetails>();
			
			// Build a List representing the class hierarchy, where the first element is the absolute superclass
			List<ClassOrInterfaceTypeDetails> cidHierarchy = new LinkedList<ClassOrInterfaceTypeDetails>();
			while (cid != null) {
				cidHierarchy.add(0, cid);  // Note to the top of the list
				cid = cid.getSuperclass();
			}
			
			// Now we add this governor, plus all of its superclasses
			for (ClassOrInterfaceTypeDetails currentClass : cidHierarchy) {
				memberHoldingTypeDetails.add(currentClass);
				
				// Locate all MetadataProvider instances that provide ITDs and thus MemberHoldingTypeDetails information
				for (MetadataProvider mp : providers) {
					// Skip non-ITD providers
					if (!(mp instanceof ItdMetadataProvider)) {
						continue;
					}
					
					// Skip myself
					if (mp.getClass().getName().equals(requestingClass)) {
						continue;
					}
					
					// Determine the key the ITD provider uses for this particular type
					String key = ((ItdMetadataProvider) mp).getIdForPhysicalJavaType(currentClass.getDeclaredByMetadataId());
					Assert.isTrue(MetadataIdentificationUtils.isIdentifyingInstance(key), "ITD metadata provider '" + mp + "' returned an illegal key ('" + key + "'");

					// Get the metadata and ensure we have ITD type details available
					MetadataItem metadataItem = metadataService.get(key);
					if (metadataItem == null || !metadataItem.isValid()) {
						continue;
					}
					Assert.isInstanceOf(ItdTypeDetailsProvidingMetadataItem.class, metadataItem, "ITD metadata provider '" + mp + "' failed to return the correct metadata type");
					ItdTypeDetailsProvidingMetadataItem itdTypeDetailsMd = (ItdTypeDetailsProvidingMetadataItem) metadataItem;
					if (itdTypeDetailsMd.getMemberHoldingTypeDetails() == null) {
						continue;
					}
		
					// Capture the member details
					memberHoldingTypeDetails.add(itdTypeDetailsMd.getMemberHoldingTypeDetails());
				}
			}

			// Turn out list of discovered members into a result
			MemberDetails result = new MemberDetailsImpl(memberHoldingTypeDetails);
			
			// Loop until such time as we complete a full loop where no changes are made to the result
			boolean additionalLoopRequired = true;
			while (additionalLoopRequired) {
				additionalLoopRequired = false;
				for (MemberDetailsDecorator decorator : decorators) {
					MemberDetails newResult = decorator.decorate(requestingClass, result);
					Assert.isTrue(newResult != null, "Decorator '" + decorator.getClass().getName() + "' returned an illegal result");
					if (newResult != null && !newResult.equals(result)) {
						additionalLoopRequired = true;
					}
					result = newResult;
				}
			}
			
			return result;
		}
	}
}