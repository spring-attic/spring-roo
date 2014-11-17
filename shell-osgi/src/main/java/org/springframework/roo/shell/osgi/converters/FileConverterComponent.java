package org.springframework.roo.shell.osgi.converters;

import java.io.File;
import java.util.logging.Logger;
import org.apache.commons.lang3.Validate;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.shell.converters.FileConverter;
import org.osgi.service.component.ComponentContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.roo.support.logging.HandlerUtils;


/**
 * OSGi component launcher for {@link FileConverter}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
@Component
@Service
public class FileConverterComponent extends FileConverter {
    
	private Shell shell;
    
    protected final static Logger LOGGER = HandlerUtils.getLogger(FileConverterComponent.class);
    
    // ------------ OSGi component attributes ----------------
   	private BundleContext context;
    
    protected void activate(final ComponentContext context) {
    	this.context = context.getBundleContext();
    }

    @Override
    protected File getWorkingDirectory() {
    	if(shell == null){
    		shell = getShell();
    	}
    	Validate.notNull(shell, "Shell required");
        return shell.getHome();
    }
    
    public Shell getShell(){
    	// Get all Services implement Shell interface
		try {
			ServiceReference<?>[] references = this.context.getAllServiceReferences(Shell.class.getName(), null);
			
			for(ServiceReference<?> ref : references){
				return (Shell) this.context.getService(ref);
			}
			
			return null;
			
		} catch (InvalidSyntaxException e) {
			LOGGER.warning("Cannot load Shell on DefaultFileManager.");
			return null;
		}
    }

}