package __TOP_LEVEL_PACKAGE__.client.scaffold.place;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

/**
 * Extends {@link AbstractProxyEditActivity} to work from a {@link EntityProxyId}
 *
 * @param <P> the type of proxy to find and edit
 */
public abstract class FindAndEditProxy<P extends EntityProxy> extends AbstractProxyEditActivity<P> {
	private final RequestFactory factory;
	private final EntityProxyId<P> proxyId;
	private P proxy;

	public FindAndEditProxy(EntityProxyId<P> proxyId, RequestFactory factory, ProxyEditView<P, ?> view, PlaceController placeController) {
		super(view, placeController);
		this.proxyId = proxyId;
		this.factory = factory;
	}

	@Override
	public void start(final AcceptsOneWidget display, final EventBus eventBus) {
		factory.find(proxyId).with(view.createEditorDriver().getPaths()).fire(new Receiver<P>() {
			@Override
			public void onSuccess(P response) {
				proxy = response;
				FindAndEditProxy.super.start(display, eventBus);
			}
		});
	}

	@Override
	protected P getProxy() {
		return proxy;
	}
}
