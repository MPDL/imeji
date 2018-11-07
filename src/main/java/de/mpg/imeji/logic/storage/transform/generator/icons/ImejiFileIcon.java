package de.mpg.imeji.logic.storage.transform.generator.icons;

/**
 * Represents an icon file that can be loaded
 * 
 * @author breddin
 *
 */
public class ImejiFileIcon {

	/**
	 * path of icons files
	 */
	public static final String ICONS_PATH = "images/";
	private static final String JPG_FORMAT = ".jpg";
	private String iconFilePath;

	public ImejiFileIcon(String iconName) {
		this.iconFilePath = ICONS_PATH + iconName + JPG_FORMAT;
	}

	public String getIconPath() {
		return this.iconFilePath;
	}
}
