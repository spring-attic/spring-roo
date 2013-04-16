package org.springframework.roo.addon.gwt;

import static org.springframework.roo.addon.gwt.GwtJavaType.ACCEPTS_ONE_WIDGET;
import static org.springframework.roo.addon.gwt.GwtJavaType.ENTITY_PROXY;
import static org.springframework.roo.addon.gwt.GwtJavaType.EVENT_BUS;
import static org.springframework.roo.addon.gwt.GwtJavaType.PLACE;
import static org.springframework.roo.addon.gwt.GwtJavaType.RECEIVER;
import static org.springframework.roo.model.JdkJavaType.COLLECTION;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

public enum GwtType {

    ACTIVITIES_MAPPER(GwtPath.MANAGED_ACTIVITY, true, "ActivitiesMapper",
            "activitiesMapper", "ActivitiesMapper", false, true, false),

    // Represents shared types. There is one such type per application using
    // Roo's GWT support
    APP_ENTITY_TYPES_PROCESSOR(GwtPath.MANAGED_REQUEST, false, "",
            "entityTypes", "ApplicationEntityTypesProcessor", false, false,
            true), APP_REQUEST_FACTORY(GwtPath.MANAGED_REQUEST, false, "",
            "requestFactory", "ApplicationRequestFactory", false, false, true), DETAIL_ACTIVITY(
            GwtPath.MANAGED_ACTIVITY, true, "DetailsActivity",
            "detailsActivity", "DetailsActivity", false, true, false), DETAILS_ACTIVITIES(
            GwtPath.MANAGED_ACTIVITY, false, "", "detailsActivities",
            "ApplicationDetailsActivities", false, true, false), DETAILS_VIEW(
            GwtPath.MANAGED_UI, true, "DetailsView", "detailsView",
            "DetailsView", false, false, false), DESKTOP_DETAILS_VIEW(
            GwtPath.MANAGED_UI_DESKTOP, true, "DesktopDetailsView",
            "desktopDetailsView", "DesktopDetailsView", true, true, false), EDIT_ACTIVITY(
            GwtPath.MANAGED_ACTIVITY, true, "EditActivity", "editActivity",
            "EditActivity", false, false, false), EDIT_ACTIVITY_WRAPPER(
            GwtPath.MANAGED_ACTIVITY, true, "EditActivityWrapper",
            "editActivityWrapper", "EditActivityWrapper", false, true, false), EDIT_RENDERER(
            GwtPath.MANAGED_UI_RENDERER, true, "ProxyRenderer", "renderer",
            "EditRenderer", false, false, false), EDIT_VIEW(GwtPath.MANAGED_UI,
            true, "EditView", "editView", "EditView", false, false, false), DESKTOP_EDIT_VIEW(
            GwtPath.MANAGED_UI_DESKTOP, true, "DesktopEditView",
            "desktopEditView", "DesktopEditView", true, true, false), IS_SCAFFOLD_MOBILE_ACTIVITY(
            GwtPath.SCAFFOLD_ACTIVITY, false, "", "isScaffoldMobileActivity",
            "IsScaffoldMobileActivity", false, false, false), LIST_ACTIVITY(
            GwtPath.MANAGED_ACTIVITY, true, "ListActivity", "listActivity",
            "ListActivity", false, true, false), LIST_EDITOR(
            GwtPath.MANAGED_UI_EDITOR, true, "ListEditor", "listEditor",
            "ListEditor", true, true, false), LIST_PLACE_RENDERER(
            GwtPath.MANAGED_UI_RENDERER, false, "", "listPlaceRenderer",
            "ApplicationListPlaceRenderer", false, true, false), DESKTOP_LIST_VIEW(
            GwtPath.MANAGED_UI_DESKTOP, true, "DesktopListView",
            "desktopListView", "DesktopListView", true, true, false), MASTER_ACTIVITIES(
            GwtPath.MANAGED_ACTIVITY, false, "", "masterActivities",
            "ApplicationMasterActivities", false, true, false),

