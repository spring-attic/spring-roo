package org.springframework.roo.addon.web.mvc.controller.addon.scaffold;

import org.springframework.roo.addon.web.mvc.controller.addon.scaffold.WebScaffoldAnnotationValues;
import org.springframework.roo.addon.web.mvc.controller.annotations.scaffold.RooWebScaffold;
import org.springframework.roo.classpath.details.annotations.populator.AnnotationValuesTestCase;

/**
 * Unit test of {@link WebScaffoldAnnotationValues}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class WebScaffoldAnnotationValuesTest extends
    AnnotationValuesTestCase<RooWebScaffold, WebScaffoldAnnotationValues> {

  @Override
  protected Class<RooWebScaffold> getAnnotationClass() {
    return RooWebScaffold.class;
  }

  @Override
  protected Class<WebScaffoldAnnotationValues> getValuesClass() {
    return WebScaffoldAnnotationValues.class;
  }
}
