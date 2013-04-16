package org.springframework.roo.addon.gwt;

import static org.springframework.roo.model.JavaType.LONG_OBJECT;
import static org.springframework.roo.model.JavaType.OBJECT;
import static org.springframework.roo.model.JdkJavaType.BIG_DECIMAL;
import static org.springframework.roo.model.JdkJavaType.BIG_INTEGER;
import static org.springframework.roo.model.JdkJavaType.DATE;
import static org.springframework.roo.model.JpaJavaType.EMBEDDABLE;
import static org.springframework.roo.model.SpringJavaType.DATE_TIME_FORMAT;
import static org.springframework.roo.model.SpringJavaType.NUMBER_FORMAT;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

public class GwtProxyProperty {

    public static String getProxyRendererType(
            final JavaPackage topLevelPackage, final JavaType javaType) {
        return GwtType.EDIT_RENDERER.getPath().packageName(topLevelPackage)
                + "." + javaType.getSimpleTypeName() + "Renderer";
    }

    private List<AnnotationMetadata> annotations;
    private String getter;
    private String name;
    private final ClassOrInterfaceTypeDetails ptmd;
    private final JavaPackage topLevelPackage;

    private final JavaType type;

    public GwtProxyProperty(final JavaPackage topLevelPackage,
            final ClassOrInterfaceTypeDetails ptmd, final JavaType type) {
        Validate.notNull(type, "Type required");
        this.topLevelPackage = topLevelPackage;
        this.ptmd = ptmd;
        this.type = type;
    }

    public GwtProxyProperty(final JavaPackage topLevelPackage,
            final ClassOrInterfaceTypeDetails ptmd, final JavaType type,
            final String name, final List<AnnotationMetadata> annotations,
            final String getter) {
        this(topLevelPackage, ptmd, type);
        this.name = name;
        this.annotations = annotations;
        this.getter = getter;
    }

    public String forEditView() {
        String initializer = "";

        if (isBoolean()) {
            initializer = " = " + getCheckboxSubtype();
        }

        if (isEnum() && !isCollection()) {
            initializer = String.format(" = new ValueListBox<%s>(%s)",
                    type.getFullyQualifiedTypeName(), getRenderer());
        }

        if (isProxy()) {
            initializer = String
                    .format(" = new ValueListBox<%1$s>(%2$s.instance(), new com.google.web.bindery.requestfactory.gwt.ui.client.EntityProxyKeyProvider<%1$s>())",
                            type.getFullyQualifiedTypeName(),
                            getProxyRendererType());
        }

        return String.format("@UiField %s %s %s", getEditor(), getName(),
                initializer);
    }

    public String forMobileListView(final String rendererName) {
        return new StringBuilder("if (value.").append(getGetter())
                .append("() != null) {\n\t\t\t\tsb.appendEscaped(")
                .append(rendererName).append(".render(value.")
                .append(getGetter()).append("()));\n\t\t\t}").toString();
    }

    public String getBinder() {
        if (type.equals(JavaType.DOUBLE_OBJECT)) {
            return "g:DoubleBox";
        }
        if (type.equals(LONG_OBJECT)) {
            return "g:LongBox";
        }
        if (type.equals(JavaType.INT_OBJECT)) {
            return "g:IntegerBox";
        }
        if (type.equals(JavaType.FLOAT_OBJECT)) {
            return "r:FloatBox";
        }
        if (type.equals(JavaType.BYTE_OBJECT)) {
            return "r:ByteBox";
        }
        if (type.equals(JavaType.SHORT_OBJECT)) {
            return "r:ShortBox";
        }
        if (type.equals(JavaType.CHAR_OBJECT)) {
            return "r:CharBox";
        }
        if (type.equals(BIG_DECIMAL)) {
            return "r:BigDecimalBox";
        }
        return isCollection() ? "e:" + getSetEditor() : isDate() ? "d:DateBox"
                : isBoolean() ? "g:CheckBox" : isString() ? "g:TextBox"
                        : "g:ValueListBox";
    }

    public String getCheckboxSubtype() {
        // TODO: Ugly hack, fix in M4
        return "new CheckBox() { public void setValue(Boolean value) { super.setValue(value == null ? Boolean.FALSE : value); } }";
    }