    MOBILE_ACTIVITIES(GwtPath.MANAGED_ACTIVITY, false, "", "mobileActivities",
            "ScaffoldMobileActivities", false, false, false), MOBILE_DETAILS_VIEW(
            GwtPath.MANAGED_UI_MOBILE, true, "MobileDetailsView",
            "mobileDetailsView", "MobileDetailsView", true, true, false), MOBILE_EDIT_VIEW(
            GwtPath.MANAGED_UI_MOBILE, true, "MobileEditView",
            "mobileEditView", "MobileEditView", true, true, false), MOBILE_LIST_VIEW(
            GwtPath.MANAGED_UI_MOBILE, true, "MobileListView",
            "mobileListView", "MobileListView", false, true, false), MOBILE_PROXY_LIST_VIEW(
            GwtPath.SCAFFOLD_UI, false, "", "mobileProxyListView",
            "MobileProxyListView", false, false, false),

    // Represents mirror types classes. There are one of these for each entity
    // mirrored by Roo.
    PROXY(GwtPath.MANAGED_REQUEST, true, "Proxy", "proxy", null, false, false,
            true), REQUEST(GwtPath.MANAGED_REQUEST, true, "Request", "request",
            null, false, false, true), SCAFFOLD_APP(GwtPath.SCAFFOLD, false,
            "", "scaffoldApp", "ScaffoldApp", false, false, false), SCAFFOLD_MOBILE_APP(
            GwtPath.SCAFFOLD, false, "", "scaffoldMobileApp",
            "ScaffoldMobileApp", false, false, false), SET_EDITOR(
            GwtPath.MANAGED_UI_EDITOR, true, "SetEditor", "setEditor",
            "SetEditor", true, true, false);

    public static List<GwtType> getMirrorTypes() {
        final List<GwtType> mirrorTypes = new ArrayList<GwtType>();
        for (final GwtType gwtType : GwtType.values()) {
            if (gwtType.isMirrorType()) {
                mirrorTypes.add(gwtType);
            }
        }
        return mirrorTypes;
    }

    private boolean createAbstract = false;
    private final boolean createUiXml;
    private boolean mirrorType = false;
    private final String name;
    private boolean overwriteConcrete = false;
    private final GwtPath path;
    private final String suffix;
    private final String template;
    private List<JavaSymbolName> watchedFieldNames = new ArrayList<JavaSymbolName>();

    private Map<JavaSymbolName, List<JavaType>> watchedMethods = new LinkedHashMap<JavaSymbolName, List<JavaType>>();

    private GwtType(final GwtPath path, final boolean mirrorType,
            final String suffix, final String name, final String template,
            final boolean createUiXml, final boolean createAbstract,
            final boolean overwriteConcrete) {
        this.path = path;
        this.mirrorType = mirrorType;
        this.suffix = suffix;
        this.name = name;
        this.template = template;
        this.createUiXml = createUiXml;
        this.createAbstract = createAbstract;
        this.overwriteConcrete = overwriteConcrete;
    }

    private List<JavaSymbolName> convertToJavaSymbolNames(final String... names) {
        final List<JavaSymbolName> javaSymbolNames = new ArrayList<JavaSymbolName>();
        for (final String name : names) {
            if (!javaSymbolNames.contains(new JavaSymbolName(name))) {
                javaSymbolNames.add(new JavaSymbolName(name));
            }
        }
        return javaSymbolNames;
    }

