package org.springframework.roo.addon.op4j;

import org.springframework.roo.model.JavaType;

/**
 * Interface of Op4j commands that are available via the Roo shell.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public interface Op4jOperations {

    void annotateType(JavaType type);

    boolean isOp4jInstallationPossible();

    void setup();
}