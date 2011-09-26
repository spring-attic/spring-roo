package org.springframework.roo.project.packaging;

/**
 * Unit test of the {@link Jar} {@link PackagingType}
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class JarTest extends PackagingTypeTestCase<Jar> {

	@Override
	protected Jar getPackagingType() {
		return new Jar();
	}
}
