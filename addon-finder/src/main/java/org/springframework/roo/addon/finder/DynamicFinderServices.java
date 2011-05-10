package org.springframework.roo.addon.finder;

import java.util.List;
import java.util.Set;

import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;

/**
 * The {@link DynamicFinderServices} is used for the generation of dynamic finder methods based on a 
 * {@link JavaSymbolName} which would look like, for example, 'findByFirstNameAndLastName'. 
 * This class will suggest possible finder combinations, create a query String and provide access 
 * to parameter types and names. 
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @since 1.0
 */
public interface DynamicFinderServices {
	
	/**
	 * This method provides a convenient generator for all possible combinations of finder
	 * method signatures. This is used by the {@link FinderCommands} to locate possible finders the user
	 * may wish to install.
	 * 
	 * @param memberDetails the {@link MemberDetails} object to search (required)
	 * @param plural the pluralised form of the entity name, which is used for finder method names (required)
	 * @param depth the depth of combinations used for finder signatures combinations (a depth of 2 will combine a maximum of two attributes from the member details (required)
	 * @param exclusions field names which should not be contained in the suggested finders
	 * @return immutable representation of all possible finder method signatures for the given depth (never returns null, but list may be empty)
	 */
	List<JavaSymbolName> getFinders(MemberDetails memberDetails, String plural, int depth, Set<JavaSymbolName> exclusions);
		
	/**
	 * This method generates a {@link QueryHolder} object that consists of the:
	 * <ul>
	 * <li>named JPA query string to be used in JPA entity manager queries
	 * <li>parameter types used in the named JPA query
	 * <li>parameter names used in the named JPA query
	 * </ul>
	 * 
	 * @param memberDetails the {@link MemberDetails} object to search (required)
	 * @param finderName the finder method signature to use (required; must be a valid signature)
	 * @param plural the pluralised form of the entity name, which is used for finder method names (required)
	 * @param entityName the name of the entity to be used in the Query
	 * @return a {@link QueryHolder} object containing all the attributes to be used in a JPA named query (null if the finder is unable to be built at this time)
	 */
	QueryHolder getQueryHolder(MemberDetails memberDetails, JavaSymbolName finderName, String plural, String entityName);
}
