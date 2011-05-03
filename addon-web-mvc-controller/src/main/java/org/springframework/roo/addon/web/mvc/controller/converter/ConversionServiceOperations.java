package org.springframework.roo.addon.web.mvc.controller.converter;

import org.springframework.roo.model.JavaPackage;

/**
 * Installs and configures an application-wide FormattingConversionServiceFactoryBean that can be used
 * to register application-specific Converters and Formatters. 
 * 
 * @author Rossen Stoyanchev
 * @since 1.1.1
 */
public interface ConversionServiceOperations {
	
	public static final String CONVERSION_SERVICE_SIMPLE_TYPE = "ApplicationConversionServiceFactoryBean";
	public static final String CONVERSION_SERVICE_BEAN_NAME = "applicationConversionService";
	public static final String CONVERSION_SERVICE_EXPOSING_INTERCEPTOR_NAME = "conversionServiceExposingInterceptor";

	/**
	 * Installs a sub-type of FormattingConversionServiceFactoryBean in the given package and plugs it 
	 * in through the &lt;mvc:annotation-driven&gt; element. The method can be invoked any number of 
	 * times but the conversion service will be installed only once.
	 * 
	 * @param thePackage the package where the conversion service is to be installed.
	 */
	void installConversionService(JavaPackage thePackage);

}