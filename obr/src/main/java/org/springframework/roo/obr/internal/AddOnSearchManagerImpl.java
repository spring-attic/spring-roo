package org.springframework.roo.obr.internal;

import java.util.SortedMap;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.obr.AddOnFinder;
import org.springframework.roo.obr.AddOnSearchManager;
import org.springframework.roo.obr.ObrResourceFinder;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Default implementation of {@link AddOnSearchManager}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
@Component
@Service
public class AddOnSearchManagerImpl implements AddOnSearchManager {
	private static final Logger logger = HandlerUtils.getLogger(AddOnSearchManagerImpl.class);
	@Reference	private ObrResourceFinder obrResourceFinder;

	public int completeAddOnSearch(String criteria, AddOnFinder finder) {
		SortedMap<String, String> result = finder.findAddOnsOffering(criteria);
		if (result == null) {
			logger.warning("No remote OBR repositories have been downloaded; no search of available " + finder.getFinderTargetPlural() + " performed");
			return 0;
		}
		if (result.size() == 0) {
			if (obrResourceFinder.getRepositoryCount() == 0) {
				logger.warning("No remote OBR repositories registered; no search of available " + finder.getFinderTargetPlural() + " performed");
			} else {
				logger.warning("No Spring Roo add-ons were found that offer a similar " + finder.getFinderTargetSingular());
			}
		} else if (result.size() == 1) {
			logger.warning("The following Spring Roo add-on offers a similar " + finder.getFinderTargetSingular() + " (to install use 'osgi obr start'):");
		} else if (result.size() > 1) {
			logger.warning("The following Spring Roo add-ons offer a similar " + finder.getFinderTargetSingular() + " (to install use 'osgi obr start'):");
		}
		for (String bsn : result.keySet()) {
			logger.warning(bsn + ": " + result.get(bsn));
		}
		return result.size();
	}

}
