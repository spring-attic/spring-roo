package org.springframework.roo.addon.gwt;

import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.util.StringUtils;

import java.util.List;

class GwtProxyProperty {
	private final ProjectMetadata projectMetadata;
	private final String name;
	private final String getter;
	private final JavaType type;
	private PhysicalTypeMetadata ptmd;

	public GwtProxyProperty(ProjectMetadata projectMetadata, JavaType type, PhysicalTypeMetadata ptmd) {
		this.projectMetadata = projectMetadata;
		this.type = type;
		this.ptmd = ptmd;
		this.getter = null;
		this.name = null;
	}

	public GwtProxyProperty(ProjectMetadata projectMetadata, JavaType type, PhysicalTypeMetadata ptmd, String name, String accessorMethodName) {
		this.projectMetadata = projectMetadata;
		this.type = type;
		this.ptmd = ptmd;
		this.getter = accessorMethodName;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getGetter() {
		return getter;
	}

	public String getType() {
		return type.getFullyQualifiedTypeName();
	}

	public JavaType getPropertyType() {
		return type;
	}

	public boolean isBoolean() {
		return type != null && type.equals(JavaType.BOOLEAN_OBJECT);
	}

	public boolean isDate() {
		return type != null && type.equals(new JavaType("java.util.Date"));
	}

	public boolean isPrimitive() {
		return type.isPrimitive()
				|| isDate()
				|| isString()
				|| isBoolean()
				|| type.equals(JavaType.DOUBLE_OBJECT)
				|| type.equals(JavaType.LONG_OBJECT)
				|| type.equals(JavaType.INT_OBJECT)
				|| type.equals(JavaType.FLOAT_OBJECT)
				|| type.equals(JavaType.BYTE_OBJECT)
				|| type.equals(JavaType.SHORT_OBJECT)
				|| type.equals(JavaType.CHAR_OBJECT)
				|| type.equals(new JavaType("java.math.BigDecimal"));
	}

	public boolean isString() {
		return type != null && type.equals(JavaType.STRING_OBJECT);
	}

	public String getBinder() {
		if (type.equals(JavaType.DOUBLE_OBJECT)) {
			return "g:DoubleBox";
		}
		if (type.equals(JavaType.LONG_OBJECT)) {
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
		if (type.equals(new JavaType("java.math.BigDecimal"))) {
			return "r:BigDecimalBox";
		}
		return isCollection() ? "a:" + getSetEditor() : isDate() ? "d:DateBox" : isBoolean() ? "g:CheckBox" : isString() ? "g:TextBox" : "g:ValueListBox";
	}

	private String getSetEditor() {
		String typeName = "java.lang.Object";
		if (type.getParameters().size() > 0) {
			typeName = type.getParameters().get(0).getSimpleTypeName();
		}
		if (typeName.endsWith(GwtType.PROXY.getSuffix())) {
			typeName = typeName.substring(0, typeName.length() - GwtType.PROXY.getSuffix().length());
		}
		return typeName + (type.getSimpleTypeName().equals("Set") ? GwtType.SET_EDITOR.getSuffix() : GwtType.LIST_EDITOR.getSuffix());
	}

	public JavaType getSetEditorType() {
		return new JavaType(GwtType.SET_EDITOR.getPath().packageName(projectMetadata) + "." + getSetEditor());
	}

	private String getEditor() {
		if (type.equals(JavaType.DOUBLE_OBJECT)) {
			return "DoubleBox";
		}
		if (type.equals(JavaType.LONG_OBJECT)) {
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
		if (type.equals(new JavaType("java.math.BigDecimal"))) {
			return "BigDecimalBox";
		}
		if (isBoolean()) {
			return "(provided = true) CheckBox";
		}
		return isCollection() ? getSetEditor() : isDate() ? "DateBox" : isString() ? "TextBox" : "(provided = true) ValueListBox<" + type.getFullyQualifiedTypeName() + ">";
	}

	public String getCollectionRenderer() {
		JavaType arg = new JavaType("java.lang.Object");
		if (type.getParameters().size() > 0) {
			arg = type.getParameters().get(0);
		}
		return GwtPath.SCAFFOLD_PLACE.packageName(projectMetadata) + ".CollectionRenderer.of(" + new GwtProxyProperty(projectMetadata, arg, ptmd).getRenderer() + ")";
	}

	public String getFormatter() {
		return isCollectionOfProxy() ? getCollectionRenderer() + ".render" : isDate() ? "DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_SHORT).format" : isProxy() ? getProxyRendererType() + ".instance().render" : "String.valueOf";
	}

	public String getRenderer() {
		return isCollection() ? getCollectionRenderer() : isDate() ? "new DateTimeFormatRenderer(DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_SHORT))" : isPrimitive() || isEnum() || isEmbeddable() || type.equals(new JavaType("java.lang.Object")) ? "new AbstractRenderer<" + getType() + ">() {\n        public String render(" + getType() + " obj) {\n          return obj == null ? \"\" : String.valueOf(obj);\n        }\n      }" : getProxyRendererType() + ".instance()";
	}

	String getProxyRendererType() {
		return getProxyRendererType(projectMetadata, isCollectionOfProxy() ? type.getParameters().get(0) : type);
	}

	public static String getProxyRendererType(ProjectMetadata pmd, JavaType javaType) {
		return GwtType.EDIT_RENDERER.getPath().packageName(pmd) + "." + javaType.getSimpleTypeName() + "Renderer";
	}

	public String getCheckboxSubtype() {
		// TODO: Ugly hack, fix in M4
		return "new CheckBox() { public void setValue(Boolean value) { super.setValue(value == null ? Boolean.FALSE : value); } }";
	}

	public String getReadableName() {
		return new JavaSymbolName(name).getReadableSymbolName();
	}

	public String forEditView() {
		String initializer = "";

		if (isBoolean()) {
			initializer = " = " + getCheckboxSubtype();
		}

		if (isEnum() && !isCollection()) {
			initializer = String.format(" = new ValueListBox<%s>(%s)", type.getFullyQualifiedTypeName(), getRenderer());
		}

		if (isProxy()) {
			initializer = String.format(" = new ValueListBox<%1$s>(%2$s.instance(), new com.google.gwt.requestfactory.ui.client.EntityProxyKeyProvider<%1$s>())", type.getFullyQualifiedTypeName(), getProxyRendererType());
		}

		return String.format("@UiField %s %s %s", getEditor(), getName(), initializer);
	}

	public String forMobileListView(String rendererName) {
		return new StringBuilder("if (value.").append(getGetter()).append("() != null) {\n\t\t\t\tsb.appendEscaped(").append(rendererName).append(".render(value.").append(getGetter()).append("()));\n\t\t\t}").toString();
	}

	public boolean isProxy() {
		return ptmd != null && !isDate() && !isString() && !isPrimitive() && !isEnum() && !isCollection() && !isEmbeddable() && !type.getFullyQualifiedTypeName().equals("java.lang.Object");
	}

	public boolean isCollection() {
		return type != null && (type.getFullyQualifiedTypeName().equals("java.util.List") || type.getFullyQualifiedTypeName().equals("java.util.Set"));
	}

	boolean isEnum() {
		return ptmd != null && ptmd.getMemberHoldingTypeDetails() != null && ptmd.getMemberHoldingTypeDetails().getPhysicalTypeCategory() == PhysicalTypeCategory.ENUMERATION;
	}


	public boolean isEmbeddable() {
		if (ptmd != null && ptmd.getMemberHoldingTypeDetails() != null) {
			if (ptmd.getMemberHoldingTypeDetails() instanceof ClassOrInterfaceTypeDetails) {
				List<AnnotationMetadata> annotations = ptmd.getMemberHoldingTypeDetails().getAnnotations();
				for (AnnotationMetadata annotation : annotations) {
					if (annotation.getAnnotationType().equals(new JavaType("javax.persistence.Embeddable"))) {
						return true;
					}
				}
			}
		}

		return false;
	}

	public String getSetValuePickerMethod() {
		return "\tpublic void " + getSetValuePickerMethodName() + "(Collection<" + (isCollection() ? type.getParameters().get(0).getSimpleTypeName() : type.getSimpleTypeName()) + "> values) {\n" + "\t\t" + getName() + ".setAcceptableValues(values);\n" + "\t}\n";
	}

	String getSetValuePickerMethodName() {
		return "set" + StringUtils.capitalize(getName()) + "PickerValues";
	}

	public boolean isCollectionOfProxy() {
		return type.getParameters().size() != 0 && isCollection() && new GwtProxyProperty(projectMetadata, type.getParameters().get(0), ptmd).isProxy();
	}

	public JavaType getValueType() {
		if (isCollection()) {
			return type.getParameters().get(0);
		}
		return type;
	}
}