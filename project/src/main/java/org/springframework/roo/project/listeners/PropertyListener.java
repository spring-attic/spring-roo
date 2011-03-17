package org.springframework.roo.project.listeners;

import org.springframework.roo.project.Property;

/**
 * Property listener interface that clients can implement in order
 * to be notified of changes to project properties
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public interface PropertyListener {

	void propertyAdded(Property property);

	void propertyRemoved(Property property);
}
