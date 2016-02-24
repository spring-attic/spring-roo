package org.springframework.roo.shell.osgi.converters;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.converters.LongConverter;

/**
 * OSGi component launcher for {@link LongConverterComponent}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
@Component
@Service
public class LongConverterComponent extends LongConverter {
}