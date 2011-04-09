package __TOP_LEVEL_PACKAGE__.client.scaffold.place;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.HasData;

/**
 * Abstract implementation of ProxyListView.
 *
 * @param <P> the type of the proxy
 */
public abstract class AbstractProxyListView<P extends EntityProxy> extends Composite implements ProxyListView<P> {
	private HasData<P> display;
	private ProxyListView.Delegate<P> delegate;

	public HasData<P> asHasData() {
		return display;
	}

	@Override
	public AbstractProxyListView<P> asWidget() {
		return this;
	}

	public void setDelegate(final Delegate<P> delegate) {
		this.delegate = delegate;
	}

	protected void init(Widget root, HasData<P> display, Button newButton) {
		super.initWidget(root);
		this.display = display;

		newButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				delegate.createClicked();
			}
		});
	}

	protected void initWidget(Widget widget) {
		throw new UnsupportedOperationException("AbstractRecordListView must be initialized via init(Widget, HasData<P>, Button) ");
	}
}
