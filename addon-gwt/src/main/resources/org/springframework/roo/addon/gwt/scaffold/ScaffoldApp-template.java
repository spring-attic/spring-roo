package __TOP_LEVEL_PACKAGE__.gwt.scaffold;

import com.google.gwt.app.place.PlaceController;
import com.google.gwt.app.place.ProxyListPlace;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.inject.Inject;
import __TOP_LEVEL_PACKAGE__.gwt.request.ApplicationEntityTypesProcessor;
import __TOP_LEVEL_PACKAGE__.gwt.request.ApplicationRequestFactory;

import java.util.HashSet;
import java.util.Set;


public class ScaffoldApp {

    protected ApplicationRequestFactory requestFactory;
    protected EventBus eventBus;
    protected PlaceController placeController;
    protected ScaffoldMobileActivities scaffoldMobileActivities;
    protected PlaceHistoryFactory placeHistoryFactory;
    protected ApplicationMasterActivities applicationMasterActivities;
    protected ApplicationDetailsActivities applicationDetailsActivities;

    public void run() {}

    protected HashSet<ProxyListPlace> getTopPlaces() {
        Set<Class<? extends EntityProxy>> types = ApplicationEntityTypesProcessor.getAll();
        HashSet<ProxyListPlace> rtn = new HashSet<ProxyListPlace>(types.size());

        for (Class<? extends EntityProxy> type : types) {
            rtn.add(new ProxyListPlace(type));
        }

        return rtn;
    }

    @Inject
    public void setRequestFactory(ApplicationRequestFactory requestFactory) {
        this.requestFactory = requestFactory;
    }

    @Inject
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Inject
    public void setPlaceController(PlaceController placeController) {
        this.placeController = placeController;
    }

    @Inject
    public void setScaffoldMobileActivities(ScaffoldMobileActivities scaffoldMobileActivities) {
        this.scaffoldMobileActivities = scaffoldMobileActivities;
    }

    @Inject
    public void setPlaceHistoryFactory(PlaceHistoryFactory placeHistoryFactory) {
        this.placeHistoryFactory = placeHistoryFactory;
    }

    @Inject
    public void setApplicationMasterActivities(ApplicationMasterActivities applicationMasterActivities) {
        this.applicationMasterActivities = applicationMasterActivities;
    }

    @Inject
    public void setApplicationDetailsActivities(ApplicationDetailsActivities applicationDetailsActivities) {
        this.applicationDetailsActivities = applicationDetailsActivities;
    }
}