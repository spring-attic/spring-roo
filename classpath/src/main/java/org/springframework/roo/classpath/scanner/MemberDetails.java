package org.springframework.roo.classpath.scanner;

import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.details.IdentifiableJavaStructure;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataProvider;

/**
 * Immutable representation of member details scanned at a particular point in time.
 * 
 * <p>
 * Please note that {@link MemberDetails} as well as all types it eventually refers to
 * are all immutable.
 * 
 * <p>
 * {@link MemberDetails} represents a convenient, customizable aggregation of member information at a particular
 * point in time and for a particular {@link MetadataProvider}. It is distinct from other key types as follows:
 * 
 * <ul>
 * <li>{@link MetadataItem}s are specific to a given add-on. They also frequently reflect a single AspectJ ITD.</li>
 * <li>{@link PhysicalTypeDetails} instances only contain details of a parsed .java source file.</li>
 * <li>{@link ItdTypeDetails} instances only represent details of a created .aj ITD source file.</li>
 * <li>{@link MemberHoldingTypeDetails}s represent a single compilation unit.</li>
 * <li>{@link IdentifiableJavaStructure}s represent a single member within a {@link MemberHoldingTypeDetails} instance.</li>
 * <li>{@link MemberDetails}represent <em>all</em> available {@link MemberHoldingTypeDetails} for a given type at this time 
 * and for this metadata provider.</li>
 * </ul>
 * 
 * @author Ben Alex
 * @since 1.1
 *
 */
public interface MemberDetails {

	/**
	 * @return an immutable representation of the member holders (never null or empty)
	 */
	List<MemberHoldingTypeDetails> getDetails();

}