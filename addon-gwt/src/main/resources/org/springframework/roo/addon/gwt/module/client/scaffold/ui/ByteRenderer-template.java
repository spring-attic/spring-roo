package __TOP_LEVEL_PACKAGE__.client.scaffold.ui;

import com.google.gwt.text.shared.AbstractRenderer;
import com.google.gwt.text.shared.Renderer;

/**
 * A simple renderer of Byte values.
 */
public class ByteRenderer extends AbstractRenderer<Byte> {
	private static ByteRenderer INSTANCE;

	/**
	 * @return the instance
	 */
	public static Renderer<Byte> instance() {
		if (INSTANCE == null) {
			INSTANCE = new ByteRenderer();
		}
		return INSTANCE;
	}

	protected ByteRenderer() {
	}

	public String render(Byte object) {
		if (object == null) {
			return "";
		}

		return object.toString();
	}
}
