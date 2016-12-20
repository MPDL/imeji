package de.mpg.imeji.presentation.upload;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.presentation.beans.MetadataLabels;


@ManagedBean(name = "SingleUploadSession")
@SessionScoped
public class SingleUploadSession implements Serializable {
  private static final long serialVersionUID = -7330919735840276789L;
  private String selectedCollectionItem;
  private CollectionImeji collection;
  private MetadataLabels labels;
  private IngestImage ingestImage;
  private List<String> techMD = new ArrayList<String>();
  private boolean uploadFileToTemp = false;
  private boolean uploadFileToItem = false;
  private Item uploadedItem;
  private String fFile;

  public void reset() {
    selectedCollectionItem = "";
    collection = null;
    ingestImage = null;
    techMD.clear();;
    labels = null;
    uploadFileToTemp = false;
    uploadFileToItem = false;
    uploadedItem = null;
    fFile = "";
  }

  public void copyToTemp() {
    uploadFileToTemp = true;
    uploadFileToItem = false;
    uploadedItem = null;
    fFile = "";
  }

  public void uploaded() {
    selectedCollectionItem = "";
    collection = null;
    ingestImage = null;
    techMD.clear();
    labels = null;
    uploadFileToTemp = false;
    uploadFileToItem = true;
    fFile = "";
  }


  public List<String> getTechMD() {
    return techMD;
  }

  public void setTechMD(List<String> techMD) {
    this.techMD = techMD;
  }

  public boolean isUploadFileToItem() {
    return uploadFileToItem;
  }

  public void setUploadFileToItem(boolean uploadFileToItem) {
    this.uploadFileToItem = uploadFileToItem;
  }

  public boolean isUploadFileToTemp() {
    return uploadFileToTemp;
  }

  public void setUploadFileToTemp(boolean uploadFileToTemp) {
    this.uploadFileToTemp = uploadFileToTemp;
  }

  public String getSelectedCollectionItem() {
    return selectedCollectionItem;
  }

  public void setSelectedCollectionItem(String selectedCollectionItem) {
    this.selectedCollectionItem = selectedCollectionItem;
  }

  public CollectionImeji getCollection() {
    return collection;
  }

  public void setCollection(CollectionImeji collection) {
    this.collection = collection;
  }

  public MetadataLabels getLabels() {
    return labels;
  }

  public void setLabels(MetadataLabels labels) {
    this.labels = labels;
  }

  public Item getUploadedItem() {
    return uploadedItem;
  }

  public void setUploadedItem(Item uploadedItem) {
    this.uploadedItem = uploadedItem;
  }

  public String getfFile() {
    return fFile;
  }

  public void setfFile(String fFile) {
    this.fFile = fFile;
  }

  public IngestImage getIngestImage() {
    return ingestImage;
  }

  public void setIngestImage(IngestImage ingestImage) {
    this.ingestImage = ingestImage;
  }
}
