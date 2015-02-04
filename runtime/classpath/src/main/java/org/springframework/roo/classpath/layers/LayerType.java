package org.springframework.roo.classpath.layers;

/**
 * Typical layers within a user application. Roo is not limited to these layers
 * alone; layer-providing addons can specify any desired integer position in
 * order to appear in the correct part of the application architecture relative
 * to the core position values shown below.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
public enum LayerType {

    /**
     * The pattern by which entities provide their own persistence methods.
     */
    ACTIVE_RECORD(20),

    /**
     * Infrastructure component that provides low-level persistence operations,
     * e.g. to a single table of a relational database. Usually implemented via
     * a specific persistence technology such as JPA or JDBC.
     */
    DAO(40),

    /**
     * The ultimate consumer of persistence-related operations, for example the
     * application's web or integration test layer.
     */
    HIGHEST(100),

    /**
     * Domain type that provides collection-like access to instances of
     * aggregate roots; implementations are usually persistence agnostic.
     */
    REPOSITORY(60),

    /**
     * Domain type that implements an application's use-cases.
     */
    SERVICE(80);

    private final int position;

    /**
     * Constructor
     * 
     * @param position the position of this layer relative to other layers
     */
    private LayerType(final int position) {
        this.position = position;
    }

    /**
     * Returns the position of this layer relative to other layers
     * 
     * @return any integer
     */
    public int getPosition() {
        return position;
    }
}
