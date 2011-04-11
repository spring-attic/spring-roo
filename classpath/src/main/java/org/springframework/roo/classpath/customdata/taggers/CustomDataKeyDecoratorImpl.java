package org.springframework.roo.classpath.customdata.taggers;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsBuilder;
import org.springframework.roo.support.util.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An implementation of {@link CustomDataKeyDecorator}.
 *
 * @author James Tyrrell
 * @since 1.1.3
 */
@Component
@Service
public class CustomDataKeyDecoratorImpl implements CustomDataKeyDecorator {

	private HashMap<String, Matcher> taggerMap = new HashMap<String, Matcher>();

	public MemberDetails decorate(String requestingClass, MemberDetails memberDetails) {
		MemberDetailsBuilder memberDetailsBuilder = new MemberDetailsBuilder(memberDetails);

		// Locate any requests that we add custom data to identifiable java structures
		for (FieldMatcher fieldTagger : getFieldTaggers()) {
			for (FieldMetadata fieldMetadata : fieldTagger.matches(memberDetails.getDetails())) {
				memberDetailsBuilder.tag(fieldMetadata, fieldTagger.getCustomDataKey(), fieldTagger.getTagValue(fieldMetadata));
			}
		}

		for (MethodMatcher methodTagger : getMethodTaggers()) {
			for (MethodMetadata methodMetadata : methodTagger.matches(memberDetails.getDetails())) {
				memberDetailsBuilder.tag(methodMetadata, methodTagger.getCustomDataKey(), methodTagger.getTagValue(methodMetadata));
			}
		}

		for (ConstructorMatcher constructorTagger : getConstructorTaggers()) {
			for (ConstructorMetadata constructorMetadata : constructorTagger.matches(memberDetails.getDetails())) {
				memberDetailsBuilder.tag(constructorMetadata, constructorTagger.getCustomDataKey(), constructorTagger.getTagValue(constructorMetadata));
			}
		}

		for (TypeMatcher typeTagger : getTypeTaggers()) {
			for (MemberHoldingTypeDetails typeDetails : typeTagger.matches(memberDetails.getDetails())) {
				memberDetailsBuilder.tag(typeDetails, typeTagger.getCustomDataKey(), typeTagger.getTagValue(typeDetails));
			}
		}

		return memberDetailsBuilder.build();
	}

	public void registerMatcher(String addingClass, Matcher matcher) {
		Assert.notNull(addingClass, "The calling class must be specified");
		Assert.notNull(matcher, "The matcher must be specified");
		taggerMap.put(addingClass + matcher.getCustomDataKey().toString(), matcher);
	}

	public void unregisterMatchers(String addingClass) {
		Set<String> toRemove = new HashSet<String>();
		for (String taggerKey : taggerMap.keySet()) {
			if (taggerKey.startsWith(addingClass)) {
				toRemove.add(taggerKey);
			}
		}
		for (String taggerKey : toRemove) {
			taggerMap.remove(taggerKey);
		}
	}

	public List<MethodMatcher> getMethodTaggers() {
		List<MethodMatcher> methodTaggers = new ArrayList<MethodMatcher>();
		for (Matcher matcher : taggerMap.values()) {
			if (matcher instanceof MethodMatcher) {
				methodTaggers.add((MethodMatcher) matcher);
			}
		}
		return methodTaggers;
	}

	public List<FieldMatcher> getFieldTaggers() {
		List<FieldMatcher> fieldTaggers = new ArrayList<FieldMatcher>();
		for (Matcher matcher : taggerMap.values()) {
			if (matcher instanceof FieldMatcher) {
				fieldTaggers.add((FieldMatcher) matcher);
			}
		}
		return fieldTaggers;
	}

	public List<ConstructorMatcher> getConstructorTaggers() {
		List<ConstructorMatcher> constructorTaggers = new ArrayList<ConstructorMatcher>();
		for (Matcher matcher : taggerMap.values()) {
			if (matcher instanceof ConstructorMatcher) {
				constructorTaggers.add((ConstructorMatcher) matcher);
			}
		}
		return constructorTaggers;
	}

	public List<TypeMatcher> getTypeTaggers() {
		List<TypeMatcher> typeTaggers = new ArrayList<TypeMatcher>();
		for (Matcher matcher : taggerMap.values()) {
			if (matcher instanceof TypeMatcher) {
				typeTaggers.add((TypeMatcher) matcher);
			}
		}
		return typeTaggers;
	}
}
