package de.mpg.imeji.logic.util;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.mpg.imeji.logic.model.aspects.ChangeMember.ActionType;

/**
 * Add/edit/remove elements from/in a list of unique objects (i.e. the list contains only one object
 * of the same kind).
 * 
 * @author breddin
 *
 */

public class URIListHelper {

  private static final Logger LOGGER = LogManager.getLogger(URIListHelper.class);

  /**
   * Use this function when: - You have a URI list, i.e. a list that contains only unique elements.
   * - You have an element that you want to add/edit/remove to/from the list. - You have already
   * conducted a search in order to find out whether the unique element that you want to
   * add/edit/remove is already contained in the list, and know it's index (-1 if not contained) you
   * can use this function to add/edit/remove the element from the list
   * 
   * @param URIList
   * @param action
   * @param element
   * @param indexOfExistingElement
   */
  public static <T> void addEditRemoveElementOfURIList(List<T> URIList, ActionType action, T element, int indexOfExistingElement) {

    if (action.equals(ActionType.ADD)) {
      if (indexOfExistingElement == -1) {
        URIList.add(element);
      } else {
        LOGGER.error("Could not add element to URI list, because element already exists " + element.toString());
      }
    } else if (action.equals(ActionType.ADD_OVERRIDE)) {
      if (indexOfExistingElement == -1) {
        URIList.add(element);
      } else {
        URIList.remove(indexOfExistingElement);
        URIList.add(element);
      }
    } else if (action.equals(ActionType.EDIT)) {
      if (indexOfExistingElement == -1) {
        LOGGER.error("Could not edit element of URI list, because element does not exist " + element.toString());
      } else {
        URIList.remove(indexOfExistingElement);
        URIList.add(element);
      }
    } else if (action.equals(ActionType.REMOVE)) {
      if (indexOfExistingElement == -1) {
        LOGGER.error("Could not remove element from URI list, because element does not exist " + element.toString());
      } else {
        URIList.remove(indexOfExistingElement);
      }
    }

  }

}
