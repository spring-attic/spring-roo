package org.springframework.roo.project;

/**
 * Immutable representation of common file path conventions used in Maven projects.
 *
 * <p>
 * {@link PathResolver} instances provide the ability to resolve these paths
 * to and from physical locations.
 *
 * <p>
 * A name cannot include the question mark character.
 *
 * <p>
 * Presented as a class instead of an enumeration to enable extension.
 *
 * @author Ben Alex
 * @since 1.0
 */
public enum Path {

	SRC_MAIN_JAVA,
	SRC_MAIN_RESOURCES,
	SRC_TEST_JAVA,
	SRC_TEST_RESOURCES,
	SRC_MAIN_WEBAPP,
	ROOT,
	SPRING_CONFIG_ROOT;

	public ContextualPath contextualize() {
		return ContextualPath.getInstance(this);
	}

	public ContextualPath contextualize(final String context) {
		return ContextualPath.getInstance(this, context);
	}
}
