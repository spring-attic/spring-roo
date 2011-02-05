package org.springframework.roo.addon.gwt;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ProjectMetadata;

import java.util.*;

public enum GwtType {

	//Represents mirror types classes. There are one of these for each entity mirrored by Roo.
	PROXY(GwtPath.MANAGED_REQUEST, true, "Proxy", "proxy", null, false, false),
	REQUEST(GwtPath.MANAGED_REQUEST, true, "Request", "request", null, false, false),
	ACTIVITIES_MAPPER(GwtPath.MANAGED_ACTIVITY, true, "ActivitiesMapper", "activitiesMapper", "ActivitiesMapper", false, false),
	DETAIL_ACTIVITY(GwtPath.MANAGED_ACTIVITY, true, "DetailsActivity", "detailsActivity", "DetailsActivity", false, true),
	EDIT_ACTIVITY_WRAPPER(GwtPath.MANAGED_ACTIVITY, true, "EditActivityWrapper", "editActivityWrapper", "EditActivityWrapper", false, true),
	LIST_ACTIVITY(GwtPath.MANAGED_ACTIVITY, true, "ListActivity", "listActivity", "ListActivity", false, false),
	LIST_VIEW(GwtPath.MANAGED_UI, true, "ListView", "listView", "ListView", true, true),
	MOBILE_LIST_VIEW(GwtPath.MANAGED_UI, true, "MobileListView", "mobileListView", "MobileListView", false, true),
	DETAILS_VIEW(GwtPath.MANAGED_UI, true, "DetailsView", "detailsView", "DetailsView", true, true),
	MOBILE_DETAILS_VIEW(GwtPath.MANAGED_UI, true, "MobileDetailsView", "mobileDetailsView", "MobileDetailsView", true, true),
	EDIT_VIEW(GwtPath.MANAGED_UI, true, "EditView", "editView", "EditView", true, true),
	MOBILE_EDIT_VIEW(GwtPath.MANAGED_UI, true, "MobileEditView", "mobileEditView", "MobileEditView", true, true),
	EDIT_RENDERER(GwtPath.MANAGED_UI, true, "ProxyRenderer", "renderer", "EditRenderer", false, false),
	SET_EDITOR(GwtPath.MANAGED_UI, true, "SetEditor", "setEditor", "SetEditor", true, true),
	LIST_EDITOR(GwtPath.MANAGED_UI, true, "ListEditor", "listEditor", "ListEditor", true, true),

	APP_ENTITY_TYPES_PROCESSOR(GwtPath.MANAGED_REQUEST, false, "", "entityTypes", "ApplicationEntityTypesProcessor", false, false),
	APP_REQUEST_FACTORY(GwtPath.MANAGED_REQUEST, false, "", "requestFactory", "ApplicationRequestFactory", false, false),
	LIST_PLACE_RENDERER(GwtPath.MANAGED_UI, false, "", "listPlaceRenderer", "ApplicationListPlaceRenderer", false, true),
	SCAFFOLD_APP(GwtPath.SCAFFOLD, false, "", "scaffoldApp", "ScaffoldApp", false, false),
	SCAFFOLD_MOBILE_APP(GwtPath.SCAFFOLD, false, "", "scaffoldMobileApp", "ScaffoldMobileApp", false, false),
	IS_SCAFFOLD_MOBILE_ACTIVITY(GwtPath.SCAFFOLD_ACTIVITY, false, "", "isScaffoldMobileActivity", "IsScaffoldMobileActivity", false, false),
	MOBILE_PROXY_LIST_VIEW(GwtPath.SCAFFOLD_UI, false, "", "mobileProxyListView", "MobileProxyListView", false, false),
	MASTER_ACTIVITIES(GwtPath.MANAGED_ACTIVITY, false, "", "masterActivities", "ApplicationMasterActivities", false, true),
	DETAILS_ACTIVITIES(GwtPath.MANAGED_ACTIVITY, false, "", "detailsActivities", "ApplicationDetailsActivities", false, true),
	MOBILE_ACTIVITIES(GwtPath.MANAGED_ACTIVITY, false, "", "mobileActivities", "ScaffoldMobileActivities", false, false);

	private final GwtPath path;
	private final String suffix;
	private final String name;
	private final String template;
	private List<JavaSymbolName> watchedFieldNames = new ArrayList<JavaSymbolName>();
	private Map<JavaSymbolName, List<JavaType>> watchedMethods = new HashMap<JavaSymbolName, List<JavaType>>();
	private List<JavaType> watchedInnerTypes = new ArrayList<JavaType>();
	private boolean createAbstract = false;
	private boolean overwriteConcrete = false;
	private boolean createUiXml;
	private boolean mirrorType = false;
	private List<GwtType> references;

	private GwtType(GwtPath path, boolean mirrorType, String suffix, String name, String template, boolean createUiXml, boolean createAbstract) {
		this.path = path;
		this.mirrorType = mirrorType;
		this.suffix = suffix;
		this.name = name;
		this.template = template;
		this.createUiXml = createUiXml;
		//this.references = resolveReferences(this);
		this.createAbstract = createAbstract;
	}

