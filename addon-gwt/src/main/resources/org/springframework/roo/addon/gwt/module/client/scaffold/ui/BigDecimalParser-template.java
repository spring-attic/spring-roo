package __TOP_LEVEL_PACKAGE__.client.scaffold.ui;

import com.google.gwt.text.shared.Parser;

import java.math.BigDecimal;
import java.text.ParseException;

/**
 * Simple parser of BigDecimal that wraps {@link BigDecimal#toString()}.
 */
public class BigDecimalParser implements Parser<BigDecimal> {
	private static BigDecimalParser INSTANCE;

	/**
	 * @return the instance of the no-op renderer
	 */
	public static Parser<BigDecimal> instance() {
		if (INSTANCE == null) {
			INSTANCE = new BigDecimalParser();
		}
		return INSTANCE;
	}

	protected BigDecimalParser() {
	}

	public BigDecimal parse(CharSequence object) throws ParseException {
		if (object == null || "".equals(object.toString())) {
			return null;
		}

		try {
			return new BigDecimal(object.toString());
		} catch (NumberFormatException e) {
			throw new ParseException(e.getMessage(), 0);
		}
	}
}