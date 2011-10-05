package org.springframework.roo.addon.solr;

import org.springframework.roo.model.JavaType;

/**
 * Provides Solr Search configuration operations.
 *
 * @author Stefan Schmidt
 * @since 1.1
 */
public interface SolrOperations {

	boolean isInstallSearchAvailable();

	boolean isSearchAvailable();

	void setupConfig(String solrServerUrl);

	void addSearch(JavaType javaType);

	void addAll();
}