	private List<GwtType> resolveReferences(GwtType type) {
		switch (type) {
			case ACTIVITIES_MAPPER:
				return Arrays.asList(GwtType.APP_REQUEST_FACTORY, GwtType.SCAFFOLD_APP, GwtType.DETAIL_ACTIVITY, GwtType.EDIT_ACTIVITY_WRAPPER,
						GwtType.LIST_VIEW, GwtType.DETAILS_VIEW, GwtType.MOBILE_DETAILS_VIEW, GwtType.EDIT_VIEW, GwtType.MOBILE_EDIT_VIEW,
						GwtType.REQUEST);
			case DETAIL_ACTIVITY:
				return Arrays.asList(GwtType.APP_REQUEST_FACTORY, GwtType.IS_SCAFFOLD_MOBILE_ACTIVITY);
			case EDIT_ACTIVITY_WRAPPER:
				return Arrays.asList(GwtType.APP_REQUEST_FACTORY, GwtType.IS_SCAFFOLD_MOBILE_ACTIVITY);
			case LIST_ACTIVITY:
				return Arrays.asList(GwtType.APP_REQUEST_FACTORY, GwtType.IS_SCAFFOLD_MOBILE_ACTIVITY, GwtType.SCAFFOLD_MOBILE_APP);
			case MOBILE_LIST_VIEW:
				return Arrays.asList(GwtType.MOBILE_PROXY_LIST_VIEW, GwtType.SCAFFOLD_MOBILE_APP);
			case EDIT_VIEW:
				return Arrays.asList(GwtType.EDIT_ACTIVITY_WRAPPER);
			case MOBILE_EDIT_VIEW:
				return Arrays.asList(GwtType.EDIT_ACTIVITY_WRAPPER);
			case LIST_PLACE_RENDERER:
				return Arrays.asList(GwtType.APP_ENTITY_TYPES_PROCESSOR);
			case MASTER_ACTIVITIES:
				return Arrays.asList(GwtType.APP_REQUEST_FACTORY, GwtType.APP_ENTITY_TYPES_PROCESSOR, GwtType.SCAFFOLD_APP);
			case DETAILS_ACTIVITIES:
				return Arrays.asList(GwtType.APP_REQUEST_FACTORY, GwtType.APP_ENTITY_TYPES_PROCESSOR);
			default:
				return new ArrayList<GwtType>();
		}

	}

	private List<JavaSymbolName> resolveWatchedFieldNames(GwtType type) {
		switch (type) {
			case EDIT_ACTIVITY_WRAPPER:
				return convertToJavaSymbolNames("wrapped", "view", "requests");
			case DETAIL_ACTIVITY:
				return convertToJavaSymbolNames("requests", "proxyId");
			case MOBILE_LIST_VIEW:
				return convertToJavaSymbolNames("paths");
			case LIST_VIEW:
				return convertToJavaSymbolNames("table", "paths");
			default:
				return new ArrayList<JavaSymbolName>();

		}
	}

	private Map<JavaSymbolName, List<JavaType>> resolveMethodsToWatch(GwtType type) {
		Map<JavaSymbolName, List<JavaType>> methodsToWatch = new HashMap<JavaSymbolName, List<JavaType>>();
		switch (type) {
			case EDIT_ACTIVITY_WRAPPER:
				List<JavaType> params = new ArrayList<JavaType>();
				params.add(new JavaType("com.google.gwt.user.client.ui.AcceptsOneWidget"));
				params.add(new JavaType("com.google.gwt.event.shared.EventBus"));
				methodsToWatch.put(new JavaSymbolName("start"), params);
				break;
			case DETAIL_ACTIVITY:
				params = Arrays.asList(new JavaType("com.google.gwt.requestfactory.shared.Receiver", 0, DataType.TYPE, null, Collections.singletonList(new JavaType("com.google.gwt.requestfactory.shared.EntityProxy"))));
				methodsToWatch.put(new JavaSymbolName("find"), params);
				break;
			case MOBILE_LIST_VIEW:
				methodsToWatch.put(new JavaSymbolName("init"), new ArrayList<JavaType>());
				break;
			case LIST_VIEW:
				methodsToWatch.put(new JavaSymbolName("init"), new ArrayList<JavaType>());
				break;
		}

		return methodsToWatch;

	}

