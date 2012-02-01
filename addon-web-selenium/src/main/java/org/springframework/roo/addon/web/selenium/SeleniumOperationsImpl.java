package org.springframework.roo.addon.web.selenium;

import static org.springframework.roo.model.JavaType.LONG_OBJECT;
import static org.springframework.roo.model.JdkJavaType.BIG_DECIMAL;
import static org.springframework.roo.model.Jsr303JavaType.FUTURE;
import static org.springframework.roo.model.Jsr303JavaType.MIN;
import static org.springframework.roo.model.Jsr303JavaType.PAST;
import static org.springframework.roo.model.SpringJavaType.DATE_TIME_FORMAT;

import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.web.mvc.controller.details.WebMetadataService;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldMetadata;
import org.springframework.roo.addon.web.mvc.jsp.menu.MenuOperations;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.operations.DateTime;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Implementation of {@link SeleniumOperations}.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component
@Service
public class SeleniumOperationsImpl implements SeleniumOperations {

    private static final Logger LOGGER = HandlerUtils
            .getLogger(SeleniumOperationsImpl.class);

    @Reference private FileManager fileManager;
    @Reference private MemberDetailsScanner memberDetailsScanner;
    @Reference private MenuOperations menuOperations;
    @Reference private MetadataService metadataService;
    @Reference private PathResolver pathResolver;
    @Reference private PersistenceMemberLocator persistenceMemberLocator;
    @Reference private ProjectOperations projectOperations;
    @Reference private TypeLocationService typeLocationService;
    @Reference private WebMetadataService webMetadataService;

    private Node clickAndWaitCommand(final Document document,
            final String linkTarget) {
        final Node tr = document.createElement("tr");

        final Node td1 = tr.appendChild(document.createElement("td"));
        td1.setTextContent("clickAndWait");

        final Node td2 = tr.appendChild(document.createElement("td"));
        td2.setTextContent(linkTarget);

        final Node td3 = tr.appendChild(document.createElement("td"));
        td3.setTextContent(" ");

        return tr;
    }

