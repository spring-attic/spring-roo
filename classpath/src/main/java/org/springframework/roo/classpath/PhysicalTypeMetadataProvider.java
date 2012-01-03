package org.springframework.roo.classpath;

import org.springframework.roo.metadata.MetadataProvider;

/**
 * Provides a classpath-related finder for {@link PhysicalTypeMetadata}
 * instances.
 * <p>
 * Add-ons can rely on there being only one {@link PhysicalTypeMetadataProvider}
 * active at a time. Initially this will be because there will only be one
 * implementation that uses source code AST parsing, although it is intended
 * that a bytecode-based parser may also be added in the future. If more than
 * one implementation is eventually developed, they will be hidden below a
 * single visible delegating implementation. As such add-ons do not need to
 * consult a list of different implementations.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface PhysicalTypeMetadataProvider extends MetadataProvider {
}
