package de.mpg.imeji.j2j.controler;

import org.apache.jena.rdf.model.Model;

import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.ReloadBeforeSaveException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.j2j.helper.J2JHelper;
import de.mpg.imeji.logic.model.aspects.AccessMember;
import de.mpg.imeji.logic.model.aspects.AccessMember.ChangeMember;
import de.mpg.imeji.logic.model.aspects.CloneURI;

/**
 * Controls access (create, update, delete) to members of resources.
 * Use this to set a certain field of a data object.
 * 
 * @author breddin
 *
 */
public class ResourceElementController extends ResourceController {

  public ResourceElementController(Model model) {
    super(model, false);
  }


  /**
   * Change the field of a data object. 
   * 
   * @param changeMember specifies data object, the action (add, edit, delete), the field and the value to set.
   * @return data object with changed field (latest version from database)
   * @throws NotFoundException
   * @throws ReloadBeforeSaveException
   * @throws UnprocessableError
   */
  public Object changeMember(ChangeMember changeMember) throws NotFoundException, ReloadBeforeSaveException, UnprocessableError {

    if (changeMember.getImejiDataObject() instanceof CloneURI) {

      Object emptyObjectWithURI = ((CloneURI) changeMember.getImejiDataObject()).cloneURI();
      Object dataObjectInStore = read(emptyObjectWithURI);
      if (dataObjectInStore instanceof AccessMember) {
        ((AccessMember) dataObjectInStore).accessMember(changeMember);
        Object newObject = update(dataObjectInStore);
        return newObject;
      }
    }

    throw new UnprocessableError(
        "Could not update member of " + J2JHelper.getId(changeMember.getImejiDataObject()).getPath().replace("imeji/", "")
            + ". Reason: Required interfaces not implemented.");
  }
}
