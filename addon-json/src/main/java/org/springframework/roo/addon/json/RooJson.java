package org.springframework.roo.addon.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Trigger annotation for addon-json
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooJson {

    /**
     * Enable deep serialization of object graph
     * 
     * @return an indication if deep serialization should be enabled (defaults
     *         to false; optional)
     */
    boolean deepSerialize() default false;

    /**
     * Enable formatting of dates as specified by ISO 8601.
     * 
     * @return an indication if dates should be formatted according to ISO 8601
     *         (defaults to false; optional)
     */
    boolean iso8601Dates() default false;

    /**
     * Specify name of the "fromJsonArrayTo<TypeNamePlural>" method to generate.
     * Use a value of "" to avoid the generation of this method.
     * 
     * @return the name of the "fromJsonArrayTo<TypeNamePlural>" method to
     *         generate (defaults to "fromJsonArrayTo<TypeNamePlural>";
     *         mandatory)
     */
    String fromJsonArrayMethod() default "fromJsonArrayTo<TypeNamePlural>";

    /**
     * Specify name of the "fromJsonTo<TypeName>" method to generate. Use a
     * value of "" to avoid the generation of this method.
     * 
     * @return the name of the "fromJsonTo<TypeName>" method to generate
     *         (defaults to "fromJsonTo<TypeName>"; mandatory)
     */
    String fromJsonMethod() default "fromJsonTo<TypeName>";

    /**
     * Specify the root name of the JSON document.
     * 
     * @return the custom root name (optional)
     */
    String rootName() default "";

    /**
     * Specify name of the "toJsonArray" method to generate. Use a value of ""
     * to avoid the generation of this method.
     * 
     * @return the name of the "toJsonArray" method to generate (defaults to
     *         "toJsonArray"; mandatory)
     */
    String toJsonArrayMethod() default "toJsonArray";

    /**
     * Specify name of the "toJson" method to generate. Use a value of "" to
     * avoid the generation of this method.
     * 
     * @return the name of the "toJson" method to generate (defaults to
     *         "toJson"; mandatory)
     */
    String toJsonMethod() default "toJson";
}
