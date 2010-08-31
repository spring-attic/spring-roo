package __TOP_LEVEL_PACKAGE__.gwt.scaffold.ioc;

import com.google.gwt.app.place.PlaceTokenizer;
import com.google.gwt.app.place.ProxyListPlace;
import com.google.gwt.app.place.ProxyPlace;
import com.google.inject.Inject;

public class PlaceHistoryFactory {
	
	private final ProxyListPlace.Tokenizer  proxyListPlaceTokenizer;
	private final ProxyPlace.Tokenizer  proxyPlaceTokenizer;
	
	@Inject
	public PlaceHistoryFactory(ProxyListPlace.Tokenizer  proxyListPlaceTokenizer, ProxyPlace.Tokenizer  proxyPlaceTokenizer) {
		this.proxyListPlaceTokenizer = proxyListPlaceTokenizer;
		this.proxyPlaceTokenizer = proxyPlaceTokenizer;
	}
	
	public PlaceTokenizer<ProxyListPlace> getProxyListPlaceTokenizer() {
		return proxyListPlaceTokenizer;
	}

	public PlaceTokenizer<ProxyPlace> getProxyPlaceTokenizer() {
		return proxyPlaceTokenizer;
	}

}
