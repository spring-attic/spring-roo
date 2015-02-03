package __TOP_LEVEL_PACKAGE__.client.scaffold.ui;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.ValueBox;

/**
 * A ValueBox that uses {@link FloatParser} and {@link FloatRenderer}.
 */
public class FloatBox extends ValueBox<Float> {

	public FloatBox() {
		super(Document.get().createTextInputElement(), FloatRenderer.instance(), FloatParser.instance());
	}
}