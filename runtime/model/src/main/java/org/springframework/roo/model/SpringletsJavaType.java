package org.springframework.roo.model;

/**
 * = _SpringletsJavaType_
 *
 * Constants for Springlets-specific {@link JavaType}s. Use them in preference to
 * creating new instances of these types.
 *
 * @author Jose Manuel Vivó
 * @author Juan Carlos García
 * @author Sergio Clares
 * @since 2.0.0
 */
public class SpringletsJavaType {

  public static final JavaType SPRINGLETS_CONTROLLER_METHOD_LINK_BUILDER_FACTORY = new JavaType(
      "io.springlets.web.mvc.util.ControllerMethodLinkBuilderFactory");

  public static final JavaType SPRINGLETS_ENTITY_FORMAT = new JavaType(
      "io.springlets.format.EntityFormat");
  public static final JavaType SPRINGLETS_ENTITY_RESOLVER = new JavaType(
      "io.springlets.format.EntityResolver");

  public static final JavaType SPRINGLETS_GLOBAL_SEARCH = new JavaType(
      "io.springlets.data.domain.GlobalSearch");
  public static final JavaType SPRINGLETS_GLOBAL_SEARCH_ARGUMENT_RESOLVER = new JavaType(
      "io.springlets.data.web.GlobalSearchHandlerMethodArgumentResolver");

  public static final JavaType SPRINGLETS_DETACHABLE_JPA_REPOSITORY = new JavaType(
      "io.springlets.data.jpa.repository.DetachableJpaRepository");
  public static final JavaType SPRINGLETS_DETACHABLE_JPA_REPOSITORY_IMPL = new JavaType(
      "io.springlets.data.jpa.repository.support.DetachableJpaRepositoryImpl");
  public static final JavaType SPRINGLETS_QUERYDSL_REPOSITORY_SUPPORT_EXT = new JavaType(
      "io.springlets.data.jpa.repository.support.QueryDslRepositorySupportExt");
  public static final JavaType SPRINGLETS_QUERYDSL_REPOSITORY_SUPPORT_ATTRIBUTE_BUILDER =
      new JavaType(
          "io.springlets.data.jpa.repository.support.QueryDslRepositorySupportExt.AttributeMappingBuilder");

  public static final JavaType SPRINGLETS_USER_DETAILS_SERVICE = new JavaType(
      "io.springlets.security.web.SpringletsUserDetailsService");
  public static final JavaType SPRINGLETS_NOT_FOUND_EXCEPTION = new JavaType(
      "io.springlets.web.NotFoundException");
  public static final JavaType SPRINGLETS_DATATABLES = new JavaType(
      "io.springlets.data.web.datatables.Datatables");
  public static final JavaType SPRINGLETS_DATATABLES_COLUMNS = new JavaType(
      "io.springlets.data.web.datatables.DatatablesColumns");
  public static final JavaType SPRINGLETS_DATATABLES_PAGEABLE = new JavaType(
      "io.springlets.data.web.datatables.DatatablesPageable");
  public static final JavaType SPRINGLETS_DATATABLES_DATA = new JavaType(
      "io.springlets.data.web.datatables.DatatablesData");
  public static final JavaType SPRINGLETS_CONVERTED_DATATABLES_DATA = new JavaType(
      "io.springlets.data.web.datatables.ConvertedDatatablesData");


  public static final JavaType SPRINGLETS_METHOD_LINK_FACTORY = new JavaType(
      "io.springlets.web.mvc.util.MethodLinkFactory");
  public static final JavaType SPRINGLETS_METHOD_LINK_FACTORY_SUPPORT = new JavaType(
      "io.springlets.web.mvc.support.MethodLinkFactorySupport");
  public static final JavaType SPRINGLETS_METHOD_LINK_BUILDER_FACTORY = new JavaType(
      "io.springlets.web.mvc.util.MethodLinkBuilderFactory");

  public static final JavaType SPRINGLETS_SELECT2_DATA = new JavaType(
      "io.springlets.data.web.select2.Select2Data");
  public static final JavaType SPRINGLETS_SELECT2_DATA_SUPPORT = new JavaType(
      "io.springlets.data.web.select2.Select2DataSupport");
  public static final JavaType SPRINGLETS_SELECT2_DATA_WITH_CONVERSION = new JavaType(
      "io.springlets.data.web.select2.Select2DataWithConversion");

  public static final JavaType SPRINGLETS_MAIL_RECEIVER_SERVICE = new JavaType(
      "io.springlets.mail.MailReceiverService");

  public static final JavaType SPRINGLETS_MVC_URI_COMPONENTS_BUILDER = new JavaType(
      "io.springlets.web.mvc.util.SpringletsMvcUriComponentsBuilder");

  public static final JavaType SPRINGLETS_WEB_MVC_TEST = new JavaType(
      "io.springlets.boot.test.autoconfigure.web.servlet.SpringletsWebMvcTest");

  public static final JavaType SPRINGLETS_JMS_SENDING_SERVICE = new JavaType(
      "io.springlets.jms.JmsMessageSenderService");

  // Adapters
  public static final JavaType SPRINGLETS_GLOBAL_SEARCH_ADAPTER = new JavaType(
      "io.springlets.data.domain.jaxb.GlobalSearchAdapter");
  public static final JavaType SPRINGLETS_ITERABLE_ADAPTER = new JavaType(
      "io.springlets.data.domain.jaxb.IterableAdapter");
  public static final JavaType SPRINGLETS_PAGE_ADAPTER = new JavaType(
      "io.springlets.data.domain.jaxb.PageAdapter");
  public static final JavaType SPRINGLETS_PAGEABLE_ADAPTER = new JavaType(
      "io.springlets.data.domain.jaxb.PageableAdapter");



  /**
   * Constructor is private to prevent instantiation
   */
  private SpringletsJavaType() {}

}
