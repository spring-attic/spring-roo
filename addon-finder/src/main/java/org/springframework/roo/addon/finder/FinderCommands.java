package org.springframework.roo.addon.finder;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.support.lifecycle.ScopeDevelopmentShell;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Commands for the 'finder' add-on to be used by the ROO shell.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
@ScopeDevelopmentShell
public class FinderCommands implements CommandMarker {
	
	private FinderOperations finderOperations;

	public FinderCommands(FinderOperations finderOperations) {
		Assert.notNull(finderOperations, "Finder operations required");
		this.finderOperations = finderOperations;
	}

	@CliAvailabilityIndicator({"list finders for", "install finder"})
	public boolean isFinderCommandAvailable() {
		return finderOperations.isFinderCommandAvailable();
	}
	
	@CliCommand(value="list finders for", help="List all finders for a given target (must be an entity")
	public SortedSet<String> listFinders(@CliOption(key="class", mandatory=true, unspecifiedDefaultValue="*", optionContext="update,project", help="The controller or entity for which the finders are generated") JavaType typeName,
			@CliOption(key={"","depth"}, mandatory=false, unspecifiedDefaultValue="1", specifiedDefaultValue="1", help="The depth of attribute combinations to be generated for the finders") Integer depth,
			@CliOption(key="filter", mandatory=false, help="A comma separated list of strings that must be present in a filter to be included") String filter) {
		Set<String> requiredEntries = new HashSet<String>();
		if (!"".equals(filter)) {
			for (String requiredString : StringUtils.commaDelimitedListToSet(filter)) {
				requiredEntries.add(requiredString.toLowerCase());
			}
		}
		SortedSet<String> result = new TreeSet<String>();
		for (String finder : finderOperations.listFindersFor(typeName, depth)) {
			if (requiredEntries.size() == 0) {
				result.add(finder);
			} else {
				boolean include = true;
				for (String requiredString : requiredEntries) {
					if (!finder.toLowerCase().contains(requiredString)) {
						include = false;
						break;
					}
				}
				if (include) {
					result.add(finder);
				}
			}
		}
		return result;
	}
	
	@CliCommand(value="install finder", help="Install finders in the given target (must be an entity)")
	public void installFinders(
			@CliOption(key="class", mandatory=false, unspecifiedDefaultValue="*", optionContext="update,project", help="The controller or entity for which the finders are generated") JavaType typeName,
			@CliOption(key={"finderName",""}, mandatory=true, help="The finder string as generated with 'list finders for'") JavaSymbolName finderName){
		finderOperations.installFinder(typeName, finderName);		
	}	
}
