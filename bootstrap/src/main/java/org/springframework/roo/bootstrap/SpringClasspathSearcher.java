package org.springframework.roo.bootstrap;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.roo.support.classloader.ClasspathSearcher;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;

/**
 * Implementation of {@link ClasspathSearcher} that delegates to Spring Framework.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopment
public class SpringClasspathSearcher implements ApplicationContextAware, ClasspathSearcher {

	private ResourcePatternResolver patternResolver;
	
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Assert.notNull(applicationContext, "Application context required");
		this.patternResolver = applicationContext;
	}

	public List<URL> findMatchingClasspathResources(String antPath) {
		try {
			List<URL> result = new ArrayList<URL>();
			for (Resource r : this.patternResolver.getResources("classpath*:" + antPath)) {
				result.add(r.getURL());
			}
			return result;
			
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
	}

}
