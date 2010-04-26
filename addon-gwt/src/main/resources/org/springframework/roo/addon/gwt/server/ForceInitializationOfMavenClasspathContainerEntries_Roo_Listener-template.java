package __TOP_LEVEL_PACKAGE__.server;

import java.io.File;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

// WARNING: Do not rename; this type is excluded in pom.xml via the *_Roo_* filter for openjpa-maven-plugin
public class ForceInitializationOfMavenClasspathContainerEntries_Roo_Listener implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent sce) {}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		String[] classpath = System.getProperty("java.class.path").split("\\" + File.pathSeparator);
		outer:
		for (String cpEntry : classpath) {
			if (cpEntry.endsWith(".jar") && !cpEntry.contains("gwt-")) {
				File jar = new File(cpEntry);
				if (!jar.exists()) {
					continue;
				}
				JarFile jarFile;
				try {
					jarFile = new JarFile(jar);
					Enumeration<JarEntry> entries = jarFile.entries();
					while (entries.hasMoreElements()) {
						JarEntry jarEntry = entries.nextElement();
						String entryName = jarEntry.getName();
						if (entryName.endsWith(".class")) {
							// Finally, something we can request and force initialization
							String className = entryName.substring(0, entryName.length()-6).replace('/', '.');
							try {
								Class.forName(className, false, Thread.currentThread().getContextClassLoader());
								continue outer;
							} catch (ClassNotFoundException cnf) {
							}
						}
					}
				} catch (Exception ioe) {
					throw new IllegalStateException(ioe);
				}
			}
		}
	}
}
