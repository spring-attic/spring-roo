package org.springframework.roo.classpath.details.comments;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.List;

/**
 * = JavadocComment
 * 
 * Holds a full JavaDoc comment which can be added to a {@link CommentStructure}
 * This class is responsible of adding the right JavaDoc syntax in its proper 
 * place inside the structure. Having this in mind, adding manually any syntax 
 * *is discouraged*.
 * 
 * Please, avoid using specific system line separators. Use 
 * `org.apache.commons.io.IOUtils.LINE_SEPARATOR` to specify line breaks.
 * 
 * @author Mike De Haan
 * @author Sergio Clares
 */
public class JavadocComment extends AbstractComment {

  /**
   * The JavaDoc comment main description
   */
  private String description;

  /**
   * The JavaDoc comment parameter info Each item is showed as ` * @param ...` 
   */
  private List<String> paramsInfo;

  /**
   * The JavaDoc comment return info, showed as ` * @return ...`
   */
  private String returnInfo;

  /**
   * The JavaDoc comment throws info. Each item is showed as ` * @throws ...`
   */
  private List<String> throwsInfo;

  /**
   * The `StringBuilder` instance which builds the 'comment' field.
   */
  private StringBuilder commentBuilder;

  //---- Indexes used as 'cache' to locate comment positions faster ----//

  private int beginDescriptionIndex;

  private int endDescriptionIndex;

  private int beginParamsIndex;

  private int endParamsIndex;

  private int beginReturnIndex;

  private int endReturnIndex;

  private int beginThrowsIndex;

  private int endThrowsIndex;

  private boolean initDone = false;

  /**
   * Default empty constructor.
   */
  public JavadocComment() {}

  /**
   * Used to add the full JavaDoc comment during the instantiation.
   * The comment will be checked and formatted with the JavaDoc syntax before 
   * setting it. Line breaks will be taken in count in order to form the 
   * syntax.
   * 
   * @param comment the String with the comment to add as JavaDoc syntax.
   */
  public JavadocComment(String comment) {
    super(comment);
    this.setComment(comment);
  }

  /**
   * Constructor used to generate a full Javadoc with description, params, 
   * return and throws. `null` arguments are accepted. JavaDoc comment block 
   * syntax will be auto-generated.
   * 
   * ROO-3862: Improve JavaDoc generation for generated methods and constructors
   * 
   * @param description the `String` with the block description to add. Please, use 
   *            `org.apache.commons.io.IOUtils.LINE_SEPARATOR` for line breaks.
   * @param paramsInfo the `List<String>` with the parameter info. One entry for 
   *            each parameter. JavaDoc ` * @param` syntax will be automatically 
   *            added. 
   * @param returnInfo the `String` with the return info. JavaDoc ` * @return` 
   *            syntax will be automatically added.
   * @param throwsInfo the `List<String>` with the throws info. One entry for 
   *            each throws type. JavaDoc ` * @throws` syntax will be automatically 
   *            added.
   */
  public JavadocComment(final String description, final List<String> paramsInfo,
      final String returnInfo, List<String> throwsTypes) {
    this.description = description;
    this.paramsInfo = paramsInfo;
    this.returnInfo = returnInfo;
    this.throwsInfo = throwsTypes;

    addDescription(this.description);
    addParamsInfo(this.paramsInfo);
    addReturnInfo(this.returnInfo);
    addThrowsInfo(this.throwsInfo);
  }

