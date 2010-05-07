package org.springframework.roo.addon.solr;

import org.springframework.roo.model.JavaType;

/**
 * Provides Solr Search configuration operations.
 *
 * @author Stefan Schmidt
 * @since 1.1
 */
public interface SolrOperations {
	
	public boolean isInstallSearchAvailable();
	
	public void setupConfig(String solrServerUrl);
	
	public void addSearch(JavaType javaType);
	
	public void addAll();
}