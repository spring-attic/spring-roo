package __TOP_LEVEL_PACKAGE__.gwt.scaffold;

import com.google.gwt.app.place.Activity;
import com.google.gwt.app.place.ActivityManager;
import com.google.gwt.app.place.ActivityMapper;
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
import com.google.gwt.requestfactory.shared.UserInformationRecord;
import com.google.gwt.requestfactory.shared.RequestEvent.State;
import __TOP_LEVEL_PACKAGE__.gwt.request.ApplicationEntityTypesProcessor;
import __TOP_LEVEL_PACKAGE__.gwt.request.ApplicationRequestFactory;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.ui.HasConstrainedValue;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.requestfactory.shared.Record;
import com.google.gwt.requestfactory.shared.SyncResult;

import java.util.HashSet;
import java.util.Set;

/**
 * Mobile application for browsing entities.
 * 
 * TODO (jgw): Make this actually mobile-friendly.
 */
public class ScaffoldMobile implements EntryPoint {

	public void onModuleLoad() {
	
	//Silly having two modules when you can detect browser 
	//and use deferred binding to use mobile specific shell.
	
	}

	// TODO (rjrjr) No reason to make the place objects in advance, just make
	// it list the class objects themselves. Needs to be sorted by rendered name, too
	private Set<ProxyListPlace> getTopPlaces() {
		Set<Class<? extends Record>> types = ApplicationEntityTypesProcessor.getAll();
		Set<ProxyListPlace> rtn = new HashSet<ProxyListPlace>(types.size());

		for (Class<? extends Record> type : types) {
			rtn.add(new ProxyListPlace(type));
		}

		return rtn;
	}
}
