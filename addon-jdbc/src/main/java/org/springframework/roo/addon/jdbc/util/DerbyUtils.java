package org.springframework.roo.addon.jdbc.util;

import java.io.OutputStream;

/**
 * Utility class to direct output of derby logging to an {@link OutputStream},
 * thus suppressing the creation of the derby.log file.
 * <p>
 * To take effect in Roo, either add the following to the roo-dev and
 * roo-dev.bat files <br>
 * <code>-Dderby.stream.error.field=org.springframework.roo.addon.jdbc.util.DerbyUtils.DEV_NULL</code>
 * <br>
 * or entering,
 * <code>export ROO_OPTS="-Dderby.stream.error.field=org.springframework.roo.addon.jdbc.util.DerbyUtils.DEV_NULL"</code>
 * <br>
 * on the command line before starting the Roo shell.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public abstract class DerbyUtils {

    public static final OutputStream DEV_NULL = new OutputStream() {
        @Override
        public void write(final int b) {
        }
    };
}
