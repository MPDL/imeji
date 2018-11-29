package de.mpg.imeji.presentation.notification;

import java.util.Locale;

import de.mpg.imeji.logic.config.Imeji;

/**
 * Helper class for common messages support
 *
 * Created by vlad on 26.03.15.
 */
public class CommonMessages {

  public static String getSuccessCollectionDeleteMessage(String collectionName, Locale locale) {
    return Imeji.RESOURCE_BUNDLE.getMessage("success_collection_delete", locale).replace("XXX_collectionName_XXX", collectionName);
  }

}
