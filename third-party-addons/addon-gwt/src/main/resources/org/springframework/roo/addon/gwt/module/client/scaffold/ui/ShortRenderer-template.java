package __TOP_LEVEL_PACKAGE__.client.scaffold.ui;

import com.google.gwt.text.shared.AbstractRenderer;
import com.google.gwt.text.shared.Renderer;

/**
 * A simple renderer of Short values.
 */
public class ShortRenderer extends AbstractRenderer<Short> {
	private static ShortRenderer INSTANCE;

	/**
	 * @return the instance
	 */
	public static Renderer<Short> instance() {
		if (INSTANCE == null) {
			INSTANCE = new ShortRenderer();
		}
		return INSTANCE;
	}

	protected ShortRenderer() {
	}

	public String render(Short object) {
		if (object == null) {
			return "";
		}

		return object.toString();
	}
}