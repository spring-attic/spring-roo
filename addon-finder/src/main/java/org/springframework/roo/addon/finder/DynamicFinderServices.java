package org.springframework.roo.addon.finder;

import java.util.List;
import java.util.Set;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * The {@link DynamicFinderServices} is used for the generation of "dynamic finder" methods based on a 
 * {@link JavaSymbolName} which would look like 'findByFirstNameAndLastName'. This class will suggest 
 * possible finder combinations, create a query String and provide access to parameter types 
 * and names. 
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @since 1.0
 */
public interface DynamicFinderServices {

	/**
	 * This method provides a convenient generator for all possible combinations of finder
	 * method signatures.
	 * 
	 * @param memberDetails the metadata for the {@link JavaType} for which the finder signatures combinations are generated. (required)
	 * @param plural the pluralised form of the entity name, which is used for finder method names (required)
	 * @param maxDepth the depth of combinations used for finder signatures combinations (a depth of 2 will combine a maximum of two attributes from the {@link BeanInfoMetadata} (required)
	 * @param exclusions Field names which should not be contained in the suggested finders
	 * @return all possible finder method signatures for the given depth.
	 */
	List<JavaSymbolName> getFindersFor(MemberDetails memberDetails, String plural, int maxDepth, Set<JavaSymbolName> exclusions);
	
	/**
	 * This method generates a named JPA query String to be used in JPA entity manager queries. 
	 * 
	 * @param finderName the finder method signature to use. Must be a valid signature. (required)
	 * @param beanInfoMetadata the metadata for the {@link BeanInfoMetadata} for which the finder signatures combinations were generated. (required)
	 * @return the String to be used in a JPA named query
	 */
	String getJpaQueryFor(JavaSymbolName finderName, String plural, BeanInfoMetadata beanInfoMetadata);
	
	/**
	 * This method should be used in combination with JPA entity manager queries
	 * in order to find out about the parameter types used in the named JPA query.
	 * 
	 * @param finderName the finder method signature to use. Must be a valid signature. (required)
	 * @param beanInfoMetadata the metadata for the {@link BeanInfoMetadata} for which the finder signatures combinations were generated. (required)
	 * @return the {@link JavaType} array representing the parameter types for the finder method signature presented
	 */
	List<JavaType> getParameterTypes(JavaSymbolName finderName, String plural, BeanInfoMetadata beanInfoMetadata);
	
	/**
	 * This method should be used in combination with {@link #getJpaQueryFor(JavaSymbolName, String, BeanInfoMetadata)} 
	 * in order to find out about the parameter names used in the named JPA query.
	 * 
	 * @param finderName the finder method signature to use. Must be a valid signature. (required)
	 * @param beanInfoMetadata the metadata for the {@link BeanInfoMetadata} for which the finder signatures combinations were generated. (required)
	 * @return the {@link JavaSymbolName} array representing the parameter names for the finder method signature presented
	 */
	List<JavaSymbolName> getParameterNames(JavaSymbolName finderName, String plural, BeanInfoMetadata beanInfoMetadata);
}
