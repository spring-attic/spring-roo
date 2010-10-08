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
	private final String getter;
	private final JavaType type;

  private PhysicalTypeMetadata ptmd;

	public GwtProxyProperty(ProjectMetadata projectMetadata, MethodMetadata getterMethod, PhysicalTypeMetadata ptmd) {
          this.projectMetadata = projectMetadata;
          this.type = getterMethod.getReturnType();
          this.ptmd = ptmd;
    this.getter = getterMethod.getMethodName().getSymbolName();
    
    String prefix = null;
    prefix = "get";
    
    this.name = getter.substring(prefix.length(), prefix.length() +1).toLowerCase() + getter.substring(prefix.length() + 1);
	}

  public GwtProxyProperty(ProjectMetadata projectMetadata, JavaType type, PhysicalTypeMetadata ptmd) {
	  this.projectMetadata = projectMetadata;
          this.type = type;
          this.ptmd = ptmd;
          this.getter = null;
          this.name = null;
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
		return type != null && type.equals(new JavaType("java.lang.String"));
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
    String typeName = type.getParameters().get(0).getSimpleTypeName();
    if (typeName.endsWith(MirrorType.PROXY.getSuffix())) {
      typeName = typeName.substring(0, typeName.length() - MirrorType.PROXY.getSuffix().length());
    }
    return typeName + MirrorType
        .SET_EDITOR.getSuffix();
  }

  public JavaType getSetEditorType() {
    return new JavaType(MirrorType.SET_EDITOR.getPath().packageName(projectMetadata) + "." + getSetEditor());
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
           JavaType arg = type.getParameters().get(0);
           return GwtPath.PLACE.packageName(projectMetadata) + ".CollectionRenderer.of(" + new GwtProxyProperty(projectMetadata, arg, ptmd).getRenderer() + ")";
        }

	public String getFormatter() {
		return isCollectionOfProxy() ? getCollectionRenderer()+".render(" : isDate() ? "DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_SHORT).format" : isProxy() ? getProxyRendererType() + ".instance().render" : "String.valueOf";
	}

	public String getRenderer() {
		return isCollection() ? getCollectionRenderer() : isDate() ? "new DateTimeFormatRenderer(DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_SHORT))" : isPrimitive() || isEnum() ? "new AbstractRenderer<" + getType() + ">() {\n        public String render(" + getType() + " obj) {\n          return obj == null ? \"\" : String.valueOf(obj);\n        }\n      }" : getProxyRendererType() + ".instance()";
	}

	String getProxyRendererType() {
               return getProxyRendererType(projectMetadata, isCollectionOfProxy() ? type.getParameters().get(0) : type);
        }
  
        public static String getProxyRendererType(ProjectMetadata pmd, JavaType javaType) {
		return MirrorType.EDIT_RENDERER.getPath().packageName(pmd)+ "." + javaType.getSimpleTypeName() + "Renderer";
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

		if (isEnum()) {
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
		return !isDate() && !isString() && !isPrimitive() && !isEnum() && !isCollection();
	}

  private boolean isCollection() {
    return type != null && (type.equals(new JavaType("java.util.List")) || type.equals(new JavaType("java.util.Set")));
  }

  boolean isEnum() {
		return ptmd != null && ptmd.getPhysicalTypeDetails() != null && ptmd.getPhysicalTypeDetails().getPhysicalTypeCategory() == PhysicalTypeCategory.ENUMERATION;
	}

	public String getSetValuePickerMethod() {
		return "\tpublic void " + getSetValuePickerMethodName() + "(Collection<" + (isCollection() ? type.getParameters().get(0).getSimpleTypeName() : type.getSimpleTypeName()) + "> values) {\n" + "\t\t" + getName() + ".setAcceptableValues(values);\n" + "\t}\n";
	}

	String getSetValuePickerMethodName() {
		return "set" + StringUtils.capitalize(getName()) + "PickerValues";
	}

  public boolean isCollectionOfProxy() {
    return isCollection() && new GwtProxyProperty(projectMetadata, type.getParameters().get(0), ptmd).isProxy();
  }

  public JavaType getValueType() {
    if (isCollection()) {
      return type.getParameters().get(0);
    }
    return type;
  }
}