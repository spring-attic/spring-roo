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
     * Command to remove installed repository
     */
    public static final String SPRING_ROO_REMOVE_REPOSITORY_COMMAND = "addon repository remove --url";
    
    /**
     * Command to install new Bundle
     */
    public static final String SPRING_ROO_INSTALL_BUNDLE = "addon install bundle --bundleSymbolicName";
    
    /**
     * Command to remove installed Bundle
     */
    public static final String SPRING_ROO_REMOVE_BUNDLE = "addon remove --bundleSymbolicName";
    
    /**
     * Command to install new Roo Addon Suite
     */
    public static final String SPRING_ROO_INSTALL_SUITE = "addon suite install name --symbolicName";
    
    /**
     * Command to remove installed Roo Addon Suite
     */
    public static final String SPRING_ROO_REMOVE_SUITE = "addon suite uninstall --symbolicName";
    
     /**
     * Command to start repository manager UI
     */
    public static final String SPRING_ROO_REPOSITORY_MANAGER = "addon repository manager";
}
