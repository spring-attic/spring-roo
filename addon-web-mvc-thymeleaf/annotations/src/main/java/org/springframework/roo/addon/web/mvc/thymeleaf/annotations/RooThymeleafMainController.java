package org.springframework.roo.addon.web.mvc.thymeleaf.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a type that defines ROO THYMELEAF main controller.
 * <p>
 * This annotation will cause ROO to produce code that would typically appear in
 * MainController methods. In the current release this code will be
 * emitted to an ITD.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooThymeleafMainController {


}
