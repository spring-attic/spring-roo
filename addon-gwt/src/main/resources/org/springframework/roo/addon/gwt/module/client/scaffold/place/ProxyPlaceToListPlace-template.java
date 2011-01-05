package __TOP_LEVEL_PACKAGE__.client.scaffold.place;

import com.google.gwt.activity.shared.FilteredActivityMapper;
import com.google.gwt.place.shared.Place;

/**
 * Converts a {@link #ProxyPlace} to a {@link ProxyListPlace}.
 */
public class ProxyPlaceToListPlace implements FilteredActivityMapper.Filter {

	/**
	 * Required by {@link FilteredActivityMapper.Filter}, calls
	 * {@link #proxyListPlaceFor()}.
	 */
	public Place filter(Place place) {
		return proxyListPlaceFor(place);
	}

	/**
	 * @param place a place to process
	 * @return an appropriate ProxyListPlace, or null if the given place has
	 *         nothing to do with proxies
	 */
	public ProxyListPlace proxyListPlaceFor(Place place) {
		if (place instanceof ProxyListPlace) {
			return (ProxyListPlace) place;
		}

		if (!(place instanceof ProxyPlace)) {
			return null;
		}

		ProxyPlace proxyPlace = (ProxyPlace) place;
		return new ProxyListPlace(proxyPlace.getProxyClass());
	}
}
