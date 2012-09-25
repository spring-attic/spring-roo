package __PACKAGE__;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

public class MessageFactory {

    private static String DEFAULT_DETAIL_SUFFIX = "_detail";

    private MessageFactory() {
    }

    public static FacesMessage getMessage(Locale locale, String messageId,
            FacesMessage.Severity severity, Object... params) {
        FacesMessage facesMessage = getMessage(locale, messageId, params);
        facesMessage.setSeverity(severity);
        return facesMessage;
    }

    public static FacesMessage getMessage(String messageId,
            FacesMessage.Severity severity, Object... params) {
        FacesMessage facesMessage = getMessage(getLocale(), messageId, params);
        facesMessage.setSeverity(severity);
        return facesMessage;
    }

    public static FacesMessage getMessage(String messageId, Object... params) {
        return getMessage(getLocale(), messageId, params);
    }

    public static FacesMessage getMessage(Locale locale, String messageId,
            Object... params) {
        String summary = null;
        String detail = null;
        FacesContext context = FacesContext.getCurrentInstance();
        ResourceBundle bundle = context.getApplication().getResourceBundle(
                context, "messages");

        try {
            summary = getFormattedText(locale, bundle.getString(messageId),
                    params);
        }
        catch (MissingResourceException e) {
            summary = messageId;
        }

        try {
            detail = getFormattedText(locale,
                    bundle.getString(messageId + DEFAULT_DETAIL_SUFFIX), params);
        }
        catch (MissingResourceException e) {
            // NoOp
        }

        return new FacesMessage(summary, detail);
    }

    private static String getFormattedText(Locale locale, String message,
            Object params[]) {
        MessageFormat messageFormat = null;

        if (params == null || message == null) {
            return message;
        }

        if (locale != null) {
            messageFormat = new MessageFormat(message, locale);
        }
        else {
            messageFormat = new MessageFormat(message);
        }

        return messageFormat.format(params);
    }

    private static Locale getLocale() {
        Locale locale = null;
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null && facesContext.getViewRoot() != null) {
            locale = facesContext.getViewRoot().getLocale();
            if (locale == null)
                locale = Locale.getDefault();
        }
        else {
            locale = Locale.getDefault();
        }

        return locale;
    }
}