    public void dynamicallyResolveFieldsToWatch(
            final Map<JavaSymbolName, GwtProxyProperty> proxyFieldTypeMap) {
        watchedFieldNames = resolveWatchedFieldNames(this);
        switch (this) {
        case DESKTOP_DETAILS_VIEW:
            watchedFieldNames.addAll(proxyFieldTypeMap.keySet());
            watchedFieldNames.addAll(convertToJavaSymbolNames("proxy",
                    "displayRenderer"));
            break;
        case MOBILE_DETAILS_VIEW:
            watchedFieldNames.addAll(proxyFieldTypeMap.keySet());
            watchedFieldNames.addAll(convertToJavaSymbolNames("proxy",
                    "displayRenderer"));
            break;
        case DESKTOP_EDIT_VIEW:
            watchedFieldNames.addAll(proxyFieldTypeMap.keySet());
            break;
        case MOBILE_EDIT_VIEW:
            watchedFieldNames.addAll(proxyFieldTypeMap.keySet());
            break;
        default:
            break;
        }
    }

    public void dynamicallyResolveMethodsToWatch(final JavaType proxy,
            final Map<JavaSymbolName, GwtProxyProperty> proxyFieldTypeMap,
            final JavaPackage topLevelPackage) {
        watchedMethods = resolveMethodsToWatch(this);
        switch (this) {
        case DESKTOP_DETAILS_VIEW:
            watchedMethods.put(new JavaSymbolName("setValue"),
                    Collections.singletonList(proxy));
            break;
        case MOBILE_DETAILS_VIEW:
            watchedMethods.put(new JavaSymbolName("setValue"),
                    Collections.singletonList(proxy));
            break;
        case DESKTOP_EDIT_VIEW:
            for (final GwtProxyProperty property : proxyFieldTypeMap.values()) {
                if (property.isEnum() || property.isProxy()
                        || property.isEmbeddable()
                        || property.isCollectionOfProxy()) {
                    final List<JavaType> params = new ArrayList<JavaType>();
                    final JavaType param = new JavaType(
                            COLLECTION.getFullyQualifiedTypeName(), 0,
                            DataType.TYPE, null,
                            Collections.singletonList(property.getValueType()));
                    params.add(param);
                    watchedMethods.put(
                            new JavaSymbolName(property
                                    .getSetValuePickerMethodName()), params);
                }
            }
            break;
        case MOBILE_EDIT_VIEW:
            for (final GwtProxyProperty property : proxyFieldTypeMap.values()) {
                if (property.isEnum() || property.isProxy()
                        || property.isEmbeddable()
                        || property.isCollectionOfProxy()) {
                    final List<JavaType> params = new ArrayList<JavaType>();
                    final JavaType param = new JavaType(
                            COLLECTION.getFullyQualifiedTypeName(), 0,
                            DataType.TYPE, null,
                            Collections.singletonList(property.getValueType()));
                    params.add(param);
                    watchedMethods.put(
                            new JavaSymbolName(property
                                    .getSetValuePickerMethodName()), params);
                }
            }
            break;
        case LIST_PLACE_RENDERER:
            for (final GwtProxyProperty property : proxyFieldTypeMap.values()) {
                if (property.isEnum() || property.isProxy()
                        || property.isEmbeddable()
                        || property.isCollectionOfProxy()) {
                    final List<JavaType> params = new ArrayList<JavaType>();
                    final JavaType param = new JavaType(
                            COLLECTION.getFullyQualifiedTypeName(), 0,
                            DataType.TYPE, null,
                            Collections.singletonList(property.getValueType()));
                    params.add(param);
                    watchedMethods.put(
                            new JavaSymbolName(property
                                    .getSetValuePickerMethodName()), params);
                }
            }
            watchedMethods.put(new JavaSymbolName("render"), Collections
                    .singletonList(new JavaType(topLevelPackage
                            .getFullyQualifiedPackageName()
                            + ".client.scaffold.place.ProxyListPlace")));
            break;
        case ACTIVITIES_MAPPER:
            final List<JavaType> params = new ArrayList<JavaType>();
            params.add(new JavaType(topLevelPackage
                    .getFullyQualifiedPackageName()
                    + ".client.scaffold.place.ProxyPlace"));
            watchedMethods.put(new JavaSymbolName("makeEditActivity"), params);
            watchedMethods.put(new JavaSymbolName("coerceId"), params);
            watchedMethods.put(new JavaSymbolName("makeCreateActivity"),
                    new ArrayList<JavaType>());
            break;
        case EDIT_RENDERER:
            watchedMethods.put(new JavaSymbolName("render"),
                    Collections.singletonList(proxy));
            break;
        default:
            break;
        }
    }

