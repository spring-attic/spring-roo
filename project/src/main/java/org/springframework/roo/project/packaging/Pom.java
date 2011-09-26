package org.springframework.roo.project.packaging;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;

/**
 * The Maven "pom" {@link PackagingType}
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component
@Service
public class Pom extends AbstractPackagingType {

	/**
	 * Constructor
	 */
	public Pom() {
		super("pom", "parent-pom-template.xml");
	}
}
