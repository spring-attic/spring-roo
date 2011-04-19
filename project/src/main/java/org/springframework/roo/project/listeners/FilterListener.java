package org.springframework.roo.project.listeners;

import org.springframework.roo.project.Filter;

/**
 * Filter listener interface that clients can implement in order
 * to be notified of changes to project filters
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Deprecated
public interface FilterListener {

	void filterAdded(Filter filter);

	void filterRemoved(Filter filter);
}
