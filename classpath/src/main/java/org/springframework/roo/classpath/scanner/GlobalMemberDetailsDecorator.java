package org.springframework.roo.classpath.scanner;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.customdata.taggers.ConstructorTagger;
import org.springframework.roo.classpath.customdata.taggers.FieldTagger;
import org.springframework.roo.classpath.customdata.taggers.MethodTagger;
import org.springframework.roo.classpath.customdata.taggers.TaggerRegistry;
import org.springframework.roo.classpath.customdata.taggers.TypeTagger;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;

/**
 * A {@link MemberDetailsDecorator} that decorates members based the member
 * matching a {@link org.springframework.roo.classpath.customdata.taggers.Tagger}s
 * specification.
 *
 * @author Ben Alex
 * @author Alan Stewart
 * @author James Tyrrell
 * @since 1.1.3
 */
@Service
@Component
public class GlobalMemberDetailsDecorator implements MemberDetailsDecorator {

	@Reference TaggerRegistry taggerRegistry;

	public MemberDetails decorate(String requestingClass, MemberDetails memberDetails) {
		MemberDetailsBuilder memberDetailsBuilder = new MemberDetailsBuilder(memberDetails);

		// Locate any requests that we add custom data to identifiable java structures
		for (FieldTagger fieldTagger : taggerRegistry.getFieldTaggers()) {
			for (FieldMetadata fieldMetadata : fieldTagger.matches(memberDetails.getDetails())) {
				memberDetailsBuilder.tag(fieldMetadata, fieldTagger.getTagKey(), fieldTagger.getTagValue(fieldMetadata));
			}
		}

		for (MethodTagger methodTagger : taggerRegistry.getMethodTaggers()) {
			for (MethodMetadata methodMetadata : methodTagger.matches(memberDetails.getDetails())) {
				memberDetailsBuilder.tag(methodMetadata, methodTagger.getTagKey(), methodTagger.getTagValue(methodMetadata));
			}
		}

		for (ConstructorTagger constructorTagger : taggerRegistry.getConstructorTaggers()) {
			for (ConstructorMetadata constructorMetadata : constructorTagger.matches(memberDetails.getDetails())) {
				memberDetailsBuilder.tag(constructorMetadata, constructorTagger.getTagKey(), constructorTagger.getTagValue(constructorMetadata));
			}
		}

		for (TypeTagger typeTagger : taggerRegistry.getTypeTaggers()) {
			for (MemberHoldingTypeDetails typeDetails : typeTagger.matches(memberDetails.getDetails())) {
				memberDetailsBuilder.tag(typeDetails, typeTagger.getTagKey(), typeTagger.getTagValue(typeDetails));
			}
		}

		return memberDetailsBuilder.build();
	}
}
