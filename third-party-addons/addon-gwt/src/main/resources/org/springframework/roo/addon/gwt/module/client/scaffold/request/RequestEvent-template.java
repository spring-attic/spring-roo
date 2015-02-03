package __TOP_LEVEL_PACKAGE__.client.scaffold.request;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;

/**
 * An event posted whenever an RPC request is sent or its response is received.
 */
public class RequestEvent extends GwtEvent<RequestEvent.Handler> {

	/**
	 * Implemented by handlers of this type of event.
	 */
	public interface Handler extends EventHandler {
		
		/**
		 * Called when a {@link RequestEvent} is fired.
		 *
		 * @param requestEvent a {@link RequestEvent} instance
		 */
		void onRequestEvent(RequestEvent requestEvent);
	}

	/**
	 * The request state.
	 */
	public enum State {
		SENT, RECEIVED
	}

	private static final Type<Handler> TYPE = new Type<Handler>();

	/**
	 * Register a {@link RequestEvent.Handler} on an {@link EventBus}.
	 *
	 * @param eventBus the {@link EventBus}
	 * @param handler  a {@link RequestEvent.Handler}
	 * @return a {@link HandlerRegistration} instance
	 */
	public static HandlerRegistration register(EventBus eventBus, RequestEvent.Handler handler) {
		return eventBus.addHandler(TYPE, handler);
	}

	private final State state;

	/**
	 * Constructs a new @{link RequestEvent}.
	 *
	 * @param state a {@link State} instance
	 */
	public RequestEvent(State state) {
		this.state = state;
	}

	@Override
	public GwtEvent.Type<Handler> getAssociatedType() {
		return TYPE;
	}

	/**
	 * Returns the {@link State} associated with this event.
	 *
	 * @return a {@link State} instance
	 */
	public State getState() {
		return state;
	}

	@Override
	protected void dispatch(Handler handler) {
		handler.onRequestEvent(this);
	}
}
