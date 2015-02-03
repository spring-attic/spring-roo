package __TOP_LEVEL_PACKAGE__.__SEGMENT_PACKAGE__;

import __TOP_LEVEL_PACKAGE__.client.managed.activity.*;
import __TOP_LEVEL_PACKAGE__.client.managed.request.ApplicationRequestFactory;
import __TOP_LEVEL_PACKAGE__.client.managed.ui.renderer.ApplicationListPlaceRenderer;
import __TOP_LEVEL_PACKAGE__.client.scaffold.activity.IsScaffoldMobileActivity;
import __TOP_LEVEL_PACKAGE__.client.scaffold.place.*;
import __TOP_LEVEL_PACKAGE__.client.scaffold.gae.GaeHelper;
import __TOP_LEVEL_PACKAGE__.client.style.MobileListResources;
import com.google.gwt.activity.shared.*;
import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.logging.client.LogConfiguration;
import com.google.gwt.place.shared.*;
import com.google.web.bindery.requestfactory.shared.LoggingRequest;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.Renderer;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryLogHandler;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
__GAE_IMPORT__

/**
 * Mobile application for browsing entities.
 */
public class ScaffoldMobileApp extends ScaffoldApp {
	
	
	private static final Logger log = Logger.getLogger(Scaffold.class.getName());
	public static final Place ROOT_PLACE = new Place() {};

	/**
	 * The root activity that shows all entities.
	 */
	private static class DefaultActivity extends AbstractActivity implements IsScaffoldMobileActivity {
		private final Widget widget;

		public DefaultActivity(Widget widget) {
			this.widget = widget;
		}

		@Override
		public void start(AcceptsOneWidget panel, EventBus eventBus) {
			panel.setWidget(widget);
		}

		public Place getBackButtonPlace() {
			return null;
		}

		public String getBackButtonText() {
			return null;
		}

		public Place getEditButtonPlace() {
			return null;
		}

		public String getTitleText() {
			return "All Entities";
		}

		public boolean hasEditButton() {
			return false;
		}
	}

	private static MobileListResources res = GWT.create(MobileListResources.class);

	/**
	 * Get the list resources used for mobile.
	 */
	public static MobileListResources getMobileListResources() {
		if (res == null) {
			res = GWT.create(MobileListResources.class);
			res.cellListStyle().ensureInjected();
		}
		return res;
	}

	private IsScaffoldMobileActivity lastActivity;

	private final ScaffoldMobileShell shell;
	private final ScaffoldMobileActivities scaffoldMobileActivities;
	private final ApplicationRequestFactory requestFactory;
	private final EventBus eventBus;
	private final PlaceController placeController;
	private final PlaceHistoryFactory placeHistoryFactory;

	@Inject
	public ScaffoldMobileApp(ScaffoldMobileShell shell, ApplicationRequestFactory requestFactory, EventBus eventBus, PlaceController placeController, ScaffoldMobileActivities scaffoldMobileActivities, PlaceHistoryFactory placeHistoryFactory, GaeHelper gaeHelper) {
		this.shell = shell;
		this.requestFactory = requestFactory;
		this.eventBus = eventBus;
		this.placeController = placeController;
		this.scaffoldMobileActivities = scaffoldMobileActivities;
		this.placeHistoryFactory = placeHistoryFactory;
	}

	@Override
	public void run() {
		isMobile = true;

		/* Add handlers, setup activities */
		init();

		/* Hide the loading message */
		Element loading = Document.get().getElementById("loading");
		loading.getParentElement().removeChild(loading);

		/* And show the user the shell */
		// TODO (jlabanca): Use RootLayoutPanel when we switch to DockLayoutPanel.
		RootPanel.get().add(shell);
	}

	private void init() {
		GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
			public void onUncaughtException(Throwable e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
		});

