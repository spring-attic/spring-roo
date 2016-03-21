package org.springframework.roo.converters;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.converters.BigDecimalConverter;

/**
 * OSGi component launcher for {@link BigDecimalConverter}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
@Component
@Service
public class BigDecimalConverterComponent extends BigDecimalConverter {
}
