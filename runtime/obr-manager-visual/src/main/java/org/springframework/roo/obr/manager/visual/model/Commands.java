package org.springframework.roo.obr.manager.visual.model;

/**
 *
 * This class provides Spring Roo commands that 
 * will be executed by UI
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class Commands {
    
    /**
     * Command to install new repository
     */
    public static final String SPRING_ROO_ADD_REPOSITORY_COMMAND = "addon repository add --url";
    
    
    /**
     * Command to install new Bundle
     */
    public static final String SPRING_ROO_INSTALL_BUNDLE = "addon install bundle --bundleSymbolicName";
    
    /**
     * Command to remove new Bundle
     */
    public static final String SPRING_ROO_REMOVE_BUNDLE = "addon remove --bundleSymbolicName";
    
    
     /**
     * Command to start repository manager UI
     */
    public static final String SPRING_ROO_REPOSITORY_MANAGER = "addon repository manager";
}
