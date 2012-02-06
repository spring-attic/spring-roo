package org.springframework.roo.addon.tailor.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.roo.addon.tailor.CommandTransformation;
import org.springframework.roo.project.MavenOperationsImpl;

/**
 * Tests for {@link FocusModule}.
 * 
 * @author Birgitta Boeckeler
 */
public class TestFocusModule {

    /**
     * Mock ProjectOperations to return a list of test module names
     */
    private class MockProjectOperations extends MavenOperationsImpl {

        @Override
        public Collection<String> getModuleNames() {
            final List<String> result = new ArrayList<String>();
            result.add("domain-it");
            result.add("domain");
            result.add("data");
            result.add("data-it");
            return result;
        }

    }

    private FocusModule createTestActionObject() {
        final FocusModule action = new FocusModule();
        action.projectOperations = new MockProjectOperations();
        return action;
    }

    /**
     * Tests a list of match strings for the module name.
     */
    @Test
    public void testList() {
        final FocusModule action = createTestActionObject();
        final ActionConfig config = ActionConfigFactory
                .focusModuleAction("data,it");
        final CommandTransformation trafo = new CommandTransformation(
                "command not relevant for this test");
        action.execute(trafo, config);
        // Test data: "data" module is first, "data-it" second.
        // Expected that action will discard "data" and choose "data-it"
        Assert.assertTrue(trafo.getOutputCommands().contains(
                "module focus --moduleName data-it"));
    }

    @Test
    public void testListWithout() {
        final FocusModule action = createTestActionObject();
        final ActionConfig config = ActionConfigFactory
                .focusModuleAction("domain,/it");
        final CommandTransformation trafo = new CommandTransformation(
                "command not relevant for this test");
        action.execute(trafo, config);
        // Test data: "domain-it" module is first, "domain" second.
        // Expected that action will discard "domain-it" because it contains
        // "it", and choose "domain"
        Assert.assertTrue(trafo.getOutputCommands().contains(
                "module focus --moduleName domain"));
    }

    @Test
    public void testStandard() {
        final FocusModule action = createTestActionObject();
        final ActionConfig config = ActionConfigFactory
                .focusModuleAction("domain");
        final CommandTransformation trafo = new CommandTransformation(
                "command not relevant for this test");
        action.execute(trafo, config);
        // Test data: "domain-it" module is first, "domain" second.
        // Expected that action will choose "domain-it" as the first positive
        // match
        Assert.assertTrue(trafo.getOutputCommands().contains(
                "module focus --moduleName domain-it"));
    }

}
