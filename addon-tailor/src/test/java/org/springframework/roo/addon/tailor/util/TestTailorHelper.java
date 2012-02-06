package org.springframework.roo.addon.tailor.util;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.roo.addon.tailor.CommandTransformation;

/**
 * Tests for {@link TailorHelper#replaceVars(CommandTransformation, String)}
 * 
 * @author Birgitta Boeckeler
 */
public class TestTailorHelper {

    /**
     * Tests a standard case: 2 arguments in the trigger command, both of them
     * represented as placeholders in the target
     */
    @Test
    public void testReplaceVars() {
        final CommandTransformation rooCommand = new CommandTransformation(
                "project --topLevelPackage com.foo.sample --projectName test --domain otherdomainname");
        final String result = TailorHelper
                .replaceVars(rooCommand,
                        "module create --moduleName ${domain} --topLevelPackage ${topLevelPackage}");
        final String expectedResult = "module create --moduleName otherdomainname --topLevelPackage com.foo.sample";
        Assert.assertEquals("Unexpected result: " + result, expectedResult,
                result);
    }

    /**
     * Test replaceVars when there is more than one occurence of the same
     * placeholder in the target
     */
    @Test
    public void testReplaceVarsDuplicatePlaceholders() {
        final CommandTransformation rooCommand = new CommandTransformation(
                "project --topLevelPackage com.foo.sample --projectName test --domain otherdomainname");
        final String result = TailorHelper
                .replaceVars(
                        rooCommand,
                        "module create --moduleName ${domain} --topLevelPackage ${topLevelPackage}.${domain}");
        final String expectedResult = "module create --moduleName otherdomainname --topLevelPackage com.foo.sample.otherdomainname";
        Assert.assertEquals("Unexpected result: " + result, expectedResult,
                result);
    }

    /**
     * Test for "unnamed argument": Use of ${*} as placeholder should result in
     * replacing it with the last fragment of the trigger command, i.e. the
     * first "unnamed" argument.
     */
    @Test
    public void testReplaceVarsForUnnamedArgument() {
        final CommandTransformation rooCommand = new CommandTransformation(
                "cd test-data");
        final String result = TailorHelper.replaceVars(rooCommand,
                "module focus --moduleName ${*}");
        Assert.assertTrue("* was not replaced: " + result,
                result.endsWith("test-data"));
    }

    /**
     * Test makes sure that the regex used by TailorHelper does not match
     * placeholders that are similar, but different. (Prefix and suffix)
     */
    @Test
    public void testReplaceVarsSimilarPlaceholders() {
        final CommandTransformation rooCommand = new CommandTransformation(
                "project --topLevelPackage com.foo.sample --projectName test --domain otherdomainname");
        final String result = TailorHelper
                .replaceVars(rooCommand,
                        "module create --moduleName ${xdomain} --topLevelPackage ${topLevelPackagex}");
        final String expectedResult = "module create --moduleName ${xdomain} --topLevelPackage ${topLevelPackagex}";
        Assert.assertEquals("Unexpected result: " + result, expectedResult,
                result);
    }
}
