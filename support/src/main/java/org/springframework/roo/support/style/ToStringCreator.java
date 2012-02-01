/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.roo.support.style;

import org.apache.commons.lang3.Validate;

/**
 * Utility class that builds pretty-printing <code>toString()</code> methods
 * with pluggable styling conventions. By default, ToStringCreator adheres to
 * Spring's <code>toString()</code> styling conventions.
 * 
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 1.2.2
 */
public class ToStringCreator {

    /**
     * Default ToStringStyler instance used by this ToStringCreator.
     */
    private static final ToStringStyler DEFAULT_TO_STRING_STYLER = new DefaultToStringStyler(
            StylerUtils.DEFAULT_VALUE_STYLER);

    private final StringBuilder buffer = new StringBuilder(512);
    private final Object object;
    private boolean styledFirstField;

    private final ToStringStyler styler;

    /**
     * Create a ToStringCreator for the given object.
     * 
     * @param obj the object to be stringified
     */
    public ToStringCreator(final Object obj) {
        this(obj, (ToStringStyler) null);
    }

    /**
     * Create a ToStringCreator for the given object, using the provided style.
     * 
     * @param obj the object to be stringified
     * @param styler the ToStringStyler encapsulating pretty-print instructions
     */
    public ToStringCreator(final Object obj, final ToStringStyler styler) {
        Validate.notNull(obj, "The object to be styled must not be null");
        object = obj;
        this.styler = styler != null ? styler : DEFAULT_TO_STRING_STYLER;
        this.styler.styleStart(buffer, object);
    }

    /**
     * Create a ToStringCreator for the given object, using the provided style.
     * 
     * @param obj the object to be stringified
     * @param styler the ValueStyler encapsulating pretty-print instructions
     */
    public ToStringCreator(final Object obj, final ValueStyler styler) {
        this(obj, new DefaultToStringStyler(styler != null ? styler
                : StylerUtils.DEFAULT_VALUE_STYLER));
    }

    /**
     * Append the provided value.
     * 
     * @param value The value to append
     * @return this, to support call-chaining.
     */
    public ToStringCreator append(final Object value) {
        styler.styleValue(buffer, value);
        return this;
    }

    /**
     * Append a boolean field value.
     * 
     * @param fieldName the name of the field, usually the member variable name
     * @param value the field value
     * @return this, to support call-chaining
     */
    public ToStringCreator append(final String fieldName, final boolean value) {
        return append(fieldName, Boolean.valueOf(value));
    }

    /**
     * Append a byte field value.
     * 
     * @param fieldName the name of the field, usually the member variable name
     * @param value the field value
     * @return this, to support call-chaining
     */
    public ToStringCreator append(final String fieldName, final byte value) {
        return append(fieldName, Byte.valueOf(value));
    }

    /**
     * Append a double field value.
     * 
     * @param fieldName the name of the field, usually the member variable name
     * @param value the field value
     * @return this, to support call-chaining
     */
    public ToStringCreator append(final String fieldName, final double value) {
        return append(fieldName, new Double(value));
    }

    /**
     * Append a float field value.
     * 
     * @param fieldName the name of the field, usually the member variable name
     * @param value the field value
     * @return this, to support call-chaining
     */
    public ToStringCreator append(final String fieldName, final float value) {
        return append(fieldName, new Float(value));
    }

    /**
     * Append a integer field value.
     * 
     * @param fieldName the name of the field, usually the member variable name
     * @param value the field value
     * @return this, to support call-chaining
     */
    public ToStringCreator append(final String fieldName, final int value) {
        return append(fieldName, Integer.valueOf(value));
    }

    /**
     * Append a long field value.
     * 
     * @param fieldName the name of the field, usually the member variable name
     * @param value the field value
     * @return this, to support call-chaining
     */
    public ToStringCreator append(final String fieldName, final long value) {
        return append(fieldName, Long.valueOf(value));
    }

    /**
     * Append a field value.
     * 
     * @param fieldName the name of the field, usually the member variable name
     * @param value the field value; can be <code>null</code>
     * @return this, to support call-chaining
     */
    public ToStringCreator append(final String fieldName, final Object value) {
        printFieldSeparatorIfNecessary();
        styler.styleField(buffer, fieldName, value);
        return this;
    }

    /**
     * Append a short field value.
     * 
     * @param fieldName the name of the field, usually the member variable name
     * @param value the field value
     * @return this, to support call-chaining
     */
    public ToStringCreator append(final String fieldName, final short value) {
        return append(fieldName, Short.valueOf(value));
    }

    private void printFieldSeparatorIfNecessary() {
        if (styledFirstField) {
            styler.styleFieldSeparator(buffer);
        }
        else {
            styledFirstField = true;
        }
    }

    /**
     * Return the String representation that this ToStringCreator built.
     */
    @Override
    public String toString() {
        styler.styleEnd(buffer, object);
        return buffer.toString();
    }
}
