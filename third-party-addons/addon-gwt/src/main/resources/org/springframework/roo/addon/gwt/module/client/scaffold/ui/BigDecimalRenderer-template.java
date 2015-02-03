package __TOP_LEVEL_PACKAGE__.client.scaffold.ui;

import com.google.gwt.text.shared.AbstractRenderer;
import com.google.gwt.text.shared.Renderer;

import java.math.BigDecimal;

/**
 * A simple renderer of Float values.
 */
public class BigDecimalRenderer extends AbstractRenderer<BigDecimal> {
	private static BigDecimalRenderer INSTANCE;

	/**
	 * @return the instance
	 */
	public static Renderer<BigDecimal> instance() {
		if (INSTANCE == null) {
			INSTANCE = new BigDecimalRenderer();
		}
		return INSTANCE;
	}

	protected BigDecimalRenderer() {
	}

	public String render(BigDecimal object) {
		if (object == null) {
			return "";
		}

		return object.toString();
	}
}