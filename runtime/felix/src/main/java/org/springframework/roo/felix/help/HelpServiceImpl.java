package org.springframework.roo.felix.help;

import static org.apache.commons.io.IOUtils.LINE_SEPARATOR;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.shell.AbstractShell;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;
import org.springframework.roo.shell.NaturalOrderComparator;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Default implementation of {@link HelpService}.
 * 
 * @author Enrique Ruiz at http://www.disid.com[DISID Corporation S.L.]
 * @author Juan Carlos García at http://www.disid.com[DISID Corporation S.L.]
 * @since 1.3
 */
@Component
@Service
public class HelpServiceImpl implements HelpService {

  private static final Logger LOGGER = HandlerUtils.getLogger(HelpServiceImpl.class);

  // ------------ OSGi component attributes ----------------
  public BundleContext context;
  private static final Comparator<Object> COMPARATOR = new NaturalOrderComparator<Object>();

  private final Map<String, MethodTarget> availabilityIndicators =
      new HashMap<String, MethodTarget>();
  private final Set<CommandMarker> commands = new HashSet<CommandMarker>();
  private final Set<Converter<?>> converters = new HashSet<Converter<?>>();

  static final String NULL = "__NULL__";

  private final Object mutex = new Object();

  protected void activate(final ComponentContext cContext) {
    context = cContext.getBundleContext();
  }

  /**
   * {@inheritDoc}
   * 
   * TODO: Refactor this method to use the Freemarker template engine. See {@link #obtainHelp(String)}.
   */
  public void helpReferenceGuide() {
    synchronized (mutex) {

      // Get all Services implement CommandMarker interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(CommandMarker.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          CommandMarker command = (CommandMarker) this.context.getService(ref);
          if (!commands.contains(command)) {
            add(command);
          }
        }

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load CommandMarker on SimpleParser.");
      }

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
      final SortedMap<String, Object> sections = new TreeMap<String, Object>(COMPARATOR);
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
              throw new IllegalStateException("Section name '" + sectionName + "' not unique");
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
        final SortedMap<String, Element> individualCommands =
            new TreeMap<String, Element>(COMPARATOR);

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
            for (final Annotation[] ann : m.getParameterAnnotations()) {
              for (final Annotation a : ann) {
                if (a instanceof CliOption) {
                  final CliOption option = (CliOption) a;
                  // Figure out which key we want to use (use
                  // first non-empty string, or make it
                  // "(default)" if needed)
                  String key = option.key()[0];
                  if ("".equals(key)) {
                    for (final String otherKey : option.key()) {
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
                  } else {
                    help.append(option.help());
                  }
                  if (option.specifiedDefaultValue().equals(option.unspecifiedDefaultValue())) {
                    if (option.specifiedDefaultValue().equals(null)) {
                      help.append("; no default value");
                    } else {
                      help.append("; default: '").append(option.specifiedDefaultValue())
                          .append("'");
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
                  help.append(option.mandatory() ? " " : "");

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
            Element variableListElement = document.createElement("variablelist");
            boolean anyVars = false;
            for (final String optionKey : optionKeys) {
              anyVars = true;
              final String help = optionDetails.get(optionKey);
              variableListElement.appendChild(new XmlElementBuilder("varlistentry", document)
                  .addChild(new XmlElementBuilder("term", document).setText(optionKey).build())
                  .addChild(
                      new XmlElementBuilder("listitem", document).addChild(
                          new XmlElementBuilder("para", document).setText(help).build()).build())
                  .build());
            }

            if (!anyVars) {
              variableListElement =
                  new XmlElementBuilder("para", document).setText(
                      "This command does not accept any options.").build();
            }

            // Now we've figured out the options, store this
            // individual command
            final CDATASection progList = document.createCDATASection(cmdSyntax.toString());
            final String safeName =
                cmd.value()[0].replace("\\", "BCK").replace("/", "FWD").replace("*", "ASX");
            final Element element =
                new XmlElementBuilder("section", document)
                    .addAttribute("xml:id",
                        "command-index-" + safeName.toLowerCase().replace(' ', '-'))
                    .addChild(
                        new XmlElementBuilder("title", document).setText(cmd.value()[0]).build())
                    .addChild(new XmlElementBuilder("para", document).setText(cmd.help()).build())
                    .addChild(
                        new XmlElementBuilder("programlisting", document).addChild(progList)
                            .build()).addChild(variableListElement).build();

            individualCommands.put(cmdSyntax.toString(), element);
          }
        }

        final Element topSection = document.createElement("section");
        topSection.setAttribute("xml:id", "command-index-"
            + section.toLowerCase().replace(' ', '-'));
        topSection.appendChild(new XmlElementBuilder("title", document).setText(section).build());
        topSection.appendChild(new XmlElementBuilder("para", document).setText(
            section + " are contained in " + target.getClass().getName() + ".").build());

        for (final Element value : individualCommands.values()) {
          topSection.appendChild(value);
        }

        builtSections.add(topSection);
      }

      final Element appendix = document.createElement("appendix");
      appendix.setAttribute("xmlns", "http://docbook.org/ns/docbook");
      appendix.setAttribute("version", "5.0");
      appendix.setAttribute("xml:id", "command-index");
      appendix.appendChild(new XmlElementBuilder("title", document).setText("Command Index")
          .build());
      appendix.appendChild(new XmlElementBuilder("para", document).setText(
          "This appendix was automatically built from Roo " + AbstractShell.versionInfo() + ".")
          .build());
      appendix
          .appendChild(new XmlElementBuilder("para", document)
              .setText(
                  "Commands are listed in alphabetic order, and are shown in monospaced font with any mandatory options you must specify when using the command. Most commands accept a large number of options, and all of the possible options for each command are presented in this appendix.")
              .build());

      for (final Element section : builtSections) {
        appendix.appendChild(section);
      }
      document.appendChild(appendix);

      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

      final Transformer transformer = XmlUtils.createIndentingTransformer();
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
        FileUtils.writeByteArrayToFile(output, byteArrayOutputStream.toByteArray());
      } catch (final IOException ioe) {
        throw new IllegalStateException(ioe);
      } finally {
        IOUtils.closeQuietly(byteArrayOutputStream);
      }
    }
  }