    private String convertToInitializer(final FieldMetadata field) {
        String initializer = " ";
        short index = 1;
        final AnnotationMetadata min = MemberFindingUtils.getAnnotationOfType(
                field.getAnnotations(), MIN);
        if (min != null) {
            final AnnotationAttributeValue<?> value = min
                    .getAttribute(new JavaSymbolName("value"));
            if (value != null) {
                index = new Short(value.getValue().toString());
            }
        }
        final JavaType fieldType = field.getFieldType();
        if (field.getFieldName().getSymbolName().contains("email")
                || field.getFieldName().getSymbolName().contains("Email")) {
            initializer = "some@email.com";
        }
        else if (fieldType.equals(JavaType.STRING)) {
            initializer = "some"
                    + field.getFieldName()
                            .getSymbolNameCapitalisedFirstLetter() + index;
        }
        else if (fieldType.equals(new JavaType(Date.class.getName()))
                || fieldType.equals(new JavaType(Calendar.class.getName()))) {
            final Calendar cal = Calendar.getInstance();
            AnnotationMetadata dateTimeFormat = null;
            String style = null;
            if ((dateTimeFormat = MemberFindingUtils.getAnnotationOfType(
                    field.getAnnotations(), DATE_TIME_FORMAT)) != null) {
                final AnnotationAttributeValue<?> value = dateTimeFormat
                        .getAttribute(new JavaSymbolName("style"));
                if (value != null) {
                    style = value.getValue().toString();
                }
            }
            if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(),
                    PAST) != null) {
                cal.add(Calendar.YEAR, -1);
                cal.add(Calendar.MONTH, -1);
                cal.add(Calendar.DAY_OF_MONTH, -1);
            }
            else if (MemberFindingUtils.getAnnotationOfType(
                    field.getAnnotations(), FUTURE) != null) {
                cal.add(Calendar.YEAR, 1);
                cal.add(Calendar.MONTH, 1);
                cal.add(Calendar.DAY_OF_MONTH, 1);
            }
            if (style != null) {
                if (style.startsWith("-")) {
                    initializer = ((SimpleDateFormat) DateFormat
                            .getTimeInstance(
                                    DateTime.parseDateFormat(style.charAt(1)),
                                    Locale.getDefault())).format(cal.getTime());
                }
                else if (style.endsWith("-")) {
                    initializer = ((SimpleDateFormat) DateFormat
                            .getDateInstance(
                                    DateTime.parseDateFormat(style.charAt(0)),
                                    Locale.getDefault())).format(cal.getTime());
                }
                else {
                    initializer = ((SimpleDateFormat) DateFormat
                            .getDateTimeInstance(
                                    DateTime.parseDateFormat(style.charAt(0)),
                                    DateTime.parseDateFormat(style.charAt(1)),
                                    Locale.getDefault())).format(cal.getTime());
                }
            }
            else {
                initializer = ((SimpleDateFormat) DateFormat.getDateInstance(
                        DateFormat.SHORT, Locale.getDefault())).format(cal
                        .getTime());
            }

        }
        else if (fieldType.equals(JavaType.BOOLEAN_OBJECT)
                || fieldType.equals(JavaType.BOOLEAN_PRIMITIVE)) {
            initializer = Boolean.valueOf(false).toString();
        }
        else if (fieldType.equals(JavaType.INT_OBJECT)
                || fieldType.equals(JavaType.INT_PRIMITIVE)) {
            initializer = Integer.valueOf(index).toString();
        }
        else if (fieldType.equals(JavaType.DOUBLE_OBJECT)
                || fieldType.equals(JavaType.DOUBLE_PRIMITIVE)) {
            initializer = Double.toString(index);
        }
        else if (fieldType.equals(JavaType.FLOAT_OBJECT)
                || fieldType.equals(JavaType.FLOAT_PRIMITIVE)) {
            initializer = Float.toString(index);
        }
        else if (fieldType.equals(LONG_OBJECT)
                || fieldType.equals(JavaType.LONG_PRIMITIVE)) {
            initializer = Long.valueOf(index).toString();
        }
        else if (fieldType.equals(JavaType.SHORT_OBJECT)
                || fieldType.equals(JavaType.SHORT_PRIMITIVE)) {
            initializer = Short.valueOf(index).toString();
        }
        else if (fieldType.equals(BIG_DECIMAL)) {
            initializer = new BigDecimal(index).toString();
        }
        return initializer;
    }

    /**
     * Creates a new Selenium testcase
     * 
     * @param controller the JavaType of the controller under test (required)
     * @param name the name of the test case (optional)
     */
    public void generateTest(final JavaType controller, String name,
            String serverURL) {
        Validate.notNull(controller, "Controller type required");

        final ClassOrInterfaceTypeDetails controllerTypeDetails = typeLocationService
                .getTypeDetails(controller);
        Validate.notNull(controllerTypeDetails,
                "Class or interface type details for type '" + controller
                        + "' could not be resolved");

        final LogicalPath path = PhysicalTypeIdentifier
                .getPath(controllerTypeDetails.getDeclaredByMetadataId());
        final String webScaffoldMetadataIdentifier = WebScaffoldMetadata
                .createIdentifier(controller, path);
        final WebScaffoldMetadata webScaffoldMetadata = (WebScaffoldMetadata) metadataService
                .get(webScaffoldMetadataIdentifier);
        Validate.notNull(
                webScaffoldMetadata,
                "Web controller '"
                        + controller.getFullyQualifiedTypeName()
                        + "' does not appear to be an automatic, scaffolded controller");

        // We abort the creation of a selenium test if the controller does not
        // allow the creation of new instances for the form backing object
        if (!webScaffoldMetadata.getAnnotationValues().isCreate()) {
            LOGGER.warning("The controller you specified does not allow the creation of new instances of the form backing object. No Selenium tests created.");
            return;
        }

        if (!serverURL.endsWith("/")) {
            serverURL = serverURL + "/";
        }

        final JavaType formBackingType = webScaffoldMetadata
                .getAnnotationValues().getFormBackingObject();
        final String relativeTestFilePath = "selenium/test-"
                + formBackingType.getSimpleTypeName().toLowerCase() + ".xhtml";
        final String seleniumPath = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, relativeTestFilePath);

        final InputStream templateInputStream = FileUtils.getInputStream(
                getClass(), "selenium-template.xhtml");
        Validate.notNull(templateInputStream,
                "Could not acquire selenium.xhtml template");
        final Document document = XmlUtils.readXml(templateInputStream);

        final Element root = (Element) document.getLastChild();
        if (root == null || !"html".equals(root.getNodeName())) {
            throw new IllegalArgumentException(
                    "Could not parse selenium test case template file!");
        }

        name = name != null ? name : "Selenium test for "
                + controller.getSimpleTypeName();
        XmlUtils.findRequiredElement("/html/head/title", root).setTextContent(
                name);

        XmlUtils.findRequiredElement("/html/body/table/thead/tr/td", root)
                .setTextContent(name);

        final Element tbody = XmlUtils.findRequiredElement(
                "/html/body/table/tbody", root);
        tbody.appendChild(openCommand(
                document,
                serverURL
                        + projectOperations.getProjectName(projectOperations
                                .getFocusedModuleName()) + "/"
                        + webScaffoldMetadata.getAnnotationValues().getPath()
                        + "?form"));

        final ClassOrInterfaceTypeDetails formBackingTypeDetails = typeLocationService
                .getTypeDetails(formBackingType);
        Validate.notNull(formBackingType,
                "Class or interface type details for type '" + formBackingType
                        + "' could not be resolved");
        final MemberDetails memberDetails = memberDetailsScanner
                .getMemberDetails(getClass().getName(), formBackingTypeDetails);

        // Add composite PK identifier fields if needed
        for (final FieldMetadata field : persistenceMemberLocator
                .getEmbeddedIdentifierFields(formBackingType)) {
            final JavaType fieldType = field.getFieldType();
            if (!fieldType.isCommonCollectionType()
                    && !isSpecialType(fieldType)) {
                final FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(
                        field);
                final String fieldName = field.getFieldName().getSymbolName();
                fieldBuilder.setFieldName(new JavaSymbolName(fieldName + "."
                        + fieldName));
                tbody.appendChild(typeCommand(document, fieldBuilder.build()));
            }
        }

        // Add all other fields
        final List<FieldMetadata> fields = webMetadataService
                .getScaffoldEligibleFieldMetadata(formBackingType,
                        memberDetails, null);
        for (final FieldMetadata field : fields) {
            final JavaType fieldType = field.getFieldType();
            if (!fieldType.isCommonCollectionType()
                    && !isSpecialType(fieldType)) {
                tbody.appendChild(typeCommand(document, field));
            }
        }

        tbody.appendChild(clickAndWaitCommand(document,
                "//input[@id = 'proceed']"));

        // Add verifications for all other fields
        for (final FieldMetadata field : fields) {
            final JavaType fieldType = field.getFieldType();
            if (!fieldType.isCommonCollectionType()
                    && !isSpecialType(fieldType)) {
                tbody.appendChild(verifyTextCommand(document, formBackingType,
                        field));
            }
        }

        fileManager.createOrUpdateTextFileIfRequired(seleniumPath,
                XmlUtils.nodeToString(document), false);

        manageTestSuite(relativeTestFilePath, name, serverURL);

        // Install selenium-maven-plugin
        installMavenPlugin();
    }

    private void installMavenPlugin() {
        // Stop if the plugin is already installed
        for (final Plugin plugin : projectOperations.getFocusedModule()
                .getBuildPlugins()) {
            if (plugin.getArtifactId().equals("selenium-maven-plugin")) {
                return;
            }
        }

        final Element configuration = XmlUtils.getConfiguration(getClass());
        final Element plugin = XmlUtils.findFirstElement(
                "/configuration/selenium/plugin", configuration);

        // Now install the plugin itself
        if (plugin != null) {
            projectOperations.addBuildPlugin(
                    projectOperations.getFocusedModuleName(),
                    new Plugin(plugin));
        }
    }

    public boolean isSeleniumInstallationPossible() {
        return projectOperations.isFocusedProjectAvailable()
                && projectOperations.isFeatureInstalled(FeatureNames.MVC);
    }

    private boolean isSpecialType(final JavaType javaType) {
        return typeLocationService.isInProject(javaType);
    }

    private void manageTestSuite(final String testPath, final String name,
            final String serverURL) {
        final String relativeTestFilePath = "selenium/test-suite.xhtml";
        final String seleniumPath = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, relativeTestFilePath);

        final InputStream inputStream;
        if (fileManager.exists(seleniumPath)) {
            inputStream = fileManager.getInputStream(seleniumPath);
        }
        else {
            inputStream = FileUtils.getInputStream(getClass(),
                    "selenium-test-suite-template.xhtml");
            Validate.notNull(inputStream,
                    "Could not acquire selenium test suite template");
        }

        final Document suite = XmlUtils.readXml(inputStream);
        final Element root = (Element) suite.getLastChild();

        XmlUtils.findRequiredElement("/html/head/title", root).setTextContent(
                "Test suite for "
                        + projectOperations.getProjectName(projectOperations
                                .getFocusedModuleName()) + "project");

        final Element tr = suite.createElement("tr");
        final Element td = suite.createElement("td");
        tr.appendChild(td);
        final Element a = suite.createElement("a");
        a.setAttribute(
                "href",
                serverURL
                        + projectOperations.getProjectName(projectOperations
                                .getFocusedModuleName()) + "/resources/"
                        + testPath);
        a.setTextContent(name);
        td.appendChild(a);

        XmlUtils.findRequiredElement("/html/body/table", root).appendChild(tr);

        fileManager.createOrUpdateTextFileIfRequired(seleniumPath,
                XmlUtils.nodeToString(suite), false);

        menuOperations.addMenuItem(new JavaSymbolName("SeleniumTests"),
                new JavaSymbolName("Test"), "Test", "selenium_menu_test_suite",
                "/resources/" + relativeTestFilePath, "si_",
                pathResolver.getFocusedPath(Path.SRC_MAIN_WEBAPP));
    }

    private Node openCommand(final Document document, final String linkTarget) {
        final Node tr = document.createElement("tr");

        final Node td1 = tr.appendChild(document.createElement("td"));
        td1.setTextContent("open");

        final Node td2 = tr.appendChild(document.createElement("td"));
        td2.setTextContent(linkTarget + (linkTarget.contains("?") ? "&" : "?")
                + "lang=" + Locale.getDefault());

        final Node td3 = tr.appendChild(document.createElement("td"));
        td3.setTextContent(" ");

        return tr;
    }

    private Node typeCommand(final Document document, final FieldMetadata field) {
        final Node tr = document.createElement("tr");

        final Node td1 = tr.appendChild(document.createElement("td"));
        td1.setTextContent("type");

        final Node td2 = tr.appendChild(document.createElement("td"));
        td2.setTextContent("_" + field.getFieldName().getSymbolName() + "_id");

        final Node td3 = tr.appendChild(document.createElement("td"));
        td3.setTextContent(convertToInitializer(field));

        return tr;
    }

    private Node verifyTextCommand(final Document document,
            final JavaType formBackingType, final FieldMetadata field) {
        final Node tr = document.createElement("tr");

        final Node td1 = tr.appendChild(document.createElement("td"));
        td1.setTextContent("verifyText");

        final Node td2 = tr.appendChild(document.createElement("td"));
        td2.setTextContent(XmlUtils.convertId("_s_"
                + formBackingType.getFullyQualifiedTypeName() + "_"
                + field.getFieldName().getSymbolName() + "_"
                + field.getFieldName().getSymbolName() + "_id"));

        final Node td3 = tr.appendChild(document.createElement("td"));
        td3.setTextContent(convertToInitializer(field));

        return tr;
    }
}
