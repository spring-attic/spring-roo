package __TOP_LEVEL_PACKAGE__.client.scaffold.place;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.RequestContext;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

/**
 * Extends {@link AbstractProxyEditActivity} to first create an instance to edit
 *
 * @param <P> the type of proxy to create and edit
 */
public abstract class CreateAndEditProxy<P extends EntityProxy> extends AbstractProxyEditActivity<P> {

	private final P proxy;
	private final PlaceController placeController;
	private Class<P> proxyClass;

	public CreateAndEditProxy(Class<P> proxyClass, RequestContext request,
	                          ProxyEditView<P, ?> view, PlaceController placeController) {
		super(view, placeController);
		this.proxy = request.create(proxyClass);
		this.placeController = placeController;
		this.proxyClass = proxyClass;
	}

	@Override
	public void start(AcceptsOneWidget display, EventBus eventBus) {
		super.start(display, eventBus);
	}

	/**
	 * Called when the user cancels or has successfully saved. Refines the default
	 * implementation to clear the display given at {@link #start} on cancel.
	 *
	 * @param saved true if changes were comitted, false if user canceled
	 */
	@Override
	protected void exit(boolean saved) {
		if (!saved) {
			placeController.goTo(new ProxyListPlace(proxyClass));
		} else {
			super.exit(saved);
		}
	}

	@Override
	protected P getProxy() {
		return proxy;
	}
}
