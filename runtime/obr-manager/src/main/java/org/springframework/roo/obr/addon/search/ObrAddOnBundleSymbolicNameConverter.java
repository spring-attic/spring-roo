package org.springframework.roo.obr.addon.search;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Bundle;
import org.springframework.roo.obr.addon.search.model.ObrBundle;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * {@link Converter} for {@link ObrAddOnBundleSymbolicName}.
 * 
 * @author Juan Carlos García
 * @since 2.0.0
 */
@Component
@Service
public class ObrAddOnBundleSymbolicNameConverter implements Converter<ObrAddOnBundleSymbolicName> {

  @Reference
  private ObrAddOnSearchOperations operations;

  public ObrAddOnBundleSymbolicName convertFromText(final String value,
      final Class<?> requiredType, final String optionContext) {
    return new ObrAddOnBundleSymbolicName(value.trim());
  }

  public boolean getAllPossibleValues(final List<Completion> completions,
      final Class<?> requiredType, final String originalUserInput, final String optionContext,
      final MethodTarget target) {
    final Map<String, ObrBundle> bundles = operations.getAddOnCache();
    for (final Entry<String, ObrBundle> entry : bundles.entrySet()) {
      final String bsn = entry.getKey();
      final ObrBundle bundle = entry.getValue();
      completions.add(new Completion(bsn));
    }
    return false;
  }

  public boolean supports(final Class<?> requiredType, final String optionContext) {
    return ObrAddOnBundleSymbolicName.class.isAssignableFrom(requiredType);
  }
}