  /**
   * Adds a new description or changes an existing in the JavadocComment.
   * 
   * @param description the `String` to add as comment description.
   */
  private void addDescription(final String description) {
    String[] descriptionLines = description.split(IOUtils.LINE_SEPARATOR);

    if (this.getComment() == null || this.getComment().isEmpty()) {

      // Build a StringBuilder instance
      Validate.isTrue(StringUtils.isNotBlank(description),
          "The provided comment must be not null, not blank nor empty.");
      checkJavadocSyntax(description);
      initializeCommentIndexes();
    } else {

      // Initialize indexes
      initializeCommentIndexes();

      // Delete existing description
      this.commentBuilder.delete(this.beginDescriptionIndex, this.endDescriptionIndex);

      // Add each new line with JavaDoc syntax
      for (String line : descriptionLines) {
        String newLine = (" * ").concat(line).concat(IOUtils.LINE_SEPARATOR);
        this.commentBuilder.insert(this.beginDescriptionIndex, newLine);

        // Update indexes
        int newLineLenght = newLine.length();
        this.endDescriptionIndex += newLineLenght;
        this.endParamsIndex += newLineLenght;
        this.endReturnIndex += newLineLenght;
        this.endThrowsIndex += newLineLenght;
      }
    }
    super.setComment(this.commentBuilder.toString());
  }

  /**
   * Adds or changes JavadocComment parameters info. Existing info will be 
   * replaced by this one.
   * 
   * @param paramsInfo the `List<String>` to add as parameters info. One 
   *            entry for each parameter.
   */
  private void addParamsInfo(final List<String> paramsInfo) {
    Validate.isTrue(StringUtils.isNotBlank(getComment()),
        "JavadocComment needs to have a description value before adding parameters info to it. "
            + "You can do that using 'setDescription(String)' method first, or full constructor.");
    if (paramsInfo == null || paramsInfo.isEmpty()) {
      return;
    }

    // Initialize indexes
    initializeCommentIndexes();

    // Delete existing params info
    this.beginParamsIndex = this.endDescriptionIndex;
    this.commentBuilder.delete(this.beginParamsIndex, this.endParamsIndex);

    // Add each new line with JavaDoc syntax
    int nextLineStartingIndex = this.beginParamsIndex;
    for (String paramInfo : paramsInfo) {
      String[] lines = paramInfo.split(IOUtils.LINE_SEPARATOR);
      for (int i = 0; i < lines.length; i++) {
        String newLine = "";
        if (i == 0) {
          newLine = (" * @param ").concat(lines[i]).concat(IOUtils.LINE_SEPARATOR);
        } else {
          newLine = (" * ").concat("\t\t\t\t").concat(lines[i]).concat(IOUtils.LINE_SEPARATOR);
        }
        this.commentBuilder.insert(nextLineStartingIndex, newLine);

        // Update indexes
        int newLineLenght = newLine.length();
        nextLineStartingIndex += newLineLenght;
        this.endParamsIndex += newLineLenght;
        this.endReturnIndex += newLineLenght;
        this.endThrowsIndex += newLineLenght;
      }

    }

    super.setComment(this.commentBuilder.toString());
  }

  /**
   * Adds or changes JavadocComment return info. Existing info will be 
   * replaced by this one.
   * 
   * @param returnInfo the `String` to add as return info.
   */
  private void addReturnInfo(final String returnInfo) {
    Validate.isTrue(StringUtils.isNotBlank(getComment()),
        "JavadocComment needs to have a description value before adding return info to it. "
            + "You can do that using 'setDescription(String)' method first, or full constructor.");
    if (StringUtils.isBlank(returnInfo)) {
      return;
    }

    // Initialize indexes
    initializeCommentIndexes();

    // Delete existing return info
    this.beginReturnIndex = this.endParamsIndex;
    this.commentBuilder.delete(this.beginReturnIndex, this.endReturnIndex);

    // Add each new line with JavaDoc syntax
    String[] lines = returnInfo.split(IOUtils.LINE_SEPARATOR);
    int nextLineStartingIndex = this.beginReturnIndex;
    for (int i = 0; i < lines.length; i++) {
      String newLine = "";
      if (i == 0) {
        newLine = (" * @return ").concat(lines[i]).concat(IOUtils.LINE_SEPARATOR);
      } else {
        newLine = (" * ").concat("\t\t\t\t").concat(lines[i]).concat(IOUtils.LINE_SEPARATOR);
      }
      this.commentBuilder.insert(nextLineStartingIndex, newLine);

      // Update indexes
      int newLineLenght = newLine.length();
      nextLineStartingIndex += newLineLenght;
      this.endReturnIndex += newLineLenght;
      this.endThrowsIndex += newLineLenght;
    }

    super.setComment(this.commentBuilder.toString());
  }

