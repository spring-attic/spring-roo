package org.springframework.roo.project.layers;

/**
 * A built-in {@link LayerAdapter}, in other words one provided by a core addon.
 *
 * @author Andrew Swan
 * @since 1.2
 */
public abstract class CoreLayerProvider<M extends Enum<M>> extends LayerAdapter<M> {

	// Constants
	private static final boolean DEBUG = false;

	/**
	 * This implementation returns {@link LayerProvider#CORE_LAYER_PRIORITY} 
	 */
	public int getPriority() {
		return CORE_LAYER_PRIORITY;
	}
	
	/**
	 * Logs the given message to the console
	 * 
	 * @param message can be blank
	 */
	protected void log(final String message) {
		if (DEBUG) {
			System.out.println(">>>>>>>> " + getClass().getSimpleName() + ": " + message);
		}
	}
}