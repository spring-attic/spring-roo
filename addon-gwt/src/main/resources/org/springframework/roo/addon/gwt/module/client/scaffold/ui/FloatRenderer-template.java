package __TOP_LEVEL_PACKAGE__.client.scaffold.ui;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.text.shared.AbstractRenderer;
import com.google.gwt.text.shared.Renderer;

/**
 * A simple renderer of Float values.
 */
public class FloatRenderer extends AbstractRenderer<Float> {
	private static FloatRenderer INSTANCE;

	/**
	 * @return the instance
	 */
	public static Renderer<Float> instance() {
		if (INSTANCE == null) {
			INSTANCE = new FloatRenderer();
		}
		return INSTANCE;
	}

	protected FloatRenderer() {
	}

	public String render(Float object) {
		if (object == null) {
			return "";
		}

		return NumberFormat.getDecimalFormat().format(object);
	}
}