  // @formatter:off
  private final String cmdIndexTemplateStr = 
      "[#ftl]" 
      + "${LINE_SEPARATOR}"
      + "COMMAND INDEX ${LINE_SEPARATOR}" 
      + "${LINE_SEPARATOR}" 
      + "[#list commands?keys as key]"
      + "[#list commands[key] as line]"
      + "[#if key?trim?length gte CMD_MAX_LENGTH && line?is_first]"
      + "${key} ${LINE_SEPARATOR}"
      + "${line}"
      + "[#elseif line?is_first]"
      + "${key}${line?trim}"
      + "[#else]"
      + "${line}"
      + "[/#if]"
      + "${LINE_SEPARATOR}" 
      + "[/#list]"
      + "${LINE_SEPARATOR}" 
      + "[/#list]";

  private final String cmdTemplateStr = 
      "[#ftl]" 
      + "${LINE_SEPARATOR}"

      + "SYNOPSIS ${LINE_SEPARATOR}" 
      + "[#list synopsis as item]"
      + "[#if item?trim?length &gt; 0]"
      + "${item} [#if options?has_content][OPTIONS][/#if]${LINE_SEPARATOR}" 
      + "[/#if]"
      + "[/#list]" 
      + "${LINE_SEPARATOR}"

      + "DESCRIPTION ${LINE_SEPARATOR}"
      + "[#list description as line]"
      + "${line}${LINE_SEPARATOR}" 
      + "[/#list]" 
      + "${LINE_SEPARATOR}"

      + "[#if options?has_content]"
      + "OPTIONS ${LINE_SEPARATOR}" 
      + "[#list options?keys as key]"
      + "[#list options[key] as line]"
      + "[#if key?trim?length gte OPT_MAX_LENGTH && line?is_first]"
      + "${key} ${LINE_SEPARATOR}"
      + "${line}"
      + "[#elseif line?is_first]"
      + "${key}${line?trim}"
      + "[#else]"
      + "${line}"
      + "[/#if]"
      + "${LINE_SEPARATOR}"
      + "[/#list]"
      + "${LINE_SEPARATOR}"
      + "[/#list]"
      + "[/#if]";
  // @formatter:on

  private static int LINE_MAX_LENGTH = 80;
  private static int CMD_MAX_LENGTH = 20;
  private static int OPT_MAX_LENGTH = 10;
  private static int CMD_HELP_LEFT_PAD = 5;
  private static int CMD_INDEX_LEFT_PAD = 0;

