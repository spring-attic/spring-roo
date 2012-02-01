package org.springframework.roo.addon.logging;

import java.util.Arrays;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Provides information related to the configuration of the LOGGER.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
public enum LoggerPackage {
    ALL_SPRING("org.springframework"), AOP("org.springframework.aop",
            "org.springframework.aspects"), PERSISTENCE(
            "org.springframework.orm"), PROJECT, ROOT, SECURITY(
            "org.springframework.security"), TRANSACTIONS(
            "org.springframework.transactions"), WEB("org.springframework.web");

    private String[] packageNames;

    private LoggerPackage(final String... packageNames) {
        this.packageNames = packageNames;
    }

    public String[] getPackageNames() {
        return packageNames;
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("layer", name());
        builder.append("package names", Arrays.asList(packageNames));
        return builder.toString();
    }
}
