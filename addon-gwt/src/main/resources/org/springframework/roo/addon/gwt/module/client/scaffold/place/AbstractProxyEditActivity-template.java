package __TOP_LEVEL_PACKAGE__.client.scaffold.place;

import javax.validation.ConstraintViolation;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryEditorDriver;
import com.google.web.bindery.requestfactory.shared.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import __TOP_LEVEL_PACKAGE__.client.managed.request.ApplicationRequestFactory;
import __TOP_LEVEL_PACKAGE__.client.scaffold.place.AbstractProxyEditActivity;
import __TOP_LEVEL_PACKAGE__.client.scaffold.place.ProxyEditView;
import __TOP_LEVEL_PACKAGE__.client.scaffold.place.ProxyListPlace;

import java.util.Set;

/**
 * Abstract activity for editing a record. Subclasses must provide access to the
 * request that will be fired when Save is clicked.
 * <p/>
 * Instances are not reusable. Once an activity is stopped, it cannot be
 * restarted.
 *
 * @param <P> the type of Proxy being edited
 */
public abstract class AbstractProxyEditActivity<P extends EntityProxy> implements Activity, ProxyEditView.Delegate {
	protected final PlaceController placeController;
	protected final ApplicationRequestFactory factory;
	protected final EntityProxyId<P> proxyId;

	protected RequestFactoryEditorDriver<P, ?> editorDriver;
	protected P proxy;
	protected AcceptsOneWidget display;
	protected EventBus eventBus;

	private boolean waiting;

	public AbstractProxyEditActivity(EntityProxyId<P> proxyId, ApplicationRequestFactory factory, PlaceController placeController) {
		this.factory = factory;
		this.proxyId = proxyId;
		this.placeController = placeController;
	}

	protected abstract ProxyEditView<P, ?> getView();

	protected abstract P createProxy();

	/**
	 * Called once to create the appropriate request to save changes.
	 * 
	 * @return the request context to fire when the save button is clicked
	 */
	protected abstract RequestContext createSaveRequest(P proxy);

	/**
	 * Get the proxy to be edited. Must be mutable, typically via a call to
	 * {@link RequestContext#edit(EntityProxy)}, or
	 * {@link RequestContext#create(Class)}.
	 */
	protected P getProxy() {
		return proxy;
	}

	@Override
	public void start(AcceptsOneWidget display, EventBus eventBus)	{
		this.display = display;
		this.eventBus = eventBus;
		if (proxyId != null)		{
			createFindRequest().fire(new Receiver<P>() {

				@Override
				public void onSuccess(P response) {
					AbstractProxyEditActivity.this.proxy = response;
					bindToView();
				}
			});
		}
		else {
			this.proxy = createProxy();
			bindToView();
		}
	}

	protected Request<P> createFindRequest() {
		return this.factory.find(this.proxyId).with(getView().createEditorDriver().getPaths());
	}

	public void cancelClicked()	{
		String unsavedChangesWarning = mayStop();
		if ((unsavedChangesWarning == null) || Window.confirm(unsavedChangesWarning)) {
			this.editorDriver = null;
			exit(false);
		}
	}

	public String mayStop() {
		if (isWaiting() || changed()) {
			return "Are you sure you want to abandon your changes?";
		}

		return null;
	}

	public void onCancel() {
		onStop();
	}

	public void onStop() {
		this.editorDriver = null;
	}

	public void saveClicked() {
		if (!changed()) {
			return;
		}

		RequestContext request = this.editorDriver.flush();
		if (this.editorDriver.hasErrors()) {
			return;
		}

		setWaiting(true);
		request.fire(new Receiver<Void>() {
			/*
			 * Callbacks do nothing if editorDriver is null, we were stopped in
			 * midflight
			 */
			@Override
			public void onFailure(ServerFailure error) {
				if (AbstractProxyEditActivity.this.editorDriver != null) {
					setWaiting(false);
					super.onFailure(error);
				}
			}

			@Override
			public void onSuccess(Void ignore) {
				executePostSaveActions();
			}

			@Override
			public void onConstraintViolation(Set<ConstraintViolation<?>> violations) {
				if (AbstractProxyEditActivity.this.editorDriver != null) {
					setWaiting(false);
					AbstractProxyEditActivity.this.editorDriver.setConstraintViolations(violations);
				}
			}
		});
	}

	protected void executePostSaveActions()
	{
		if (this.editorDriver != null) {
			// We want no warnings from mayStop, so:

			// Defeat isChanged check
			this.editorDriver = null;

			// Defeat call-in-flight check
			setWaiting(false);

			exit(true);
		}
	}

	public void bindToView() {
		this.editorDriver = getView().createEditorDriver();
		executeBeforeBind();
		this.editorDriver.edit(getProxy(), createSaveRequest(getProxy()));
		this.editorDriver.flush();
		executeAfterBind();
		this.display.setWidget(getView());
	}

	
	/**
	 * Overridable method to perform actions before editorDriver.edit is called.  
	 * By default it is empty.
	 */
	protected void executeBeforeBind() {

	}

	/**
	 * Overridable method to perform actions after editorDriver.edit is called.
	 * By default it is empty.
	 */
	protected void executeAfterBind() {

	}

	/**
	 * Overridable method to perform actions after the view is made visible. 
	 * By default it is empty.
	 */
	protected void executeDisplaySet() {

	}

	@SuppressWarnings("unchecked")
	// id type always matches proxy type
	protected EntityProxyId<P> getProxyId() {
		return (EntityProxyId<P>) getProxy().stableId();
	}

	protected boolean changed() {
		return this.editorDriver != null && this.editorDriver.flush().isChanged();
	}

	/**
	 * Called when the user cancels or has successfully saved. This default
	 * implementation tells the {@link PlaceController} to show the details of
	 * the edited record.
	 * 
	 * @param saved
	 *            true if changes were comitted, false if user canceled
	 */
	protected void exit(boolean saved) {
		this.placeController.goTo(new ProxyListPlace(getProxyId().getProxyClass()));
	}

	/**
	 * @return true if we're waiting for an rpc response.
	 */
	protected boolean isWaiting() {
		return this.waiting;
	}

	/**
	 * While we are waiting for a response, we cannot poke setters on the proxy
	 * (that is, we cannot call editorDriver.flush). So we set the waiting flag
	 * to warn ourselves not to, and to disable the view.
	 */
	protected void setWaiting(boolean wait)	{
		this.waiting = wait;
		getView().setEnabled(!wait);
	}
}
