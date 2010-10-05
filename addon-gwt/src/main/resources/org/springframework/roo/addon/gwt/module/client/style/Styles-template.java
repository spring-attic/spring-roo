package __TOP_LEVEL_PACKAGE__.__SEGMENT_PACKAGE__;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.CssResource.NotStrict;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;

/**
 * The Styles used in bikeshed.
 */
public class Styles {

  /**
   * Common styles.
   */
  public interface Common extends CssResource {
    String box();

    String header();

    String headerLeft();

    String headerMain();

    String padded();

    String table();
  }

  /**
   * Shared resources.
   */
  public interface Resources extends ClientBundle {
    @NotStrict
    @Source("common.css")
    Common common();

    /**
     * Icon used to represent a user group.
     */
    ImageResource groupIcon();

    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    ImageResource openGradient();

    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    ImageResource selectionGradient();

    /**
     * Icon used to represent a user.
     */
    ImageResource userIcon();
  }

  private static Resources resources;

  static {
    resources = GWT.create(Resources.class);
    resources.common().ensureInjected();
  }

  public static Common common() {
    return resources.common();
  }

  public static Resources resources() {
    return resources;
  }
}
