package de.mpg.imeji.logic.validation.impl;

import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.util.LicenseUtil;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.License;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata;
import de.mpg.imeji.logic.vo.util.MetadataAndProfileHelper;

/**
 * {@link Validator} for an {@link Item}. Only working when {@link MetadataProfile} is passed
 *
 * @author saquet
 *
 */
public class ItemValidator extends ObjectValidator implements Validator<Item> {

  private static final MetadataValidator METADATA_VALIDATOR = new MetadataValidator();

  @Override
  public void validate(Item t, Method m) throws UnprocessableError {
    throw new UnprocessableError("Item needs a profile to be validated");
  }

  @Override
  public void validate(Item item, MetadataProfile p, Method m) throws UnprocessableError {
    UnprocessableError error = new UnprocessableError();
    setValidateForMethod(m);

    if (isDelete()) {
      return;
    }

    // Check that all publish items have a license
    if (!item.getStatus().equals(Status.PENDING)) {
      License lic = LicenseUtil.getActiveLicense(item);
      if (lic == null || lic.isEmtpy()) {
        error = new UnprocessableError("Items must have a license to be released", error);
      }
    }

    if (p != null) {
      // List of the statement which are not defined as Multiple
      List<String> nonMultipleStatement = new ArrayList<String>();
      Object[] itemMetadataList = item.getMetadataSet().getMetadata().toArray();

      // Validate that every child has its parent filled
      for (int i = 0; i < itemMetadataList.length; i++) {
        Statement s = MetadataAndProfileHelper
            .getStatement(((Metadata) itemMetadataList[i]).getStatement(), p);
        if (s.getParent() != null) {
          Statement parentStatement = MetadataAndProfileHelper.getStatement(s.getParent(), p);
          // First element can not have a parent
          if (i == 0) {
            throw new UnprocessableError(parentStatement.getLabel() + " has to be filled");
          } else {
            Statement preStatement = MetadataAndProfileHelper
                .getStatement(((Metadata) itemMetadataList[i - 1]).getStatement(), p);
            if (parentStatement.getId().equals(preStatement.getId())
                && MetadataAndProfileHelper.isEmpty((Metadata) itemMetadataList[i - 1])) {
              error =
                  new UnprocessableError(parentStatement.getLabel() + " has to be filled", error);
              // Statement has to be preceded by same statement (multiple childs) or parent
              // statement
              // or a descendant statement (multiple statement with childs)
            }
          }
        }
      }

      // Validate each metadata Value
      for (Metadata md : item.getMetadataSet().getMetadata()) {
        Statement s = MetadataAndProfileHelper.getStatement(md.getStatement(), p);
        try {
          METADATA_VALIDATOR.validate(md, p, getValidateForMethod());
        } catch (UnprocessableError e) {
          error = new UnprocessableError(e.getMessages(), error);
        }

        boolean isMultiple = isMultipleStatement(s, p);
        if (!isMultiple) {
          // if (s.getMaxOccurs() == null || s.getMaxOccurs().equals("1")) {

          if (nonMultipleStatement.contains(s.getId().toString())) {
            error = new UnprocessableError("Multiple value not allowed for metadata "
                + s.getLabels().iterator().next().getValue() + "(ID: " + s.getId() + "", error);
          } else {
            nonMultipleStatement.add(s.getId().toString());
          }
        }
      }
    }
    if (error.hasMessages()) {
      throw error;
    }
  }

  private boolean isSuccessor(Statement ancestor, Statement successor, MetadataProfile p) {
    Statement current = successor;
    while (current.getParent() != null) {
      current = MetadataAndProfileHelper.getStatement(current.getParent(), p);
      if (current.getId().toString().equals(ancestor.getId().toString())) {
        return true;
      }
    }
    return false;
  }


  /**
   * @param s {@link Statement}
   * @param p {@link MetadataProfile}
   * @return boolean
   *
   *         method is pretty dummy, it only finds out if the metadata statement can be multiple at
   *         any place in general, method should find out if the metadata statement can be multiple
   *         in context with its parent however now it makes no troubles during saving of data as
   *         previously
   */
  private boolean isMultipleStatement(Statement s, MetadataProfile p) {
    if (s.getParent() == null) {
      return (!(s.getMaxOccurs() == null || s.getMaxOccurs().equals("1")));
    } else {
      return ((s.getMaxOccurs() != null && !s.getMaxOccurs().equals("1"))
          || isMultipleStatement(MetadataAndProfileHelper.getStatement(s.getParent(), p), p));
    }
  }

}
