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
    public static final JavaType ASYNC = new JavaType(
            "org.springframework.scheduling.annotation.Async");
    public static final JavaType AUTHENTICATION = new JavaType(
            "org.springframework.security.core.Authentication");
    public static final JavaType AUTOWIRED = new JavaType(
            "org.springframework.beans.factory.annotation.Autowired");
    public static final JavaType BINDING_RESULT = new JavaType(
            "org.springframework.validation.BindingResult");
    public static final JavaType CHARACTER_ENCODING_FILTER = new JavaType(
            "org.springframework.web.filter.CharacterEncodingFilter");
    public static final JavaType COMPONENT = new JavaType(
            "org.springframework.stereotype.Component");
    public static final JavaType CONFIGURABLE = new JavaType(
            "org.springframework.beans.factory.annotation.Configurable");
    public static final JavaType CONTEXT_CONFIGURATION = new JavaType(
            "org.springframework.test.context.ContextConfiguration");
    public static final JavaType CONTEXT_LOADER_LISTENER = new JavaType(
            "org.springframework.web.context.ContextLoaderListener");
    public static final JavaType CONTROLLER = new JavaType(
            "org.springframework.stereotype.Controller");
    public static final JavaType CONVERSION_SERVICE = new JavaType(
            "org.springframework.core.convert.ConversionService");
    public static final JavaType CONVERSION_SERVICE_EXPOSING_INTERCEPTOR = new JavaType(
            "org.springframework.web.servlet.handler.ConversionServiceExposingInterceptor");
    public static final JavaType DATA_ID = new JavaType(
            "org.springframework.data.annotation.Id");
    public static final JavaType DATE_TIME_FORMAT = new JavaType(
            "org.springframework.format.annotation.DateTimeFormat");
    public static final JavaType DISPATCHER_SERVLET = new JavaType(
            "org.springframework.web.servlet.DispatcherServlet");
    public static final JavaType FLOW_HANDLER_MAPPING = new JavaType(
            "org.springframework.webflow.mvc.servlet.FlowHandlerMapping");
    public static final JavaType FORMATTER_REGISTRY = new JavaType(
            "org.springframework.format.FormatterRegistry");
    public static final JavaType HIDDEN_HTTP_METHOD_FILTER = new JavaType(
            "org.springframework.web.filter.HiddenHttpMethodFilter");
    public static final JavaType HTTP_HEADERS = new JavaType(
            "org.springframework.http.HttpHeaders");
    public static final JavaType HTTP_STATUS = new JavaType(
            "org.springframework.http.HttpStatus");
    public static final JavaType JAVA_MAIL_SENDER_IMPL = new JavaType(
            "org.springframework.mail.javamail.JavaMailSenderImpl");
    public static final JavaType JMS_OPERATIONS = new JavaType(
            "org.springframework.jms.core.JmsOperations");
    public static final JavaType JMS_TEMPLATE = new JavaType(
            "org.springframework.jms.core.JmsTemplate");
    public static final JavaType JPA_TRANSACTION_MANAGER = new JavaType(
            "org.springframework.orm.jpa.JpaTransactionManager");
    public static final JavaType LOCAL_CONTAINER_ENTITY_MANAGER_FACTORY_BEAN = new JavaType(
            "org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean");
    public static final JavaType LOCAL_ENTITY_MANAGER_FACTORY_BEAN = new JavaType(
            "org.springframework.orm.jpa.LocalEntityManagerFactoryBean");
    public static final JavaType LOCALE_CONTEXT_HOLDER = new JavaType(
            "org.springframework.context.i18n.LocaleContextHolder");
    public static final JavaType MAIL_SENDER = new JavaType(
            "org.springframework.mail.MailSender");
    public static final JavaType MOCK_STATIC_ENTITY_METHODS = new JavaType(
            "org.springframework.mock.staticmock.MockStaticEntityMethods");
    public static final JavaType MODEL = new JavaType(
            "org.springframework.ui.Model");
    public static final JavaType MODEL_ATTRIBUTE = new JavaType(
            "org.springframework.web.bind.annotation.ModelAttribute");
    public static final JavaType MODEL_MAP = new JavaType(
            "org.springframework.ui.ModelMap");
    public static final JavaType NUMBER_FORMAT = new JavaType(
            "org.springframework.format.annotation.NumberFormat");
    public static final JavaType OPEN_ENTITY_MANAGER_IN_VIEW_FILTER = new JavaType(
            "org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter");
    public static final JavaType PATH_VARIABLE = new JavaType(
            "org.springframework.web.bind.annotation.PathVariable");
    public static final JavaType PERMISSION_EVALUATOR = new JavaType(
            "org.springframework.security.access.PermissionEvaluator");
    public static final JavaType PERSISTENT = new JavaType(
            "org.springframework.data.annotation.Persistent");
    public static final JavaType PRE_AUTHORIZE = new JavaType(
            "org.springframework.security.access.prepost.PreAuthorize");
    public static final JavaType POST_AUTHORIZE = new JavaType(
            "org.springframework.security.access.prepost.PostAuthorize");
    public static final JavaType PROPAGATION = new JavaType(
            "org.springframework.transaction.annotation.Propagation");
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
    public static final JavaType SERVICE = new JavaType(
            "org.springframework.stereotype.Service");
    public static final JavaType SIMPLE_MAIL_MESSAGE = new JavaType(
            "org.springframework.mail.SimpleMailMessage");
    public static final JavaType SIMPLE_TYPE_CONVERTER = new JavaType(
            "org.springframework.beans.SimpleTypeConverter");
    public static final JavaType TRANSACTIONAL = new JavaType(
            "org.springframework.transaction.annotation.Transactional");
    public static final JavaType URI_UTILS = new JavaType(
            "org.springframework.web.util.UriUtils");
    public static final JavaType VALUE = new JavaType(
            "org.springframework.beans.factory.annotation.Value");
    public static final JavaType WEB_UTILS = new JavaType(
            "org.springframework.web.util.WebUtils");

    /**
     * Returns the {@link JavaType} for a Spring converter
     * 
     * @param fromType the type being converted from (required)
     * @param toType the type being converted to (required)
     * @return a non-<code>null</code> type
     */
    public static JavaType getConverterType(final JavaType fromType,
            final JavaType toType) {
        Validate.notNull(fromType, "'From' type is required");
        Validate.notNull(toType, "'To' type is required");
        return new JavaType(
                "org.springframework.core.convert.converter.Converter", 0,
                DataType.TYPE, null, Arrays.asList(fromType, toType));
    }

    /**
     * Constructor is private to prevent instantiation
     */
    private SpringJavaType() {
    }
}