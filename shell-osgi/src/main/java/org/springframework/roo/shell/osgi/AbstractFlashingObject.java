package org.springframework.roo.shell.osgi;

import java.util.logging.Level;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.springframework.roo.shell.Shell;

/**
 * Provides an easy way for subclasses to publish flash messages if a {@link Shell} is available
 * but without creating a dependency on the {@link Shell} being available. This abstract class
 * also enables subclasses to safely determine if the {@link Shell} is in development mode.
 * 
 * <p>
 * Subclasses should not use the normal {@link Shell#flash(Level, String, String)} method. Instead
 * they should use {@link #flash(Level, String, String)} and not declare a direct dependency on
 * {@link Shell}.
 * 
 * <p>
 * If a {@link Shell} is not available, this class will simply not publish flash messages. If a
 * {@link Shell} is available, flash messages will be sent to that {@link Shell}.
 * 
 * 
 * @author Ben Alex
 * @since 1.1
 *
 */
@Component(componentAbstract=true)
@Reference(name="shell", strategy=ReferenceStrategy.EVENT, policy=ReferencePolicy.DYNAMIC, referenceInterface=Shell.class, cardinality=ReferenceCardinality.OPTIONAL_UNARY)
public abstract class AbstractFlashingObject {

	/**
	 * Provided as a convenience for subclasses so they have a unique slot name for flash messages.
	 */
	protected final String MY_SLOT = getClass().getName();
	private final Class<?> mutex = getClass();
	private Shell shell;

	protected final void bindShell(Shell shell) {
		synchronized (mutex) {
			this.shell = shell;
		}
	}

	protected final void unbindShell(Shell shell) {
		synchronized (mutex) {
			this.shell = null;
		}
	}

	/**
	 * Delegates to the {@link Shell#isDevelopmentMode()} method if available. If no {@link Shell}
	 * is available, simply returns false.
	 * 
	 * @return true if the shell is available and it is in development mode (false in any other case)
	 */
	protected final boolean isDevelopmentMode() {
		synchronized (mutex) {
			if (shell != null) {
				return shell.isDevelopmentMode();
			}
			return false;
		}
	}

	/**
	 * Same signature as {@link Shell#flash(Level, String, String)}. If this method is called and the
	 * {@link Shell} is not available, it will simply discard the flash message.
	 * 
	 * @param level see {@link Shell#flash(Level, String, String)}
	 * @param message see {@link Shell#flash(Level, String, String)}
	 * @param slot see {@link Shell#flash(Level, String, String)}
	 */
	protected final void flash(Level level, String message, String slot) {
		synchronized (mutex) {
			if (shell != null) {
				shell.flash(level, message, slot);
			}
		}
	}

}