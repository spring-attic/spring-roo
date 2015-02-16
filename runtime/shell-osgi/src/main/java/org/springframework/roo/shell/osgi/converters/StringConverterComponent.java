package org.springframework.roo.shell.osgi.converters;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.converters.StringConverter;

/**
 * OSGi component launcher for {@link StringConverter}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
@Service
@Component
public class StringConverterComponent extends StringConverter {
}