    public String getName() {
        return name;
    }

    public GwtPath getPath() {
        return path;
    }

    public List<GwtType> getReferences() {
        return resolveReferences(this);
    }

    public String getSuffix() {
        return suffix;
    }

    public String getTemplate() {
        return template;
    }

    public List<JavaSymbolName> getWatchedFieldNames() {
        return watchedFieldNames;
    }

    public List<JavaType> getWatchedInnerTypes() {
        return resolveInnerTypesToWatch(this);
    }

    public Map<JavaSymbolName, List<JavaType>> getWatchedMethods() {
        return watchedMethods;
    }

    public boolean isCreateAbstract() {
        return createAbstract;
    }

    public boolean isCreateUiXml() {
        return createUiXml;
    }

    public boolean isMirrorType() {
        return mirrorType;
    }

    public boolean isOverwriteConcrete() {
        return overwriteConcrete;
    }

    private List<JavaType> resolveInnerTypesToWatch(final GwtType type) {
        switch (type) {
        case EDIT_ACTIVITY_WRAPPER:
            return Arrays.asList(new JavaType("View"));
        default:
            return new ArrayList<JavaType>();
        }
    }

    public Map<JavaSymbolName, List<JavaType>> resolveMethodsToWatch(
            final GwtType type) {
        watchedMethods = new HashMap<JavaSymbolName, List<JavaType>>();
        switch (type) {
        case EDIT_ACTIVITY_WRAPPER:
            watchedMethods.put(new JavaSymbolName("start"),
                    Arrays.asList(ACCEPTS_ONE_WIDGET, EVENT_BUS));
            break;
        case DETAIL_ACTIVITY:
            watchedMethods.put(new JavaSymbolName("find"),
                    Arrays.asList(GwtUtils.getReceiverType(ENTITY_PROXY)));
            watchedMethods.put(new JavaSymbolName("deleteClicked"),
                    new ArrayList<JavaType>());
            break;
        case MOBILE_LIST_VIEW:
            watchedMethods.put(new JavaSymbolName("init"),
                    new ArrayList<JavaType>());
            break;
        case DESKTOP_LIST_VIEW:
            watchedMethods.put(new JavaSymbolName("init"),
                    new ArrayList<JavaType>());
            break;
        case MASTER_ACTIVITIES:
            watchedMethods.put(new JavaSymbolName("getActivity"),
                    Collections.singletonList(PLACE));
            break;
        case DETAILS_ACTIVITIES:
            watchedMethods.put(new JavaSymbolName("getActivity"),
                    Collections.singletonList(PLACE));
            break;
        case LIST_ACTIVITY:
            watchedMethods.put(new JavaSymbolName("fireCountRequest"),
                    Collections.singletonList(RECEIVER));
            break;
        default:
            break;
        }
        return watchedMethods;
    }

