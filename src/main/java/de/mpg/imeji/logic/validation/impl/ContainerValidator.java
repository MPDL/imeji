package de.mpg.imeji.logic.validation.impl;

import static com.google.common.base.Strings.isNullOrEmpty;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.Container;
import de.mpg.imeji.logic.vo.ContainerAdditionalInfo;

/**
 * Abstract call for common container methods
 *
 * @author bastiens
 *
 */
public abstract class ContainerValidator extends ObjectValidator {

  protected abstract UnprocessableError getException();

  protected abstract void setException(UnprocessableError e);

  protected void validateContainerMetadata(Container c) {
    if (isDelete()) {
      return;
    }
    if (StringHelper.hasInvalidTags(c.getMetadata().getDescription())) {
      setException(new UnprocessableError("error_bad_format_description", getException()));
    }
    if (isNullOrEmpty(c.getMetadata().getTitle().trim())) {
      setException(new UnprocessableError("error_collection_need_title", getException()));
    }
    validateAdditionalInfos(c);
  }

  private void validateAdditionalInfos(Container c) {
    for (ContainerAdditionalInfo info : c.getMetadata().getAdditionalInformations()) {
      if (isNullOrEmpty(info.getLabel())) {
        setException(new UnprocessableError("error_additionalinfo_need_label", getException()));
      }
      if ( isNullOrEmpty(info.getText()) && isNullOrEmpty(info.getUrl())) {
        setException(new UnprocessableError("error_additionalinfo_need_value", getException()));
      }
    }
  }
}
