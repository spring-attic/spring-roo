package __TOP_LEVEL_PACKAGE__.client.scaffold.ui;

import com.google.gwt.text.shared.Parser;

import java.text.ParseException;

/**
 * Simple parser of Float that wraps {@link Float#valueOf(String)}.
 */
public class FloatParser implements Parser<Float> {
	private static FloatParser INSTANCE;

	/**
	 * @return the instance of the no-op renderer
	 */
	public static Parser<Float> instance() {
		if (INSTANCE == null) {
			INSTANCE = new FloatParser();
		}
		return INSTANCE;
	}

	protected FloatParser() {
	}

	public Float parse(CharSequence object) throws ParseException {
		if (object == null || "".equals(object.toString())) {
			return null;
		}

		try {
			return Float.valueOf(object.toString());
		} catch (NumberFormatException e) {
			throw new ParseException(e.getMessage(), 0);
		}
	}
}
