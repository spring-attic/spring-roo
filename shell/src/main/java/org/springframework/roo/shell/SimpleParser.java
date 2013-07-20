package org.springframework.roo.shell;

import static org.apache.commons.io.IOUtils.LINE_SEPARATOR;
import static org.springframework.roo.shell.CliOption.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.CollectionUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Default implementation of {@link Parser}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class SimpleParser implements Parser {

    private static final Comparator<Object> COMPARATOR = new NaturalOrderComparator<Object>();
    private static final Logger LOGGER = HandlerUtils
            .getLogger(SimpleParser.class);

    static String isMatch(final String buffer, final String command,
            final boolean strictMatching) {
        if ("".equals(buffer.trim())) {
            return "";
        }
        final String[] commandWords = StringUtils.split(command, " ");
        int lastCommandWordUsed = 0;
        Validate.notEmpty(commandWords, "Command required");

        String bufferToReturn = null;
        String lastWord = null;

        next_buffer_loop: for (int bufferIndex = 0; bufferIndex < buffer
                .length(); bufferIndex++) {
            final String bufferSoFarIncludingThis = buffer.substring(0,
                    bufferIndex + 1);
            final String bufferRemaining = buffer.substring(bufferIndex + 1);

            final int bufferLastIndexOfWord = bufferSoFarIncludingThis
                    .lastIndexOf(" ");
            String wordSoFarIncludingThis = bufferSoFarIncludingThis;
            if (bufferLastIndexOfWord != -1) {
                wordSoFarIncludingThis = bufferSoFarIncludingThis
                        .substring(bufferLastIndexOfWord);
            }

            if (wordSoFarIncludingThis.equals(" ")
                    || bufferIndex == buffer.length() - 1) {
                if (bufferIndex == buffer.length() - 1
                        && !"".equals(wordSoFarIncludingThis.trim())) {
                    lastWord = wordSoFarIncludingThis.trim();
                }

                // At end of word or buffer. Let's see if a word matched or not
                for (int candidate = lastCommandWordUsed; candidate < commandWords.length; candidate++) {
                    if (lastWord != null && lastWord.length() > 0
                            && commandWords[candidate].startsWith(lastWord)) {
                        if (bufferToReturn == null) {
                            // This is the first match, so ensure the intended
                            // match really represents the start of a command
                            // and not a later word within it
                            if (lastCommandWordUsed == 0 && candidate > 0) {
                                // This is not a valid match
                                break next_buffer_loop;
                            }
                        }

                        if (bufferToReturn != null) {
                            // We already matched something earlier, so ensure
                            // we didn't skip any word
                            if (candidate != lastCommandWordUsed + 1) {
                                // User has skipped a word
                                bufferToReturn = null;
                                break next_buffer_loop;
                            }
                        }

                        bufferToReturn = bufferRemaining;
                        lastCommandWordUsed = candidate;
                        if (candidate + 1 == commandWords.length) {
                            // This was a match for the final word in the
                            // command, so abort
                            break next_buffer_loop;
                        }
                        // There are more words left to potentially match, so
                        // continue
                        continue next_buffer_loop;
                    }
                }

                // This word is unrecognised as part of a command, so abort
                bufferToReturn = null;
                break next_buffer_loop;
            }

            lastWord = wordSoFarIncludingThis.trim();
        }

        // We only consider it a match if ALL words were actually used
        if (bufferToReturn != null) {
            if (!strictMatching
                    || lastCommandWordUsed + 1 == commandWords.length) {
                return bufferToReturn;
            }
        }

        return null; // Not a match
    }

    private final Map<String, MethodTarget> availabilityIndicators = new HashMap<String, MethodTarget>();
    private final Set<CommandMarker> commands = new HashSet<CommandMarker>();
    private final Set<Converter<?>> converters = new HashSet<Converter<?>>();

    private final Object mutex = new Object();

    public final void add(final CommandMarker command) {
        synchronized (mutex) {
            commands.add(command);
            for (final Method method : command.getClass().getMethods()) {
                final CliAvailabilityIndicator availability = method
                        .getAnnotation(CliAvailabilityIndicator.class);
                if (availability != null) {
                    Validate.isTrue(
                            method.getParameterTypes().length == 0,
                            "CliAvailabilityIndicator is only legal for 0 parameter methods ('%s')",
                            method.toGenericString());
                    Validate.isTrue(
                            method.getReturnType().equals(Boolean.TYPE),
                            "CliAvailabilityIndicator is only legal for primitive boolean return types (%s)",
                            method.toGenericString());
                    for (final String cmd : availability.value()) {
                        Validate.isTrue(
                                !availabilityIndicators.containsKey(cmd),
                                "Cannot specify an availability indicator for '%s' more than once",
                                cmd);
                        availabilityIndicators.put(cmd, new MethodTarget(
                                method, command));
                    }
                }
            }
        }
    }

    public final void add(final Converter<?> converter) {
        synchronized (mutex) {
            converters.add(converter);
        }
    }

    protected void commandNotFound(final Logger logger, final String buffer) {
        logger.warning("Command '" + buffer
                + "' not found (for assistance press "
                + AbstractShell.completionKeys
                + " or type \"hint\" then hit ENTER)");
    }

    public int complete(final String buffer, final int cursor,
            final List<String> candidates) {
        final List<Completion> completions = new ArrayList<Completion>();
        final int result = completeAdvanced(buffer, cursor, completions);
        for (final Completion completion : completions) {
            candidates.add(completion.getValue());
        }
        return result;
    }

    public int completeAdvanced(String buffer, int cursor,
            final List<Completion> candidates) {
        synchronized (mutex) {
            Validate.notNull(buffer, "Buffer required");
            Validate.notNull(candidates, "Candidates list required");

            // Remove all spaces from beginning of command
            while (buffer.startsWith(" ")) {
                buffer = buffer.replaceFirst("^ ", "");
                cursor--;
            }

            // Replace all multiple spaces with a single space
            while (buffer.contains("  ")) {
                buffer = StringUtils.replace(buffer, "  ", " ", 1);
                cursor--;
            }

            // Begin by only including the portion of the buffer represented to
            // the present cursor position
            final String translated = buffer.substring(0, cursor);

            // Start by locating a method that matches
            final Collection<MethodTarget> targets = locateTargets(translated,
                    false, true);
            final SortedSet<Completion> results = new TreeSet<Completion>(
                    COMPARATOR);

            if (targets.isEmpty()) {
                // Nothing matches the buffer they've presented
                return cursor;
            }
            if (targets.size() > 1) {
                // Assist them locate a particular target
                for (final MethodTarget target : targets) {
                    // Calculate the correct starting position
                    final int startAt = translated.length();

                    // Only add the first word of each target
                    int stopAt = target.getKey().indexOf(" ", startAt);
                    if (stopAt == -1) {
                        stopAt = target.getKey().length();
                    }

                    results.add(new Completion(target.getKey().substring(0,
                            stopAt)
                            + " "));
                }
                candidates.addAll(results);
                return 0;
            }

            // There is a single target of this method, so provide completion
            // services for it
            final MethodTarget methodTarget = targets.iterator().next();

            // Identify the command we're working with
            final CliCommand cmd = methodTarget.getMethod().getAnnotation(
                    CliCommand.class);
            Validate.notNull(cmd, "CliCommand unavailable for '%s'",
                    methodTarget.getMethod().toGenericString());

            // Make a reasonable attempt at parsing the remainingBuffer
            Map<String, String> options;
            try {
                options = ParserUtils.tokenize(methodTarget
                        .getRemainingBuffer());
            }
            catch (final IllegalArgumentException ex) {
                // Assume any IllegalArgumentException is due to a quotation
                // mark mismatch
                candidates.add(new Completion(translated + "\""));
                return 0;
            }

            // Lookup arguments for this target
            final Annotation[][] parameterAnnotations = methodTarget
                    .getMethod().getParameterAnnotations();

            // If there aren't any parameters for the method, at least ensure
            // they have typed the command properly
            if (parameterAnnotations.length == 0) {
                for (final String value : cmd.value()) {
                    if (buffer.startsWith(value) || value.startsWith(buffer)) {
                        // No space at the end, as there's no need to continue
                        // the command further
                        results.add(new Completion(value));
                    }
                }
                candidates.addAll(results);
                return 0;
            }

            // If they haven't specified any parameters yet, at least verify the
            // command name is fully completed
            if (options.isEmpty()) {
                for (final String value : cmd.value()) {
                    if (value.startsWith(buffer)) {
                        // They are potentially trying to type this command
                        // We only need provide completion, though, if they
                        // failed to specify it fully
                        if (!buffer.startsWith(value)) {
                            // They failed to specify the command fully
                            results.add(new Completion(value + " "));
                        }
                    }
                }

                // Only quit right now if they have to finish specifying the
                // command name
                if (results.size() > 0) {
                    candidates.addAll(results);
                    return 0;
                }
            }

            // To get this far, we know there are arguments required for this
            // CliCommand, and they specified a valid command name

            // Record all the CliOptions applicable to this command
            final List<CliOption> cliOptions = new ArrayList<CliOption>();
            for (final Annotation[] annotations : parameterAnnotations) {
                CliOption cliOption = null;
                for (final Annotation a : annotations) {
                    if (a instanceof CliOption) {
                        cliOption = (CliOption) a;
                    }
                }
                Validate.notNull(cliOption,
                        "CliOption not found for parameter '%s'",
                        Arrays.toString(annotations));
                cliOptions.add(cliOption);
            }

            // Make a list of all CliOptions they've already included or are
            // system-provided
            final List<CliOption> alreadySpecified = new ArrayList<CliOption>();
            for (final CliOption option : cliOptions) {
                for (final String value : option.key()) {
                    if (options.containsKey(value)) {
                        alreadySpecified.add(option);
                        break;
                    }
                }
                if (option.systemProvided()) {
                    alreadySpecified.add(option);
                }
            }

            // Make a list of all CliOptions they have not provided
            final List<CliOption> unspecified = new ArrayList<CliOption>(
                    cliOptions);
            unspecified.removeAll(alreadySpecified);

            // Determine whether they're presently editing an option key or an
            // option value
            // (and if possible, the full or partial name of the said option key
            // being edited)
            String lastOptionKey = null;
            String lastOptionValue = null;

            // The last item in the options map is *always* the option key
            // they're editing (will never be null)
            if (options.size() > 0) {
                lastOptionKey = new ArrayList<String>(options.keySet())
                        .get(options.keySet().size() - 1);
                lastOptionValue = options.get(lastOptionKey);
            }

            // Handle if they are trying to find out the available option keys;
            // always present option keys in order
            // of their declaration on the method signature, thus we can stop
            // when mandatory options are filled in
            if (methodTarget.getRemainingBuffer().endsWith("--")) {
                boolean showAllRemaining = true;
                for (final CliOption include : unspecified) {
                    if (include.mandatory()) {
                        showAllRemaining = false;
                        break;
                    }
                }

                for (final CliOption include : unspecified) {
                    for (final String value : include.key()) {
                        if (!"".equals(value)) {
                            results.add(new Completion(translated + value + " "));
                        }
                    }
                    if (!showAllRemaining) {
                        break;
                    }
                }
                candidates.addAll(results);
                return 0;
            }

            // Handle suggesting an option key if they haven't got one presently
            // specified (or they've completed a full option key/value pair)
            if (lastOptionKey == null || !"".equals(lastOptionKey)
                    && !"".equals(lastOptionValue) && translated.endsWith(" ")) {
                // We have either NEVER specified an option key/value pair
                // OR we have specified a full option key/value pair

                // Let's list some other options the user might want to try
                // (naturally skip the "" option, as that's the default)
                for (final CliOption include : unspecified) {
                    for (final String value : include.key()) {
                        // Manually determine if this non-mandatory but
                        // unspecifiedDefaultValue=* requiring option is able to
                        // be bound
                        if (!include.mandatory()
                                && "*".equals(include.unspecifiedDefaultValue())
                                && !"".equals(value)) {
                            try {
                                for (final Converter<?> candidate : converters) {
                                    // Find the target parameter
                                    Class<?> paramType = null;
                                    int index = -1;
                                    for (final Annotation[] a : methodTarget
                                            .getMethod()
                                            .getParameterAnnotations()) {
                                        index++;
                                        for (final Annotation an : a) {
                                            if (an instanceof CliOption) {
                                                if (an.equals(include)) {
                                                    // Found the parameter, so
                                                    // store it
                                                    paramType = methodTarget
                                                            .getMethod()
                                                            .getParameterTypes()[index];
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    if (paramType != null
                                            && candidate.supports(paramType,
                                                    include.optionContext())) {
                                        // Try to invoke this usable converter
                                        candidate.convertFromText("*",
                                                paramType,
                                                include.optionContext());
                                        // If we got this far, the converter is
                                        // happy with "*" so we need not bother
                                        // the user with entering the data in
                                        // themselves
                                        break;
                                    }
                                }
                            }
                            catch (final RuntimeException notYetReady) {
                                if (translated.endsWith(" ")) {
                                    results.add(new Completion(translated
                                            + "--" + value + " "));
                                }
                                else {
                                    results.add(new Completion(translated
                                            + " --" + value + " "));
                                }
                                continue;
                            }
                        }

                        // Handle normal mandatory options
                        if (!"".equals(value) && include.mandatory()) {
                            if (translated.endsWith(" ")) {
                                results.add(new Completion(translated + "--"
                                        + value + " "));
                            }
                            else {
                                results.add(new Completion(translated + " --"
                                        + value + " "));
                            }
                        }
                    }
                }

                // Only abort at this point if we have some suggestions;
                // otherwise we might want to try to complete the "" option
                if (results.size() > 0) {
                    candidates.addAll(results);
                    return 0;
                }
            }

            // Handle completing the option key they're presently typing
            if ((lastOptionValue == null || "".equals(lastOptionValue))
                    && !translated.endsWith(" ")) {
                // Given we haven't got an option value of any form, and there's
                // no space at the buffer end, we must still be typing an option
                // key

                for (final CliOption option : cliOptions) {
                    for (final String value : option.key()) {
                        if (value != null
                                && lastOptionKey != null
                                && value.regionMatches(true, 0, lastOptionKey,
                                        0, lastOptionKey.length())) {
                            final String completionValue = translated
                                    .substring(0, translated.length()
                                            - lastOptionKey.length())
                                    + value + " ";
                            results.add(new Completion(completionValue));
                        }
                    }
                }
                candidates.addAll(results);
                return 0;
            }

            // To be here, we are NOT typing an option key (or we might be, and
            // there are no further option keys left)
            if (lastOptionKey != null && !"".equals(lastOptionKey)) {
                // Lookup the relevant CliOption that applies to this
                // lastOptionKey
                // We do this via the parameter type
                final Class<?>[] parameterTypes = methodTarget.getMethod()
                        .getParameterTypes();
                for (int i = 0; i < parameterTypes.length; i++) {
                    final CliOption option = cliOptions.get(i);
                    final Class<?> parameterType = parameterTypes[i];

                    for (final String key : option.key()) {
                        if (key.equals(lastOptionKey)) {
                            final List<Completion> allValues = new ArrayList<Completion>();
                            String suffix = " ";

                            // Let's use a Converter if one is available
                            for (final Converter<?> candidate : converters) {
                                if (candidate.supports(parameterType,
                                        option.optionContext())) {
                                    // Found a usable converter
                                    final boolean addSpace = candidate
                                            .getAllPossibleValues(allValues,
                                                    parameterType,
                                                    lastOptionValue,
                                                    option.optionContext(),
                                                    methodTarget);
                                    if (!addSpace) {
                                        suffix = "";
                                    }
                                    break;
                                }
                            }

                            if (allValues.isEmpty()) {
                                // Doesn't appear to be a custom Converter, so
                                // let's go and provide defaults for simple
                                // types

                                // Provide some simple options for common types
                                if (Boolean.class
                                        .isAssignableFrom(parameterType)
                                        || Boolean.TYPE
                                                .isAssignableFrom(parameterType)) {
                                    allValues.add(new Completion("true"));
                                    allValues.add(new Completion("false"));
                                }

                                if (Number.class
                                        .isAssignableFrom(parameterType)) {
                                    allValues.add(new Completion("0"));
                                    allValues.add(new Completion("1"));
                                    allValues.add(new Completion("2"));
                                    allValues.add(new Completion("3"));
                                    allValues.add(new Completion("4"));
                                    allValues.add(new Completion("5"));
                                    allValues.add(new Completion("6"));
                                    allValues.add(new Completion("7"));
                                    allValues.add(new Completion("8"));
                                    allValues.add(new Completion("9"));
                                }
                            }

                            String prefix = "";
                            if (!translated.endsWith(" ")) {
                                prefix = " ";
                            }

                            // Only include in the candidates those results
                            // which are compatible with the present buffer
                            for (final Completion currentValue : allValues) {
                                // We only provide a suggestion if the
                                // lastOptionValue == ""
                                if (StringUtils.isBlank(lastOptionValue)) {
                                    // We should add the result, as they haven't
                                    // typed anything yet
                                    results.add(new Completion(prefix
                                            + currentValue.getValue() + suffix,
                                            currentValue.getFormattedValue(),
                                            currentValue.getHeading(),
                                            currentValue.getOrder()));
                                }
                                else {
                                    // Only add the result **if** what they've
                                    // typed is compatible *AND* they haven't
                                    // already typed it in full
                                    if (currentValue
                                            .getValue()
                                            .toLowerCase()
                                            .startsWith(
                                                    lastOptionValue
                                                            .toLowerCase())
                                            && !lastOptionValue
                                                    .equalsIgnoreCase(currentValue
                                                            .getValue())
                                            && lastOptionValue.length() < currentValue
                                                    .getValue().length()) {
                                        results.add(new Completion(prefix
                                                + currentValue.getValue()
                                                + suffix, currentValue
                                                .getFormattedValue(),
                                                currentValue.getHeading(),
                                                currentValue.getOrder()));
                                    }
                                }
                            }

                            // ROO-389: give inline options given there's
                            // multiple choices available and we want to help
                            // the user
                            final StringBuilder help = new StringBuilder();
                            help.append(LINE_SEPARATOR);
                            help.append(option.mandatory() ? "required --"
                                    : "optional --");
                            if ("".equals(option.help())) {
                                help.append(lastOptionKey).append(": ")
                                        .append("No help available");
                            }
                            else {
                                help.append(lastOptionKey).append(": ")
                                        .append(option.help());
                            }
                            if (option.specifiedDefaultValue().equals(
                                    option.unspecifiedDefaultValue())) {
                                if (option.specifiedDefaultValue().equals(
                                        NULL)) {
                                    help.append("; no default value");
                                }
                                else {
                                    help.append("; default: '")
                                            .append(option
                                                    .specifiedDefaultValue())
                                            .append("'");
                                }
                            }
                            else {
                                if (!"".equals(option.specifiedDefaultValue())
                                        && !NULL.equals(option
                                                .specifiedDefaultValue())) {
                                    help.append(
                                            "; default if option present: '")
                                            .append(option
                                                    .specifiedDefaultValue())
                                            .append("'");
                                }
                                if (!"".equals(option.unspecifiedDefaultValue())
                                        && !NULL.equals(option
                                                .unspecifiedDefaultValue())) {
                                    help.append(
                                            "; default if option not present: '")
                                            .append(option
                                                    .unspecifiedDefaultValue())
                                            .append("'");
                                }
                            }
                            LOGGER.info(help.toString());

                            if (results.size() == 1) {
                                final String suggestion = results.iterator()
                                        .next().getValue().trim();
                                if (suggestion.equals(lastOptionValue)) {
                                    // They have pressed TAB in the default
                                    // value, and the default value has already
                                    // been provided as an explicit option
                                    return 0;
                                }
                            }

                            if (results.size() > 0) {
                                candidates.addAll(results);
                                // Values presented from the last space onwards
                                if (translated.endsWith(" ")) {
                                    return translated.lastIndexOf(" ") + 1;
                                }
                                return translated.trim().lastIndexOf(" ");
                            }
                            return 0;
                        }
                    }
                }
            }

            return 0;
        }
    }

    private MethodTarget getAvailabilityIndicator(final String command) {
        return availabilityIndicators.get(command);
    }

    private Set<CliOption> getCliOptions(
            final Annotation[][] parameterAnnotations) {
        final Set<CliOption> cliOptions = new LinkedHashSet<CliOption>();
        for (final Annotation[] annotations : parameterAnnotations) {
            for (final Annotation annotation : annotations) {
                if (annotation instanceof CliOption) {
                    final CliOption cliOption = (CliOption) annotation;
                    cliOptions.add(cliOption);
                }
            }
        }
        return cliOptions;
    }

    public Set<String> getEveryCommand() {
        synchronized (mutex) {
            final SortedSet<String> result = new TreeSet<String>(COMPARATOR);
            for (final Object o : commands) {
                final Method[] methods = o.getClass().getMethods();
                for (final Method m : methods) {
                    final CliCommand cmd = m.getAnnotation(CliCommand.class);
                    if (cmd != null) {
                        result.addAll(Arrays.asList(cmd.value()));
                    }
                }
            }
            return result;
        }
    }

    private Set<String> getSpecifiedUnavailableOptions(
            final Set<CliOption> cliOptions, final Map<String, String> options) {
        final Set<String> cliOptionKeySet = new LinkedHashSet<String>();
        for (final CliOption cliOption : cliOptions) {
            for (final String key : cliOption.key()) {
                cliOptionKeySet.add(key.toLowerCase());
            }
        }
        final Set<String> unavailableOptions = new LinkedHashSet<String>();
        for (final String suppliedOption : options.keySet()) {
            if (!cliOptionKeySet.contains(suppliedOption.toLowerCase())) {
                unavailableOptions.add(suppliedOption);
            }
        }
        return unavailableOptions;
    }

    public void helpReferenceGuide() {
        synchronized (mutex) {
            final File f = new File(".");
            final File[] existing = f.listFiles(new FileFilter() {
                public boolean accept(final File pathname) {
                    return pathname.getName().startsWith("appendix_");
                }
            });
            for (final File e : existing) {
                e.delete();
            }

            // Compute the sections we'll be outputting, and get them into a
            // nice order
            final SortedMap<String, Object> sections = new TreeMap<String, Object>(
                    COMPARATOR);
            next_target: for (final Object target : commands) {
                final Method[] methods = target.getClass().getMethods();
                for (final Method m : methods) {
                    final CliCommand cmd = m.getAnnotation(CliCommand.class);
                    if (cmd != null) {
                        String sectionName = target.getClass().getSimpleName();
                        final Pattern p = Pattern.compile("[A-Z][^A-Z]*");
                        final Matcher matcher = p.matcher(sectionName);
                        final StringBuilder string = new StringBuilder();
                        while (matcher.find()) {
                            string.append(matcher.group()).append(" ");
                        }
                        sectionName = string.toString().trim();
                        if (sections.containsKey(sectionName)) {
                            throw new IllegalStateException("Section name '"
                                    + sectionName + "' not unique");
                        }
                        sections.put(sectionName, target);
                        continue next_target;
                    }
                }
            }

            // Build each section of the appendix
            final DocumentBuilder builder = XmlUtils.getDocumentBuilder();
            final Document document = builder.newDocument();
            final List<Element> builtSections = new ArrayList<Element>();

            for (final Entry<String, Object> entry : sections.entrySet()) {
                final String section = entry.getKey();
                final Object target = entry.getValue();
                final SortedMap<String, Element> individualCommands = new TreeMap<String, Element>(
                        COMPARATOR);

                final Method[] methods = target.getClass().getMethods();
                for (final Method m : methods) {
                    final CliCommand cmd = m.getAnnotation(CliCommand.class);
                    if (cmd != null) {
                        final StringBuilder cmdSyntax = new StringBuilder();
                        cmdSyntax.append(cmd.value()[0]);

                        // Build the syntax list

                        // Store the order options appear
                        final List<String> optionKeys = new ArrayList<String>();
                        // key: option key, value: help text
                        final Map<String, String> optionDetails = new HashMap<String, String>();
                        for (final Annotation[] ann : m
                                .getParameterAnnotations()) {
                            for (final Annotation a : ann) {
                                if (a instanceof CliOption) {
                                    final CliOption option = (CliOption) a;
                                    // Figure out which key we want to use (use
                                    // first non-empty string, or make it
                                    // "(default)" if needed)
                                    String key = option.key()[0];
                                    if ("".equals(key)) {
                                        for (final String otherKey : option
                                                .key()) {
                                            if (!"".equals(otherKey)) {
                                                key = otherKey;
                                                break;
                                            }
                                        }
                                        if ("".equals(key)) {
                                            key = "[default]";
                                        }
                                    }

                                    final StringBuilder help = new StringBuilder();
                                    if ("".equals(option.help())) {
                                        help.append("No help available");
                                    }
                                    else {
                                        help.append(option.help());
                                    }
                                    if (option.specifiedDefaultValue().equals(
                                            option.unspecifiedDefaultValue())) {
                                        if (option.specifiedDefaultValue()
                                                .equals(NULL)) {
                                            help.append("; no default value");
                                        }
                                        else {
                                            help.append("; default: '")
                                                    .append(option
                                                            .specifiedDefaultValue())
                                                    .append("'");
                                        }
                                    }
                                    else {
                                        if (!"".equals(option
                                                .specifiedDefaultValue())
                                                && !NULL
                                                        .equals(option
                                                                .specifiedDefaultValue())) {
                                            help.append(
                                                    "; default if option present: '")
                                                    .append(option
                                                            .specifiedDefaultValue())
                                                    .append("'");
                                        }
                                        if (!"".equals(option
                                                .unspecifiedDefaultValue())
                                                && !NULL
                                                        .equals(option
                                                                .unspecifiedDefaultValue())) {
                                            help.append(
                                                    "; default if option not present: '")
                                                    .append(option
                                                            .unspecifiedDefaultValue())
                                                    .append("'");
                                        }
                                    }
                                    help.append(option.mandatory() ? " (mandatory) "
                                            : "");

                                    // Store details for later
                                    key = "--" + key;
                                    optionKeys.add(key);
                                    optionDetails.put(key, help.toString());

                                    // Include it in the mandatory syntax
                                    if (option.mandatory()) {
                                        cmdSyntax.append(" ").append(key);
                                    }
                                }
                            }
                        }

                        // Make a variable list element
                        Element variableListElement = document
                                .createElement("variablelist");
                        boolean anyVars = false;
                        for (final String optionKey : optionKeys) {
                            anyVars = true;
                            final String help = optionDetails.get(optionKey);
                            variableListElement
                                    .appendChild(new XmlElementBuilder(
                                            "varlistentry", document)
                                            .addChild(
                                                    new XmlElementBuilder(
                                                            "term", document)
                                                            .setText(optionKey)
                                                            .build())
                                            .addChild(
                                                    new XmlElementBuilder(
                                                            "listitem",
                                                            document)
                                                            .addChild(
                                                                    new XmlElementBuilder(
                                                                            "para",
                                                                            document)
                                                                            .setText(
                                                                                    help)
                                                                            .build())
                                                            .build()).build());
                        }

                        if (!anyVars) {
                            variableListElement = new XmlElementBuilder("para",
                                    document)
                                    .setText(
                                            "This command does not accept any options.")
                                    .build();
                        }

                        // Now we've figured out the options, store this
                        // individual command
                        final CDATASection progList = document
                                .createCDATASection(cmdSyntax.toString());
                        final String safeName = cmd.value()[0]
                                .replace("\\", "BCK").replace("/", "FWD")
                                .replace("*", "ASX");
                        final Element element = new XmlElementBuilder(
                                "section", document)
                                .addAttribute(
                                        "xml:id",
                                        "command-index-"
                                                + safeName.toLowerCase()
                                                        .replace(' ', '-'))
                                .addChild(
                                        new XmlElementBuilder("title", document)
                                                .setText(cmd.value()[0])
                                                .build())
                                .addChild(
                                        new XmlElementBuilder("para", document)
                                                .setText(cmd.help()).build())
                                .addChild(
                                        new XmlElementBuilder("programlisting",
                                                document).addChild(progList)
                                                .build())
                                .addChild(variableListElement).build();

                        individualCommands.put(cmdSyntax.toString(), element);
                    }
                }

                final Element topSection = document.createElement("section");
                topSection.setAttribute("xml:id", "command-index-"
                        + section.toLowerCase().replace(' ', '-'));
                topSection.appendChild(new XmlElementBuilder("title", document)
                        .setText(section).build());
                topSection.appendChild(new XmlElementBuilder("para", document)
                        .setText(
                                section + " are contained in "
                                        + target.getClass().getName() + ".")
                        .build());

                for (final Element value : individualCommands.values()) {
                    topSection.appendChild(value);
                }

                builtSections.add(topSection);
            }

            final Element appendix = document.createElement("appendix");
            appendix.setAttribute("xmlns", "http://docbook.org/ns/docbook");
            appendix.setAttribute("version", "5.0");
            appendix.setAttribute("xml:id", "command-index");
            appendix.appendChild(new XmlElementBuilder("title", document)
                    .setText("Command Index").build());
            appendix.appendChild(new XmlElementBuilder("para", document)
                    .setText(
                            "This appendix was automatically built from Roo "
                                    + AbstractShell.versionInfo() + ".")
                    .build());
            appendix.appendChild(new XmlElementBuilder("para", document)
                    .setText(
                            "Commands are listed in alphabetic order, and are shown in monospaced font with any mandatory options you must specify when using the command. Most commands accept a large number of options, and all of the possible options for each command are presented in this appendix.")
                    .build());

            for (final Element section : builtSections) {
                appendix.appendChild(section);
            }
            document.appendChild(appendix);

            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            final Transformer transformer = XmlUtils
                    .createIndentingTransformer();
            // Causes an
            // "Error reported by XML parser: Multiple notations were used which had the name 'linespecific', but which were not determined to be duplicates."
            // when creating the DocBook
            // transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC,
            // "-//OASIS//DTD DocBook XML V4.5//EN");
            // transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,
            // "http://www.oasis-open.org/docbook/xml/4.5/docbookx.dtd");

            XmlUtils.writeXml(transformer, byteArrayOutputStream, document);
            try {
                final File output = new File(f, "appendix-command-index.xml");
                FileUtils.writeByteArrayToFile(output,
                        byteArrayOutputStream.toByteArray());
            }
            catch (final IOException ioe) {
                throw new IllegalStateException(ioe);
            }
            finally {
                IOUtils.closeQuietly(byteArrayOutputStream);
            }
        }
    }

    private Collection<MethodTarget> locateTargets(final String buffer,
            final boolean strictMatching,
            final boolean checkAvailabilityIndicators) {
        Validate.notNull(buffer, "Buffer required");
        final Collection<MethodTarget> result = new HashSet<MethodTarget>();

        // The reflection could certainly be optimised, but it's good enough for
        // now (and cached reflection
        // is unlikely to be noticeable to a human being using the CLI)
        for (final CommandMarker command : commands) {
            for (final Method method : command.getClass().getMethods()) {
                final CliCommand cmd = method.getAnnotation(CliCommand.class);
                if (cmd != null) {
                    // We have a @CliCommand.
                    if (checkAvailabilityIndicators) {
                        // Decide if this @CliCommand is available at this
                        // moment
                        Boolean available = null;
                        for (final String value : cmd.value()) {
                            final MethodTarget mt = getAvailabilityIndicator(value);
                            if (mt != null) {
                                Validate.isTrue(available == null,
                                        "More than one availability indicator is defined for '"
                                                + method.toGenericString()
                                                + "'");
                                try {
                                    available = (Boolean) mt.getMethod()
                                            .invoke(mt.getTarget());
                                    // We should "break" here, but we loop over
                                    // all to ensure no conflicting availability
                                    // indicators are defined
                                }
                                catch (final Exception e) {
                                    available = false;
                                }
                            }
                        }
                        // Skip this @CliCommand if it's not available
                        if (available != null && !available) {
                            continue;
                        }
                    }

                    for (final String value : cmd.value()) {
                        final String remainingBuffer = isMatch(buffer, value,
                                strictMatching);
                        if (remainingBuffer != null) {
                            result.add(new MethodTarget(method, command,
                                    remainingBuffer, value));
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Normalises the given raw user input string ready for parsing
     * 
     * @param rawInput the string to normalise; can't be <code>null</code>
     * @return a non-<code>null</code> string
     */
    String normalise(final String rawInput) {
        // Replace all multiple spaces with a single space and then trim
        return rawInput.replaceAll(" +", " ").trim();
    }

    public void obtainHelp(
            @CliOption(key = { "", "command" }, optionContext = "availableCommands", help = "Command name to provide help for") String buffer) {
        synchronized (mutex) {
            if (buffer == null) {
                buffer = "";
            }

            final StringBuilder sb = new StringBuilder();

            // Figure out if there's a single command we can offer help for
            final Collection<MethodTarget> matchingTargets = locateTargets(
                    buffer, false, false);
            if (matchingTargets.size() == 1) {
                // Single command help
                final MethodTarget methodTarget = matchingTargets.iterator()
                        .next();

                // Argument conversion time
                final Annotation[][] parameterAnnotations = methodTarget
                        .getMethod().getParameterAnnotations();
                if (parameterAnnotations.length > 0) {
                    // Offer specified help
                    final CliCommand cmd = methodTarget.getMethod()
                            .getAnnotation(CliCommand.class);
                    Validate.notNull(cmd, "CliCommand not found");

                    for (final String value : cmd.value()) {
                        sb.append("Keyword:                   ").append(value)
                                .append(LINE_SEPARATOR);
                    }

                    sb.append("Description:               ").append(cmd.help())
                            .append(LINE_SEPARATOR);

                    for (final Annotation[] annotations : parameterAnnotations) {
                        CliOption cliOption = null;
                        for (final Annotation a : annotations) {
                            if (a instanceof CliOption) {
                                cliOption = (CliOption) a;

                                for (String key : cliOption.key()) {
                                    if ("".equals(key)) {
                                        key = "** default **";
                                    }
                                    sb.append(" Keyword:                  ")
                                            .append(key).append(LINE_SEPARATOR);
                                }

                                sb.append("   Help:                   ")
                                        .append(cliOption.help())
                                        .append(LINE_SEPARATOR);
                                sb.append("   Mandatory:              ")
                                        .append(cliOption.mandatory())
                                        .append(LINE_SEPARATOR);
                                sb.append("   Default if specified:   '")
                                        .append(cliOption
                                                .specifiedDefaultValue())
                                        .append("'").append(LINE_SEPARATOR);
                                sb.append("   Default if unspecified: '")
                                        .append(cliOption
                                                .unspecifiedDefaultValue())
                                        .append("'").append(LINE_SEPARATOR);
                                sb.append(LINE_SEPARATOR);
                            }

                        }
                        Validate.notNull(cliOption,
                                "CliOption not found for parameter '%s'",
                                Arrays.toString(annotations));
                    }
                }
                // Only a single argument, so default to the normal help
                // operation
            }

            final SortedSet<String> result = new TreeSet<String>(COMPARATOR);
            for (final MethodTarget mt : matchingTargets) {
                final CliCommand cmd = mt.getMethod().getAnnotation(
                        CliCommand.class);
                if (cmd != null) {
                    for (final String value : cmd.value()) {
                        if ("".equals(cmd.help())) {
                            result.add("* " + value);
                        }
                        else {
                            result.add("* " + value + " - " + cmd.help());
                        }
                    }
                }
            }

            for (final String s : result) {
                sb.append(s).append(LINE_SEPARATOR);
            }

            LOGGER.info(sb.toString());
            LOGGER.warning("** Type 'hint' (without the quotes) and hit ENTER for step-by-step guidance **"
                    + LINE_SEPARATOR);
        }
    }

    public ParseResult parse(final String rawInput) {
        synchronized (mutex) {
            Validate.notNull(rawInput, "Raw input required");
            final String input = normalise(rawInput);

            // Locate the applicable targets which match this buffer
            final Collection<MethodTarget> matchingTargets = locateTargets(
                    input, true, true);
            if (matchingTargets.isEmpty()) {
                // Before we just give up, let's see if we can offer a more
                // informative message to the user
                // by seeing the command is simply unavailable at this point in
                // time
                CollectionUtils.populate(matchingTargets,
                        locateTargets(input, true, false));
                if (matchingTargets.isEmpty()) {
                    commandNotFound(LOGGER, input);
                }
                else {
                    LOGGER.warning("Command '"
                            + input
                            + "' was found but is not currently available (type 'help' then ENTER to learn about this command)");
                }
                return null;
            }
            if (matchingTargets.size() > 1) {
                LOGGER.warning("Ambigious command '" + input
                        + "' (for assistance press "
                        + AbstractShell.completionKeys
                        + " or type \"hint\" then hit ENTER)");
                return null;
            }
            final MethodTarget methodTarget = matchingTargets.iterator().next();

            // Argument conversion time
            final Annotation[][] parameterAnnotations = methodTarget
                    .getMethod().getParameterAnnotations();
            if (parameterAnnotations.length == 0) {
                // No args
                return new ParseResult(methodTarget.getMethod(),
                        methodTarget.getTarget(), null);
            }

            // Oh well, we need to convert some arguments
            final List<Object> arguments = new ArrayList<Object>(methodTarget
                    .getMethod().getParameterTypes().length);

            // Attempt to parse
            Map<String, String> options = null;
            try {
                options = ParserUtils.tokenize(methodTarget
                        .getRemainingBuffer());
            }
            catch (final IllegalArgumentException e) {
                LOGGER.warning(StringUtils.defaultIfBlank(
                        ExceptionUtils.getRootCauseMessage(e), e.getMessage()));
                return null;
            }

            final Set<CliOption> cliOptions = getCliOptions(parameterAnnotations);
            for (final CliOption cliOption : cliOptions) {
                final Class<?> requiredType = methodTarget.getMethod()
                        .getParameterTypes()[arguments.size()];

                if (cliOption.systemProvided()) {
                    Object result;
                    if (SimpleParser.class.isAssignableFrom(requiredType)) {
                        result = this;
                    }
                    else {
                        LOGGER.warning("Parameter type '" + requiredType
                                + "' is not system provided");
                        return null;
                    }
                    arguments.add(result);
                    continue;
                }

                // Obtain the value the user specified, taking care to ensure
                // they only specified it via a single alias
                String value = null;
                String sourcedFrom = null;
                for (final String possibleKey : cliOption.key()) {
                    if (options.containsKey(possibleKey)) {
                        if (sourcedFrom != null) {
                            LOGGER.warning("You cannot specify option '"
                                    + possibleKey
                                    + "' when you have also specified '"
                                    + sourcedFrom + "' in the same command");
                            return null;
                        }
                        sourcedFrom = possibleKey;
                        value = options.get(possibleKey);
                    }
                }

                // Ensure the user specified a value if the value is mandatory
                if (StringUtils.isBlank(value) && cliOption.mandatory()) {
                    if ("".equals(cliOption.key()[0])) {
                        final StringBuilder message = new StringBuilder(
                                "You must specify a default option ");
                        if (cliOption.key().length > 1) {
                            message.append("(otherwise known as option '")
                                    .append(cliOption.key()[1]).append("') ");
                        }
                        message.append("for this command");
                        LOGGER.warning(message.toString());
                    }
                    else {
                        LOGGER.warning("You must specify option '"
                                + cliOption.key()[0] + "' for this command");
                    }
                    return null;
                }

                // Accept a default if the user specified the option, but didn't
                // provide a value
                if ("".equals(value)) {
                    value = cliOption.specifiedDefaultValue();
                }

                // Accept a default if the user didn't specify the option at all
                if (value == null) {
                    value = cliOption.unspecifiedDefaultValue();
                }

                // Special token that denotes a null value is sought (useful for
                // default values)
                if (NULL.equals(value)) {
                    if (requiredType.isPrimitive()) {
                        LOGGER.warning("Nulls cannot be presented to primitive type "
                                + requiredType.getSimpleName()
                                + " for option '"
                                + StringUtils.join(cliOption.key(), ",") + "'");
                        return null;
                    }
                    arguments.add(null);
                    continue;
                }

                // Change the empty string marker back into an empty string now
                // that we are passed the default and null value checks.
                if (EMPTY.equals(value)) {
                    value = "";
                }

                // Now we're ready to perform a conversion
                try {
                    CliOptionContext
                            .setOptionContext(cliOption.optionContext());
                    CliSimpleParserContext.setSimpleParserContext(this);
                    Object result;
                    Converter<?> c = null;
                    for (final Converter<?> candidate : converters) {
                        if (candidate.supports(requiredType,
                                cliOption.optionContext())) {
                            // Found a usable converter
                            c = candidate;
                            break;
                        }
                    }
                    if (c == null) {
                        throw new IllegalStateException(
                                "TODO: Add basic type conversion");
                        // TODO Fall back to a normal SimpleTypeConverter and
                        // attempt conversion
                        // SimpleTypeConverter simpleTypeConverter = new
                        // SimpleTypeConverter();
                        // result =
                        // simpleTypeConverter.convertIfNecessary(value,
                        // requiredType, mp);
                    }

                    // Use the converter
                    result = c.convertFromText(value, requiredType,
                            cliOption.optionContext());

                    // If the option has been specified to be mandatory then the
                    // result should never be null
                    if (result == null && cliOption.mandatory()) {
                        throw new IllegalStateException();
                    }
                    arguments.add(result);
                }
                catch (final RuntimeException e) {
                    LOGGER.warning(e.getClass().getName()
                            + ": Failed to convert '" + value + "' to type "
                            + requiredType.getSimpleName() + " for option '"
                            + StringUtils.join(cliOption.key(), ",") + "'");
                    if (StringUtils.isNotBlank(e.getMessage())) {
                        LOGGER.warning(e.getMessage());
                    }
                    return null;
                }
                finally {
                    CliOptionContext.resetOptionContext();
                    CliSimpleParserContext.resetSimpleParserContext();
                }
            }

            // Check for options specified by the user but are unavailable for
            // the command
            final Set<String> unavailableOptions = getSpecifiedUnavailableOptions(
                    cliOptions, options);
            if (!unavailableOptions.isEmpty()) {
                final StringBuilder message = new StringBuilder();
                if (unavailableOptions.size() == 1) {
                    message.append("Option '")
                            .append(unavailableOptions.iterator().next())
                            .append("' is not available for this command. ");
                }
                else {
                    message.append("Options ")
                            .append(collectionToDelimitedString(
                                    unavailableOptions, ", ", "'", "'"))
                            .append(" are not available for this command. ");
                }
                message.append("Use tab assist or the \"help\" command to see the legal options");
                LOGGER.warning(message.toString());
                return null;
            }

            return new ParseResult(methodTarget.getMethod(),
                    methodTarget.getTarget(), arguments.toArray());
        }
    }

    private String collectionToDelimitedString(final Collection<?> coll,
            final String delim, final String prefix, final String suffix) {
        if (CollectionUtils.isEmpty(coll)) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        final Iterator<?> it = coll.iterator();
        while (it.hasNext()) {
            sb.append(prefix).append(it.next()).append(suffix);
            if (it.hasNext() && delim != null) {
                sb.append(delim);
            }
        }
        return sb.toString();
    }

    public final void remove(final CommandMarker command) {
        synchronized (mutex) {
            commands.remove(command);
            for (final Method m : command.getClass().getMethods()) {
                final CliAvailabilityIndicator availability = m
                        .getAnnotation(CliAvailabilityIndicator.class);
                if (availability != null) {
                    for (final String cmd : availability.value()) {
                        availabilityIndicators.remove(cmd);
                    }
                }
            }
        }
    }

    public final void remove(final Converter<?> converter) {
        synchronized (mutex) {
            converters.remove(converter);
        }
    }
}
