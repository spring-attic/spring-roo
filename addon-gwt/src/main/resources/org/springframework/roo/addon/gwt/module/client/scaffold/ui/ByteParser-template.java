package __TOP_LEVEL_PACKAGE__.client.scaffold.ui;

import com.google.gwt.text.shared.Parser;

import java.text.ParseException;

/**
 * Simple parser of Byte that wraps {@link Byte#valueOf(String)}.
 */
public class ByteParser implements Parser<Byte> {
	private static ByteParser INSTANCE;

	/**
	 * @return the instance of the no-op renderer
	 */
	public static Parser<Byte> instance() {
		if (INSTANCE == null) {
			INSTANCE = new ByteParser();
		}
		return INSTANCE;
	}

	protected ByteParser() {
	}

	public Byte parse(CharSequence object) throws ParseException {
		if (object == null || "".equals(object.toString())) {
			return null;
		}

		try {
			return Byte.valueOf(object.toString());
		} catch (NumberFormatException e) {
			throw new ParseException(e.getMessage(), 0);
		}
	}
}
