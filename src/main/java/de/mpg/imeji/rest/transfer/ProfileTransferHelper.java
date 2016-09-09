package de.mpg.imeji.rest.transfer;

import de.mpg.imeji.exceptions.BadRequestException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.rest.to.MetadataProfileTO;
import de.mpg.imeji.rest.to.StatementTO;
import de.mpg.imeji.util.LocalizedString;

/**
 * Helper for profile transfer operation
 *
 * @author bastiens
 *
 */
public class ProfileTransferHelper {
  /**
   * Find the {@link StatementTO} according to its label. If not found, return null
   *
   * @param label
   * @param profile
   * @return
   * @throws BadRequestException
   */
  public static StatementTO findStatementByLabel(String label, MetadataProfileTO profile)
      throws UnprocessableError {
    for (StatementTO st : profile.getStatements()) {
      for (LocalizedString l : st.getLabels()) {
        if (l.getValue().equals(label)) {
          return st;
        }
      }
    }
    throw new UnprocessableError("Metadata \"" + label + "\" not found!");
  }

  public static boolean hasChildStatement(String statementId, MetadataProfileTO profile) {
    for (StatementTO st : profile.getStatements()) {
      if (st.getParentStatementId() != null && st.getParentStatementId().equals(statementId)) {
        return true;
      }
    }

    return false;
  }

  public static String getParentStatementLabel(String parentStatementId,
      MetadataProfileTO profile) {
    if (parentStatementId == null) {
      return "";
    }

    for (StatementTO st : profile.getStatements()) {
      if (st.getId().equals(parentStatementId)) {
        return st.getLabels().get(0).getValue();
      }
    }
    return "";
  }

  public static String getParentStatementLabels(String parentStatementId, MetadataProfileTO profile,
      String previousStatementLabel) {

    if (parentStatementId == null) {
      return previousStatementLabel;
    }
    for (StatementTO st : profile.getStatements()) {
      if (st.getId().equals(parentStatementId)) {
        previousStatementLabel =
            previousStatementLabel.equals("") ? st.getLabels().get(0).getValue()
                : previousStatementLabel + "->" + st.getLabels().get(0).getValue();
        return getParentStatementLabels(st.getParentStatementId(), profile, previousStatementLabel);
      }
    }

    return previousStatementLabel;
  }

  public static StatementTO validateStatementByLabelAndParent(String label,
      MetadataProfileTO profile, String parentStatementId) throws UnprocessableError {
    for (StatementTO st : profile.getStatements()) {
      for (LocalizedString l : st.getLabels()) {
        if (l.getValue().equals(label)) {
          return st;
        }
      }
    }
    String parentStatementLabel = getParentStatementLabels(parentStatementId, profile, "");
    throw new UnprocessableError("Metadata \"" + label
        + "\" not found in metadata profiles for parents " + parentStatementLabel + "!");
  }
}
