package org.springframework.roo.bootstrap;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.roo.support.classloader.ClassLoaderProvider;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;

/**
 * Basic implementation of {@link ClassLoaderProvider} that uses a Spring Framework-injected
 * {@link ClassLoader}. This defers responsibility to Spring and however it was loaded.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopment
public class SpringClassLoaderProvider implements ClassLoaderProvider, BeanClassLoaderAware {

	private ClassLoader classLoader;
	
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	public ClassLoader getClassLoader() {
		return classLoader;
	}

}
