package de.mpg.imeji.rest.to;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Created by vlad on 13.01.15.
 */
@XmlRootElement
@XmlType(propOrder = {"uploadWhiteList", "uploadBlackList"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StorageTO implements Serializable {

	private static final long serialVersionUID = -8291542243783127323L;

	private String uploadWhiteList;

	private String uploadBlackList;

	public String getUploadWhiteList() {
		return uploadWhiteList;
	}

	public void setUploadWhiteList(String uploadWhiteList) {
		this.uploadWhiteList = uploadWhiteList;
	}

	public String getUploadBlackList() {
		return uploadBlackList;
	}

	public void setUploadBlackList(String uploadBlackList) {
		this.uploadBlackList = uploadBlackList;
	}

}
