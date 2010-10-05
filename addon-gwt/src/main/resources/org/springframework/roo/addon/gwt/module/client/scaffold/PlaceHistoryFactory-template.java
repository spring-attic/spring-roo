package __TOP_LEVEL_PACKAGE__.__SEGMENT_PACKAGE__;

import com.google.gwt.app.place.PlaceTokenizer;
import com.google.gwt.app.place.ProxyListPlace;
import com.google.gwt.app.place.ProxyPlace;
import com.google.inject.Inject;
import __TOP_LEVEL_PACKAGE__.client.request.ApplicationRequestFactory;

public class PlaceHistoryFactory {
	
	private final ProxyListPlace.Tokenizer proxyListPlaceTokenizer;
    private final ProxyPlace.Tokenizer proxyPlaceTokenizer;

    @Inject
    public PlaceHistoryFactory(ApplicationRequestFactory requestFactory) {
        this.proxyListPlaceTokenizer = new ProxyListPlace.Tokenizer(requestFactory);
        this.proxyPlaceTokenizer = new ProxyPlace.Tokenizer(requestFactory);
    }

    public PlaceTokenizer<ProxyListPlace> getProxyListPlaceTokenizer() {
        return proxyListPlaceTokenizer;
    }

    public PlaceTokenizer<ProxyPlace> getProxyPlaceTokenizer() {
        return proxyPlaceTokenizer;
    }

}
