package org.springframework.roo.classpath.converters;

import static org.springframework.roo.classpath.converters.JavaPackageConverter.TOP_LEVEL_PACKAGE_SYMBOL;
import static org.springframework.roo.project.LogicalPath.MODULE_PATH_SEPARATOR;

import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.CommandListener;
import org.springframework.roo.shell.ParseResult;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.AnsiEscapeCode;

/**
 * Records the last Java package and type used.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component
@Service
public class LastUsedImpl implements LastUsed, CommandListener {
	
	 private static final Logger LOGGER = HandlerUtils
	            .getLogger(LastUsedImpl.class);
	    
    // ------------ OSGi component attributes ----------------
   	private BundleContext context;
   	
   	protected void deactivate(final ComponentContext cContext) {
   		if (shell != null) {
   			shell.removeListener(this);
   		}
    }

	// Verified fields
    private JavaPackage topLevelPackage;
    private JavaPackage javaPackage;
    private JavaType javaType;
    private Pom module;
    
    // Not Verified fields
    private JavaPackage topLevelPackageNotVerified;
    private JavaPackage javaPackageNotVerified;
    private JavaType javaTypeNotVerified;
    private Pom moduleNotVerified;
    
    private boolean isVerified;
    
    @Reference private ProjectOperations projectOperations;
    @Reference private Shell shell;
    @Reference private TypeLocationService typeLocationService;

	private boolean listenerRegistered;

    public JavaPackage getJavaPackage() {
        return javaPackage;
    }

    public JavaType getJavaType() {
        return javaType;
    }

    public JavaPackage getTopLevelPackage() {
        return topLevelPackage;
    }
    
    public JavaPackage getJavaPackageNotVerified() {
        return javaPackageNotVerified;
    }

    public JavaType getJavaTypeNotVerified() {
        return javaTypeNotVerified;
    }

    public JavaPackage getTopLevelPackageNotVerified() {
        return topLevelPackageNotVerified;
    }
    
    public boolean isVerified(){
    	return isVerified;
    }

    public void setPackage(final JavaPackage javaPackage) {
        Validate.notNull(javaPackage, "JavaPackage required");
        if (javaPackage.getFullyQualifiedPackageName().startsWith("java.")) {
            return;
        }
        javaType = null;
        this.javaPackage = javaPackage;
        setPromptPath(javaPackage.getFullyQualifiedPackageName());
    }

    private void setPromptPath(final String fullyQualifiedName) {
        if (topLevelPackage == null) {
            return;
        }

        String moduleName = "";
        if (module != null && StringUtils.isNotBlank(module.getModuleName())) {
            moduleName = AnsiEscapeCode.decorate(module.getModuleName()
                    + MODULE_PATH_SEPARATOR, AnsiEscapeCode.FG_CYAN);
        }

        topLevelPackage = new JavaPackage(
                typeLocationService
                        .getTopLevelPackageForModule(projectOperations
                                .getFocusedModule()));
        final String path = moduleName
                + fullyQualifiedName.replace(
                        topLevelPackage.getFullyQualifiedPackageName(),
                        TOP_LEVEL_PACKAGE_SYMBOL);
        shell.setPromptPath(path, StringUtils.isNotBlank(moduleName));
    }

    public void setTopLevelPackage(final JavaPackage topLevelPackage) {
        this.topLevelPackage = topLevelPackage;
    }

    public void setType(final JavaType javaType) {
        Validate.notNull(javaType, "JavaType required");
        if (javaType.getPackage().getFullyQualifiedPackageName()
                .startsWith("java.")) {
            return;
        }
        this.javaType = javaType;
        javaPackage = javaType.getPackage();
        setPromptPath(javaType.getFullyQualifiedTypeName());
    }
    
	public void setTypeNotVerified(JavaType javaType) {
		Validate.notNull(javaType, "JavaType required");
        if (javaType.getPackage().getFullyQualifiedPackageName()
                .startsWith("java.")) {
            return;
        }
        registerListener();
        this.javaTypeNotVerified = javaType;
        javaPackageNotVerified = javaType.getPackage();
        this.isVerified = false;
	}

    private void registerListener() {
		if (listenerRegistered) {
			return;
		}
		shell.addListerner(this);
		listenerRegistered = true;
	}

	public void setType(final JavaType javaType, final Pom module) {
        Validate.notNull(javaType, "JavaType required");
        if (javaType.getPackage().getFullyQualifiedPackageName()
                .startsWith("java.")) {
            return;
        }
        this.module = module;
        this.javaType = javaType;
        javaPackage = javaType.getPackage();
        setPromptPath(javaType.getFullyQualifiedTypeName());
    }
    
	public void setTypeNotVerified(JavaType javaType, Pom module) {
        Validate.notNull(javaType, "JavaType required");
        if (javaType.getPackage().getFullyQualifiedPackageName()
                .startsWith("java.")) {
            return;
        }
        registerListener();
        this.moduleNotVerified = module;
        this.javaTypeNotVerified = javaType;
        javaPackageNotVerified = javaType.getPackage();
        this.isVerified = false;
	}

    
    /**
     * CommandListener methods
     */
	@Override
	public void onCommandSuccess() {
		// If is not verified but finish success, set last used
		if (!isVerified){
			setType(javaTypeNotVerified, moduleNotVerified);
		}
	}

	@Override
	public void onCommandFails() {
		this.moduleNotVerified = null;
        this.javaTypeNotVerified = null;
        javaPackageNotVerified = null;
        this.isVerified = false;
	}

	@Override
	public void onCommandBegin(ParseResult parseResult) {
		// TODO Auto-generated method stub
		
	}

}
