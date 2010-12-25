package org.springframework.roo.classpath.scanner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
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
 * during execution of the {@link #getMemberDetails(MetadataProvider, ClassOrInterfaceTypeDetails)} method.
 * 
 * @author Ben Alex
 * @since 1.1
 *
 */
@Component
@Service
@Reference(name="memberHoldingDecorator", strategy=ReferenceStrategy.EVENT, policy=ReferencePolicy.DYNAMIC, referenceInterface=MemberDetailsDecorator.class, cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE)
public final class MemberDetailsScannerImpl implements MemberDetailsScanner {
	
	private SortedSet<MemberDetailsDecorator> decorators = new TreeSet<MemberDetailsDecorator>(new Comparator<MemberDetailsDecorator>() {
		public int compare(MemberDetailsDecorator o1, MemberDetailsDecorator o2) {
			return o1.getClass().getName().compareTo(o2.getClass().getName());
		}
	});
	@Reference protected MetadataService metadataService;
	
	protected void bindMemberHoldingDecorator(MemberDetailsDecorator decorator) {
		synchronized (decorators) {
			decorators.add(decorator);
		}
	}

	protected void unbindMemberHoldingDecorator(MemberDetailsDecorator decorator) {
		synchronized (decorators) {
			decorators.remove(decorator);
		}
	}
	
	protected void deactivate(ComponentContext componentContext) {
		synchronized (metadataService) {}
	}

	public final MemberDetails getMemberDetails(String requestingClass, ClassOrInterfaceTypeDetails cid) {
		synchronized (metadataService) {
			// Create a list of discovered members
			List<MemberHoldingTypeDetails> memberHoldingTypeDetails = new ArrayList<MemberHoldingTypeDetails>();
			
			// Build a List representing the class hierarchy, where the first element is the absolute superclass
			List<ClassOrInterfaceTypeDetails> cidHierarchy = new ArrayList<ClassOrInterfaceTypeDetails>();
			while (cid != null) {
				cidHierarchy.add(0, cid);  // note to the top of the list
				cid = cid.getSuperclass();
			}
			
			// Now we add this governor, plus all of its superclasses
			for (ClassOrInterfaceTypeDetails currentClass : cidHierarchy) {
				memberHoldingTypeDetails.add(currentClass);
				
				// Locate all MetadataProvider instances that provide ITDs and thus MemberHoldingTypeDetails information
				for (MetadataProvider mp : metadataService.getRegisteredProviders()) {
					// Skip non-ITD providers
					if (!(mp instanceof ItdMetadataProvider)) {
						continue;
					}
					
					// Skip myself
					if (mp.getClass().equals(requestingClass.getClass())) {
						continue;
					}
					
					// Determine the key the ITD provider uses for this particular type
					String key = ((ItdMetadataProvider)mp).getIdForPhysicalJavaType(currentClass.getDeclaredByMetadataId());
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
			
			synchronized (decorators) {
				// Loop until such time as we complete a full loop where no changes are made to the result
				boolean additionalLoopRequired = true;
				while (additionalLoopRequired) {
					additionalLoopRequired = false;
					for (MemberDetailsDecorator decorator : decorators) {
						MemberDetails newResult = decorator.decorate(requestingClass, result);
						Assert.isTrue(newResult != null, "Decorator '" + decorator.getClass().getName() + "' returned an illegal result");
						if (!newResult.equals(result)) {
							additionalLoopRequired = true;
						}
						result = newResult;
					}
				}
			}
			
			return result;
		}
	}
	
}
