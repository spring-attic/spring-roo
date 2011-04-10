package org.springframework.roo.classpath.customdata.taggers;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.support.util.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An implementation of {@link TaggerRegistry}.
 *
 * @author James Tyrrell
 * @since 1.1.3
 */
@Component
@Service
public class TaggerRegistryImpl implements TaggerRegistry {

	private HashMap<String, Tagger> taggerMap = new HashMap<String, Tagger>();

	public void registerTagger(Class addingClass, Tagger tagger) {
		Assert.notNull(addingClass, "The calling class must be specified");
		Assert.notNull(tagger, "The tagger must be specified");
		taggerMap.put(addingClass.getName() + tagger.getTagKey().toString(), tagger);
	}

	public void unregisterTaggers(Class addingClass) {
		Set<String> toRemove = new HashSet<String>();
		for (String taggerKey : taggerMap.keySet()) {
			if (taggerKey.startsWith(addingClass.getName())) {
				toRemove.add(taggerKey);
			}
		}
		for (String taggerKey : toRemove) {
			taggerMap.remove(taggerKey);
		}
	}

	public List<MethodTagger> getMethodTaggers() {
		List<MethodTagger> methodTaggers = new ArrayList<MethodTagger>();
		for (Tagger tagger : taggerMap.values()) {
			if (tagger instanceof MethodTagger) {
				methodTaggers.add((MethodTagger) tagger);
			}
		}
		return methodTaggers;
	}

	public List<FieldTagger> getFieldTaggers() {
		List<FieldTagger> fieldTaggers = new ArrayList<FieldTagger>();
		for (Tagger tagger : taggerMap.values()) {
			if (tagger instanceof FieldTagger) {
				fieldTaggers.add((FieldTagger) tagger);
			}
		}
		return fieldTaggers;
	}

	public List<ConstructorTagger> getConstructorTaggers() {
		List<ConstructorTagger> constructorTaggers = new ArrayList<ConstructorTagger>();
		for (Tagger tagger : taggerMap.values()) {
			if (tagger instanceof ConstructorTagger) {
				constructorTaggers.add((ConstructorTagger) tagger);
			}
		}
		return constructorTaggers;
	}

	public List<TypeTagger> getTypeTaggers() {
		List<TypeTagger> typeTaggers = new ArrayList<TypeTagger>();
		for (Tagger tagger : taggerMap.values()) {
			if (tagger instanceof TypeTagger) {
				typeTaggers.add((TypeTagger) tagger);
			}
		}
		return typeTaggers;
	}
}
