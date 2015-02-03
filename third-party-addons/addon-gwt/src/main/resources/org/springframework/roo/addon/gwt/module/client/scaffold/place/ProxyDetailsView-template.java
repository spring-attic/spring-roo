package __TOP_LEVEL_PACKAGE__.client.scaffold.place;

import com.google.gwt.user.client.TakesValue;
import com.google.gwt.user.client.ui.IsWidget;

/**
 * Implemented by views that show the details of an object.
 *
 * @param <P> the type of object to show
 */
public interface ProxyDetailsView<P> extends TakesValue<P>, IsWidget {

	/**
	 * Implemented by the owner of the view.
	 */
	interface Delegate {
		
		void deleteClicked();

		void editClicked();
	}

	boolean confirm(String msg);
}