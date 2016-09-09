package de.mpg.imeji.logic.validation.impl;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata;
import de.mpg.imeji.util.LocalizedString;

/**
 * {@link Validator} for {@link MetadataProfile}
 *
 * @author saquet
 *
 */
public class ProfileValidator extends ObjectValidator implements Validator<MetadataProfile> {

  @Override
  public void validate(MetadataProfile profile, Method m) throws UnprocessableError {
    setValidateForMethod(m);
    if (isDelete()) {
      return;
    }
    
    if (Method.CREATE == m &&  profile.getDefault() && Imeji.defaultMetadataProfile != null && profile.getId() != Imeji.defaultMetadataProfile.getId()) {
      throw new UnprocessableError("default_metadata_profile_already_exists" + profile.getId()+" - "+Imeji.defaultMetadataProfile.getId());
    }

    if (isNullOrEmpty(profile.getTitle())) {
      throw new UnprocessableError("error_profile_need_title");
    }
    if (profile.getStatements() == null) {
      throw new UnprocessableError("error_profile_need_statement");
    }
    int i = 0;

    // helper hashmap to validate uniqueness of metadata labels
    HashMap<String, URI> labels = new HashMap<>();

    for (Statement s : profile.getStatements()) {
      // helper check duplication language input
      List<String> langs = new ArrayList<String>();

      for (LocalizedString ls : s.getLabels()) {
        if (ls.getLang() == null || "".equals(ls.getLang())) {
          throw new UnprocessableError("error_profile_label_no_lang");
        }
        // validate uniqueness of metadata labels
        if (labels.containsKey(ls.getValue()) && !labels.get(ls.getValue()).equals(s.getId())) {
          throw new UnprocessableError("labels_have_to_be_unique");
        }
        if (langs.contains(ls.getLang())) {
          throw new UnprocessableError("labels_duplicate_lang");
        } else {
          langs.add(ls.getLang());
          labels.put(ls.getValue(), s.getId());
        }
      }
      if (s.getType() == null) {
        throw new UnprocessableError("error_profile_select_metadata_type");
      } else if (s.getLabels().isEmpty()
          || "".equals(((List<LocalizedString>) s.getLabels()).get(0).getValue())) {
        throw new UnprocessableError("error_profile_labels_required");
      }
      validateConstraints(s);
      s.setPos(i);
      i++;
    }
  }

  /**
   * Validate the constraints according to the type of the metadata
   *
   * @param s
   * @throws UnprocessableError
   */
  private void validateConstraints(Statement s) throws UnprocessableError {
    for (String str : s.getLiteralConstraints()) {
      Metadata.Types type = Metadata.Types.valueOfUri(s.getType().toString());
      switch (type) {
        case NUMBER:
          try {
            Double.parseDouble(str);
          } catch (Exception e) {
            throw new UnprocessableError("Unvalid number format: " + str + " (example: 12.34)");
          }
          break;
        case LINK:
          if (!UrlHelper.isValidURL(str)) {
            throw new UnprocessableError(
                "Unvalid url format: " + str + " (example: http://example.org)");
          }
          break;
        default:
          break;
      }
    }
  }

  @Override
  public void validate(MetadataProfile t, MetadataProfile p, Method m) throws UnprocessableError {
    validate(t, m);
  }

}
