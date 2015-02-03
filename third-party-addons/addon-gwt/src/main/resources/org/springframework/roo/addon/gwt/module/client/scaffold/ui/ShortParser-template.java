package __TOP_LEVEL_PACKAGE__.client.scaffold.ui;

import com.google.gwt.text.shared.Parser;

import java.text.ParseException;

/**
 * Simple parser of Short that wraps {@link Short#valueOf(String)}.
 */
public class ShortParser implements Parser<Short> {
	private static ShortParser INSTANCE;

	/**
	 * @return the instance of the no-op renderer
	 */
	public static Parser<Short> instance() {
		if (INSTANCE == null) {
			INSTANCE = new ShortParser();
		}
		return INSTANCE;
	}

	protected ShortParser() {
	}

	public Short parse(CharSequence object) throws ParseException {
		if (object == null || "".equals(object.toString())) {
			return null;
		}

		try {
			return Short.valueOf(object.toString());
		} catch (NumberFormatException e) {
			throw new ParseException(e.getMessage(), 0);
		}
	}
}
