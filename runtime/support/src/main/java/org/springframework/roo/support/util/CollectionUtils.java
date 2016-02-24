package org.springframework.roo.support.util;

/*
 * Copyright 2002-2008 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Miscellaneous collection utility methods. Mainly for internal use within the
 * framework.
 * 
 * @author Alan Stewart
 * @author Andrew Swan
 * @since 1.1.3
 */
public final class CollectionUtils {

    /**
     * Convert the supplied array into a List. A primitive array gets converted
     * into a List of the appropriate wrapper type.
     * <p>
     * A <code>null</code> source value will be converted to an empty List.
     * 
     * @param source the (potentially primitive) array
     * @return the converted List result
     * @see ObjectUtils#toObjectArray(Object)
     */
    public static List<?> arrayToList(final Object source) {
        return Arrays.asList(ObjectUtils.toObjectArray(source));
    }

    /**
     * Filters (removes elements from) the given {@link Iterable} using the
     * given filter.
     * 
     * @param <T> the type of object being filtered
     * @param unfiltered the iterable to filter; can be <code>null</code>
     * @param filter the filter to apply; can be <code>null</code> for none
     * @return a non-<code>null</code> list
     */
    public static <T> List<T> filter(final Iterable<? extends T> unfiltered,
            final Filter<T> filter) {
        final List<T> filtered = new ArrayList<T>();
        if (unfiltered != null) {
            for (final T element : unfiltered) {
                if (filter == null || filter.include(element)) {
                    filtered.add(element);
                }
            }
        }
        return filtered;
    }

    /**
     * Returns the first element of the given collection
     * 
     * @param <T>
     * @param collection
     * @return <code>null</code> if the first element is <code>null</code> or
     *         the collection is <code>null</code> or empty
     */
    public static <T> T firstElementOf(final Collection<? extends T> collection) {
        if (isEmpty(collection)) {
            return null;
        }
        return collection.iterator().next();
    }

    /**
     * Return <code>true</code> if the supplied Collection is <code>null</code>
     * or empty. Otherwise, return <code>false</code>.
     * 
     * @param collection the Collection to check
     * @return whether the given Collection is empty
     */
    public static boolean isEmpty(final Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Return <code>true</code> if the supplied Map is <code>null</code> or
     * empty. Otherwise, return <code>false</code>.
     * 
     * @param map the Map to check
     * @return whether the given Map is empty
     */
    public static boolean isEmpty(final Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * Populates the given collection by replacing any existing contents with
     * the given elements, in a null-safe way.
     * 
     * @param <T> the type of element in the collection
     * @param collection the collection to populate (can be <code>null</code>)
     * @param items the items with which to populate the collection (can be
     *            <code>null</code> or empty for none)
     * @return the given collection (useful if it was anonymous)
     */
    public static <T> Collection<T> populate(final Collection<T> collection,
            final Collection<? extends T> items) {
        if (collection != null) {
            collection.clear();
            if (items != null) {
                collection.addAll(items);
            }
        }
        return collection;
    }

    /**
     * Constructor is private to prevent instantiation
     * 
     * @since 1.2.0
     */
    private CollectionUtils() {
    }
}