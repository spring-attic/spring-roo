package org.springframework.roo.project;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Immutable representation of an execution specification for a (Maven) build
 * plugin
 * 
 * @author Adrian Colyer
 * @author Alan Stewart
 * @author Andrew Swan
 * @since 1.0
 */
public class Execution implements Comparable<Execution> {

    private final Configuration configuration;
    private final List<String> goals;
    private final String id;
    private final String phase;

    /**
     * Constructor
     * 
     * @param id the unique ID of this execution (required)
     * @param phase the Maven life-cycle phase to which this execution is bound
     *            (required)
     * @param configuration the execution-level configuration; can be
     *            <code>null</code>
     * @param goals the goals to execute (must be at least one)
     * @since 1.2.0
     */
    public Execution(final String id, final String phase,
            final Configuration configuration, final String... goals) {
        Validate.notNull(id, "execution id must be specified");
        Validate.notNull(phase, "execution phase must be specified");
        this.configuration = configuration;
        this.goals = goals == null ? Collections.<String> emptyList()
                : Collections.unmodifiableList(Arrays.asList(goals));
        this.id = id.trim();
        this.phase = phase.trim();
    }

    /**
     * Constructor for no execution-level {@link Configuration}
     * 
     * @param id the unique ID of this execution (required)
     * @param phase the Maven life-cycle phase to which this execution is bound
     *            (required)
     * @param goals the goals to execute (must be at least one)
     */
    public Execution(final String id, final String phase, final String... goals) {
        this(id, phase, null, goals);
    }

    public int compareTo(final Execution other) {
        if (other == null) {
            throw new NullPointerException();
        }
        int result = id.compareTo(other.id);
        if (result == 0) {
            result = phase.compareTo(other.phase);
        }
        if (result == 0) {
            final String[] thisGoals = (String[]) goals.toArray();
            final String[] oGoals = (String[]) other.goals.toArray();
            Arrays.sort(thisGoals);
            Arrays.sort(oGoals);
            result = Arrays.toString(thisGoals).compareTo(
                    Arrays.toString(oGoals));
        }
        if (result == 0) {
            result = ObjectUtils.compare(configuration, other.configuration);
        }
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof Execution && compareTo((Execution) obj) == 0;
    }

    /**
     * Returns this execution's configuration, if any; this is separate from any
     * configuration defined at the plugin level
     * 
     * @return <code>null</code> if there is none
     * @since 1.2.0
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Returns the XML element for this execution within the given Maven POM
     * 
     * @param document the Maven POM to which to add the element (required)
     * @return a non-<code>null</code> element
     */
    public Element getElement(final Document document) {
        final Element executionElement = document.createElement("execution");

        // ID
        if (StringUtils.isNotBlank(id)) {
            executionElement.appendChild(XmlUtils.createTextElement(document,
                    "id", id));
        }

        // Phase
        if (StringUtils.isNotBlank(phase)) {
            executionElement.appendChild(XmlUtils.createTextElement(document,
                    "phase", phase));
        }

        // Goals
        final Element goalsElement = DomUtils.createChildElement("goals",
                executionElement, document);
        for (final String goal : goals) {
            goalsElement.appendChild(XmlUtils.createTextElement(document,
                    "goal", goal));
        }

        // Configuration
        if (configuration != null) {
            final Node configurationNode = document.importNode(
                    configuration.getConfiguration(), true);
            executionElement.appendChild(configurationNode);
        }

        return executionElement;
    }

    /**
     * Returns the goals this execution will execute
     * 
     * @return a non-empty list
     */
    public List<String> getGoals() {
        return goals;
    }

    /**
     * Returns the unique ID of this execution
     * 
     * @return a non-blank ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the Maven lifecycle phase to which this execution is bound
     * 
     * @return a non-blank phase name
     */
    public String getPhase() {
        return phase;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ObjectUtils.hashCode(goals);
        result = prime * result + ObjectUtils.hashCode(id);
        result = prime * result + ObjectUtils.hashCode(phase);
        result = prime * result + ObjectUtils.hashCode(configuration);
        return result;
    }

    @Override
    public String toString() {
        final ToStringBuilder tsb = new ToStringBuilder(this);
        tsb.append("id", id);
        tsb.append("phase", phase);
        tsb.append("goals", goals);
        tsb.append("configuration", configuration);
        return tsb.toString();
    }
}
