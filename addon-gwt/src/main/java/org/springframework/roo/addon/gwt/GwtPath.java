package org.springframework.roo.addon.gwt;

import java.io.File;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.model.JavaPackage;

public enum GwtPath {

    CLIENT("/client", "module/client/" + GwtPath.templateSelector), GWT_ROOT(
            "/", "module/" + GwtPath.templateSelector), IMAGES(
            "/client/style/images", "module/client/style/images/"
                    + GwtPath.wildCardSelector), LOCATOR("/server/locator",
            "module/server/locator/" + GwtPath.templateSelector), // GWT_REQUEST
    MANAGED("/client/managed", "module/client/managed/"
            + GwtPath.templateSelector), MANAGED_ACTIVITY(
            "/client/managed/activity", "module/client/managed/activity/"
                    + GwtPath.templateSelector), // GWT_SCAFFOLD
    MANAGED_REQUEST("/client/managed/request", "module/client/request/"
            + GwtPath.templateSelector), // GWT_SCAFFOLD_GENERATED
    MANAGED_UI("/client/managed/ui", "module/client/managed/ui/"
            + GwtPath.templateSelector), // GWT_SCAFFOLD_UI
    SCAFFOLD("/client/scaffold", "module/client/scaffold/"
            + GwtPath.templateSelector), SCAFFOLD_ACTIVITY(
            "/client/scaffold/activity", "module/client/scaffold/activity/"
                    + GwtPath.templateSelector), SCAFFOLD_GAE(
            "/client/scaffold/gae", "module/client/scaffold/gae/"
                    + GwtPath.templateSelector), SCAFFOLD_IOC(
            "/client/scaffold/ioc", "module/client/scaffold/ioc/"
                    + GwtPath.templateSelector), SCAFFOLD_PLACE(
            "/client/scaffold/place", "module/client/scaffold/place/"
                    + GwtPath.templateSelector), SCAFFOLD_REQUEST(
            "/client/scaffold/request", "module/client/scaffold/request/"
                    + GwtPath.templateSelector), SCAFFOLD_UI(
            "/client/scaffold/ui", "module/client/scaffold/ui/"
                    + GwtPath.templateSelector), SERVER("/server",
            "module/server/" + GwtPath.templateSelector), // IOC
    SERVER_GAE("/server/gae", "module/server/gae/" + GwtPath.templateSelector), // PLACE
    SHARED("/shared", "module/shared/" + GwtPath.templateSelector), SHARED_GAE(
            "/shared/gae", "module/shared/gae/" + GwtPath.templateSelector), SHARED_SCAFFOLD(
            "/shared/scaffold", "module/shared/scaffold/"
                    + GwtPath.templateSelector), STYLE("/client/style",
            "module/client/style/" + GwtPath.templateSelector), WEB("",
            "webapp/" + GwtPath.wildCardSelector);

    private static final String templateSelector = "*-template.*";
    private static final String wildCardSelector = "*";

    private final String segmentName;
    private final String sourceAntPath;

    /**
     * Constructor
     * 
     * @param segmentName
     * @param sourceAntPath the Ant-style path to the source files for this
     *            {@link GwtPath}, relative to the package in which this enum is
     *            located (required)
     */
    GwtPath(final String segmentName, final String sourceAntPath) {
        Validate.notBlank(sourceAntPath, "Source Ant path is required");
        this.segmentName = segmentName;
        this.sourceAntPath = sourceAntPath;
    }

    public String getPackagePath(final JavaPackage topLevelPackage) {
        return topLevelPackage.getFullyQualifiedPackageName().replace('.',
                File.separatorChar)
                + segmentName.replace('/', File.separatorChar);
    }

    /**
     * Package access for benefit of unit test
     * 
     * @return
     */
    String getSegmentName() {
        return segmentName;
    }

    public String getSourceAntPath() {
        return sourceAntPath;
    }

    public String packageName(final JavaPackage topLevelPackage) {
        if (WEB.equals(this)) {
            return "";
        }
        return topLevelPackage.getFullyQualifiedPackageName()
                + segmentName.replace('/', '.');
    }

    public String segmentPackage() {
        if (WEB.equals(this)) {
            return "";
        }
        return segmentName.substring(1).replace('/', '.');
    }
}
