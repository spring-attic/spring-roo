package org.springframework.roo.classpath;

import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.model.JavaType;

/**
 * An ITD store which can be inspected to see if ITDs associated with a type
 * have changed.
 * 
 * @author James Tyrrell
 * @since 1.2.0
 */
public interface ItdDiscoveryService {

    /**
     * Adds the presented {@link ItdTypeDetails} to the management service.
     * 
     * @param itdTypeDetails to be added (required)
     */
    void addItdTypeDetails(ItdTypeDetails itdTypeDetails);

    /**
     * Indicates whether ITDs associate with the passed in type has changed
     * since last invocation by the requesting class.
     * 
     * @param requestingClass the class requesting the changed types
     * @param javaType the type to lookup to see if a change has occurred
     * @return a collection of MIDs which represent changed types
     */
    boolean haveItdsChanged(String requestingClass, JavaType javaType);

    /**
     * Removes the {@link ItdTypeDetails} associated with the presented String.
     * 
     * @param mid the ID of the {@link ItdTypeDetails} be removed (required)
     */
    void removeItdTypeDetails(String mid);
}