  /**
   * Adds or changes JavadocComment throws info. Existing info will be 
   * replaced by this one.
   * 
   * @param throwsInfo the `List<String>` to add as throws info. One entry 
   *            for each throws type.
   */
  private void addThrowsInfo(final List<String> throwsInfo) {
    Validate.isTrue(StringUtils.isNotBlank(getComment()),
        "JavadocComment needs to have a description value before adding throws info to it. "
            + "You can do that using 'setDescription(String)' method first, or full constructor.");
    if (throwsInfo == null || throwsInfo.isEmpty()) {
      return;
    }

    // Initialize indexes
    initializeCommentIndexes();

    // Delete existing throws info
    this.beginThrowsIndex = this.endReturnIndex;
    this.commentBuilder.delete(this.beginThrowsIndex, this.endThrowsIndex);

    // Add each new line with JavaDoc syntax
    int nextLineStartingIndex = this.beginThrowsIndex;
    for (String entry : throwsInfo) {
      String[] lines = entry.split(IOUtils.LINE_SEPARATOR);
      for (int i = 0; i < lines.length; i++) {
        String newLine = "";
        if (i == 0) {
          newLine = (" * @throws ").concat(lines[i]).concat(IOUtils.LINE_SEPARATOR);
        } else {
          newLine = (" * ").concat("\t\t\t\t").concat(lines[i]).concat(IOUtils.LINE_SEPARATOR);
        }
        this.commentBuilder.insert(nextLineStartingIndex, newLine);

        // Update indexes
        int newLineLenght = newLine.length();
        nextLineStartingIndex += newLineLenght;
        this.endThrowsIndex += newLineLenght;
      }
    }

    super.setComment(this.commentBuilder.toString());
  }

  /**
   * Checks if the provided String has the proper JavaDoc syntax and adds it 
   * if its not present.
   * 
   * @param comment the String to check
   * @return a String with the original String or the formatted String if any 
   *            format was applied.
   */
  private String checkJavadocSyntax(String comment) {
    if (comment.contains("/**")) {
      return comment;
    }

    if (this.commentBuilder == null) {
      this.commentBuilder = new StringBuilder();
    }
    String[] lines = comment.split(IOUtils.LINE_SEPARATOR);
    this.commentBuilder.append("/**").append(IOUtils.LINE_SEPARATOR);
    this.beginDescriptionIndex = this.commentBuilder.length();
    for (String line : lines) {
      this.commentBuilder.append(" * ").append(line).append(IOUtils.LINE_SEPARATOR);
    }
    this.commentBuilder.append(" * ").append(IOUtils.LINE_SEPARATOR);
    this.commentBuilder.append(" */").append(IOUtils.LINE_SEPARATOR);
    return this.commentBuilder.toString();
  }

