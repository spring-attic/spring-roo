package org.springframework.roo.shell;

import static org.apache.commons.io.IOUtils.LINE_SEPARATOR;
import static org.springframework.roo.shell.CliOption.EMPTY;
import static org.springframework.roo.shell.CliOption.NULL;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
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
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.CollectionUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Default implementation of {@link Parser}.
 * 
 * @author Ben Alex
 * @author Juan Carlos García
 * @since 1.0
 */
public class SimpleParser implements Parser {

  // ------------ OSGi component attributes ----------------
  public BundleContext context;

  private RooBundleActivator rooBundleActivator;

  private Long lastTimeUpdateComponents;

  private static final Comparator<Object> COMPARATOR = new NaturalOrderComparator<Object>();
  private static final Logger LOGGER = HandlerUtils.getLogger(SimpleParser.class);

  static String isMatch(final String buffer, final String command, final boolean strictMatching) {
    if ("".equals(buffer.trim())) {
      return "";
    }
    final String[] commandWords = StringUtils.split(command, " ");
    int lastCommandWordUsed = 0;
    Validate.notEmpty(commandWords, "Command required");

    String bufferToReturn = null;
    String lastWord = null;

    next_buffer_loop: for (int bufferIndex = 0; bufferIndex < buffer.length(); bufferIndex++) {
      final String bufferSoFarIncludingThis = buffer.substring(0, bufferIndex + 1);
      final String bufferRemaining = buffer.substring(bufferIndex + 1);

      final int bufferLastIndexOfWord = bufferSoFarIncludingThis.lastIndexOf(" ");
      String wordSoFarIncludingThis = bufferSoFarIncludingThis;
      if (bufferLastIndexOfWord != -1) {
        wordSoFarIncludingThis = bufferSoFarIncludingThis.substring(bufferLastIndexOfWord);
      }

      if (wordSoFarIncludingThis.equals(" ") || bufferIndex == buffer.length() - 1) {
        if (bufferIndex == buffer.length() - 1 && !"".equals(wordSoFarIncludingThis.trim())) {
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
      if (!strictMatching || lastCommandWordUsed + 1 == commandWords.length) {
        return bufferToReturn;
      }
    }

    return null; // Not a match
  }

  private final Map<String, MethodTarget> availabilityIndicators =
      new HashMap<String, MethodTarget>();
  private final Map<String, MethodTarget> dynamicMandatoryIndicators =
      new HashMap<String, MethodTarget>();
  private final Map<String, MethodTarget> optionVisibilityIndicators =
      new HashMap<String, MethodTarget>();
  private final Map<String, MethodTarget> optionAutocompleteIndicators =
      new HashMap<String, MethodTarget>();
  private final Set<CommandMarker> commands = new HashSet<CommandMarker>();
  private final Set<Converter<?>> converters = new HashSet<Converter<?>>();

  // ROO-3697: Include global parameters in all Spring Roo commands.
  private final List<String> globalParameters = new ArrayList<String>();

  private final Object mutex = new Object();

  public final void add(final CommandMarker command) {
    synchronized (mutex) {
      commands.add(command);
      for (final Method method : command.getClass().getMethods()) {

        // Getting method availability indicators
        final CliAvailabilityIndicator availability =
            method.getAnnotation(CliAvailabilityIndicator.class);
        if (availability != null) {
          Validate.isTrue(method.getParameterTypes().length == 0,
              "CliAvailabilityIndicator is only legal for 0 parameter methods ('%s')",
              method.toGenericString());
          Validate.isTrue(method.getReturnType().equals(Boolean.TYPE),
              "CliAvailabilityIndicator is only legal for primitive boolean return types (%s)",
              method.toGenericString());
          for (final String cmd : availability.value()) {
            Validate.isTrue(!availabilityIndicators.containsKey(cmd),
                "Cannot specify an availability indicator for '%s' more than once", cmd);
            availabilityIndicators.put(cmd, new MethodTarget(method, command));
          }
        }

        // Getting method dynamicMandatory indicators
        final CliOptionMandatoryIndicator dynamicMandatoryIndicator =
            method.getAnnotation(CliOptionMandatoryIndicator.class);
        if (dynamicMandatoryIndicator != null) {
          Validate.isTrue(method.getParameterTypes().length < 2,
              "CliDynamicMandatoryIndicator is only legal for 0 or 1 parameter methods ('%s')",
              method.toGenericString());
          if (method.getParameterTypes().length == 1) {
            Validate.isTrue(method.getParameterTypes()[0].equals(ShellContext.class),
                "CliDynamicMandatoryIndicator could only receive a ShellContext parameter ('%s')",
                method.toGenericString());
          }
          Validate.isTrue(method.getReturnType().equals(Boolean.TYPE),
              "CliDynamicMandatoryIndicator is only legal for primitive boolean return types (%s)",
              method.toGenericString());

          for (String param : dynamicMandatoryIndicator.params()) {
            dynamicMandatoryIndicators.put(
                dynamicMandatoryIndicator.command().concat("|").concat(param), new MethodTarget(
                    method, command));
          }

        }

        // Getting method options visibility indicators
        final CliOptionVisibilityIndicator optionVisibilityIndicator =
            method.getAnnotation(CliOptionVisibilityIndicator.class);
        if (optionVisibilityIndicator != null) {
          Validate.isTrue(method.getParameterTypes().length == 1,
              "CliOptionVisibilityIndicator must receive a ShellContext parameter ('%s')",
              method.toGenericString());
          Validate.isTrue(method.getParameterTypes()[0].equals(ShellContext.class),
              "CliOptionVisibilityIndicator must receive a ShellContext parameter ('%s')",
              method.toGenericString());
          Validate.isTrue(method.getReturnType().equals(Boolean.TYPE),
              "CliOptionVisibilityIndicator is only legal for primitive boolean return types (%s)",
              method.toGenericString());

          for (String param : optionVisibilityIndicator.params()) {
            optionVisibilityIndicators.put(
                optionVisibilityIndicator.command().concat("|").concat(param), new MethodTarget(
                    method, command));
          }
        }

        // Getting method option autocomplete indicators
        final CliOptionAutocompleteIndicator optionAutocompleteIndicator =
            method.getAnnotation(CliOptionAutocompleteIndicator.class);
        if (optionAutocompleteIndicator != null) {
          Validate.isTrue(
              (method.getParameterTypes().length == 0 || method.getParameterTypes().length == 1),
              "CliOptionAutocompleteIndicator must receive 0 or 1 ShellContext ('%s')",
              method.toGenericString());

          if (method.getParameterTypes().length == 1) {

            // Validate first parameter is ShellContext if any
            Validate
                .isTrue(
                    method.getParameterTypes()[0].equals(ShellContext.class),
                    "CliOptionAutocompleteIndicator must receive a ShellContext as first parameter ('%s')",
                    method.toGenericString());
          }

          // Checking the return type is a List<String>
          Validate
              .isTrue(method.getReturnType().isAssignableFrom(List.class),
                  "CliOptionAutocompleteIndicator must return a List<String>",
                  method.toGenericString());

          // Add method to option autocomplete indicators
          optionAutocompleteIndicators.put(optionAutocompleteIndicator.command().concat("|")
              .concat(optionAutocompleteIndicator.param()), new MethodTarget(method, command));

        }

        // ROO-3697: Including global parameters.
        globalParameters.add("force");
        globalParameters.add("profile");
      }
    }
  }

  public final void add(final Converter<?> converter) {
    synchronized (mutex) {
      converters.add(converter);
    }
  }

  protected void commandNotFound(final Logger logger, final String buffer) {
    logger.warning("Command '" + buffer + "' not found (for assistance press "
        + AbstractShell.completionKeys + " or type \"hint\" then hit ENTER)");
  }

  public int complete(final String buffer, final int cursor, final List<String> candidates) {
    final List<Completion> completions = new ArrayList<Completion>();
    final int result = completeAdvanced(buffer, cursor, completions);
    for (final Completion completion : completions) {
      candidates.add(completion.getValue());
    }
    return result;
  }

  public int completeAdvanced(String buffer, int cursor, final List<Completion> candidates) {
    synchronized (mutex) {

      // Create ShellContext
      ShellContextImpl shellContext = new ShellContextImpl();

      // Set command written input until now
      shellContext.setExecutedCommand(buffer);

      // ROO-3622: Validate if version change
      if (isDifferentVersion()) {
        return 0;
      }

      // Loading converters and commands if needed
      loadConvertersAndCommands();

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

      // Replace extra "-" by only "--"
      while (buffer.contains("---")) {
        buffer = StringUtils.replace(buffer, "---", "--", 1);
        cursor--;
      }

      // Begin by only including the portion of the buffer represented to
      // the present cursor position
      final String translated = buffer.substring(0, cursor);

      // Start by locating a method that matches
      final Collection<MethodTarget> targets = locateTargets(translated, false, true);
      final SortedSet<Completion> results = new TreeSet<Completion>(COMPARATOR);

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

          results.add(new Completion(target.getKey().substring(0, stopAt) + " "));
        }
        candidates.addAll(results);
        return 0;
      }

      // There is a single target of this method, so provide completion
      // services for it
      final MethodTarget methodTarget = targets.iterator().next();

      // Identify the command we're working with
      final CliCommand cmd = methodTarget.getMethod().getAnnotation(CliCommand.class);
      Validate.notNull(cmd, "CliCommand unavailable for '%s'", methodTarget.getMethod()
          .toGenericString());

      // Make a reasonable attempt at parsing the remainingBuffer
      Map<String, String> options;
      try {
        options = ParserUtils.tokenize(methodTarget.getRemainingBuffer());
      } catch (final IllegalArgumentException ex) {
        // Assume any IllegalArgumentException is due to a quotation
        // mark mismatch
        candidates.add(new Completion(translated + "\""));
        return 0;
      }

      // Set options as ShellContext parameters
      for (Entry<String, String> entry : options.entrySet()) {
        shellContext.setParameter(entry.getKey(), entry.getValue());
      }

      // Lookup arguments for this target
      final Annotation[][] parameterAnnotations =
          methodTarget.getMethod().getParameterAnnotations();

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

        // ROO-3697: Since Spring Roo 2.0, you could define a parameter
        // in methods without @CliOption annotation. This parameter will
        // not be
        // included on cliOptions list
        if (cliOption != null) {
          cliOptions.add(cliOption);
        }
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
      final List<CliOption> unspecified = new ArrayList<CliOption>(cliOptions);
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
        lastOptionKey = new ArrayList<String>(options.keySet()).get(options.keySet().size() - 1);
        lastOptionValue = options.get(lastOptionKey);
      }

      // Handle if they are trying to find out the available option keys;
      // always present option keys in order
      // of their declaration on the method signature, thus we can stop
      // when mandatory options are filled in
      if ((translated.endsWith(" ") || (translated.endsWith(" -") || translated.endsWith(" --")))
          && (!"".equals(lastOptionValue) || lastOptionKey == null)) {

        suggestOptionKey(shellContext, translated, results, methodTarget, options, unspecified,
            lastOptionValue, alreadySpecified);

        candidates.addAll(results);
        return 0;
      }

      // Handle suggesting an option key if they haven't got one presently
      // specified (or they've completed a full option key/value pair)
      if (lastOptionKey == null || !"".equals(lastOptionKey) && !"".equals(lastOptionValue)
          && translated.endsWith(" ")) {
        // We have either NEVER specified an option key/value pair
        // OR we have specified a full option key/value pair

        // Let's list some other options the user might want to try
        // (naturally skip the "" option, as that's the default)
        for (final CliOption include : unspecified) {

          // First check visibility
          if (isVisibleParam(methodTarget.getKey(), include, shellContext)) {

            for (final String value : include.key()) {
              // Manually determine if this non-mandatory but
              // unspecifiedDefaultValue=* requiring option is
              // able to
              // be bound
              if (!isMandatoryParam(methodTarget.getKey(), include, shellContext)
                  && "*".equals(include.unspecifiedDefaultValue()) && !"".equals(value)) {
                try {
                  for (final Converter<?> candidate : converters) {
                    // Find the target parameter
                    Class<?> paramType = null;
                    int index = -1;
                    for (final Annotation[] a : methodTarget.getMethod().getParameterAnnotations()) {
                      index++;
                      for (final Annotation an : a) {
                        if (an instanceof CliOption) {
                          if (an.equals(include)) {
                            // Found the parameter,
                            // so
                            // store it
                            paramType = methodTarget.getMethod().getParameterTypes()[index];
                            break;
                          }
                        }
                      }
                    }
                    if (paramType != null && candidate.supports(paramType, include.optionContext())) {
                      // Try to invoke this usable
                      // converter
                      candidate.convertFromText("*", paramType, include.optionContext());
                      // If we got this far, the converter
                      // is
                      // happy with "*" so we need not
                      // bother
                      // the user with entering the data
                      // in
                      // themselves
                      break;
                    }
                  }
                } catch (final RuntimeException notYetReady) {
                  if (translated.endsWith(" ")) {
                    results.add(new Completion(translated + "--" + value + " "));
                  } else {
                    results.add(new Completion(translated + " --" + value + " "));
                  }
                  continue;
                }
              }

              // Handle normal mandatory options
              if (!"".equals(value)
                  && isMandatoryParam(methodTarget.getKey(), include, shellContext)) {
                if (translated.endsWith(" ")) {
                  results.add(new Completion(translated + "--" + value + " "));
                } else {
                  results.add(new Completion(translated + " --" + value + " "));
                }
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

      // Handle completing the option key supposing the last value was
      // wanted as null ("")
      if ((methodTarget.getRemainingBuffer().endsWith("--") || methodTarget.getRemainingBuffer()
          .endsWith(" -")) && "".equals(lastOptionValue)) {
        suggestOptionKey(shellContext, translated, results, methodTarget, options, unspecified,
            lastOptionValue, alreadySpecified);

        candidates.addAll(results);
        return 0;
      }

      // Handle completing the option key they're presently typing
      if ((lastOptionValue == null || "".equals(lastOptionValue)) && (!translated.endsWith(" "))) {

        // Given we haven't got an option value of any form, and there's
        // no space at the buffer end, we must still be typing an option
        // key

        // Check if there are still dynamic mandatory options
        List<CliOption> remainingMandatories = new ArrayList<CliOption>();
        for (final CliOption include : unspecified) {
          if (isMandatoryParam(methodTarget.getKey(), include, shellContext)) {
            remainingMandatories.add(include);
          }
        }

        // Check if user is still writing the last mandatory key
        // which will already be in the last position of
        // "alreadySpecified"
        if (alreadySpecified.size() > 0) {
          List<String> paramsKeys =
              Arrays.asList(alreadySpecified.get(alreadySpecified.size() - 1).key());
          if (paramsKeys.contains(lastOptionKey)) {

            // User is still writing mandatory
            remainingMandatories.add(alreadySpecified.get(alreadySpecified.size() - 1));
          }
        }

        if (remainingMandatories.size() != 0) {

          // Complete with remaining mandatory keys
          for (final CliOption option : remainingMandatories) {

            // Check option's visibility
            if (isVisibleParam(methodTarget.getKey(), option, shellContext)) {

              for (final String value : option.key()) {
                if (value != null && lastOptionKey != null
                    && value.regionMatches(true, 0, lastOptionKey, 0, lastOptionKey.length())) {
                  final String completionValue =
                      translated.substring(0, translated.length() - lastOptionKey.length()) + value
                          + " ";
                  results.add(new Completion(completionValue));
                }
              }
            }
          }
        } else {

          // Complete with optional keys or global parameters
          for (final CliOption option : unspecified) {

            // Check option's visibility
            if (isVisibleParam(methodTarget.getKey(), option, shellContext)) {

              for (final String value : option.key()) {
                if (value != null && lastOptionKey != null
                    && value.regionMatches(true, 0, lastOptionKey, 0, lastOptionKey.length())) {
                  final String completionValue =
                      translated.substring(0, translated.length() - lastOptionKey.length()) + value
                          + " ";
                  results.add(new Completion(completionValue));
                }
              }
            }
          }

          // ROO-3697: check if current key completion is a
          // globalParameter
          if (hasShellContextParameter(methodTarget.getMethod())) {
            for (final String parameter : globalParameters) {
              if (parameter != null && lastOptionKey != null
                  && parameter.regionMatches(true, 0, lastOptionKey, 0, lastOptionKey.length())) {
                final String completionValue =
                    translated.substring(0, translated.length() - lastOptionKey.length())
                        + parameter + " ";
                results.add(new Completion(completionValue));
              }
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
        final Class<?>[] parameterTypes = methodTarget.getMethod().getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
          final Class<?> parameterType = parameterTypes[i];

          // ROO-3697: Check if parameter type is ShellContext
          if (parameterType.isAssignableFrom(ShellContext.class)) {
            break;
          }

          final CliOption option = cliOptions.get(i);

          for (final String key : option.key()) {
            if (key.equals(lastOptionKey)) {

              // Check if CliOption is boolean mandatory and has specific value
              if (!option.specifiedDefaultValue().equals(CliOption.NULL)
                  && parameterType.equals(boolean.class)) {

                // Complete with next option keys and let current key with default value
                suggestOptionKey(shellContext, translated, results, methodTarget, options,
                    unspecified, lastOptionValue, alreadySpecified);

                candidates.addAll(results);
                return 0;
              }

              final List<Completion> allValues = new ArrayList<Completion>();
              String suffix = " ";


              // First, use an autocomplete indicator if any
              List<String> values =
                  getPossibleValuesByIndicator(methodTarget.getKey(), option, shellContext);

              if (values != null) {
                if (!hasToIncludeSpaceOnFinish(methodTarget.getKey(), option)) {
                  suffix = "";
                }

                for (String value : values) {
                  allValues.add(new Completion(value));
                }
              } else {

                // Use a Converter if one is available
                for (final Converter<?> candidate : converters) {
                  if (candidate.supports(parameterType, option.optionContext())) {
                    // Found a usable converter
                    final boolean addSpace =
                        candidate.getAllPossibleValues(allValues, parameterType, lastOptionValue,
                            option.optionContext(), methodTarget);
                    if (!addSpace) {
                      suffix = "";
                    }
                    break;
                  }
                }
              }

              if (allValues.isEmpty()) {

                // Doesn't appear to be a custom Converter, so
                // let's go and provide defaults for simple
                // types

                // Provide some simple options for common types
                if (Boolean.class.isAssignableFrom(parameterType)
                    || Boolean.TYPE.isAssignableFrom(parameterType)) {
                  allValues.add(new Completion("true"));
                  allValues.add(new Completion("false"));
                }

                if (Number.class.isAssignableFrom(parameterType)) {
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
                  results.add(new Completion(prefix + currentValue.getValue() + suffix,
                      currentValue.getFormattedValue(), currentValue.getHeading(), currentValue
                          .getOrder()));
                } else {
                  // Only add the result **if** what they've
                  // typed is compatible *AND* they haven't
                  // already typed it in full
                  if (currentValue.getValue().toLowerCase()
                      .startsWith(lastOptionValue.toLowerCase())
                      && !lastOptionValue.equalsIgnoreCase(currentValue.getValue())
                      && lastOptionValue.length() < currentValue.getValue().length()) {
                    results.add(new Completion(prefix + currentValue.getValue() + suffix,
                        currentValue.getFormattedValue(), currentValue.getHeading(), currentValue
                            .getOrder()));
                  }
                }
              }

              // ROO-389: give inline options given there's
              // multiple choices available and we want to help
              // the user
              final StringBuilder help = new StringBuilder();
              help.append(LINE_SEPARATOR);
              help.append(isMandatoryParam(methodTarget.getKey(), option, shellContext) ? "required --"
                  : "optional --");
              if ("".equals(option.help())) {
                help.append(lastOptionKey).append(": ").append("No help available");
              } else {
                help.append(lastOptionKey).append(": ").append(option.help());
              }
              if (option.specifiedDefaultValue().equals(option.unspecifiedDefaultValue())) {
                if (option.specifiedDefaultValue().equals(NULL)) {
                  help.append("; no default value");
                } else {
                  help.append("; default: '").append(option.specifiedDefaultValue()).append("'");
                }
              } else {
                if (!"".equals(option.specifiedDefaultValue())
                    && !NULL.equals(option.specifiedDefaultValue())) {
                  help.append("; default if option present: '")
                      .append(option.specifiedDefaultValue()).append("'");
                }
                if (!"".equals(option.unspecifiedDefaultValue())
                    && !NULL.equals(option.unspecifiedDefaultValue())) {
                  help.append("; default if option not present: '")
                      .append(option.unspecifiedDefaultValue()).append("'");
                }
              }
              LOGGER.info(help.toString());

              if (results.size() == 1) {
                final String suggestion = results.iterator().next().getValue().trim();
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

  /**
   * Suggests command option keys when the user try to autocomplete.
   * 
   * @param shellContext
   * @param translated
   * @param results
   * @param methodTarget
   * @param options
   * @param unspecified
   */
  private void suggestOptionKey(ShellContextImpl shellContext, final String translated,
      final SortedSet<Completion> results, final MethodTarget methodTarget,
      Map<String, String> options, final List<CliOption> unspecified, String lastOptionValue,
      List<CliOption> alreadySpecified) {

    boolean showAllRemaining = true;

    // Check if there are still dynamic mandatory options
    for (final CliOption include : unspecified) {
      if (isMandatoryParam(methodTarget.getKey(), include, shellContext)) {
        showAllRemaining = false;
        break;
      }
    }

    // If there is a CliOption with void key and user only write a "-", last
    // value will be "-" so is needed to complete with the secondary value of the key
    if (lastOptionValue != null && lastOptionValue.equals("-") && showAllRemaining == true) {
      for (CliOption option : alreadySpecified) {
        if (option.key().length > 1) {
          for (String key : option.key()) {
            if (!("").equals(key)) {
              results.add(new Completion(translated.concat("-").concat(key).concat(" ")));
              showAllRemaining = false;
            }
          }
        }
      }
    }

    // Exists some mandatory param, so we need to show it first if
    // it is visible
    if (!showAllRemaining) {
      for (final CliOption include : unspecified) {
        if (isMandatoryParam(methodTarget.getKey(), include, shellContext)
            && isVisibleParam(methodTarget.getKey(), include, shellContext)) {

          // Auto complete with that mandatory key
          for (final String value : include.key()) {
            if (!"".equals(value)) {

              // Check if should complete with "--" prefix
              if (methodTarget.getRemainingBuffer().endsWith("--")) {
                results.add(new Completion(StringUtils.stripEnd(translated, null).concat(value)
                    .concat(" ")));
              } else if (methodTarget.getRemainingBuffer().endsWith(" -")
                  || methodTarget.getRemainingBuffer().length() == 1) {
                results.add(new Completion(StringUtils.stripEnd(translated, null).concat("-")
                    .concat(value).concat(" ")));
              } else {
                results.add(new Completion(translated.concat("--").concat(value).concat(" ")));
              }
            }
          }
          break;
        }
      }
    } else {

      // No more mandatory options remaining
      for (final CliOption include : unspecified) {

        if (isVisibleParam(methodTarget.getKey(), include, shellContext)) {

          // Auto complete with that key
          for (final String value : include.key()) {
            if (!"".equals(value)) {

              // Check if should complete with "--" prefix
              if (methodTarget.getRemainingBuffer().endsWith("--")) {
                results.add(new Completion(StringUtils.stripEnd(translated, null).concat(value)
                    .concat(" ")));
              } else if (methodTarget.getRemainingBuffer().endsWith(" -")
                  || methodTarget.getRemainingBuffer().length() == 1) {
                results.add(new Completion(StringUtils.stripEnd(translated, null).concat("-")
                    .concat(value).concat(" ")));
              } else {
                results.add(new Completion(translated.concat("--").concat(value).concat(" ")));
              }
            }
          }
        }
      }

      // ROO-3697: Including global parameters in all Spring Roo
      // commands
      // if all mandatory parameters have been defined and exists
      // a ShellContext
      // parameter defined on last position of current method.
      if (hasShellContextParameter(methodTarget.getMethod())) {
        for (String parameter : globalParameters) {

          // Check if this global parameter is already defined
          if (!options.containsKey(parameter)) {
            if (!"".equals(parameter)) {

              // Check if should complete with "--" prefix
              if (methodTarget.getRemainingBuffer().endsWith("--")) {
                results.add(new Completion(StringUtils.stripEnd(translated, null).concat(parameter)
                    .concat(" ")));
              } else if (methodTarget.getRemainingBuffer().endsWith(" -")
                  || methodTarget.getRemainingBuffer().length() == 1) {
                results.add(new Completion(StringUtils.stripEnd(translated, null).concat("-")
                    .concat(parameter).concat(" ")));
              } else {
                results.add(new Completion(translated.concat("--").concat(parameter).concat(" ")));
              }
            }
          }
        }
      }
    }
  }

  private MethodTarget getAvailabilityIndicator(final String command) {
    return availabilityIndicators.get(command);
  }

  private Set<CliOption> getCliOptions(final Annotation[][] parameterAnnotations) {
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

  /**
   * This method loads converters and commands if needed
   */
  public void loadConvertersAndCommands() {
    synchronized (mutex) {

      boolean someComponentChanges = false;

      if (commands.isEmpty() || hasToReloadComponents()) {
        // Cleaning commands and indicators
        commands.clear();
        availabilityIndicators.clear();
        // Get all Services implement CommandMarker interface
        try {
          ServiceReference<?>[] references =
              this.context.getAllServiceReferences(CommandMarker.class.getName(), null);

          for (ServiceReference<?> ref : references) {
            CommandMarker command = (CommandMarker) this.context.getService(ref);
            add(command);
          }

        } catch (InvalidSyntaxException e) {
          LOGGER.warning("Cannot load CommandMarker on SimpleParser.");
        }

        someComponentChanges = true;
      }

      if (converters.isEmpty() || hasToReloadComponents()) {
        // Cleaning converters
        converters.clear();
        // Get all Services implement Converter interface
        try {
          ServiceReference<?>[] references =
              this.context.getAllServiceReferences(Converter.class.getName(), null);

          for (ServiceReference<?> ref : references) {
            Converter<?> converter = (Converter<?>) this.context.getService(ref);
            add(converter);
          }

        } catch (InvalidSyntaxException e) {
          LOGGER.warning("Cannot load Converter on SimpleParser.");
        }

        someComponentChanges = true;

      }

      if (someComponentChanges) {
        setLasTimeUpdateComponents(System.currentTimeMillis());
      }

    }
  }

  public Set<String> getEveryCommand() {
    synchronized (mutex) {

      loadConvertersAndCommands();

      // Return commands list
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



  private Set<String> getSpecifiedUnavailableOptions(final Set<CliOption> cliOptions,
      final Map<String, String> options) {
    final Set<String> cliOptionKeySet = new LinkedHashSet<String>();
    for (final CliOption cliOption : cliOptions) {
      for (final String key : cliOption.key()) {
        cliOptionKeySet.add(key);
      }
    }

    final Set<String> unavailableOptions = new LinkedHashSet<String>();
    for (final String suppliedOption : options.keySet()) {
      // ROO-3697: Check if current parameter is a global parameter.
      boolean isGlobalParameter = globalParameters.contains(suppliedOption);

      if (!cliOptionKeySet.contains(suppliedOption) && !isGlobalParameter) {
        unavailableOptions.add(suppliedOption);
      }
    }
    return unavailableOptions;
  }

  private Collection<MethodTarget> locateTargets(final String buffer, final boolean strictMatching,
      final boolean checkAvailabilityIndicators) {

    if (commands.isEmpty() || hasToReloadComponents()) {
      // Cleaning commands
      commands.clear();
      availabilityIndicators.clear();
      // Get all Services implement CommandMarker interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(CommandMarker.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          CommandMarker command = (CommandMarker) this.context.getService(ref);
          add(command);
        }

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load CommandMarker on SimpleParser.");
      }

      setLasTimeUpdateComponents(System.currentTimeMillis());
    }

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
                Validate.isTrue(
                    available == null,
                    "More than one availability indicator is defined for '"
                        + method.toGenericString() + "'");
                try {
                  available = (Boolean) mt.getMethod().invoke(mt.getTarget());
                  // We should "break" here, but we loop over
                  // all to ensure no conflicting availability
                  // indicators are defined
                } catch (final Exception e) {
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
            final String remainingBuffer = isMatch(buffer, value, strictMatching);
            if (remainingBuffer != null) {
              result.add(new MethodTarget(method, command, remainingBuffer, value));
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

  public ParseResult parse(final String rawInput) {
    synchronized (mutex) {

      // Load converters and commands if needed
      loadConvertersAndCommands();

      Validate.notNull(rawInput, "Raw input required");
      final String input = normalise(rawInput);

      // Locate the applicable targets which match this buffer
      final Collection<MethodTarget> matchingTargets = locateTargets(input, true, true);
      if (matchingTargets.isEmpty()) {
        // Before we just give up, let's see if we can offer a more
        // informative message to the user
        // by seeing the command is simply unavailable at this point in
        // time
        CollectionUtils.populate(matchingTargets, locateTargets(input, true, false));
        if (matchingTargets.isEmpty()) {
          commandNotFound(LOGGER, input);
        } else {
          LOGGER
              .warning("Command '"
                  + input
                  + "' was found but is not currently available (type 'help' then ENTER to learn about this command)");
        }
        return null;
      }
      if (matchingTargets.size() > 1) {
        LOGGER.warning("Ambigious command '" + input + "' (for assistance press "
            + AbstractShell.completionKeys + " or type \"hint\" then hit ENTER)");
        return null;
      }
      final MethodTarget methodTarget = matchingTargets.iterator().next();

      // Argument conversion time
      final Annotation[][] parameterAnnotations =
          methodTarget.getMethod().getParameterAnnotations();
      if (parameterAnnotations.length == 0) {
        // No args
        return new ParseResult(methodTarget.getMethod(), methodTarget.getTarget(), null);
      }

      // Oh well, we need to convert some arguments
      final List<Object> arguments =
          new ArrayList<Object>(methodTarget.getMethod().getParameterTypes().length);

      // Attempt to parse
      Map<String, String> options = null;
      try {
        options = ParserUtils.tokenize(methodTarget.getRemainingBuffer());
      } catch (final IllegalArgumentException e) {
        LOGGER.warning(StringUtils.defaultIfBlank(ExceptionUtils.getRootCauseMessage(e),
            e.getMessage()));
        return null;
      }

      // Create ShellContext for checking visibility
      ShellContextImpl shellContext = new ShellContextImpl();

      // Save typed parameters
      for (Entry<String, String> option : options.entrySet()) {
        String parameter = option.getKey();
        String value = option.getValue();

        shellContext.setParameter(parameter, value);
      }

      final Set<CliOption> cliOptions = getCliOptions(parameterAnnotations);
      for (final CliOption cliOption : cliOptions) {
        final Class<?> requiredType =
            methodTarget.getMethod().getParameterTypes()[arguments.size()];

        // Validate visibility and values
        if (options.keySet().contains(cliOption.key()[0])) {

          checkVisibilityAndValues(methodTarget, options, shellContext, cliOption);
        }

        if (cliOption.systemProvided()) {
          Object result;
          if (SimpleParser.class.isAssignableFrom(requiredType)) {
            result = this;
          } else {
            LOGGER.warning("Parameter type '" + requiredType + "' is not system provided");
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
              LOGGER.warning("You cannot specify option '" + possibleKey
                  + "' when you have also specified '" + sourcedFrom + "' in the same command");
              return null;
            }
            sourcedFrom = possibleKey;
            value = options.get(possibleKey);
          }
        }

        // Ensure the user specified a value if the value is mandatory
        if (StringUtils.isBlank(value)
            && isMandatoryParam(methodTarget.getKey(), cliOption, shellContext)) {
          if ("".equals(cliOption.key()[0])) {
            final StringBuilder message = new StringBuilder("You must specify a default option ");
            if (cliOption.key().length > 1) {
              message.append("(otherwise known as option '").append(cliOption.key()[1])
                  .append("') ");
            }
            message.append("for this command");
            LOGGER.warning(message.toString());
            return null;
          } else if (!NULL.equals(cliOption.specifiedDefaultValue()) && value != null) {
            // Just continue execution
          } else {
            LOGGER.warning("You must specify option '" + cliOption.key()[0] + "' for this command");
            return null;
          }
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
                + requiredType.getSimpleName() + " for option '"
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
          CliOptionContext.setOptionContext(cliOption.optionContext());
          CliSimpleParserContext.setSimpleParserContext(this);
          Object result;
          Converter<?> c = null;
          for (final Converter<?> candidate : converters) {
            if (candidate.supports(requiredType, cliOption.optionContext())) {
              // Found a usable converter
              c = candidate;
              break;
            }
          }
          if (c == null) {
            throw new IllegalStateException("TODO: Add basic type conversion");
            // TODO Fall back to a normal SimpleTypeConverter and
            // attempt conversion
            // SimpleTypeConverter simpleTypeConverter = new
            // SimpleTypeConverter();
            // result =
            // simpleTypeConverter.convertIfNecessary(value,
            // requiredType, mp);
          }

          // Use the converter
          result = c.convertFromText(value, requiredType, cliOption.optionContext());

          // If the option has been specified to be mandatory then the
          // result should never be null
          if (result == null && isMandatoryParam(methodTarget.getKey(), cliOption, shellContext)) {
            throw new IllegalStateException();
          }
          arguments.add(result);
        } catch (final RuntimeException e) {
          LOGGER.warning(e.getClass().getName() + ": Failed to convert '" + value + "' to type "
              + requiredType.getSimpleName() + " for option '"
              + StringUtils.join(cliOption.key(), ",") + "'");
          if (StringUtils.isNotBlank(e.getMessage())) {
            LOGGER.warning(e.getMessage());
          }
          return null;
        } finally {
          CliOptionContext.resetOptionContext();
          CliSimpleParserContext.resetSimpleParserContext();
        }
      }

      // Check for options specified by the user but are unavailable for
      // the command
      final Set<String> unavailableOptions = getSpecifiedUnavailableOptions(cliOptions, options);
      if (!unavailableOptions.isEmpty()) {
        final StringBuilder message = new StringBuilder();
        if (unavailableOptions.size() == 1) {
          message.append("Option '").append(unavailableOptions.iterator().next())
              .append("' is not available for this command. ");
        } else {
          message.append("Options ")
              .append(collectionToDelimitedString(unavailableOptions, ", ", "'", "'"))
              .append(" are not available for this command. ");
        }
        message.append("Use tab assist or the \"help\" command to see the legal options");
        LOGGER.warning(message.toString());
        return null;
      }

      // ROO-3697: Use shellContext to save current shell parameters if
      // method contains ShellContext parameter
      if (hasShellContextParameter(methodTarget.getMethod())) {

        // Save executed command
        shellContext.setExecutedCommand(input);

        // Adding shellContext to command arguments
        arguments.add(shellContext);

      }

      return new ParseResult(methodTarget.getMethod(), methodTarget.getTarget(),
          arguments.toArray());
    }
  }

  /**
   * Check if executed command has compatible options and values based on its
   * indicators
   * 
   * @param methodTarget
   * @param options
   * @param shellContext
   * @param cliOption
   */
  private void checkVisibilityAndValues(final MethodTarget methodTarget,
      Map<String, String> options, ShellContextImpl shellContext, final CliOption cliOption) {

    // Ensure the user didn't specified a not visible value
    if (!this.isVisibleParam(methodTarget.getKey(), cliOption, shellContext)) {
      MethodTarget optionVisibilityIndicator =
          optionVisibilityIndicators.get(methodTarget.getKey().concat("|")
              .concat(cliOption.key()[0]));

      // The user specified incompatible options
      if (optionVisibilityIndicator != null && optionVisibilityIndicator.getMethod() != null) {
        CliOptionVisibilityIndicator visibilityIndicator =
            optionVisibilityIndicator.getMethod().getAnnotation(CliOptionVisibilityIndicator.class);

        // Get visibility indicator help message
        if (visibilityIndicator != null) {
          LOGGER.warning(visibilityIndicator.help());
          throw new RuntimeException();
        }
      }
    }

    // Ensure the user didn't specified an incorrect value if necessary
    if (autocompleteNeedsValidation(methodTarget.getKey(), cliOption)) {
      String value = options.get(cliOption.key()[0]);
      List<String> possibleValues =
          getPossibleValuesByIndicator(methodTarget.getKey(), cliOption, shellContext);
      if (possibleValues != null && !possibleValues.contains(value)) {

        // The user specified an incorrect value
        MethodTarget optionAutocompleteIndicator =
            optionAutocompleteIndicators.get(methodTarget.getKey().concat("|")
                .concat(cliOption.key()[0]));

        if (optionAutocompleteIndicator != null && optionAutocompleteIndicator.getMethod() != null) {
          CliOptionAutocompleteIndicator autocompleteIndicator =
              optionAutocompleteIndicator.getMethod().getAnnotation(
                  CliOptionAutocompleteIndicator.class);

          // Get autocomplete indicator help message
          if (autocompleteIndicator != null) {
            LOGGER.warning(autocompleteIndicator.help());
            throw new RuntimeException();
          }
        }
      }
    }
  }

  /**
   * Checks if some method has the ShellContext parameter
   * 
   * @param method
   * @return
   */
  private boolean hasShellContextParameter(Method method) {
    int paramNumbers = method.getParameterTypes().length;
    int shellContextPosition = 1;
    for (Class<?> methodParameters : method.getParameterTypes()) {
      if (methodParameters.isAssignableFrom(ShellContext.class)) {
        if (shellContextPosition != paramNumbers) {
          String msg =
              String
                  .format(
                      "ShellContext parameter is on position '%s' but should be defined after all '@CliOption' parameters.",
                      shellContextPosition);
          throw new RuntimeException(msg);
        }
        return true;
      }
      shellContextPosition++;
    }
    return false;
  }

  /**
   * Method that checks if given method attribute is mandatory. Takes in count
   * if this option could be dynamicMandatory.
   *
   * @param command
   * @param cliOption
   * @param shellContext
   * @return
   */
  private boolean isMandatoryParam(String command, CliOption cliOption, ShellContext shellContext) {
    if (cliOption.mandatory()) {
      String[] option = cliOption.key();
      try {
        MethodTarget dynamicMandatoryIndicator =
            dynamicMandatoryIndicators.get(command.concat("|").concat(option[0]));
        if (dynamicMandatoryIndicator == null) {
          return cliOption.mandatory();
        } else {
          if (dynamicMandatoryIndicator.getMethod().getParameterTypes().length == 1) {
            return (Boolean) dynamicMandatoryIndicator.getMethod().invoke(
                dynamicMandatoryIndicator.getTarget(), shellContext);
          }
          return (Boolean) dynamicMandatoryIndicator.getMethod().invoke(
              dynamicMandatoryIndicator.getTarget());
        }
      } catch (Exception e) {
        throw new RuntimeException(
            String
                .format(
                    "ERROR: Error trying to get mandatory value for '%s' option on '%s' command. Please, fix errors on indicator method.",
                    option[0], command));
      }
    } else {
      return cliOption.mandatory();
    }
  }

  /**
   * Method that checks if given method attribute is visible in the shell.
   * Takes in count the dependencies with other parameters in same command.
   *
   * @param command
   * @param cliOption
   * @param shellContext
   * @return
   */
  private boolean isVisibleParam(String command, CliOption cliOption, ShellContextImpl shellContext) {
    String[] option = cliOption.key();
    try {
      MethodTarget optionVisibilityIndicator =
          optionVisibilityIndicators.get(command.concat("|").concat(option[0]));
      if (optionVisibilityIndicator == null) {
        return true;
      } else {
        return (Boolean) optionVisibilityIndicator.getMethod().invoke(
            optionVisibilityIndicator.getTarget(), shellContext);
      }
    } catch (Exception e) {
      throw new RuntimeException(
          String
              .format(
                  "ERROR: Error trying to get option visibility value for '%s' option on '%s' command. Please, fix errors on indicator method.",
                  option[0], command));
    }
  }

  /**
   * Method that returns the autocompletion list of values for a command's
   * param, if any
   * 
   * @param command
   * @param cliOption
   * @param shellContext
   * @return List<String> with the possible values, or null if that parameter
   *         hasn't an autocompletion indicator
   */
  @SuppressWarnings("unchecked")
  private List<String> getPossibleValuesByIndicator(String command, CliOption cliOption,
      ShellContextImpl shellContext) {
    String[] option = cliOption.key();
    try {
      MethodTarget optionAutocompleteIndicator =
          optionAutocompleteIndicators.get(command.concat("|").concat(option[0]));
      if (optionAutocompleteIndicator == null) {
        return null;
      } else {
        if (optionAutocompleteIndicator.getMethod().getParameterTypes().length == 0) {
          return (List<String>) optionAutocompleteIndicator.getMethod().invoke(
              optionAutocompleteIndicator.getTarget());
        } else {
          return (List<String>) optionAutocompleteIndicator.getMethod().invoke(
              optionAutocompleteIndicator.getTarget(), shellContext);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(
          String
              .format(
                  "ERROR: Error trying to get autocomplete values for '%s' option on '%s' command. Please, fix errors on indicator method and also be sure return type is a List<String>",
                  option[0], command));
    }
  }

  private boolean hasToIncludeSpaceOnFinish(String command, CliOption cliOption) {
    String[] option = cliOption.key();
    try {
      MethodTarget optionAutocompleteIndicator =
          optionAutocompleteIndicators.get(command.concat("|").concat(option[0]));
      if (optionAutocompleteIndicator != null) {
        CliOptionAutocompleteIndicator autocompleteIndicator =
            optionAutocompleteIndicator.getMethod().getAnnotation(
                CliOptionAutocompleteIndicator.class);

        return autocompleteIndicator.includeSpaceOnFinish();

      }

      return true;
    } catch (Exception e) {
      throw new RuntimeException(
          String
              .format(
                  "ERROR: Error trying to get autocomplete values for '%s' option on '%s' command. Please, fix errors on indicator method and also be sure return type is a List<String>",
                  option[0], command));
    }
  }

  /**
   * Checks if exists {@link CliOptionAutocompleteIndicator} for a {@link CliOption} and 
   * returns if value specified for the option must be validated with autocomplete results
   * 
   * @param command
   * @param cliOption
   * @return <code>true</code> if value specified shoud be validated with indicator return values
   */
  private boolean autocompleteNeedsValidation(String command, CliOption cliOption) {
    String[] option = cliOption.key();
    try {
      MethodTarget optionAutocompleteIndicator =
          optionAutocompleteIndicators.get(command.concat("|").concat(option[0]));
      if (optionAutocompleteIndicator != null) {
        CliOptionAutocompleteIndicator autocompleteIndicator =
            optionAutocompleteIndicator.getMethod().getAnnotation(
                CliOptionAutocompleteIndicator.class);

        return autocompleteIndicator.validate();

      }

      return true;
    } catch (Exception e) {
      throw new RuntimeException(
          String
              .format(
                  "ERROR: Error trying to get autocomplete values for '%s' option on '%s' command. Please, fix errors on indicator method and also be sure return type is a List<String>",
                  option[0], command));
    }
  }

  private String collectionToDelimitedString(final Collection<?> coll, final String delim,
      final String prefix, final String suffix) {
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
        final CliAvailabilityIndicator availability =
            m.getAnnotation(CliAvailabilityIndicator.class);
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

  /**
   * This method compares RooBundleActivator lastTimeBundleChange with
   * SimpleParser lasTimeUpdateCommands
   * 
   * @return
   */
  private boolean hasToReloadComponents() {
    if (getRooBundleActivator() != null) {
      return getRooBundleActivator().getLastTimeBundleChange() > getLasTimeUpdateComponents();
    }
    return true;
  }

  /**
   * @return the lasTimeUpdateCommands
   */
  public Long getLasTimeUpdateComponents() {
    return lastTimeUpdateComponents == null ? Long.MIN_VALUE : lastTimeUpdateComponents;
  }

  /**
   * @param lasTimeUpdateCommands the lasTimeUpdateCommands to set
   */
  public void setLasTimeUpdateComponents(Long lastTimeUpdateComponents) {
    this.lastTimeUpdateComponents = lastTimeUpdateComponents;
  }

  /**
   * Obtains all Services that implements RooBundleActivator
   * 
   * @return
   */
  public RooBundleActivator getRooBundleActivator() {
    if (rooBundleActivator == null) {
      // Get all Services implement RooBundleActivator interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(RooBundleActivator.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          rooBundleActivator = (RooBundleActivator) context.getService(ref);
          return rooBundleActivator;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load RooBundleActivator on SimpleParser.");
        return null;
      }
    } else {
      return rooBundleActivator;
    }

  }

  private boolean isDifferentVersion() {
    String rooVersion = getRooProjectVersion();

    if ("UNKNOWN".equals(rooVersion)) {
      return false;
    }

    return !rooVersion.equals(versionInfoWithoutGit());
  }

  private String getRooProjectVersion() {
    String homePath = new File(".").getPath();
    String pomPath = homePath + "/pom.xml";
    File pom = new File(pomPath);
    try {
      if (pom.exists()) {
        InputStream is = new FileInputStream(pom);
        Document docXml = XmlUtils.readXml(is);
        Element document = docXml.getDocumentElement();
        Element rooVersionElement = XmlUtils.findFirstElement("properties/roo.version", document);
        String rooVersion = rooVersionElement.getTextContent();

        return rooVersion;
      }

      return "UNKNOWN";

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    return "";
  }

  public static String versionInfoWithoutGit() {
    // Try to determine the bundle version
    String bundleVersion = null;
    JarFile jarFile = null;
    try {
      final URL classContainer =
          AbstractShell.class.getProtectionDomain().getCodeSource().getLocation();
      if (classContainer.toString().endsWith(".jar")) {
        // Attempt to obtain the "Bundle-Version" version from the
        // manifest
        jarFile = new JarFile(new File(classContainer.toURI()), false);
        final ZipEntry manifestEntry = jarFile.getEntry("META-INF/MANIFEST.MF");
        final Manifest manifest = new Manifest(jarFile.getInputStream(manifestEntry));
        bundleVersion = manifest.getMainAttributes().getValue("Bundle-Version");
      }
    } catch (final IOException ignoreAndMoveOn) {
    } catch (final URISyntaxException ignoreAndMoveOn) {
    } finally {
      if (jarFile != null) {
        try {
          jarFile.close();
        } catch (final IOException ignored) {
        }
      }
    }

    final StringBuilder sb = new StringBuilder();

    if (bundleVersion != null) {
      sb.append(bundleVersion);
    }

    if (sb.length() == 0) {
      sb.append("UNKNOWN VERSION");
    }

    return sb.toString();
  }
}
