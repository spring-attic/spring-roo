package __TOP_LEVEL_PACKAGE__.client.scaffold.ui;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.ValueBox;

import java.math.BigDecimal;

/**
 * A ValueBox that uses {@link BigDecimalParser} and {@link BigDecimalRenderer}.
 */
public class BigDecimalBox extends ValueBox<BigDecimal> {

	public BigDecimalBox() {
		super(Document.get().createTextInputElement(), BigDecimalRenderer.instance(),
				BigDecimalParser.instance());
	}
}