  /**
   * Indexes the JavadocComment indexes to know each component location within 
   * the entire `String` (description, paramsInfo, returnInfo and throwsInfo).
   */
  private void initializeCommentIndexes() {

    // First, check if initialization has already been done
    if (this.initDone) {
      return;
    }

    // Get existing comment
    if (this.commentBuilder == null) {
      this.commentBuilder = new StringBuilder(this.getComment());
    }

    // Set description indexes
    if (this.beginDescriptionIndex == 0) {
      this.beginDescriptionIndex = this.commentBuilder.indexOf(" * ");
    }
    if (this.endDescriptionIndex == 0) {
      this.endDescriptionIndex =
          this.commentBuilder.lastIndexOf(" * ".concat(IOUtils.LINE_SEPARATOR)) + 3
              + IOUtils.LINE_SEPARATOR.length();

      // Ensure it finds description end when Sring is trimmed
      if (this.endDescriptionIndex == -1) {
        this.endDescriptionIndex =
            this.commentBuilder.lastIndexOf(" *".concat(IOUtils.LINE_SEPARATOR)) + 2
                + IOUtils.LINE_SEPARATOR.length();
      }
    }

    // Set begin indexes, which also indicates when a component does not exist
    if (this.beginParamsIndex == 0) {
      this.beginParamsIndex = this.commentBuilder.indexOf(" * @param", this.endDescriptionIndex);
    }
    if (this.beginReturnIndex == 0) {
      this.beginReturnIndex = this.commentBuilder.indexOf(" * @return", this.endDescriptionIndex);
    }
    if (this.beginThrowsIndex == 0) {
      this.beginThrowsIndex = this.commentBuilder.indexOf(" * @throws", this.endDescriptionIndex);
    }

    // Set end of param index
    if (this.beginParamsIndex == -1) {
      this.endParamsIndex = this.endDescriptionIndex;
    } else if (this.beginReturnIndex != -1) {
      this.endParamsIndex = this.beginReturnIndex - 1;
    } else if (this.beginReturnIndex == -1 && this.beginThrowsIndex != 1) {
      this.endParamsIndex = this.beginThrowsIndex - 1;
    } else {
      this.endParamsIndex = this.commentBuilder.indexOf(" */", this.endDescriptionIndex) - 1;
    }

    // Set end of return index
    if (this.beginReturnIndex == -1) {
      this.endReturnIndex = this.endParamsIndex;
    } else if (this.beginThrowsIndex != -1) {
      this.endReturnIndex = this.endThrowsIndex - 1;
    } else {
      this.endReturnIndex = this.commentBuilder.indexOf(" */", this.endDescriptionIndex) - 1;
    }

    // Set end of throws index
    if (this.beginThrowsIndex == -1) {
      this.endThrowsIndex = this.endReturnIndex;
    } else {
      this.endThrowsIndex = this.commentBuilder.indexOf(" */", this.endDescriptionIndex) - 1;
    }

    this.initDone = true;
  }

  @Override
  public void setComment(String comment) {
    Validate.isTrue(StringUtils.isNotBlank(comment),
        "You must add a not empty, not null nor blank String as a comment");
    super.setComment(checkJavadocSyntax(comment));
    initializeCommentIndexes();
  }

  public String getDescription() {
    // This check is needed in case that 'comment' field had been filled directly
    if (this.beginDescriptionIndex == -1) {
      return null;
    }

    if (StringUtils.isBlank(this.description)) {
      return null;
    }
    return this.description;
  }

  public void setDescription(String description) {
    this.description = description;
    addDescription(description);
  }

  public List<String> getParamsInfo() {
    // This check is needed in case that 'comment' field had been filled directly
    if (this.beginParamsIndex == -1) {
      return null;
    }

    if (this.paramsInfo == null || this.paramsInfo.isEmpty()) {
      return null;
    }
    return this.paramsInfo;
  }

  public void setParamsInfo(List<String> paramsInfo) {
    this.paramsInfo = paramsInfo;
    addParamsInfo(paramsInfo);
  }

  public String getReturnInfo() {
    // This check is needed in case that 'comment' field had been filled directly
    if (this.beginReturnIndex == -1) {
      return null;
    }

    if (StringUtils.isBlank(this.returnInfo)) {
      return null;
    }
    return this.returnInfo;
  }

  public void setReturnInfo(String returnInfo) {
    this.returnInfo = returnInfo;
    addReturnInfo(returnInfo);
  }

  public List<String> getThrowsInfo() {
    // This check is needed in case that 'comment' field had been filled directly
    if (this.beginThrowsIndex == -1) {
      return null;
    }

    if (this.throwsInfo == null || this.throwsInfo.isEmpty()) {
      return null;
    }
    return this.throwsInfo;
  }

  public void setThrowsInfo(List<String> throwsInfo) {
    this.throwsInfo = throwsInfo;
    addThrowsInfo(throwsInfo);
  }

}
