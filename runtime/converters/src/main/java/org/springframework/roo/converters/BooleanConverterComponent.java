package org.springframework.roo.converters;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.converters.BooleanConverter;

/**
 * OSGi component launcher for {@link BooleanConverter}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
@Component
@Service
public class BooleanConverterComponent extends BooleanConverter {
}
