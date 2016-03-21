package org.springframework.roo.converters;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.converters.DateConverter;

/**
 * OSGi component launcher for {@link DateConverter}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
@Component
@Service
public class DateConverterComponent extends DateConverter {
}
