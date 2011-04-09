package __TOP_LEVEL_PACKAGE__.client.scaffold.ui;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.ValueBox;

/**
 * A ValueBox that uses {@link ByteParser} and {@link ByteRenderer}.
 */
public class ByteBox extends ValueBox<Byte> {

	public ByteBox() {
		super(Document.get().createTextInputElement(), ByteRenderer.instance(), ByteParser.instance());
	}
}