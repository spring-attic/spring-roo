package __TOP_LEVEL_PACKAGE__.client.scaffold.ui;

import com.google.gwt.text.shared.AbstractRenderer;
import com.google.gwt.text.shared.Renderer;

/**
 * A simple renderer of Character values.
 */
public class CharRenderer extends AbstractRenderer<Character> {
	private static CharRenderer INSTANCE;

	/**
	 * @return the instance
	 */
	public static Renderer<Character> instance() {
		if (INSTANCE == null) {
			INSTANCE = new CharRenderer();
		}
		return INSTANCE;
	}

	protected CharRenderer() {
	}

	public String render(Character object) {
		if (object == null) {
			return "";
		}

		return object.toString();
	}
}