		if (LogConfiguration.loggingIsEnabled()) {
			/* Add remote logging handler */
			RequestFactoryLogHandler.LoggingRequestProvider provider = new RequestFactoryLogHandler.LoggingRequestProvider() {
				public LoggingRequest getLoggingRequest() {
					return requestFactory.loggingRequest();
				}
			};
			Logger.getLogger("").addHandler(new RequestFactoryLogHandler(provider, Level.WARNING, new ArrayList<String>()));
		}

		/* Left side lets us pick from all the types of entities */

		final Renderer<ProxyListPlace> placePickerRenderer = new ApplicationListPlaceRenderer();
		Cell<ProxyListPlace> placePickerCell = new AbstractCell<ProxyListPlace>() {
			@Override
			public void render(Context context, ProxyListPlace value, SafeHtmlBuilder sb) {
				sb.appendEscaped(placePickerRenderer.render(value));
			}
		};
		CellList<ProxyListPlace> placePickerList = new CellList<ProxyListPlace>(placePickerCell, getMobileListResources());
		placePickerList.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
		final ValuePicker<ProxyListPlace> placePickerView = new ValuePicker<ProxyListPlace>(placePickerList);
		Activity defaultActivity = new DefaultActivity(placePickerView);
		ProxyPlaceToListPlace proxyPlaceToListPlace = new ProxyPlaceToListPlace();
		ProxyListPlacePicker proxyListPlacePicker = new ProxyListPlacePicker(placeController, proxyPlaceToListPlace);
		placePickerView.setAcceptableValues(getTopPlaces());
		proxyListPlacePicker.register(eventBus, placePickerView);

		/*
		 * Wrap the scaffoldMobileActivities so we can intercept activity requests
		 * and remember the last activity (for back button support).
		 */

		scaffoldMobileActivities.setRootActivity(defaultActivity);
		ActivityMapper activityMapper = new ActivityMapper() {
			public Activity getActivity(Place place) {
				// Defer to scaffoldMobileActivities.
				Activity nextActivity = scaffoldMobileActivities.getActivity(place);

				// Clear the value of the placePicker so we can select a new top level
				// value.
				placePickerView.setValue(null, false);

				// Update the title, back and edit buttons.
				Button backButton = shell.getBackButton();
				if (nextActivity instanceof IsScaffoldMobileActivity) {
					lastActivity = (IsScaffoldMobileActivity) nextActivity;

					// Update the title.
					shell.setTitleText(lastActivity.getTitleText());

					// Update the back button.
					String backButtonText = lastActivity.getBackButtonText();
					if (backButtonText == null || backButtonText.length() == 0) {
						shell.setBackButtonVisible(false);
					} else {
						shell.setBackButtonVisible(true);
						backButton.setText(backButtonText);
					}

					// Update the edit button.
					shell.setEditButtonVisible(lastActivity.hasEditButton());
				} else {
					lastActivity = null;
					shell.setTitleText("");
					shell.setBackButtonVisible(false);
					shell.setEditButtonVisible(false);
				}

				// Return the activity.
				return nextActivity;
			}
		};

		/*
		 * The body is run by an ActivityManager that listens for PlaceChange events
		 * and finds the corresponding Activity to run
		 */

		final ActivityManager activityManager = new ActivityManager(activityMapper, eventBus);

		activityManager.setDisplay(shell.getBody());

		/* Browser history integration */
		ScaffoldPlaceHistoryMapper mapper = GWT.create(ScaffoldPlaceHistoryMapper.class);
		mapper.setFactory(placeHistoryFactory);
		PlaceHistoryHandler placeHistoryHandler = new PlaceHistoryHandler(mapper);
		placeHistoryHandler.register(placeController, eventBus, ROOT_PLACE);
		placeHistoryHandler.handleCurrentHistory();

		shell.getBackButton().addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				if (lastActivity != null) {
					Place backPlace = lastActivity.getBackButtonPlace();
					if (backPlace != null) {
						placeController.goTo(backPlace);
					}
				}
			}
		});
		shell.getEditButton().addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				if (lastActivity != null) {
					Place editPlace = lastActivity.getEditButtonPlace();
					if (editPlace != null) {
						placeController.goTo(editPlace);
					}
				}
			}
		});
	}
}
