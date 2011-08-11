package __TOP_LEVEL_PACKAGE__.client.scaffold.request;

import com.google.gwt.event.shared.EventBus;
import com.google.web.bindery.requestfactory.gwt.client.DefaultRequestTransport;
import com.google.web.bindery.requestfactory.shared.RequestTransport;
import com.google.web.bindery.requestfactory.shared.ServerFailure;

/**
 * Wraps {@link RequestTransport} to post events as requests are sent
 * and received.
 */
public class EventSourceRequestTransport implements RequestTransport {
	private final EventBus eventBus;
	private final RequestTransport wrapped;

	public EventSourceRequestTransport(EventBus eventBus) {
		this(eventBus, new DefaultRequestTransport());
	}

	public EventSourceRequestTransport(EventBus eventBus, RequestTransport wrapped) {
		this.eventBus = eventBus;
		this.wrapped = wrapped;
	}

	public void send(String payload, final TransportReceiver receiver) {
		TransportReceiver myReceiver = new TransportReceiver() {

			@Override
			public void onTransportSuccess(String payload) {
				try {
					receiver.onTransportSuccess(payload);
				} finally {
					eventBus.fireEvent(new RequestEvent(RequestEvent.State.RECEIVED));
				}
			}

			@Override
			public void onTransportFailure(ServerFailure failure) {
				try {
					receiver.onTransportFailure(failure);
				} finally {
					eventBus.fireEvent(new RequestEvent(RequestEvent.State.RECEIVED));
				}
			}
		};

		try {
			wrapped.send(payload, myReceiver);
		} finally {
			eventBus.fireEvent(new RequestEvent(RequestEvent.State.SENT));
		}
	}
}
