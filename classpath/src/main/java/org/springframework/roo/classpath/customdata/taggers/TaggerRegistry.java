package org.springframework.roo.classpath.customdata.taggers;

import org.springframework.roo.classpath.details.ItdTypeDetails;

import java.util.List;

/**
 * Provides a universal registry for {@link Tagger} objects. Initial
 * no checks are being performed upon adding new Tagger instances, it
 * is envisioned that this will change and an alert would be provided
 * if a {@link Tagger} object was in conflict with another. For this to
 * happen a more fleshed out matching API needs to be implemented as so
 * comparison of {@link Tagger}s can be performed easily. In addition to
 * registering {@link Tagger}s this interface allows {@link Tagger} objects
 * to be unregistered based on the registering class which is useful if
 * the registering class is no longer in play.
 *
 * @author James Tyrrell
 * @since 1.1.3
 */
public interface TaggerRegistry {

	void registerTagger(Class addingClass, Tagger tagger);

	void unregisterTaggers(Class addingClass);

	List<MethodTagger> getMethodTaggers();

	List<FieldTagger> getFieldTaggers();

	List<ConstructorTagger> getConstructorTaggers();

	List<TypeTagger> getTypeTaggers();
}
