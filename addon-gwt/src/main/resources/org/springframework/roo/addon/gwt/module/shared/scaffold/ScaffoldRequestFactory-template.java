package __TOP_LEVEL_PACKAGE__.shared.scaffold;

import com.google.gwt.requestfactory.shared.LoggingRequest;
import com.google.gwt.requestfactory.shared.RequestFactory;

/**
 * The base request factory interface for this app. Add
 * new custom request types here without fear of them
 * being managed away by Roo.
 */
public interface ScaffoldRequestFactory extends RequestFactory {
	/**
	 * Return a GWT logging request.
	 */
	LoggingRequest loggingRequest();
}
