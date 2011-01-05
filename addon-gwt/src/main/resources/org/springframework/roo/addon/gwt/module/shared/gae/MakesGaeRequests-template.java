package __TOP_LEVEL_PACKAGE__.__SEGMENT_PACKAGE__;

/**
 * Implemented by {@link com.google.gwt.requestfactory.shared.RequestFactory}s
 * that vend AppEngine requests.
 */
public interface MakesGaeRequests {

	/**
	 * Return a request selector.
	 */
	GaeUserServiceRequest userServiceRequest();
}
