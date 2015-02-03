package __TOP_LEVEL_PACKAGE__.client.scaffold.place;

import com.google.web.bindery.requestfactory.shared.EntityProxy;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.view.client.HasData;

/**
 * A view of a list of {@link EntityProxy}s, which declares which properties it
 * is able to display.
 * <p/>
 * It is expected that such views will typically (eventually) be defined largely
 * in ui.xml files which declare the properties of interest, which is why the
 * view is a source of a property set rather than a receiver of one.
 *
 * @param <P> the type of the records to display
 */
public interface ProxyListView<P extends EntityProxy> extends IsWidget {
	
	/**
	 * Implemented by the owner of a RecordTableView.
	 *
	 * @param <R> the type of the records to display
	 */
	interface Delegate<R extends EntityProxy> {

		void createClicked();
	}

	HasData<P> asHasData();

	/**
	 * @return the set of properties this view displays
	 */
	String[] getPaths();

	/**
	 * Sets the delegate.
	 */
	void setDelegate(Delegate<P> delegate);
}
