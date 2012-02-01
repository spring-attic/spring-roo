package org.springframework.roo.project.maven;

import org.apache.commons.lang3.Validate;

/**
 * A module of a Maven multi-module project.
 * 
 * @author James Tyrrell
 * @since 1.2.0
 */
public class Module {

    private final String name;
    private final String pomPath;

    /**
     * Constructor
     * 
     * @param name the module's name (can't be blank)
     * @param pomPath the canonical path of the module's POM file (can't be
     *            blank)
     */
    public Module(final String name, final String pomPath) {
        Validate.notBlank(name, "Invalid module name '" + name + "'");
        Validate.notBlank(pomPath, "Invalid path '" + pomPath + "'");
        this.name = name;
        this.pomPath = pomPath;
    }

    public String getName() {
        return name;
    }

    public String getPomPath() {
        return pomPath;
    }
}
