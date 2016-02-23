package org.springframework.roo.addon.cloud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.cloud.providers.CloudProvider;
import org.springframework.roo.addon.cloud.providers.CloudProviderId;
import org.springframework.roo.project.ProjectOperations;

/**
 * Provides operations implementation to install Cloud Provider that provides
 * functions to deploy Spring Roo Application on Cloud Servers.
 * 
 * @author Juan Carlos García del Canto
 * @since 1.2.6
 */
@Component
@Service
@Reference(name = "provider", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC,
    referenceInterface = CloudProvider.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
public class CloudOperationsImpl implements CloudOperations {

  private List<CloudProvider> providers = new ArrayList<CloudProvider>();
  private List<CloudProviderId> providersId = null;

  @Reference
  private ProjectOperations projectOperations;

  @Override
  public boolean isSetupCommandAvailable() {
    return projectOperations.isProjectAvailable(projectOperations.getFocusedModuleName());
  }

  @Override
  public void installProvider(CloudProviderId prov, String configuration) {
    CloudProvider provider = null;
    for (CloudProvider tmpProvider : providers) {
      if (prov.is(tmpProvider)) {
        provider = tmpProvider;
        break;
      }
    }
    if (provider == null) {
      throw new RuntimeException("Provider '".concat(prov.getId()).concat("' not found'"));
    }
    provider.setup(configuration);

  }

  /**
   * This method gets providerId using name
   */
  @Override
  public CloudProviderId getProviderIdByName(String name) {
    CloudProviderId provider = null;
    for (CloudProvider tmpProvider : providers) {
      if (tmpProvider.getName().equals(name)) {
        provider = new CloudProviderId(tmpProvider);
      }
    }
    return provider;
  }

  /**
   * This method load new providers
   * 
   * @param provider
   */
  protected void bindProvider(final CloudProvider provider) {
    providers.add(provider);
  }

  /**
   * This method remove providers
   * 
   * @param provider
   */
  protected void unbindProvider(final CloudProvider provider) {
    providers.remove(provider);
  }

  /**
   * This method gets a List of available providers
   */
  @Override
  public List<CloudProviderId> getProvidersId() {
    if (providersId == null) {
      providersId = new ArrayList<CloudProviderId>();
      for (CloudProvider tmpProvider : providers) {
        providersId.add(new CloudProviderId(tmpProvider));
      }
      providersId = Collections.unmodifiableList(providersId);
    }
    return providersId;
  }
}
