package __TOP_LEVEL_PACKAGE__.client.scaffold.place;

import javax.validation.ConstraintViolation;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryEditorDriver;
import com.google.web.bindery.requestfactory.shared.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

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
	protected final ProxyEditView<P, ?> view;
	private final PlaceController placeController;
	private RequestFactoryEditorDriver<P, ?> editorDriver;
	private boolean waiting;

	public AbstractProxyEditActivity(ProxyEditView<P, ?> view, PlaceController placeController) {
		this.view = view;
		this.placeController = placeController;
	}

	public void cancelClicked() {
		String unsavedChangesWarning = mayStop();
		if ((unsavedChangesWarning == null) || Window.confirm(unsavedChangesWarning)) {
			editorDriver = null;
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
		view.setDelegate(null);
		editorDriver = null;
	}

	public void saveClicked() {
		if (!changed()) {
			return;
		}

		RequestContext request = editorDriver.flush();
		if (editorDriver.hasErrors()) {
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
				if (editorDriver != null) {
					setWaiting(false);
					super.onFailure(error);
				}
			}

			@Override
			public void onSuccess(Void ignore) {
				if (editorDriver != null) {
					// We want no warnings from mayStop, so:

					// Defeat isChanged check
					editorDriver = null;

					// Defeat call-in-flight check
					setWaiting(false);

					exit(true);
				}
			}

			@Override
			public void onConstraintViolation(Set<ConstraintViolation<?>> violations) {
				if (editorDriver != null) {
					setWaiting(false);
					editorDriver.setConstraintViolations(violations);
				}
			}
		});
	}

	public void start(AcceptsOneWidget display, EventBus eventBus) {
		editorDriver = view.createEditorDriver();
		view.setDelegate(this);
		editorDriver.edit(getProxy(), createSaveRequest(getProxy()));
		editorDriver.flush();
		display.setWidget(view);
	}

	/**
	 * Called once to create the appropriate request to save
	 * changes.
	 *
	 * @return the request context to fire when the save button is clicked
	 */
	protected abstract RequestContext createSaveRequest(P proxy);

	/**
	 * Called when the user cancels or has successfully saved. This default
	 * implementation tells the {@link PlaceController} to show the details of the
	 * edited record.
	 *
	 * @param saved true if changes were comitted, false if user canceled
	 */
	protected void exit(@SuppressWarnings("unused") boolean saved) {
		placeController.goTo(new ProxyPlace(getProxyId(), ProxyPlace.Operation.DETAILS));
	}

	/**
	 * Get the proxy to be edited. Must be mutable, typically via a call to
	 * {@link RequestContext#edit(EntityProxy)}, or
	 * {@link RequestContext#create(Class)}.
	 */
	protected abstract P getProxy();

	@SuppressWarnings("unchecked")
	// id type always matches proxy type
	protected EntityProxyId<P> getProxyId() {
		return (EntityProxyId<P>) getProxy().stableId();
	}

	private boolean changed() {
		return editorDriver != null && editorDriver.flush().isChanged();
	}

	/**
	 * @return true if we're waiting for an rpc response.
	 */
	private boolean isWaiting() {
		return waiting;
	}

	/**
	 * While we are waiting for a response, we cannot poke setters on the proxy
	 * (that is, we cannot call editorDriver.flush). So we set the waiting flag to
	 * warn ourselves not to, and to disable the view.
	 */
	private void setWaiting(boolean wait) {
		this.waiting = wait;
		view.setEnabled(!wait);
	}
}
