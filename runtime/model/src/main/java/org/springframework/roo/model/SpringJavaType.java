package org.springframework.roo.model;

import java.util.Arrays;

import org.apache.commons.lang3.Validate;

/**
 * Constants for Spring-specific {@link JavaType}s. Use them in preference to
 * creating new instances of these types.
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public final class SpringJavaType {

  // org.springframework
  public static final JavaType ACTIVE_PROFILES = new JavaType(
      "org.springframework.test.context.ActiveProfiles");
  public static final JavaType ANNOTATION_CONFIGURATION = new JavaType(
      "org.springframework.context.annotation.Configuration");
  public static final JavaType ANNOTATION_IMPORT = new JavaType(
      "org.springframework.context.annotation.Import");
  public static final JavaType ANNOTATION_CONDITIONALONWEBAPPLICATION = new JavaType(
      "org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication");
  public static final JavaType ANNOTATION_UTILS = new JavaType(
      "org.springframework.core.annotation.AnnotationUtils");
  public static final JavaType ASSERT = new JavaType("org.springframework.util.Assert");
  public static final JavaType ASYNC = new JavaType(
      "org.springframework.scheduling.annotation.Async");
  public static final JavaType AUDITING_ENTITY_LISTENER = new JavaType(
      "org.springframework.data.jpa.domain.support.AuditingEntityListener");
  public static final JavaType AUTHENTICATION = new JavaType(
      "org.springframework.security.core.Authentication");
  public static final JavaType AUTHENTICATION_EVENT_PUBLISHER = new JavaType(
      "org.springframework.security.authentication.AuthenticationEventPublisher");
  public static final JavaType AUTHENTICATION_MANAGER_BUILDER =
      new JavaType(
          "org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder");
  public static final JavaType AUTOWIRED = new JavaType(
      "org.springframework.beans.factory.annotation.Autowired");
  public static final JavaType BCRYPT_PASSWORD_ENCODER = new JavaType(
      "org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder");
  public static final JavaType BINDING_RESULT = new JavaType(
      "org.springframework.validation.BindingResult");
  public static final JavaType BEAN = new JavaType("org.springframework.context.annotation.Bean");
  public static final JavaType LAZY = new JavaType("org.springframework.context.annotation.Lazy");
  public static final JavaType CHARACTER_ENCODING_FILTER = new JavaType(
      "org.springframework.web.filter.CharacterEncodingFilter");
  public static final JavaType COMPONENT = new JavaType("org.springframework.stereotype.Component");
  public static final JavaType CONFIGURABLE = new JavaType(
      "org.springframework.beans.factory.annotation.Configurable");
  public static final JavaType CONFIGURATION = new JavaType(
      "org.springframework.context.annotation.Configuration");
  public static final JavaType CONTEXT_CONFIGURATION = new JavaType(
      "org.springframework.test.context.ContextConfiguration");
  public static final JavaType CONTEXT_LOADER_LISTENER = new JavaType(
      "org.springframework.web.context.ContextLoaderListener");
  public static final JavaType CONTROLLER = new JavaType(
      "org.springframework.stereotype.Controller");
  public static final JavaType CONTROLLER_ADVICE = new JavaType(
      "org.springframework.web.bind.annotation.ControllerAdvice");
  public static final JavaType CONVERSION_SERVICE = new JavaType(
      "org.springframework.core.convert.ConversionService");
  public static final JavaType CONVERSION_SERVICE_EXPOSING_INTERCEPTOR = new JavaType(
      "org.springframework.web.servlet.handler.ConversionServiceExposingInterceptor");
  public static final JavaType CREATED_BY = new JavaType(
      "org.springframework.data.annotation.CreatedBy");
  public static final JavaType CREATED_DATE = new JavaType(
      "org.springframework.data.annotation.CreatedDate");
  public static final JavaType DATA_ID = new JavaType("org.springframework.data.annotation.Id");
  public static final JavaType DATA_JPA_TEST = new JavaType(
      "org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest");
  public static final JavaType DATE_TIME_FORMAT = new JavaType(
      "org.springframework.format.annotation.DateTimeFormat");
  public static final JavaType DELETE_MAPPING = new JavaType(
      "org.springframework.web.bind.annotation.DeleteMapping");
  public static final JavaType DISPATCHER_SERVLET = new JavaType(
      "org.springframework.web.servlet.DispatcherServlet");
  public static final JavaType ENABLE_CACHING = new JavaType(
      "org.springframework.cache.annotation.EnableCaching");
  public static final JavaType ENABLE_JMS = new JavaType(
      "org.springframework.jms.annotation.EnableJms");
  public static final JavaType ENABLE_JPA_REPOSITORIES = new JavaType(
      "org.springframework.data.jpa.repository.config.EnableJpaRepositories");
  public static final JavaType ENTITY_SCAN = new JavaType(
      "org.springframework.boot.autoconfigure.domain.EntityScan");
  public static final JavaType EXCEPTION_HANDLER = new JavaType(
      "org.springframework.web.bind.annotation.ExceptionHandler");
  public static final JavaType FLOW_HANDLER_MAPPING = new JavaType(
      "org.springframework.webflow.mvc.servlet.FlowHandlerMapping");
  public static final JavaType FORMATTER = new JavaType("org.springframework.format.Formatter");
  public static final JavaType FORMATTER_REGISTRY = new JavaType(
      "org.springframework.format.FormatterRegistry");
  public static final JavaType FORMATTING_CONVERSION_SERVICE = new JavaType(
      "org.springframework.format.support.FormattingConversionService");
  public static final JavaType GET_MAPPING = new JavaType(
      "org.springframework.web.bind.annotation.GetMapping");
  public static final JavaType HIDDEN_HTTP_METHOD_FILTER = new JavaType(
      "org.springframework.web.filter.HiddenHttpMethodFilter");
  public static final JavaType HTTP_HEADERS = new JavaType("org.springframework.http.HttpHeaders");
  public static final JavaType HTTP_METHOD = new JavaType("org.springframework.http.HttpMethod");
  public static final JavaType HTTP_STATUS = new JavaType("org.springframework.http.HttpStatus");
  public static final JavaType INIT_BINDER = new JavaType(
      "org.springframework.web.bind.annotation.InitBinder");
  public static final JavaType JAVA_MAIL_SENDER = new JavaType(
      "org.springframework.mail.javamail.JavaMailSender");
  public static final JavaType JAVA_MAIL_SENDER_IMPL = new JavaType(
      "org.springframework.mail.javamail.JavaMailSenderImpl");
  public static final JavaType JMS_LISTENER = new JavaType(
      "org.springframework.jms.annotation.JmsListener");
  public static final JavaType JMS_OPERATIONS = new JavaType(
      "org.springframework.jms.core.JmsOperations");
  public static final JavaType JMS_TEMPLATE = new JavaType(
      "org.springframework.jms.core.JmsTemplate");
  public static final JavaType JPA_TRANSACTION_MANAGER = new JavaType(
      "org.springframework.orm.jpa.JpaTransactionManager");
  public static final JavaType LAST_MODIFIED_BY = new JavaType(
      "org.springframework.data.annotation.LastModifiedBy");
  public static final JavaType LAST_MODIFIED_DATE = new JavaType(
      "org.springframework.data.annotation.LastModifiedDate");
  public static final JavaType LOCAL_CONTAINER_ENTITY_MANAGER_FACTORY_BEAN = new JavaType(
      "org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean");
  public static final JavaType LOCAL_ENTITY_MANAGER_FACTORY_BEAN = new JavaType(
      "org.springframework.orm.jpa.LocalEntityManagerFactoryBean");
  public static final JavaType LOCALE_CONTEXT_HOLDER = new JavaType(
      "org.springframework.context.i18n.LocaleContextHolder");
  public static final JavaType LOCALE_RESOLVER = new JavaType(
      "org.springframework.web.servlet.LocaleResolver");
  public static final JavaType LOCAL_VALIDATOR_FACTORY_BEAN = new JavaType(
      "org.springframework.validation.beanvalidation.LocalValidatorFactoryBean");
  public static final JavaType MAIL_SENDER = new JavaType("org.springframework.mail.MailSender");
  public static final JavaType MEDIA_TYPE = new JavaType("org.springframework.http.MediaType");
  public static final JavaType MESSAGE_SOURCE = new JavaType(
      "org.springframework.context.MessageSource");
  public static final JavaType MOCK_BEAN = new JavaType(
      "org.springframework.boot.test.mock.mockito.MockBean");
  public static final JavaType MOCK_MVC = new JavaType(
      "org.springframework.test.web.servlet.MockMvc");
  public static final JavaType MOCK_STATIC_ENTITY_METHODS = new JavaType(
      "org.springframework.mock.staticmock.MockStaticEntityMethods");
  public static final JavaType MODEL = new JavaType("org.springframework.ui.Model");
  public static final JavaType MODEL_ATTRIBUTE = new JavaType(
      "org.springframework.web.bind.annotation.ModelAttribute");
  public static final JavaType MODEL_AND_VIEW = new JavaType(
      "org.springframework.web.servlet.ModelAndView");
  public static final JavaType MODEL_MAP = new JavaType("org.springframework.ui.ModelMap");
  public static final JavaType MVC_URI_COMPONENTS_BUILDER = new JavaType(
      "org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder");
  public static final JavaType NUMBER_FORMAT = new JavaType(
      "org.springframework.format.annotation.NumberFormat");
  public static final JavaType OPEN_ENTITY_MANAGER_IN_VIEW_FILTER = new JavaType(
      "org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter");
  public static final JavaType PAGE = new JavaType("org.springframework.data.domain.Page");
  public static final JavaType PAGE_REQUEST = new JavaType(
      "org.springframework.data.domain.PageRequest");
  public static final JavaType PAGEABLE = new JavaType("org.springframework.data.domain.Pageable");
  public static final JavaType PAGEABLE_DEFAULT = new JavaType(
      "org.springframework.data.web.PageableDefault");
  public static final JavaType SPRING_JPA_REPOSITORY = new JavaType(
      "org.springframework.data.jpa.repository.JpaRepository");
  public static final JavaType SPRING_DATA_REPOSITORY = new JavaType(
      "org.springframework.data.repository.Repository");
  public static final JavaType PATH_VARIABLE = new JavaType(
      "org.springframework.web.bind.annotation.PathVariable");
  public static final JavaType PERMISSION_EVALUATOR = new JavaType(
      "org.springframework.security.access.PermissionEvaluator");
  public static final JavaType PERSISTENT = new JavaType(
      "org.springframework.data.annotation.Persistent");
  public static final JavaType PRE_AUTHORIZE = new JavaType(
      "org.springframework.security.access.prepost.PreAuthorize");
  public static final JavaType PRE_FILTER = new JavaType(
      "org.springframework.security.access.prepost.PreFilter");
  public static final JavaType POST_AUTHORIZE = new JavaType(
      "org.springframework.security.access.prepost.PostAuthorize");
  public static final JavaType POST_FILTER = new JavaType(
      "org.springframework.security.access.prepost.PostFilter");
  public static final JavaType POST_MAPPING = new JavaType(
      "org.springframework.web.bind.annotation.PostMapping");
  public static final JavaType PROPAGATION = new JavaType(
      "org.springframework.transaction.annotation.Propagation");
  public static final JavaType PRIMARY = new JavaType(
      "org.springframework.context.annotation.Primary");
  public static final JavaType PROFILE = new JavaType(
      "org.springframework.context.annotation.Profile");
  public static final JavaType PUT_MAPPING = new JavaType(
      "org.springframework.web.bind.annotation.PutMapping");
  public static final JavaType REDIRECT_ATTRIBUTES = new JavaType(
      "org.springframework.web.servlet.mvc.support.RedirectAttributes");
  public static final JavaType REPOSITORY = new JavaType(
      "org.springframework.stereotype.Repository");
  public static final JavaType REQUEST_BODY = new JavaType(
      "org.springframework.web.bind.annotation.RequestBody");
  public static final JavaType REQUEST_MAPPING = new JavaType(
      "org.springframework.web.bind.annotation.RequestMapping");
  public static final JavaType REQUEST_METHOD = new JavaType(
      "org.springframework.web.bind.annotation.RequestMethod");
  public static final JavaType REQUEST_PARAM = new JavaType(
      "org.springframework.web.bind.annotation.RequestParam");
  public static final JavaType RESPONSE_BODY = new JavaType(
      "org.springframework.web.bind.annotation.ResponseBody");
  public static final JavaType RESPONSE_ENTITY = new JavaType(
      "org.springframework.http.ResponseEntity");
  public static final JavaType RESPONSE_STATUS = new JavaType(
      "org.springframework.web.bind.annotation.ResponseStatus");
  public static final JavaType REST_CONTROLLER = new JavaType(
      "org.springframework.web.bind.annotation.RestController");
  public static final JavaType SPRING_BOOT_APPLICATION = new JavaType(
      "org.springframework.boot.autoconfigure.SpringBootApplication");
  public static final JavaType SPRING_BOOT_SERVLET_INITIALIZER = new JavaType(
      "org.springframework.boot.web.support.SpringBootServletInitializer");
  public static final JavaType SERVICE = new JavaType("org.springframework.stereotype.Service");
  public static final JavaType SESSION_LOCALE_RESOLVER = new JavaType(
      "org.springframework.web.servlet.i18n.SessionLocaleResolver");
  public static final JavaType SIMPLE_MAIL_MESSAGE = new JavaType(
      "org.springframework.mail.SimpleMailMessage");
  public static final JavaType SIMPLE_TYPE_CONVERTER = new JavaType(
      "org.springframework.beans.SimpleTypeConverter");
  public static final JavaType SPRING_BOOT_TEST = new JavaType(
      "org.springframework.boot.test.context.SpringBootTest");
  public static final JavaType SPRING_RUNNER = new JavaType(
      "org.springframework.test.context.junit4.SpringRunner");
  public static final JavaType STRING_UTILS = new JavaType("org.springframework.util.StringUtils");
  public static final JavaType TEST_CONFIGURATION = new JavaType(
      "org.springframework.boot.test.context.TestConfiguration");
  public static final JavaType TRANSACTIONAL = new JavaType(
      "org.springframework.transaction.annotation.Transactional");
  public static final JavaType URI_UTILS = new JavaType("org.springframework.web.util.UriUtils");
  public static final JavaType URI_COMPONENTS = new JavaType(
      "org.springframework.web.util.UriComponents");
  public static final JavaType URI_COMPONENTS_BUILDER = new JavaType(
      "org.springframework.web.util.UriComponentsBuilder");
  public static final JavaType VALUE = new JavaType(
      "org.springframework.beans.factory.annotation.Value");
  public static final JavaType WEB_DATA_BINDER = new JavaType(
      "org.springframework.web.bind.WebDataBinder");
  public static final JavaType WEB_MVC_CONFIGURER_ADAPTER = new JavaType(
      "org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter");
  public static final JavaType WEB_UTILS = new JavaType("org.springframework.web.util.WebUtils");
  public static final JavaType WEB_APP_CONFIGURATION = new JavaType(
      "org.springframework.test.context.web.WebAppConfiguration");
  public static final JavaType WEB_APPLICATION_INITIALIZER = new JavaType(
      "org.springframework.web.WebApplicationInitializer");

  /**
   * Returns the {@link JavaType} for a Spring converter
   *
   * @param fromType
   *            the type being converted from (required)
   * @param toType
   *            the type being converted to (required)
   * @return a non-<code>null</code> type
   */
  public static JavaType getConverterType(final JavaType fromType, final JavaType toType) {
    Validate.notNull(fromType, "'From' type is required");
    Validate.notNull(toType, "'To' type is required");

    return new JavaType("org.springframework.core.convert.converter.Converter", 0, DataType.TYPE,
        null, Arrays.asList(fromType, toType));
  }

  /**
   * Constructor is private to prevent instantiation
   */
  private SpringJavaType() {}
}
