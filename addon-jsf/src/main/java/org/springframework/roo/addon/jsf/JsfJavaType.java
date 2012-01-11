package org.springframework.roo.addon.jsf;

import org.springframework.roo.model.JavaType;

/**
 * Constants for JSF/PrimeFaces-specific {@link JavaType}s. Use them in
 * preference to creating new instances of these types.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public class JsfJavaType {

    // javax.faces
    public static final JavaType APPLICATION = new JavaType(
            "javax.faces.application.Application");
    public static final JavaType APPLICATION_SCOPED = new JavaType(
            "javax.faces.bean.ApplicationScoped");

    public static final JavaType CONVERTER = new JavaType(
            "javax.faces.convert.Converter");
    public static final JavaType DATE_TIME_CONVERTER = new JavaType(
            "javax.faces.convert.DateTimeConverter");

    // General
    public static final String DISPLAY_CREATE_DIALOG = "displayCreateDialog";
    public static final String DISPLAY_LIST = "displayList";
    public static final JavaType DOUBLE_RANGE_VALIDATOR = new JavaType(
            "javax.faces.validator.DoubleRangeValidator");
    // javax.el
    public static final JavaType EL_CONTEXT = new JavaType("javax.el.ELContext");
    public static final JavaType ENUM_CONVERTER = new JavaType(
            "javax.faces.convert.EnumConverter");
    public static final JavaType EXPRESSION_FACTORY = new JavaType(
            "javax.el.ExpressionFactory");
    public static final JavaType FACES_CONTEXT = new JavaType(
            "javax.faces.context.FacesContext");
    public static final JavaType FACES_CONVERTER = new JavaType(
            "javax.faces.convert.FacesConverter");
    public static final JavaType FACES_MESSAGE = new JavaType(
            "javax.faces.application.FacesMessage");
    public static final JavaType HTML_OUTPUT_TEXT = new JavaType(
            "javax.faces.component.html.HtmlOutputText");
    public static final JavaType HTML_PANEL_GRID = new JavaType(
            "javax.faces.component.html.HtmlPanelGrid");
    public static final JavaType LENGTH_VALIDATOR = new JavaType(
            "javax.faces.validator.LengthValidator");
    public static final JavaType LONG_RANGE_VALIDATOR = new JavaType(
            "javax.faces.validator.LongRangeValidator");
    public static final JavaType MANAGED_BEAN = new JavaType(
            "javax.faces.bean.ManagedBean");
    // org.primefaces
    public static final JavaType PRIMEFACES_AUTO_COMPLETE = new JavaType(
            "org.primefaces.component.autocomplete.AutoComplete");
    public static final JavaType PRIMEFACES_CALENDAR = new JavaType(
            "org.primefaces.component.calendar.Calendar");
    public static final JavaType PRIMEFACES_CLOSE_EVENT = new JavaType(
            "org.primefaces.event.CloseEvent");
    public static final JavaType PRIMEFACES_COMMAND_BUTTON = new JavaType(
            "org.primefaces.component.commandbutton.CommandButton");
    public static final JavaType PRIMEFACES_DEFAULT_MENU_MODEL = new JavaType(
            "org.primefaces.model.DefaultMenuModel");
    public static final JavaType PRIMEFACES_DEFAULT_STREAMED_CONTENT = new JavaType(
            "org.primefaces.model.DefaultStreamedContent");
    public static final JavaType PRIMEFACES_FILE_DOWNLOAD_ACTION_LISTENER = new JavaType(
            "org.primefaces.component.filedownload.FileDownloadActionListener");

    public static final JavaType PRIMEFACES_FILE_UPLOAD = new JavaType(
            "org.primefaces.component.fileupload.FileUpload");
    public static final JavaType PRIMEFACES_FILE_UPLOAD_EVENT = new JavaType(
            "org.primefaces.event.FileUploadEvent");
    public static final JavaType PRIMEFACES_INPUT_TEXT = new JavaType(
            "org.primefaces.component.inputtext.InputText");
    public static final JavaType PRIMEFACES_INPUT_TEXTAREA = new JavaType(
            "org.primefaces.component.inputtextarea.InputTextarea");
    public static final JavaType PRIMEFACES_KEYBOARD = new JavaType(
            "org.primefaces.component.keyboard.Keyboard");
    public static final JavaType PRIMEFACES_MENU_ITEM = new JavaType(
            "org.primefaces.component.menuitem.MenuItem");
    public static final JavaType PRIMEFACES_MENU_MODEL = new JavaType(
            "org.primefaces.model.MenuModel");
    public static final JavaType PRIMEFACES_MESSAGE = new JavaType(
            "org.primefaces.component.message.Message");
    public static final JavaType PRIMEFACES_REQUEST_CONTEXT = new JavaType(
            "org.primefaces.context.RequestContext");
    public static final JavaType PRIMEFACES_SELECT_BOOLEAN_CHECKBOX = new JavaType(
            "org.primefaces.component.selectbooleancheckbox.SelectBooleanCheckbox");
    public static final JavaType PRIMEFACES_SELECT_MANY_MENU = new JavaType(
            "org.primefaces.component.selectmanymenu.SelectManyMenu");
    public static final JavaType PRIMEFACES_SELECT_ONE_LISTBOX = new JavaType(
            "org.primefaces.component.selectonelistbox.SelectOneListbox");
    public static final JavaType PRIMEFACES_SLIDER = new JavaType(
            "org.primefaces.component.slider.Slider");
    public static final JavaType PRIMEFACES_SPINNER = new JavaType(
            "org.primefaces.component.spinner.Spinner");
    public static final JavaType PRIMEFACES_STREAMED_CONTENT = new JavaType(
            "org.primefaces.model.StreamedContent");
    public static final JavaType PRIMEFACES_SUB_MENU = new JavaType(
            "org.primefaces.component.submenu.Submenu");
    public static final JavaType PRIMEFACES_UPLOADED_FILE = new JavaType(
            "org.primefaces.model.UploadedFile");
    public static final JavaType REGEX_VALIDATOR = new JavaType(
            "javax.faces.validator.RegexValidator");
    public static final JavaType REQUEST_SCOPED = new JavaType(
            "javax.faces.bean.RequestScoped");
    public static final JavaType SESSION_SCOPED = new JavaType(
            "javax.faces.bean.SessionScoped");
    public static final JavaType UI_COMPONENT = new JavaType(
            "javax.faces.component.UIComponent");
    public static final JavaType UI_SELECT_ITEM = new JavaType(
            "javax.faces.component.UISelectItem");
    public static final JavaType UI_SELECT_ITEMS = new JavaType(
            "javax.faces.component.UISelectItems");
    public static final JavaType VIEW_SCOPED = new JavaType(
            "javax.faces.bean.ViewScoped");

    /**
     * Constructor is private to prevent instantiation
     */
    private JsfJavaType() {
    }
}
