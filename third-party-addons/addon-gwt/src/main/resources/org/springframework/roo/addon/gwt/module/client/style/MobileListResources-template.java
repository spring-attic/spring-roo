package __TOP_LEVEL_PACKAGE__.client.style;

import com.google.gwt.resources.client.CssResource.NotStrict;
import com.google.gwt.user.cellview.client.CellList;

/**
 * The styles and resources used by the mobile Scaffold.
 */
public interface MobileListResources extends CellList.Resources {

	interface MobileStyle extends CellList.Style {
		
		/**
		 * Applied to the date property in a cell.
		 */
		String dateProp();

		/**
		 * Applied to the secondary property in a cell.
		 */
		String secondaryProp();
	}

	@NotStrict
	@Source("mobile.css")
	MobileStyle cellListStyle();
}
