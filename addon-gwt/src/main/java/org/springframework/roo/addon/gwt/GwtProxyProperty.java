/*
 * Copyright 2010 Google Inc.
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
package org.springframework.roo.addon.gwt;

import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.util.StringUtils;

class GwtProxyProperty {
  private final ProjectMetadata projectMetadata;
	private final String name;
	private final String displayName;
	private final String getter;
	private final JavaType type;

  private PhysicalTypeMetadata ptmd;

	public GwtProxyProperty(ProjectMetadata projectMetadata, MethodMetadata getterMethod, PhysicalTypeMetadata ptmd) {
	  this.projectMetadata = projectMetadata;
    this.getter = getterMethod.getMethodName().getSymbolName();
    this.type = getterMethod.getReturnType();
    this.ptmd = ptmd;
    
    String prefix = null;
    // Boolean properties are still not working, *and* for some reason
    // every entity thinks it has an isChanged method
//    if (getter.startsWith("is")) {
//      prefix = "is";
//    } else {
      prefix = "get";
//    }
    
    this.name = getter.substring(prefix.length(), prefix.length() +1).toLowerCase() + getter.substring(prefix.length() + 1);
    this.displayName = JavaSymbolName.getReadableSymbolName(name);
	}

  public static boolean isAccessor(MethodMetadata method) {
    String symbolName = method.getMethodName().getSymbolName();
    return symbolName.startsWith("get") /* || symbolName.startsWith("is") */;
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
		return type.isPrimitive() || isDate() || isString() || type.equals(JavaType.DOUBLE_OBJECT) || type.equals(JavaType.LONG_OBJECT) || type.equals(JavaType.INT_OBJECT) || isBoolean();
	}

	public boolean isString() {
		return type != null && type.equals(new JavaType("java.lang.String"));
	}

	public String getBinder() {
		if (type.equals(JavaType.DOUBLE_OBJECT)) {
			return "app:DoubleBox";
		}
		if (type.equals(JavaType.LONG_OBJECT)) {
			return "app:LongBox";
		}
		if (type.equals(JavaType.INT_OBJECT)) {
			return "app:IntegerBox";
		}
		return isDate() ? "d:DateBox" : isBoolean() ? "g:CheckBox" : isString() ? "g:TextBox" : "g:ValueListBox";
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
		if (isBoolean()) {
			return "(provided = true) CheckBox";
		}
		return isDate() ? "DateBox" : isString() ? "TextBox" : "(provided = true) ValueListBox<" + type.getFullyQualifiedTypeName() + ">";
	}

	public String getFormatter() {
		return isDate() ? "DateTimeFormat.getShortDateFormat().format(" : isProxy() ? getRendererType() + ".instance().render(" : "String.valueOf(";
	}

	public String getRenderer() {
		return isDate() ? "new DateTimeFormatRenderer(DateTimeFormat.getShortDateFormat())" : isPrimitive() || isEnum() ? "new AbstractRenderer<" + getType() + ">() {\n      public String render(" + getType() + " obj) {\n        return obj == null ? \"\" : String.valueOf(obj);\n      }    \n}" : getRendererType() + ".instance()";
	}

	String getRendererType() {
		return MirrorType.EDIT_RENDERER.getPath().packageName(projectMetadata) + "." + type.getSimpleTypeName() + "Renderer";
	}

	public String getCheckboxSubtype() {
		// TODO: Ugly hack, fix in M4
		return "new CheckBox() { public void setValue(Boolean value) { super.setValue(value == null ? Boolean.FALSE : value); } }";
	}

	public String getReadableName() {
		return new JavaSymbolName(name).getReadableSymbolName();
	}

	public String forDetailsView() {
		return new StringBuilder(getName()).append(".setInnerText(").append(getFormatter()).append("proxy.").append(getGetter()).append("()));").toString();
	}

	public String forEditView() {
		String initializer = "";

		if (isBoolean()) {
			initializer = " = " + getCheckboxSubtype();
		}

		if (isEnum()) {
			initializer = String.format(" = new ValueListBox<%s>(%s)", type.getFullyQualifiedTypeName(), getRenderer());
		}

		if (isProxy()) {
			initializer = String.format(" = new ValueListBox<%1$s>(%2$s.instance(), " + "new com.google.gwt.app.place.EntityProxyKeyProvider<%1$s>())", type.getFullyQualifiedTypeName(), getRendererType());
		}

		return String.format("@UiField %s %s %s", getEditor(), getName(), initializer);
	}

	public String forDetailsUIXml() {
		return new StringBuilder("<tr><td><div class='{style.label}'>").append(getReadableName()).append(":</div></td><td><span ui:field='").append(getName()).append("'></span></td></tr>").toString();
	}

	public String forListView(String proxy) {
	  return String.format("columns.add(new PropertyColumn<%s, %s>(\"%s\", \"%s\", %s.class, %s));", proxy, getType(), getName(), getDisplayName(), getType(), getRenderer());
	}

	public String getDisplayName() {
	  return displayName;
  }

  public String forEditUiXml() {
		return new StringBuilder("<tr><td><div class='{style.label}'>").append(getReadableName()).append(":</div></td><td><").append(getBinder()).append(" ui:field='").append(getName()).append("'></").append(getBinder()).append("></td></tr>").toString();
	}

	public boolean isProxy() {
		return !isDate() && !isString() && !isPrimitive() && !isEnum();
	}

	boolean isEnum() {
		return ptmd != null && ptmd.getPhysicalTypeDetails() != null && ptmd.getPhysicalTypeDetails().getPhysicalTypeCategory() == PhysicalTypeCategory.ENUMERATION;
	}

	public String getSetValuePickerMethod() {
		return "\tpublic void " + getSetValuePickerMethodName() + "(Collection<" + type.getSimpleTypeName() + "> values) {\n" + "\t\t" + getName() + ".setAcceptableValues(values);\n" + "\t}\n";
	}

	String getSetValuePickerMethodName() {
		return "set" + StringUtils.capitalize(getName()) + "PickerValues";
	}
}