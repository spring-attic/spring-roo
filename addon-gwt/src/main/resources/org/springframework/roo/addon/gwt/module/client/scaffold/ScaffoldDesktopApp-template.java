package __TOP_LEVEL_PACKAGE__.__SEGMENT_PACKAGE__;

import __TOP_LEVEL_PACKAGE__.client.managed.activity.*;
import __TOP_LEVEL_PACKAGE__.client.managed.request.ApplicationRequestFactory;
import __TOP_LEVEL_PACKAGE__.client.scaffold.gae.GaeHelper;
import __TOP_LEVEL_PACKAGE__.client.scaffold.place.*;
import __TOP_LEVEL_PACKAGE__.client.scaffold.request.RequestEvent;
import com.google.gwt.activity.shared.*;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.logging.client.LogConfiguration;
import com.google.gwt.place.shared.*;
import com.google.gwt.requestfactory.client.RequestFactoryLogHandler;
import com.google.gwt.requestfactory.shared.LoggingRequest;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasConstrainedValue;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

__GAE_IMPORT__

/**
 * Application for browsing entities.
 */
public class ScaffoldDesktopApp extends ScaffoldApp {
	private static final Logger log = Logger.getLogger(Scaffold.class.getName());
	private final ScaffoldDesktopShell shell;
	private final ApplicationRequestFactory requestFactory;
	private final EventBus eventBus;
	private final PlaceController placeController;
	private final PlaceHistoryFactory placeHistoryFactory;
	private final ApplicationMasterActivities applicationMasterActivities;
	private final ApplicationDetailsActivities applicationDetailsActivities;

	@Inject
	public ScaffoldDesktopApp(ScaffoldDesktopShell shell, ApplicationRequestFactory requestFactory, EventBus eventBus, PlaceController placeController, PlaceHistoryFactory placeHistoryFactory, ApplicationMasterActivities applicationMasterActivities, ApplicationDetailsActivities applicationDetailsActivities, GaeHelper gaeHelper) {
		this.shell = shell;
		this.requestFactory = requestFactory;
		this.eventBus = eventBus;
		this.placeController = placeController;
		this.placeHistoryFactory = placeHistoryFactory;
		this.applicationMasterActivities = applicationMasterActivities;
		this.applicationDetailsActivities = applicationDetailsActivities;
	}

	public void run() {
		/* Add handlers, setup activities */
		init();

		/* Hide the loading message */
		Element loading = Document.get().getElementById("loading");
		loading.getParentElement().removeChild(loading);

		/* And show the user the shell */
		RootLayoutPanel.get().add(shell);
	}

	private void init() {
		GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
			public void onUncaughtException(Throwable e) {
				Window.alert("Error: " + e.getMessage());
				log.log(Level.SEVERE, e.getMessage(), e);
			}
		});

		if (LogConfiguration.loggingIsEnabled()) {
			// Add remote logging handler
			RequestFactoryLogHandler.LoggingRequestProvider provider = new RequestFactoryLogHandler.LoggingRequestProvider() {
				public LoggingRequest getLoggingRequest() {
					return requestFactory.loggingRequest();
				}
			};
			Logger.getLogger("").addHandler(new RequestFactoryLogHandler(provider, Level.WARNING, new ArrayList<String>()));
		}

		RequestEvent.register(eventBus, new RequestEvent.Handler() {
			// Only show loading status if a request isn't serviced in 250ms.
			private static final int LOADING_TIMEOUT = 250;

			public void onRequestEvent(RequestEvent requestEvent) {
				if (requestEvent.getState() == RequestEvent.State.SENT) {
					shell.getMole().showDelayed(LOADING_TIMEOUT);
				} else {
					shell.getMole().hide();
				}
			}
		});

		CachingActivityMapper cached = new CachingActivityMapper(applicationMasterActivities);
		ProxyPlaceToListPlace proxyPlaceToListPlace = new ProxyPlaceToListPlace();
		ActivityMapper masterActivityMap = new FilteredActivityMapper(proxyPlaceToListPlace, cached);
		final ActivityManager masterActivityManager = new ActivityManager(masterActivityMap, eventBus);

		masterActivityManager.setDisplay(shell.getMasterPanel());

		ProxyListPlacePicker proxyListPlacePicker = new ProxyListPlacePicker(placeController, proxyPlaceToListPlace);
		HasConstrainedValue<ProxyListPlace> listPlacePickerView = shell.getPlacesBox();
		listPlacePickerView.setAcceptableValues(getTopPlaces());
		proxyListPlacePicker.register(eventBus, listPlacePickerView);

		final ActivityManager detailsActivityManager = new ActivityManager(applicationDetailsActivities, eventBus);

		detailsActivityManager.setDisplay(shell.getDetailsPanel());

		/* Browser history integration */
		ScaffoldPlaceHistoryMapper mapper = GWT.create(ScaffoldPlaceHistoryMapper.class);
		mapper.setFactory(placeHistoryFactory);
		PlaceHistoryHandler placeHistoryHandler = new PlaceHistoryHandler(mapper);
		if (getTopPlaces().iterator().hasNext()) {
			ProxyListPlace defaultPlace = getTopPlaces().iterator().next();
			placeHistoryHandler.register(placeController, eventBus, defaultPlace);
			placeHistoryHandler.handleCurrentHistory();
		}
	}
}
