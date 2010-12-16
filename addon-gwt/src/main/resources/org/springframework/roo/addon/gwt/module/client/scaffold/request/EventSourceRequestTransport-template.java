package __TOP_LEVEL_PACKAGE__.client.scaffold.request;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.requestfactory.client.DefaultRequestTransport;
import com.google.gwt.requestfactory.shared.RequestTransport.TransportReceiver;

/**
 * Extends {@link DefaultRequestTransport} to post events as requests are sent
 * and received.
 */
public class EventSourceRequestTransport extends DefaultRequestTransport {
  private final EventBus eventBus;
  
  public EventSourceRequestTransport(EventBus eventBus) {
    this.eventBus = eventBus;
  }
  
  @Override
  public void send(String payload, TransportReceiver receiver) {
    try {
      super.send(payload, receiver);
    } finally {
      eventBus.fireEvent(new RequestEvent(RequestEvent.State.SENT));
    }
  }

  @Override
  protected RequestCallback createRequestCallback(TransportReceiver receiver) {
    final RequestCallback superCallback = super.createRequestCallback(receiver);
    
    return new RequestCallback () {
      @Override
      public void onError(Request request, Throwable exception) {
        try {
          superCallback.onError(request, exception);
        } finally {
          eventBus.fireEvent(new RequestEvent(RequestEvent.State.RECEIVED));
        }
      }

      @Override
      public void onResponseReceived(Request request, final Response response) {
        try {
          superCallback.onResponseReceived(request, response);
        } finally {
          eventBus.fireEvent(new RequestEvent(RequestEvent.State.RECEIVED));
        }
      }
    };
  }
}
