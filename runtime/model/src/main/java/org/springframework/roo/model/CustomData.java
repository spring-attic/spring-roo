package org.springframework.roo.model;

import java.util.Map;
import java.util.Set;

/**
 * Represents an immutable collection of custom data key-value pairs.
 * <p>
 * Several metadata interfaces in Spring Roo define the
 * {@link CustomDataAccessor} interface. This is the primary mechanism to obtain
 * a {@link CustomData} instance.
 * <p>
 * While this interface is essentially a subset of {@link Map}, it has been
 * introduced to simplify method signatures, descriptions and allow future
 * modification of the {@link CustomData} contract.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public interface CustomData extends Iterable<Object> {

    /**
     * Obtains a specific item of custom data.
     * <p>
     * It is important that both the key and the object are immutable. Other
     * parts of Spring Roo rely on the immutability guarantees of
     * {@link CustomData} (and particularly the classes that implement
     * {@link CustomDataAccessor}) and therefore you must ensure all keys and
     * values are genuinely immutable. Most Spring Roo types are immutable and
     * can be placed within {@link CustomData} key-value pairs. Most standard
     * Java types are also immutable and can similarly be stored (eg
     * {@link String}, {@link Boolean} etc).
     * 
     * @param key to search for (required)
     * @return the object if found, otherwise null
     */
    Object get(Object key);

    /**
     * Obtains an immutable representation of all custom data keys.
     * 
     * @return the keys (never null, but may be empty)
     */
    Set<Object> keySet();
}
