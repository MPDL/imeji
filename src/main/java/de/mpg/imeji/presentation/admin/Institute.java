package de.mpg.imeji.presentation.admin;

import org.apache.commons.io.FileUtils;

public class Institute {

  private String name;
  private long storage;

  public Institute(String name, long storage) {
    this.name = name;
    this.storage = storage;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public long getStorage() {
    return storage;
  }

  public void setStorage(long storage) {
    this.storage = storage;
  }

  public String getStorageString() {
    return FileUtils.byteCountToDisplaySize(storage);
  }

}
