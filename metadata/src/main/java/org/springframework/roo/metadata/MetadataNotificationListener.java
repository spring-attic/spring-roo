package org.springframework.roo.metadata;

/**
 * Indicates an implementation is able to process metadata notifications.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public interface MetadataNotificationListener {

	/**
	 * Invoked to notify an implementation that a particular source metadata identification has
	 * requested to notify a particular destination metadata identification of an event.
	 * 
	 * <p>
	 * Both the source and destination metadata identifications can be either a
	 * {@link MetadataIdentificationUtils#isIdentifyingClass(String)} or
	 * {@link MetadataIdentificationUtils#isIdentifyingInstance(String)}. However, both
	 * the source and metadata identifications must return true when presented to
	 * {@link MetadataIdentificationUtils#isValid(String)}.
	 * 
	 * <p>
	 * Where possible exceptions should not be thrown when processing metadata, except for
	 * genuinely fatal operations. Simple failures to obtain metadata information
	 * can be safely ignored and indicated via {@link MetadataItem#isValid()}, with an
	 * expectation that metadata depending on such metadata will call this method.
	 * 
	 * @param upstreamDependency the upstream source of the event (mandatory and must be valid)
	 * @param downstreamDependency the downstream destination of the event (may be null if no particular downstream is targeted)
	 */
	void notify(String upstreamDependency, String downstreamDependency);
}
