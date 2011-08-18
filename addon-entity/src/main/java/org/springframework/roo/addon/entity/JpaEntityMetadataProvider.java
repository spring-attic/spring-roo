package org.springframework.roo.addon.entity;

import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProvider;

/**
 * Provides metadata relating to JPA entities. Has taken over from
 * {@link EntityMetadataProvider} the management of core JPA concerns such as
 * the id and version fields and applying the JPA @Entity and @Table
 * annotations. The {@link EntityMetadataProvider} remains responsible for CRUD
 * methods such as merge(), persist(), finders, etc.
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public interface JpaEntityMetadataProvider extends ItdTriggerBasedMetadataProvider {}