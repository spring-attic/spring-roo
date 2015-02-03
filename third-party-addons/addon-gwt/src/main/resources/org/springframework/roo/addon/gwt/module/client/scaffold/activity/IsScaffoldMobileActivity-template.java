package __TOP_LEVEL_PACKAGE__.client.scaffold.activity;

import com.google.gwt.place.shared.Place;

/**
 * Implemented by mobile activities.
 */
public interface IsScaffoldMobileActivity {
	/**
	 * @return the Place to go when the back button is pressed
	 */
	Place getBackButtonPlace();

	/**
	 * @return the text to display in the back button.
	 */
	String getBackButtonText();

	/**
	 * @return the Place to go when the edit button is pressed
	 */
	Place getEditButtonPlace();

	/**
	 * @return the title of the activity
	 */
	String getTitleText();

	/**
	 * @return true if the activity has an edit button, false if not
	 */
	boolean hasEditButton();
}
