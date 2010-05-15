package org.springframework.roo.addon.gwt;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;

import java.io.InputStream;

/**
*
*/
class TemplateResourceLoader extends ResourceLoader {

  public static final String TEMPLATE_DIR = "org/springframework/roo/addon/gwt/templates/";

  @Override
  public void init(ExtendedProperties configuration) {
  }

  @Override
  public InputStream getResourceStream(String source) throws ResourceNotFoundException {
    if (source.indexOf("/") == -1) {
      source = TEMPLATE_DIR +source;
    }
    return getClass().getClassLoader().getResourceAsStream(source);
  }

  @Override
  public boolean isSourceModified(Resource resource) {
    return false; 
  }

  @Override
  public long getLastModified(Resource resource) {
    return System.currentTimeMillis();
  }
}
