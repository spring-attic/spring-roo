package __TOP_LEVEL_PACKAGE__.client.scaffold.ioc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.place.shared.PlaceController;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import __TOP_LEVEL_PACKAGE__.client.scaffold.*;
import __TOP_LEVEL_PACKAGE__.client.request.ApplicationRequestFactory;

public class ScaffoldModule extends AbstractGinModule {

	@Override
    protected void configure() {
        bind(EventBus.class).to(SimpleEventBus.class).in(Singleton.class);
        bind(ApplicationRequestFactory.class).toProvider(RequestFactoryProvider.class).in(Singleton.class);
        bind(PlaceController.class).toProvider(PlaceControllerProvider.class).in(Singleton.class);
    }

    static class PlaceControllerProvider implements Provider<PlaceController> {

        private final EventBus eventBus;

        @Inject
        public PlaceControllerProvider(EventBus eventBus) {
            this.eventBus = eventBus;
        }

        public PlaceController get() {
            return new PlaceController(eventBus);
        }
    }

    static class RequestFactoryProvider implements Provider<ApplicationRequestFactory> {

        private final EventBus eventBus;

        @Inject
        public RequestFactoryProvider(EventBus eventBus) {
            this.eventBus = eventBus;
        }

        public ApplicationRequestFactory get() {
            ApplicationRequestFactory requestFactory = GWT.create(ApplicationRequestFactory.class);
            requestFactory.initialize(eventBus);
            return requestFactory;
        }
    }
}
