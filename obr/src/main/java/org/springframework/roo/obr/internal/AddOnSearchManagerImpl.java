package org.springframework.roo.obr.internal;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.obr.AddOnFinder;
import org.springframework.roo.obr.AddOnSearchManager;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Default implementation of {@link AddOnSearchManager}.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component(immediate = true)
@Service
@Reference(name = "finder", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = AddOnFinder.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
public class AddOnSearchManagerImpl implements AddOnSearchManager {
	
	private Object mutex = this;
	private Set<AddOnFinder> finders = new HashSet<AddOnFinder>();
	private static final Logger logger = HandlerUtils.getLogger(AddOnSearchManagerImpl.class);

	public int completeAddOnSearch(String criteria) {
		SortedMap<String, String> result = new TreeMap<String, String>();
		for (AddOnFinder finder: finders) {
			result.putAll(finder.findAddOnsOffering(criteria));
		}
		printResult(result);
		return result.size();
	}
	
	public int completeAddOnSearch(String criteria, AddOnFinder finder) {
		SortedMap<String, String> result = finder.findAddOnsOffering(criteria);
		if (result == null) {
			logger.warning("No remote OBR repositories have been downloaded; no search of available " + finder.getFinderTargetPlural() + " performed");
			return 0;
		}
		printResult(result);
		return result.size();
	}
	
	private void printResult(SortedMap<String, String> result) {
		if (result.size() == 0) {
			logger.warning("No remote OBR repositories have matching add-ons matching your request");
		}
		if (result.size() == 1) {
			logger.warning("The following Spring Roo add-on offers a similar criterion (to install use 'addon install --bundleSybolicName ..'):");
		} else if (result.size() > 1) {
			logger.warning("The following Spring Roo add-ons offer a similar criteria (to install use 'addon install --bundleSybolicName ..'):");
		}
		for (String bsn : result.keySet()) {
			logger.warning(bsn + ": " + result.get(bsn));
		}
	}
	
	protected void bindFinder(AddOnFinder finder) {
		synchronized (mutex) {
			finders.add(finder);
		}
	}

	protected void unbindFinder(AddOnFinder finder) {
		synchronized (mutex) {
			finders.remove(finder);
		}
	}
}
