package __TOP_LEVEL_PACKAGE__.gwt.scaffold;

import com.google.gwt.app.place.Activity;
import com.google.gwt.app.place.ActivityManager;
import com.google.gwt.app.place.ActivityMapper;
import com.google.gwt.app.place.CachingActivityMapper;
import com.google.gwt.app.place.FilteredActivityMapper;
import com.google.gwt.app.place.IsWidget;
import com.google.gwt.app.place.Place;
import com.google.gwt.app.place.PlaceController;
import com.google.gwt.app.place.PlaceHistoryHandler;
import com.google.gwt.app.place.ProxyListPlace;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.requestfactory.client.AuthenticationFailureHandler;
import com.google.gwt.requestfactory.client.LoginWidget;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestEvent;
import com.google.gwt.requestfactory.shared.UserInformationProxy;
import com.google.gwt.requestfactory.shared.RequestEvent.State;
import __TOP_LEVEL_PACKAGE__.gwt.request.ApplicationEntityTypesProcessor;
import __TOP_LEVEL_PACKAGE__.gwt.request.ApplicationRequestFactory;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.ui.HasConstrainedValue;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.SyncResult;
import __TOP_LEVEL_PACKAGE__.gwt.scaffold.ioc.ScaffoldInjector;

import java.util.HashSet;
import java.util.Set;

/**
 * Application for browsing entities.
 */
public class Scaffold implements EntryPoint {

	final private ScaffoldInjector injector = GWT.create(ScaffoldInjector.class);
	
	public void onModuleLoad() {

		/* App controllers and services */

		final EventBus eventBus = injector.getEventBus();
		final ApplicationRequestFactory requestFactory = injector.getRequestFactory();
		final PlaceController placeController = injector.getPlaceController();

    GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
      public void onUncaughtException(Throwable e) {
        Window.alert("Error: " + e.getMessage());
        placeController.goTo(Place.NOWHERE);
      }
    });
    
		/* Top level UI */

		final ScaffoldShell shell = injector.getShell();

		/* Display loading notifications when we touch the network. */

		eventBus.addHandler(RequestEvent.TYPE, new RequestEvent.Handler() {
			// Only show loading status if a request isn't serviced in 250ms.
			private static final int LOADING_TIMEOUT = 250;

			public void onRequestEvent(RequestEvent requestEvent) {
				if (requestEvent.getState() == State.SENT) {
					shell.getMole().showDelayed(LOADING_TIMEOUT);
				} else {
					shell.getMole().hide();
				}
			}
		});

		/* Check for Authentication failures or mismatches */

		eventBus.addHandler(RequestEvent.TYPE, new AuthenticationFailureHandler());

		/* Add a login widget to the page */

		final LoginWidget login = shell.getLoginWidget();
		Receiver<UserInformationProxy> receiver = new Receiver<UserInformationProxy>() {
			public void onSuccess(UserInformationProxy userInformationProxy, Set<SyncResult> syncResults) {
				login.setUserInformation(userInformationProxy);
			}
		};
		requestFactory.userInformationRequest().getCurrentUserInformation(Location.getHref()).fire(receiver);

		/* Left side lets us pick from all the types of entities */

		HasConstrainedValue<ProxyListPlace> listPlacePickerView = shell.getPlacesBox();
		listPlacePickerView.setAcceptableValues(getTopPlaces());
		injector.getListPlacePicker().register(eventBus, listPlacePickerView);

		/*
		 * Top of the screen is the master list of a master / detail UI. Set it up to filter ProxyPlace instances to an appropriate ProxyListPlace, and put a cache on it to keep the same activity
		 * instance running rather than building a new one on each detail change.
		 */

		CachingActivityMapper cached = new CachingActivityMapper(new ApplicationMasterActivities(requestFactory, placeController));
		ActivityMapper masterActivityMap = new FilteredActivityMapper(injector.getProxyPlaceToListPlace(), cached);
		final ActivityManager masterActivityManager = new ActivityManager(masterActivityMap, eventBus);

		masterActivityManager.setDisplay(new Activity.Display() {
			public void showActivityWidget(IsWidget widget) {
				shell.getMasterPanel().setWidget(widget == null ? null : widget.asWidget());
			}
		});

		/* Bottom of the screen shows the details of a master / detail UI */

		final ActivityManager detailsActivityManager = new ActivityManager(new ApplicationDetailsActivities(requestFactory, placeController), eventBus);

		detailsActivityManager.setDisplay(new Activity.Display() {
			public void showActivityWidget(IsWidget widget) {
				shell.getDetailsPanel().setWidget(widget == null ? null : widget.asWidget());
			}
		});

		/* Hide the loading message */

		Element loading = Document.get().getElementById("loading");
		loading.getParentElement().removeChild(loading);

		/* Browser history integration */
		PlaceHistoryHandler placeHistoryHandler = injector.getPlaceHistoryHandler();
		placeHistoryHandler.register(placeController, eventBus,
		getTopPlaces().iterator().next()); /* defaultPlace */
		placeHistoryHandler.handleCurrentHistory();

		/* And show the user the shell */

		RootLayoutPanel.get().add(shell);
	}

	// TODO (rjrjr) No reason to make the place objects in advance, just make
	// it list the class objects themselves. Needs to be sorted by rendered name too
	private Set<ProxyListPlace> getTopPlaces() {
		Set<Class<? extends EntityProxy>> types = ApplicationEntityTypesProcessor.getAll();
		Set<ProxyListPlace> rtn = new HashSet<ProxyListPlace>(types.size());

		for (Class<? extends EntityProxy> type : types) {
			rtn.add(new ProxyListPlace(type));
		}

		return rtn;
	}
}
