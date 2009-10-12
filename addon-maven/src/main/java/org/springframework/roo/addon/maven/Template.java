package org.springframework.roo.addon.maven;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * The type of Maven project the user wishes to create.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class Template implements Comparable<Template> {

	private String template;

	public static final Template STANDARD_PROJECT = new Template("standard-project-template.xml");
	public static final Template ROO_ADDON_SIMPLE= new Template("roo-addon-simple-template.xml");
	
	public Template(String location) {
		Assert.notNull(location, "Location required");
		this.template = location;
	}
	
	public String getLocation() {
		return template;
	}

	public boolean isAddOn() {
		return ROO_ADDON_SIMPLE.equals(this);
	}
	
	public final boolean equals(Object obj) {
		return obj != null && obj instanceof Template && this.compareTo((Template) obj) == 0;
	}

	public final int compareTo(Template o) {
		if (o == null)
			return -1;
		int result = this.template.compareTo(o.template);

		return result;
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("location", template);
		return tsc.toString();
	}

	public String getKey() {
		return this.template;
	}
}