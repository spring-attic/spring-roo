package org.springframework.roo.classpath.details;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

import java.util.Comparator;

/**
 * Metadata concerning a particular method.
 *
 * @author Ben Alex
 * @since 1.0
 */
public interface MethodMetadata extends InvocableMemberMetadata {

  public static final Comparator<MethodMetadata> COMPARATOR_BY_NAME =
      new Comparator<MethodMetadata>() {
        @Override
        public int compare(MethodMetadata method1, MethodMetadata method2) {
          return method1.getMethodName().getSymbolName()
              .compareTo(method2.getMethodName().getSymbolName());
        }
      };

  /**
   * @return the name of the method (never null)
   */
  JavaSymbolName getMethodName();

  /**
   * @return the return type (never null, even if void)
   */
  JavaType getReturnType();

  /**
   * Indicates whether this method has the same name (case-sensitive) as any
   * of the given methods
   *
   * @param otherMethods the methods to check against; can be empty or contain
   *            <code>null</code> elements, which will be ignored
   * @return see above
   * @since 1.2.0
   */
  boolean hasSameName(final MethodMetadata... otherMethods);

  /**
   * Indicates whether this method is static
   *
   * @return see above
   * @since 1.2.0
   */
  boolean isStatic();

  /**
   * Indicates if method signature matches (name, number of parameters,
   * type of parameters) with otherMethod
   *
   * @param otherMethod
   * @return
   * @since 2.0.0
   */
  boolean matchSignature(final MethodMetadata otherMethod);
}