    public String getCollectionRenderer() {
        JavaType arg = OBJECT;
        if (type.getParameters().size() > 0) {
            arg = type.getParameters().get(0);
        }
        return GwtPath.SCAFFOLD_PLACE.packageName(topLevelPackage)
                + ".CollectionRenderer.of("
                + new GwtProxyProperty(topLevelPackage, ptmd, arg)
                        .getRenderer() + ")";
    }

    private String getDateTimeFormat() {
        String format = "DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_SHORT)";
        if (annotations == null || annotations.isEmpty()) {
            return format;
        }

        String style = "";
        final AnnotationMetadata annotation = MemberFindingUtils
                .getAnnotationOfType(annotations, DATE_TIME_FORMAT);
        if (annotation != null) {
            final AnnotationAttributeValue<?> attr = annotation
                    .getAttribute(new JavaSymbolName("style"));
            if (attr != null) {
                style = (String) attr.getValue();
            }
        }
        if (StringUtils.isNotBlank(style)) {
            if (style.equals("S")) {
                format = "DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_TIME_SHORT)";
            }
            else if (style.equals("M")) {
                format = "DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_TIME_MEDIUM)";
            }
            else if (style.equals("F")) {
                format = "DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_TIME_FULL)";
            }
            else if (style.equals("S-")) {
                format = "DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_SHORT)";
            }
            else if (style.equals("M-")) {
                format = "DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_MEDIUM)";
            }
            else if (style.equals("F-")) {
                format = "DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_FULL)";
            }
        }
        return format;
    }

    private String getEditor() {
        if (type.equals(JavaType.DOUBLE_OBJECT)) {
            return "DoubleBox";
        }
        if (type.equals(LONG_OBJECT)) {
            return "LongBox";
        }
        if (type.equals(JavaType.INT_OBJECT)) {
            return "IntegerBox";
        }
        if (type.equals(JavaType.FLOAT_OBJECT)) {
            return "FloatBox";
        }
        if (type.equals(JavaType.BYTE_OBJECT)) {
            return "ByteBox";
        }
        if (type.equals(JavaType.SHORT_OBJECT)) {
            return "ShortBox";
        }
        if (type.equals(JavaType.CHAR_OBJECT)) {
            return "CharBox";
        }
        if (type.equals(BIG_DECIMAL)) {
            return "BigDecimalBox";
        }
        if (isBoolean()) {
            return "(provided = true) CheckBox";
        }
        return isCollection() ? getSetEditor() : isDate() ? "DateBox"
                : isString() ? "TextBox" : "(provided = true) ValueListBox<"
                        + type.getFullyQualifiedTypeName() + ">";
    }

    public String getFormatter() {
        if (isCollectionOfProxy()) {
            return getCollectionRenderer() + ".render";
        }
        else if (isDate()) {
            return getDateTimeFormat() + ".format";
        }
        else if (type.equals(JavaType.INT_OBJECT)
                || type.equals(JavaType.FLOAT_OBJECT)
                || type.equals(JavaType.DOUBLE_OBJECT)
                || type.equals(BIG_INTEGER) || type.equals(BIG_DECIMAL)) {
            String formatter = "String.valueOf";
            if (annotations == null || annotations.isEmpty()) {
                return formatter;
            }

            final AnnotationMetadata annotation = MemberFindingUtils
                    .getAnnotationOfType(annotations, NUMBER_FORMAT);
            if (annotation != null) {
                final AnnotationAttributeValue<?> attr = annotation
                        .getAttribute(new JavaSymbolName("style"));
                if (attr != null) {
                    final String style = attr.getValue().toString();
                    if ("org.springframework.format.annotation.NumberFormat.Style.CURRENCY"
                            .equals(style)) {
                        formatter = "NumberFormat.getCurrencyFormat().format";
                    }
                    else if ("org.springframework.format.annotation.NumberFormat.Style.PERCENT"
                            .equals(style)) {
                        formatter = "NumberFormat.getPercentFormat().format";
                    }
                    else {
                        formatter = "NumberFormat.getDecimalFormat().format";
                    }
                }
                else {
                    formatter = "NumberFormat.getDecimalFormat().format";
                }
            }
            return formatter;
        }
        else if (isProxy()) {
            return getProxyRendererType() + ".instance().render";
        }
        else {
            return "String.valueOf";
        }
    }

    public String getGetter() {
        return getter;
    }

    public String getName() {
        return name;
    }

    public JavaType getPropertyType() {
        return type;
    }