  /**
   * If the given pattern matches several commands, i.e. "web mvc"
   * or empty, this method writes to {@link #LOGGER} the list of 
   * commands (command index) which names start with the given pattern.
   * 
   * If the given pattern matches with only one command, this method
   * writes to {@link #LOGGER} the full info about that command.
   *
   * @param pattern
   */
  public void obtainHelp(String pattern) {
    synchronized (mutex) {
      if (pattern == null) {
        pattern = "";
      }

      Template helpTemplate;

      // Create the data-model for Freemarker engine.
      Map<String, Object> fmContext = new HashMap<String, Object>();
      fmContext.put("LINE_SEPARATOR", IOUtils.LINE_SEPARATOR);
      fmContext.put("CMD_MAX_LENGTH", CMD_MAX_LENGTH);
      fmContext.put("OPT_MAX_LENGTH", OPT_MAX_LENGTH);

      // Get the methods annotated with @CliCommand that matches the pattern
      final Collection<MethodTarget> matchingTargets = locateTargets(pattern, false, false);

      try {

        // There is only one command which name matches. Example: "web mvc controller"
        // In that case the full command help will be rendered.
        if (matchingTargets.size() == 1) {

          helpTemplate =
              new Template("cmdTemplate", new StringReader(cmdTemplateStr), new Configuration(
                  Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS));

          // Single command help
          final MethodTarget methodTarget = matchingTargets.iterator().next();

          // Argument conversion time
          final Annotation[][] parameterAnnotations =
              methodTarget.getMethod().getParameterAnnotations();

          // Offer specified help
          final CliCommand cmd = methodTarget.getMethod().getAnnotation(CliCommand.class);
          Validate.notNull(cmd, "CliCommand not found");

          // The command has options, those method arguments annotated with @CliOption
          if (parameterAnnotations.length > 0) {

            // Synopsis
            fmContext.put("synopsis", justify(cmd.value(), CMD_HELP_LEFT_PAD, LINE_MAX_LENGTH));

            // Description
            fmContext.put("description", justify(cmd.help(), CMD_HELP_LEFT_PAD, LINE_MAX_LENGTH));

            // Options
            Map<String, List<String>> options = new TreeMap<String, List<String>>();
            fmContext.put("options", options);

            // Build the Map of command options analyzing the info provided by
            // method arguments annotated with the @CliOption annotation 
            for (final Annotation[] annotations : parameterAnnotations) {
              CliOption cliOption = null;
              for (final Annotation a : annotations) {
                if (a instanceof CliOption) {
                  cliOption = (CliOption) a;

                  for (final String option : cliOption.key()) {
                    String dashOption = "--".concat(option);

                    // Note justification should be done in the Freemarker template,
                    // but it is easier to do it here and adjust both justifications
                    // (cmd name and cmd help) depending on the cmd name length
                    String optStr =
                        StringUtils.repeat(" ", CMD_HELP_LEFT_PAD)
                            + (dashOption.length() <= OPT_MAX_LENGTH ? StringUtils.rightPad(
                                dashOption, OPT_MAX_LENGTH) : dashOption);

                    // Add as left padding the cmd length to avoid overwrite the command on the left
                    // +1 to add an empty char (space) between the command and the description
                    options.put(
                        optStr,
                        justify(cliOption.help(), CMD_HELP_LEFT_PAD + OPT_MAX_LENGTH,
                            LINE_MAX_LENGTH));
                  }
                }
              }
            }
          }
          // The command hasn't options, usually methods without arguments
          else {

            // Synopsis
            fmContext.put("synopsis", justify(cmd.value(), CMD_HELP_LEFT_PAD, LINE_MAX_LENGTH));

            // Description
            fmContext.put("description", justify(cmd.help(), CMD_HELP_LEFT_PAD, LINE_MAX_LENGTH));
          }
        }

        // There are several commands that matches the pattern. Example: "web mvc"
        // In that case only a list of command names and descriptions will be rendered.
        else {
          helpTemplate =
              new Template("cmdIndexTemplate", new StringReader(cmdIndexTemplateStr),
                  new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS));

          // Create the list of commands that start with the given token.
          // Note that empty token will cause all commands will be rendered.
          Map<String, List<String>> cmdList = new TreeMap<String, List<String>>();

          // Build the Map of commands analyzing the info provided by
          // method annotation @CliCommand 
          for (final MethodTarget mt : matchingTargets) {
            final CliCommand cmd = mt.getMethod().getAnnotation(CliCommand.class);
            if (cmd != null) {
              for (final String value : cmd.value()) {

                // Note justification should be done in the Freemarker template,
                // but it is easier to do it here and adjust both justifications
                // (cmd name and cmd help) depending on the cmd name length
                String cmdStr =
                    StringUtils.repeat(" ", CMD_INDEX_LEFT_PAD)
                        + (value.length() <= CMD_MAX_LENGTH ? StringUtils.rightPad(value,
                            CMD_MAX_LENGTH) : value);

                // Add as left padding the cmd length to avoid overwrite the command on the left
                // +1 to add an empty char (space) between the command and the description
                cmdList.put(cmdStr,
                    justify(cmd.help(), CMD_INDEX_LEFT_PAD + CMD_MAX_LENGTH, LINE_MAX_LENGTH));
              }
            }
          }

          // Add the command list to the Freemarker context
          fmContext.put("commands", cmdList);
        }

        // Merge data-model with template
        Writer strWriter = new StringWriter();

        helpTemplate.process(fmContext, strWriter);

        LOGGER.info(strWriter.toString());

      } catch (TemplateException e) {
        LOGGER.log(Level.SEVERE, "Help engine internal error!", e);
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Help engine internal error!", e);
      }

      LOGGER.warning("** Type 'hint' (without the quotes) and hit ENTER "
          + "for step-by-step guidance **" + LINE_SEPARATOR);
    }
  }

  /**
   * Justify the texts in the given list of texts.
   * 
   * This method delegates the justifying of each given text in
   * {@link #justify(String, int, int)}.
   * 
   * @param texts to justify
   * @param leftPad
   * @param lineLength
   * @return
   */
  private List<String> justify(String[] texts, int leftPad, int lineLength) {
    List<String> result = new ArrayList<String>();
    for (String text : texts) {
      result.addAll(justify(text, leftPad, lineLength));
    }
    return result;
  }

  /**
   * Justify text. 
   * 
   * Split the given text in several lines, each of them with the max length 
   * given by the lineLength argument.
   * 
   * All the returned lines will be left padded with the number of spaces (' ') 
   * given by the leftPad argument.
   * 
   * @param txt Text to justify
   * @param leftPad
   * @param lineLength
   * @return justified text
   */
  private List<String> justify(String txt, int leftPad, int lineLength) {
    if (StringUtils.isEmpty(txt)) {
      return Collections.emptyList();
    }

    // Text length in each line, so the lines will have the pattern 
    // "__LEFT-PAD-STR__ __RIGHT-TEXT__" which length is lineLength 
    int rightTextLength = lineLength - leftPad;
    String leftPadStr = StringUtils.repeat(" ", leftPad);

    if (txt.length() <= rightTextLength) {

      // Compose and return the single line: left padding + text
      return Collections.singletonList(leftPadStr.concat(txt));
    }

    // If given text 
    String[] wordList = StringUtils.split(txt);

    List<String> result = new ArrayList<String>();

    StringBuffer currentLine = new StringBuffer();
    int currentLineLength = 0;
    for (String word : wordList) {

      // The length of the line after adding the current word
      int futureLength = currentLineLength + word.length();

      // If the length of the result line is less than the 
      // right text length then the current word can be added to current line
      if (futureLength <= rightTextLength) {
        currentLine.append(word);
        currentLineLength += word.length();

        // If it is the last word of the text, current line must be added 
        // to the result
        if (txt.endsWith(word)) {

          // Compose and add the line: left padding + text
          result.add(leftPadStr.concat(currentLine.toString()));
        } else if (futureLength < rightTextLength) {

          // Otherwise, append space if the length of the result line is
          // strictly less than max length
          currentLine.append(" ");
          currentLineLength += 1;
        }
      } else {
        // When current word cannot be added to current line is time to
        // compose and add the current line and prepare the new line

        // Compose and add the line: left padding + text
        result.add(leftPadStr.concat(currentLine.toString()));

        // Prepare the new line

        // If it is the last word, add it as new line because current iteration
        // is the last iteration
        if (txt.endsWith(word)) {

          // Compose and add the line: left padding + text
          result.add(leftPadStr.concat(word));
        } else {

          // Otherwise initialize the next line with the current word
          currentLine = new StringBuffer(word).append(" ");
          currentLineLength = word.length() + 1;
        }
      }
    }
    return result;
  }

  /**
   * Get the methods annotated with {@link CliCommand} 
   * inside the {@link CommandMarker} classes, which value attribute matches 
   * the given pattern.
   * 
   * @param pattern
   * @param strictMatching
   * @param checkAvailabilityIndicators
   * @return the @{@link CliCommand} methods that matches the given pattern
   */
  private Collection<MethodTarget> locateTargets(final String pattern,
      final boolean strictMatching, final boolean checkAvailabilityIndicators) {

    // Get all Services implement CommandMarker interface
    try {
      ServiceReference<?>[] references =
          this.context.getAllServiceReferences(CommandMarker.class.getName(), null);

      for (ServiceReference<?> ref : references) {
        CommandMarker command = (CommandMarker) this.context.getService(ref);
        if (!commands.contains(command)) {
          add(command);
        }
      }

    } catch (InvalidSyntaxException e) {
      LOGGER.warning("Cannot load CommandMarker on SimpleParser.");
    }

    Validate.notNull(pattern, "Buffer required");
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
            final String remainingBuffer = isMatch(pattern, value, strictMatching);
            if (remainingBuffer != null) {
              result.add(new MethodTarget(method, command, remainingBuffer, value));
            }
          }
        }
      }
    }
    return result;
  }

  public final void add(final CommandMarker command) {
    synchronized (mutex) {
      commands.add(command);
      for (final Method method : command.getClass().getMethods()) {
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
      }
    }
  }

  public final void add(final Converter<?> converter) {
    synchronized (mutex) {
      converters.add(converter);
    }
  }

  private MethodTarget getAvailabilityIndicator(final String command) {
    return availabilityIndicators.get(command);
  }

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
}
