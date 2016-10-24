package org.springframework.roo.addon.plural.addon;

import java.util.Locale;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * API that defines all operations to obtain the plural of a provided
 * element.
 * 
 * @author Juan Carlos García 
 * @since 2.0
 *
 */
public interface PluralService {

  /**
   * This operation will return the plural of a provided JavaType.
   * 
   * This operation takes in count the @RooPlural annotation. If the provided
   * JavaType doesn't have the @RooPlural annotation or if it has not any value, 
   * the plural will be calculated by the JavaType name and the ENGLISH locale.
   * 
   * @param type to calculate the plural
   * @return String that contains the plural
   */
  String getPlural(JavaType type);

  /**
   * This operation will return the plural of a provided JavaType.
   * 
   * This operation takes in count the @RooPlural annotation. If the provided
   * JavaType doesn't have the @RooPlural annotation or if it has not any value, 
   * the plural will be calculated by the JavaType name and the provided Locale.
   * 
   * @param type to calculate the plural
   * @param locale to use during plural calculation if JavaType doesn't have @RooPlural annotation
   * @return String that contains the plural
   */
  String getPlural(JavaType type, Locale locale);

  /**
   * This operation will return the plural of a provided ClassOrInterfaceTypeDetails.
   * 
   * This operation takes in count the @RooPlural annotation. If the provided
   * ClassOrInterfaceTypeDetails doesn't have the @RooPlural annotation or if it has 
   * not any value, the plural will be calculated by the ClassOrInterfaceTypeDetails type 
   * name and the ENGLISH locale.
   * 
   * @param cid to calculate the plural
   * @return String that contains the plural
   */
  String getPlural(ClassOrInterfaceTypeDetails cid);

  /**
   * This operation will return the plural of a provided ClassOrInterfaceTypeDetails.
   * 
   * This operation takes in count the @RooPlural annotation. If the provided
   * ClassOrInterfaceTypeDetails doesn't have the @RooPlural annotation or if it has 
   * not any value, the plural will be calculated by the ClassOrInterfaceTypeDetails type 
   * name and the provided Locale.
   * 
   * @param cid to calculate the plural
   * @param locale to use during plural calculation if ClassOrInterfaceTypeDetails
   * 		doesn't have @RooPlural annotation
   * @return String that contains the plural
   */
  String getPlural(ClassOrInterfaceTypeDetails cid, Locale locale);

  /**
   * This operation will return the plural of the provided JavaSymbolName and the 
   * ENGLISH locale.
   * 
   * @param term to pluralize
   * @return String pluralized
   */
  String getPlural(JavaSymbolName term);

  /**
   * This operation will return the plural of the provided JavaSymbolName taking in
   * count the provided Locale.
   * 
   * @param term to pluralize
   * @param locale to use during plural calculation
   * @return String pluralized
   */
  String getPlural(JavaSymbolName term, Locale locale);

  /**
   * This operation will return the plural of the provided String and the ENGLISH locale.
   * 
   * @param term to pluralize
   * @return String pluralized
   */
  String getPlural(String term);

  /**
   * This operation will return the plural of the provided String taking in count
   * the provided Locale.
   * 
   * @param term to pluralize
   * @param locale to use during plural calculation
   * @return String pluralized
   */
  String getPlural(String term, Locale locale);
}