    String getProxyRendererType() {
        return getProxyRendererType(topLevelPackage,
                isCollectionOfProxy() ? type.getParameters().get(0) : type);
    }

    public String getReadableName() {
        return new JavaSymbolName(name).getReadableSymbolName();
    }

    public String getRenderer() {
        return isCollection() ? getCollectionRenderer()
                : isDate() ? "new DateTimeFormatRenderer("
                        + getDateTimeFormat() + ")"
                        : isPrimitive() || isEnum() || isEmbeddable()
                                || type.equals(OBJECT) ? "new AbstractRenderer<"
                                + getType()
                                + ">() {\n        public String render("
                                + getType()
                                + " obj) {\n          return obj == null ? \"\" : String.valueOf(obj);\n        }\n      }"
                                : getProxyRendererType() + ".instance()";
    }

    private String getSetEditor() {
        String typeName = OBJECT.getFullyQualifiedTypeName();
        if (type.getParameters().size() > 0) {
            typeName = type.getParameters().get(0).getSimpleTypeName();
        }
        if (typeName.endsWith(GwtType.PROXY.getSuffix())) {
            typeName = typeName.substring(0, typeName.length()
                    - GwtType.PROXY.getSuffix().length());
        }
        return typeName
                + (type.getSimpleTypeName().equals("Set") ? GwtType.SET_EDITOR
                        .getSuffix() : GwtType.LIST_EDITOR.getSuffix());
    }

    public JavaType getSetEditorType() {
        return new JavaType(GwtType.SET_EDITOR.getPath().packageName(
                topLevelPackage)
                + "." + getSetEditor());
    }

    public String getSetValuePickerMethod() {
        return "\tpublic void "
                + getSetValuePickerMethodName()
                + "(Collection<"
                + (isCollection() ? type.getParameters().get(0)
                        .getSimpleTypeName() : type.getSimpleTypeName())
                + "> values) {\n" + "\t\t" + getName()
                + ".setAcceptableValues(values);\n" + "\t}\n";
    }

    public String getSetEmptyValuePickerMethod() {
        return "\tpublic void "
                + getSetValuePickerMethodName()
                + "(Collection<"
                + (isCollection() ? type.getParameters().get(0)
                        .getSimpleTypeName() : type.getSimpleTypeName())
                + "> values) { }";
    }

    String getSetValuePickerMethodName() {
        return "set" + StringUtils.capitalize(getName()) + "PickerValues";
    }

    public String getType() {
        return type.getFullyQualifiedTypeName();
    }

    public JavaType getValueType() {
        if (isCollection()) {
            return type.getParameters().get(0);
        }
        return type;
    }

    public boolean isBoolean() {
        return type.equals(JavaType.BOOLEAN_OBJECT);
    }

    public boolean isCollection() {
        return type.isCommonCollectionType();
    }

    public boolean isCollectionOfProxy() {
        return type.getParameters().size() != 0
                && isCollection()
                && new GwtProxyProperty(topLevelPackage, ptmd, type
                        .getParameters().get(0)).isProxy();
    }

    public boolean isDate() {
        return type.equals(DATE);
    }

    public boolean isEmbeddable() {
        if (ptmd != null) {
            final List<AnnotationMetadata> annotations = ptmd.getAnnotations();
            for (final AnnotationMetadata annotation : annotations) {
                if (annotation.getAnnotationType().equals(EMBEDDABLE)) {
                    return true;
                }
            }
        }

        return false;
    }

    boolean isEnum() {
        return ptmd != null
                && ptmd.getPhysicalTypeCategory() == PhysicalTypeCategory.ENUMERATION;
    }

    public boolean isPrimitive() {
        return type.isPrimitive() || isDate() || isString() || isBoolean()
                || type.equals(JavaType.DOUBLE_OBJECT)
                || type.equals(LONG_OBJECT) || type.equals(JavaType.INT_OBJECT)
                || type.equals(JavaType.FLOAT_OBJECT)
                || type.equals(JavaType.BYTE_OBJECT)
                || type.equals(JavaType.SHORT_OBJECT)
                || type.equals(JavaType.CHAR_OBJECT)
                || type.equals(BIG_DECIMAL);
    }

    public boolean isProxy() {
        return ptmd != null && !isDate() && !isString() && !isPrimitive()
                && !isEnum() && !isCollection() && !isEmbeddable()
                && !type.equals(OBJECT);
    }

    public boolean isString() {
        return type.equals(JavaType.STRING);
    }
}