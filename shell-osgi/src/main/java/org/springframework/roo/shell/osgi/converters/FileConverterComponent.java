package org.springframework.roo.shell.osgi.converters;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.converters.FileConverter;

/**
 * OSGi component launcher for {@link FileConverter}.
 *
 * @author Ben Alex
 * @since 1.1
 */
@Component
@Service
public class FileConverterComponent extends FileConverter {}