	public void dynamicallyResolveMethodsToWatch(JavaType proxy, Map<JavaSymbolName, GwtProxyProperty> proxyFieldTypeMap, ProjectMetadata projectMetadata) {
		watchedMethods = resolveMethodsToWatch(this);
		switch (this) {
			case DETAILS_VIEW:
				watchedMethods.put(new JavaSymbolName("setValue"), Collections.singletonList(proxy));
				break;
			case MOBILE_DETAILS_VIEW:
				watchedMethods.put(new JavaSymbolName("setValue"), Collections.singletonList(proxy));
				break;
			case EDIT_VIEW:
				for (GwtProxyProperty property : proxyFieldTypeMap.values()) {
					if (property.isEnum() || property.isProxy() || property.isEmbeddable() || property.isCollectionOfProxy()) {
						List<JavaType> params = new ArrayList<JavaType>();
						JavaType param = new JavaType("java.util.Collection", 0, DataType.TYPE, null, Collections.singletonList(property.getPropertyType()));
						params.add(param);
						watchedMethods.put(new JavaSymbolName(property.getSetValuePickerMethodName()), params);
					}
				}
				break;
			case MOBILE_EDIT_VIEW:
				for (GwtProxyProperty property : proxyFieldTypeMap.values()) {
					if (property.isEnum() || property.isProxy() || property.isEmbeddable() || property.isCollectionOfProxy()) {
						List<JavaType> params = new ArrayList<JavaType>();
						JavaType param = new JavaType("java.util.Collection", 0, DataType.TYPE, null, Collections.singletonList(property.getPropertyType()));
						params.add(param);
						watchedMethods.put(new JavaSymbolName(property.getSetValuePickerMethodName()), params);
					}
				}
				break;
			case EDIT_RENDERER:
				for (GwtProxyProperty property : proxyFieldTypeMap.values()) {
					if (property.isEnum() || property.isProxy() || property.isEmbeddable() || property.isCollectionOfProxy()) {
						List<JavaType> params = new ArrayList<JavaType>();
						JavaType param = new JavaType("java.util.Collection", 0, DataType.TYPE, null, Collections.singletonList(property.getPropertyType()));
						params.add(param);
						watchedMethods.put(new JavaSymbolName(property.getSetValuePickerMethodName()), params);
					}
				}
				watchedMethods.put(new JavaSymbolName("render"), Collections.singletonList(new JavaType(projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName() + ".client.scaffold.place.ProxyListPlace")));
				break;
		}
	}

	public void dynamicallyResolveFieldsToWatch(Map<JavaSymbolName, GwtProxyProperty> proxyFieldTypeMap) {
		watchedFieldNames = resolveWatchedFieldNames(this);
		switch (this) {
			case DETAILS_VIEW:
				watchedFieldNames.addAll(proxyFieldTypeMap.keySet());
				watchedFieldNames.addAll(convertToJavaSymbolNames("proxy", "displayRenderer"));
				break;
			case MOBILE_DETAILS_VIEW:
				watchedFieldNames.addAll(proxyFieldTypeMap.keySet());
				watchedFieldNames.addAll(convertToJavaSymbolNames("proxy", "displayRenderer"));
				break;
			case EDIT_VIEW:
				watchedFieldNames.addAll(proxyFieldTypeMap.keySet());
				break;
			case MOBILE_EDIT_VIEW:
				watchedFieldNames.addAll(proxyFieldTypeMap.keySet());
				break;

		}
	}

	private List<JavaType> resolveInnerTypesToWatch(GwtType type) {
		switch (type) {
			case EDIT_ACTIVITY_WRAPPER:
				return Arrays.asList(new JavaType("View"));
			default:
				return new ArrayList<JavaType>();
		}
	}

	private List<JavaSymbolName> convertToJavaSymbolNames(String... names) {
		List<JavaSymbolName> javaSymbolNames = new ArrayList<JavaSymbolName>();
		for (String name : names) {
			javaSymbolNames.add(new JavaSymbolName(name));
		}
		return javaSymbolNames;
	}

	public static List<GwtType> getMirrorTypes() {
		List<GwtType> mirrorTypes = new ArrayList<GwtType>();
		for (GwtType gwtType : GwtType.values()) {
			if (gwtType.isMirrorType()) {
				mirrorTypes.add(gwtType);
			}
		}
		return mirrorTypes;
	}

	public GwtPath getPath() {
		return path;
	}

	public String getSuffix() {
		return suffix;
	}

	public String getName() {
		return name;
	}

	public String getTemplate() {
		return template;
	}

	public List<JavaSymbolName> getWatchedFieldNames() {
		return watchedFieldNames;
	}

	public void setWatchedFieldNames(ArrayList<JavaSymbolName> watchedFieldNames) {
		this.watchedFieldNames = watchedFieldNames;
	}

	public Map<JavaSymbolName, List<JavaType>> getWatchedMethods() {
		return watchedMethods;
	}

	public void setWatchedMethods(HashMap<JavaSymbolName, List<JavaType>> watchedMethods) {
		this.watchedMethods = watchedMethods;
	}

	public List<JavaType> getWatchedInnerTypes() {
		return resolveInnerTypesToWatch(this);
	}

	public void setWatchedInnerTypes(List<JavaType> watchedInnerTypes) {
		this.watchedInnerTypes = watchedInnerTypes;
	}

	public boolean isCreateAbstract() {
		return createAbstract;
	}

	public void setCreateAbstract(boolean createAbstract) {
		this.createAbstract = createAbstract;
	}

	public boolean isOverwriteConcrete() {
		return overwriteConcrete;
	}

	public void setOverwriteConcrete(boolean overwriteConcrete) {
		this.overwriteConcrete = overwriteConcrete;
	}

	public List<GwtType> getReferences() {
		return resolveReferences(this);
	}

	public boolean isCreateUiXml() {
		return createUiXml;
	}

	public boolean isMirrorType() {
		return mirrorType;
	}
}
