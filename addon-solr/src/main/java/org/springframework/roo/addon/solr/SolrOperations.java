package org.springframework.roo.addon.solr;

import org.springframework.roo.model.JavaType;

/**
 * Provides Solr Search configuration operations.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public interface SolrOperations {

    void addAll();

    void addSearch(JavaType javaType);

    boolean isSearchAvailable();

    boolean isSolrInstallationPossible();

    void setupConfig(String solrServerUrl);
}