package org.springframework.roo.shell.osgi.converters;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.converters.LocaleConverter;

/**
 * OSGi component launcher for {@link LocaleConverter}.
 *
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component
@Service
public class LocaleConverterComponent extends LocaleConverter {}
