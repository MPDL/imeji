package de.mpg.imeji.logic.model.aspects;

import java.lang.reflect.Field;

/**
 * 
 * 
 * @author breddin
 *
 */
public interface AccessMember {


  public void accessMember(ChangeMember changeMember);



  /**
   * 
   * 
   * @author breddin
   *
   */
  public class ChangeMember {


    /**
     * The data object you want to manipulate, i.e. an Item object
     */
    private Object imejiDataObject; // The data object whose field shall be changed  


    /**
     * The action you want to take, i.e. delete a list element, edit a field, add a list element to
     * a list
     */
    private final ActionType action;


    /**
     * The j2j annotated field of the class of the above given object, that you want to manipulate,
     * i.e. the license field in the Item class
     */
    private final Field field;

    /**
     * The value that you want to set for the field. If the field is a list field: The list element
     * that you want to append, edit or delete.
     */
    private final Object valueToSet;

    /**
     * 
     * @param action
     * @param imejiDataObject
     * @param field
     * @param valueToSet
     * @param compareFunction
     */
    public ChangeMember(ActionType action, Object imejiDataObject, Field field, Object valueToSet) {

      this.imejiDataObject = imejiDataObject;
      this.action = action;
      this.field = field;
      this.valueToSet = valueToSet;
    }

    public Object getImejiDataObject() {
      return this.imejiDataObject;
    }

    public Field getField() {
      return this.field;
    }

    public Object getValue() {
      return this.valueToSet;
    }

    public ActionType getAction() {
      return this.action;
    }

    public void setImejiDataObject(Object imejiDataObject) {
      this.imejiDataObject = imejiDataObject;
    }

  }


  public enum ActionType {
    ADD_OVERRIDE,
    ADD,
    REMOVE,
    EDIT
  }



}
