/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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
