package org.springframework.roo.metadata.internal;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.roo.metadata.MetadataIdentificationUtils;

public class DefaultMetadataDependencyRegistryTest {

    private static final String DISK_FILE = MetadataIdentificationUtils.create(
            "com.Test", "disk file");
    private static final String JAVA_TYPE_OBJECT = MetadataIdentificationUtils
            .create("com.Test", "object");
    private static final String JAVA_TYPE_PERSON = MetadataIdentificationUtils
            .create("com.Test", "person");
    private static final String JSP_PAGE_1 = MetadataIdentificationUtils
            .create("com.Test", "jsp 1");
    private static final String JSP_PAGE_2 = MetadataIdentificationUtils
            .create("com.Test", "jsp 2");
    private static final String MVC_CONTROLLER = MetadataIdentificationUtils
            .create("com.Test", "mvc ctrl");

    @Test
    public void testRegistration() {
        final DefaultMetadataDependencyRegistry reg = new DefaultMetadataDependencyRegistry();

        // Verify simple registration
        reg.registerDependency(DISK_FILE, JAVA_TYPE_OBJECT);
        Assert.assertEquals(1, reg.getDownstream(DISK_FILE).size());
        reg.registerDependency(JAVA_TYPE_OBJECT, JAVA_TYPE_PERSON);
        Assert.assertEquals(1, reg.getDownstream(DISK_FILE).size());
        Assert.assertEquals(1, reg.getDownstream(JAVA_TYPE_OBJECT).size());
        reg.registerDependency(JAVA_TYPE_PERSON, MVC_CONTROLLER);

        // Verify dependency enforcement is valid
        Assert.assertTrue(reg.isValidDependency(MVC_CONTROLLER, JSP_PAGE_1));
        Assert.assertTrue(reg.isValidDependency(MVC_CONTROLLER, JSP_PAGE_2));

        reg.registerDependency(MVC_CONTROLLER, JSP_PAGE_1);
        reg.registerDependency(MVC_CONTROLLER, JSP_PAGE_2);
        Assert.assertEquals(2, reg.getDownstream(MVC_CONTROLLER).size());

        // Can't create circular dependencies
        Assert.assertTrue(!reg.isValidDependency(JSP_PAGE_2, MVC_CONTROLLER));
        Assert.assertTrue(!reg.isValidDependency(JAVA_TYPE_PERSON,
                JAVA_TYPE_OBJECT));

        // Ensure individual deregistration works
        reg.deregisterDependency(DISK_FILE, JAVA_TYPE_OBJECT);
        Assert.assertEquals(0, reg.getDownstream(DISK_FILE).size());

        // Ensure bulk deregistration works
        Assert.assertEquals(1, reg.getDownstream(JAVA_TYPE_PERSON).size());
        Assert.assertEquals(2, reg.getDownstream(MVC_CONTROLLER).size());
        reg.deregisterDependencies(MVC_CONTROLLER);
        Assert.assertEquals(0, reg.getDownstream(JAVA_TYPE_PERSON).size());
        Assert.assertEquals(2, reg.getDownstream(MVC_CONTROLLER).size());
    }
}