    private List<GwtType> resolveReferences(final GwtType type) {
        switch (type) {
        case ACTIVITIES_MAPPER:
            return Arrays.asList(GwtType.APP_REQUEST_FACTORY,
                    GwtType.SCAFFOLD_APP, GwtType.DETAIL_ACTIVITY,
                    GwtType.EDIT_ACTIVITY, GwtType.EDIT_ACTIVITY_WRAPPER,
                    GwtType.DESKTOP_LIST_VIEW, GwtType.DESKTOP_DETAILS_VIEW,
                    GwtType.MOBILE_DETAILS_VIEW, GwtType.DESKTOP_EDIT_VIEW,
                    GwtType.MOBILE_EDIT_VIEW, GwtType.REQUEST);
        case DETAIL_ACTIVITY:
            return Arrays.asList(GwtType.APP_REQUEST_FACTORY,
                    GwtType.IS_SCAFFOLD_MOBILE_ACTIVITY, GwtType.DETAILS_VIEW);
        case EDIT_ACTIVITY:
            return Arrays.asList(GwtType.EDIT_VIEW,
                    GwtType.APP_REQUEST_FACTORY, GwtType.REQUEST);
        case EDIT_ACTIVITY_WRAPPER:
            return Arrays.asList(GwtType.APP_REQUEST_FACTORY,
                    GwtType.IS_SCAFFOLD_MOBILE_ACTIVITY, GwtType.EDIT_VIEW);
        case LIST_ACTIVITY:
            return Arrays.asList(GwtType.APP_REQUEST_FACTORY,
                    GwtType.IS_SCAFFOLD_MOBILE_ACTIVITY,
                    GwtType.SCAFFOLD_MOBILE_APP);
        case MOBILE_LIST_VIEW:
            return Arrays.asList(GwtType.MOBILE_PROXY_LIST_VIEW,
                    GwtType.SCAFFOLD_MOBILE_APP);
        case DESKTOP_EDIT_VIEW:
            return Arrays.asList(GwtType.EDIT_ACTIVITY_WRAPPER,
                    GwtType.EDIT_VIEW);
        case MOBILE_EDIT_VIEW:
            return Arrays.asList(GwtType.EDIT_ACTIVITY_WRAPPER,
                    GwtType.EDIT_VIEW);
        case DESKTOP_DETAILS_VIEW:
            return Arrays.asList(GwtType.DETAILS_VIEW);
        case MOBILE_DETAILS_VIEW:
            return Arrays.asList(GwtType.DETAILS_VIEW);
        case LIST_PLACE_RENDERER:
            return Arrays.asList(GwtType.APP_ENTITY_TYPES_PROCESSOR);
        case MASTER_ACTIVITIES:
            return Arrays.asList(GwtType.APP_REQUEST_FACTORY,
                    GwtType.APP_ENTITY_TYPES_PROCESSOR, GwtType.SCAFFOLD_APP);
        case DETAILS_ACTIVITIES:
            return Arrays.asList(GwtType.APP_REQUEST_FACTORY,
                    GwtType.APP_ENTITY_TYPES_PROCESSOR);
        default:
            return new ArrayList<GwtType>();
        }
    }

    public List<JavaSymbolName> resolveWatchedFieldNames(final GwtType type) {
        watchedFieldNames = new ArrayList<JavaSymbolName>();
        switch (type) {
        case ACTIVITIES_MAPPER:
            watchedFieldNames = convertToJavaSymbolNames("factory",
                    "placeController");
            break;
        case EDIT_ACTIVITY_WRAPPER:
            watchedFieldNames = convertToJavaSymbolNames("wrapped", "view",
                    "requests");
            break;
        case DETAIL_ACTIVITY:
            watchedFieldNames = convertToJavaSymbolNames("requests", "proxyId",
                    "placeController", "display", "view");
            break;
        case LIST_ACTIVITY:
            watchedFieldNames = convertToJavaSymbolNames("requests");
            break;
        case MOBILE_LIST_VIEW:
            watchedFieldNames = convertToJavaSymbolNames("paths");
            break;
        case DESKTOP_LIST_VIEW:
            watchedFieldNames = convertToJavaSymbolNames("table", "paths");
            break;
        case MASTER_ACTIVITIES:
            watchedFieldNames = convertToJavaSymbolNames("requests",
                    "placeController");
            break;
        case DETAILS_ACTIVITIES:
            watchedFieldNames = convertToJavaSymbolNames("requests",
                    "placeController");
            break;
        default:
            watchedFieldNames = new ArrayList<JavaSymbolName>();
        }
        return watchedFieldNames;
    }

    public void setWatchedMethods(
            final Map<JavaSymbolName, List<JavaType>> watchedMethods) {
        this.watchedMethods = watchedMethods;
    }
}
