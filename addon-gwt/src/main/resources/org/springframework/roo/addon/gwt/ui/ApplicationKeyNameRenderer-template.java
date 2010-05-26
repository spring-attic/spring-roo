package __TOP_LEVEL_PACKAGE__.gwt.ui;

import com.google.gwt.input.shared.Renderer;
import com.google.gwt.valuestore.shared.Record;

/**
 * Renders the name of an {@link Record}.
 */
// TODO i18n
public class ApplicationKeyNameRenderer implements Renderer<Record> {
	public String render(Record entity) {
		String name = entity.getClass().getName();
		if (name.lastIndexOf(".") > -1) {
			name = name.substring(name.lastIndexOf(".") + 1);
		}
		if (name.endsWith("Record")) {
			name = name.substring(0, name.length() - 6);
		}
		return name + "s";
	}
}
