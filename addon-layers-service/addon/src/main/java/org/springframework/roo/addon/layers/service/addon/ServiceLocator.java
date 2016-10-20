package org.springframework.roo.addon.layers.service.addon;

import org.springframework.roo.addon.layers.service.annotations.RooService;
import org.springframework.roo.addon.layers.service.annotations.RooServiceImpl;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.model.JavaType;

import java.util.Collection;

/**
 * Locates {@link RooService} within the user's project
 *
 * @author Jose Manuel Vivó
 * @since 2.0.0
 */
public interface ServiceLocator {

  /**
   * Returns the services that support the given domain type
   *
   * @param domainType the domain type for which to find the Services; can
   *            be <code>null</code>
   * @return a non-<code>null</code> collection
   */
  Collection<ClassOrInterfaceTypeDetails> getServices(final JavaType domainType);

  /**
   * Returns the service that support the given domain type
   *
   * @param domainType the domain type for which to find the service; not
   *            <code>null</code>
   * @return a service details or null if not found
   * @throws NullPointerException if domainType is null
   * @throws IllegalStateException if more than one repository found
   */
  ClassOrInterfaceTypeDetails getService(final JavaType domainType);

  /**
   * Returns first service that support the given domain type
   *
   * @param domainType the domain type for which to find the service; not
   *            <code>null</code>
   * @return a service details (first found) or null if not found
   * @throws NullPointerException if domainType is null
   */
  ClassOrInterfaceTypeDetails getFirstService(final JavaType domainType);

  /**
   * Returns the all {@link RooServiceImpl} that implements the service type
   *
   * @param domainType the domain type for which to find the Services; can
   *            be <code>null</code>
   * @return a non-<code>null</code> collection
   */
  Collection<ClassOrInterfaceTypeDetails> getServiceImpls(final JavaType serviceType);


  /**
   * Returns the {@link RooServiceImpl} that support the given domain type
   *
   * @param serviceType the service type for which to find the service implementation; not
   *            <code>null</code>
   * @return a repository or null if not found
   * @throws NullPointerException if domainType is null
   * @throws IllegalStateException if more than one repository found
   */
  ClassOrInterfaceTypeDetails getServiceImpl(final JavaType domainType);

  /**
   * Returns first {@link RooServiceImpl} that support the given domain type
   *
   * @param domainType the domain type for which to find the service; not
   *            <code>null</code>
   * @return a repository (first found) or null if not found
   * @throws NullPointerException if domainType is null
   */
  ClassOrInterfaceTypeDetails getFirstServiceImpl(final JavaType domainType);

}
