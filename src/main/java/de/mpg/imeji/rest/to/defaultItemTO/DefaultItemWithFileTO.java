package de.mpg.imeji.rest.to.defaultItemTO;

import java.io.File;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * A {@link DefaultItemTO} with file
 *
 * @author bastiens
 *
 */
@JsonInclude(Include.NON_NULL)
public class DefaultItemWithFileTO extends DefaultItemTO {
  private static final long serialVersionUID = -776042830013928522L;
  private File file;
  private String referenceUrl;
  private String fetchUrl;

  public File getFile() {
    return file;
  }

  public void setFile(File file) {
    this.file = file;
  }

  public String getReferenceUrl() {
    return referenceUrl;
  }

  public void setReferenceUrl(String referenceUrl) {
    this.referenceUrl = referenceUrl;
  }

  public String getFetchUrl() {
    return fetchUrl;
  }

  public void setFetchUrl(String fetchUrl) {
    this.fetchUrl = fetchUrl;
  }

}
