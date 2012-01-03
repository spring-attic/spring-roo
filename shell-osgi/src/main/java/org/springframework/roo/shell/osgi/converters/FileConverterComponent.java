package org.springframework.roo.shell.osgi.converters;

import java.io.File;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.shell.converters.FileConverter;

/**
 * OSGi component launcher for {@link FileConverter}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
@Component
@Service
public class FileConverterComponent extends FileConverter {
    @Reference private Shell shell;

    @Override
    protected File getWorkingDirectory() {
        return shell.getHome();
    }

}