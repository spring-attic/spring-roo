package __TOP_LEVEL_PACKAGE__.client.scaffold.ui;

import com.google.gwt.text.shared.Parser;

import java.text.ParseException;

/**
 * Simple parser of Character.
 */
public class CharParser implements Parser<Character> {
	private static CharParser INSTANCE;

	/**
	 * @return the instance of the no-op renderer
	 */
	public static Parser<Character> instance() {
		if (INSTANCE == null) {
			INSTANCE = new CharParser();
		}
		return INSTANCE;
	}

	protected CharParser() {
	}

	public Character parse(CharSequence object) throws ParseException {
		if (object == null || object.length() == 0 || "".equals(object.toString())) {
			return null;
		}
		try {
			return object.charAt(0);
		} catch (IndexOutOfBoundsException e) {
			throw new ParseException(e.getMessage(), 0);
		}
	}
}
