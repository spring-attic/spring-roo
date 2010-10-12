package org.springframework.roo.addon.roobot.client;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.obr.AddOnFinder;

/**
 * AddOn finder which searches the RooBoot supplied Add-on index
 * 
 * @author Stefan Schmidt
 * @since 1.1
 *
 */
@Component
@Service
public class AddOnFinderImpl implements AddOnFinder {

	@Reference private AddOnRooBotOperations addOnManagerOperations;
	
	public SortedMap<String, String> findAddOnsOffering(String criteria) {
		SortedMap<String, String> matches = new TreeMap<String, String>();
		Map<String, AddOnBundleInfo> bundles = addOnManagerOperations.getAddOnCache(false);
		for (String key: bundles.keySet()) {
			AddOnBundleInfo bundle = bundles.get(key);
			if (bundle != null) {
				for (String command: bundle.getCommands().keySet()) {
					if (criteria.startsWith(command)) {
						matches.put(bundle.getBsn(), bundle.getName());
					}
				}
			}
		}
		return matches;
	}

	public String getFinderTargetSingular() {
		return "command found in the RooBot index";
	}

	public String getFinderTargetPlural() {
		return "commands found in the RooBot index";
	}
}
