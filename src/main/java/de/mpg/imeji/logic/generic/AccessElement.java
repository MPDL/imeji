package de.mpg.imeji.logic.generic;

import java.lang.reflect.Field;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.aspects.AccessMember.ActionType;


/**
 * Implement this interface in your controller if you wish to write (add/delete/edit) parts of data
 * objects (fields) in store, i.e. for a User object you wish only to add a new Grant to it and not
 * update the complete object. Or for an Item object you only wish to change the license or for a
 * Collection object you only want to change the (parent) collection.
 * 
 * @author breddin
 *
 * @param <T>
 */
public interface AccessElement<T> {


  public T changeElement(User user, T imejiDataObject, Field elementField, ActionType action, Object element) throws ImejiException;



}
