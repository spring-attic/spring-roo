package org.springframework.roo.classpath.scanner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.Validate;
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

/**
 * Default implementation of {@link MemberDetailsScanner}.
 * <p>
 * Automatically detects all {@link MemberDetailsDecorator} instances in the
 * OSGi container and will delegate to them during execution of the
 * {@link #getMemberDetails(String, ClassOrInterfaceTypeDetails)} method.
 * <p>
 * While internally this implementation will visit {@link MetadataProvider}s and
 * {@link MemberDetailsDecorator}s in the order of their type name, it is
 * essential an add-on developer does not rely on this behaviour. Correct use of
 * the metadata infrastructure does not require special type naming approaches
 * to be employed. The ordering behaviour exists solely to simplify debugging
 * for add-on developers and log comparison between invocations.
 * 
 * @author Ben Alex
 * @since 1.1
 */
@Component(immediate = true)
@Service
@References(value = {
        @Reference(name = "memberHoldingDecorator", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = MemberDetailsDecorator.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE),
        @Reference(name = "metadataProvider", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = MetadataProvider.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE) })
public class MemberDetailsScannerImpl implements MemberDetailsScanner {

    private final SortedSet<MemberDetailsDecorator> decorators = new TreeSet<MemberDetailsDecorator>(
            new Comparator<MemberDetailsDecorator>() {
                public int compare(final MemberDetailsDecorator o1,
                        final MemberDetailsDecorator o2) {
                    return o1.getClass().getName()
                            .compareTo(o2.getClass().getName());
                }
            });

    // Mutex
    private final Object lock = new Object();

    @Reference protected MetadataService metadataService;

    private final SortedSet<MetadataProvider> providers = new TreeSet<MetadataProvider>(
            new Comparator<MetadataProvider>() {
                public int compare(final MetadataProvider o1,
                        final MetadataProvider o2) {
                    return o1.getClass().getName()
                            .compareTo(o2.getClass().getName());
                }
            });

    protected void bindMemberHoldingDecorator(
            final MemberDetailsDecorator decorator) {
        synchronized (lock) {
            decorators.add(decorator);
        }
    }

    protected void bindMetadataProvider(final MetadataProvider mp) {
        synchronized (lock) {
            Validate.notNull(mp, "Metadata provider required");
            final String mid = mp.getProvidesType();
            Validate.isTrue(
                    MetadataIdentificationUtils.isIdentifyingClass(mid),
                    "Metadata provider '%s' violated interface contract by returning '%s'",
                    mp, mid);
            providers.add(mp);
        }
    }

    protected void deactivate(final ComponentContext componentContext) {
        // Empty
    }

    public final MemberDetails getMemberDetails(final String requestingClass,
            ClassOrInterfaceTypeDetails cid) {
        if (cid == null) {
            return null;
        }
        synchronized (lock) {
            // Create a list of discovered members
            final List<MemberHoldingTypeDetails> memberHoldingTypeDetails = new ArrayList<MemberHoldingTypeDetails>();

            // Build a List representing the class hierarchy, where the first
            // element is the absolute superclass
            final List<ClassOrInterfaceTypeDetails> cidHierarchy = new ArrayList<ClassOrInterfaceTypeDetails>();
            while (cid != null) {
                cidHierarchy.add(0, cid); // Note to the top of the list
                cid = cid.getSuperclass();
            }

            // Now we add this governor, plus all of its superclasses
            for (final ClassOrInterfaceTypeDetails currentClass : cidHierarchy) {
                memberHoldingTypeDetails.add(currentClass);

                // Locate all MetadataProvider instances that provide ITDs and
                // thus MemberHoldingTypeDetails information
                for (final MetadataProvider mp : providers) {
                    // Skip non-ITD providers
                    if (!(mp instanceof ItdMetadataProvider)) {
                        continue;
                    }

                    // Skip myself
                    if (mp.getClass().getName().equals(requestingClass)) {
                        continue;
                    }

                    // Determine the key the ITD provider uses for this
                    // particular type
                    final String key = ((ItdMetadataProvider) mp)
                            .getIdForPhysicalJavaType(currentClass
                                    .getDeclaredByMetadataId());
                    Validate.isTrue(
                            MetadataIdentificationUtils
                                    .isIdentifyingInstance(key),
                            "ITD metadata provider '%s' returned an illegal key ('%s')",
                            mp, key);

                    // Get the metadata and ensure we have ITD type details
                    // available
                    final MetadataItem metadataItem = metadataService.get(key);
                    if (metadataItem == null || !metadataItem.isValid()) {
                        continue;
                    }
                    Validate.isInstanceOf(
                            ItdTypeDetailsProvidingMetadataItem.class,
                            metadataItem,
                            "ITD metadata provider '%s' failed to return the correct metadata type",
                            mp);
                    final ItdTypeDetailsProvidingMetadataItem itdTypeDetailsMd = (ItdTypeDetailsProvidingMetadataItem) metadataItem;
                    if (itdTypeDetailsMd.getMemberHoldingTypeDetails() == null) {
                        continue;
                    }

                    // Capture the member details
                    memberHoldingTypeDetails.add(itdTypeDetailsMd
                            .getMemberHoldingTypeDetails());
                }
            }

            // Turn out list of discovered members into a result
            MemberDetails result = new MemberDetailsImpl(
                    memberHoldingTypeDetails);

            // Loop until such time as we complete a full loop where no changes
            // are made to the result
            boolean additionalLoopRequired = true;
            while (additionalLoopRequired) {
                additionalLoopRequired = false;
                for (final MemberDetailsDecorator decorator : decorators) {
                    final MemberDetails newResult = decorator.decorate(
                            requestingClass, result);
                    Validate.isTrue(newResult != null,
                            "Decorator '%s' returned an illegal result",
                            decorator.getClass().getName());
                    if (newResult != null && !newResult.equals(result)) {
                        additionalLoopRequired = true;
                    }
                    result = newResult;
                }
            }

            return result;
        }
    }

    protected void unbindMemberHoldingDecorator(
            final MemberDetailsDecorator decorator) {
        synchronized (lock) {
            decorators.remove(decorator);
        }
    }

    protected void unbindMetadataProvider(final MetadataProvider mp) {
        synchronized (lock) {
            Validate.notNull(mp, "Metadata provider required");
            providers.remove(mp);
        }
    }
}