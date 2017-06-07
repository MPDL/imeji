package de.mpg.imeji.presentation.userGroup;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.faces.model.SelectItem;

import de.mpg.imeji.logic.vo.User;

public class SelectUserComponent implements Serializable {
  private static final long serialVersionUID = -8610625477511815078L;

  // email is index
  private String index;
  private Collection<User> users;

  public SelectUserComponent(Collection<User> users) {
    index = null;
    this.users = users;
  }

  public void init(String index) {
    this.index = index;
  }

  public List<String> searchForIndex(List<SelectItem> statementMenu) {
    index = index == null ? "" : index;
    return statementMenu.stream().map(i -> i.getValue().toString()).filter(s -> s.startsWith(index))
        .sorted((s1, s2) -> s1.toLowerCase().compareTo(s2.toLowerCase()))
        .collect(Collectors.toList());
  }

  public boolean indexExists() {
    for (User user : users) {
      if (user.getEmail().equals(index)) {
        return true;
      }
    }
    return false;
  }


  /**
   * Remove the first ":" form the containerId to reuse it in javascript methods
   * 
   * @param containerId
   * @return
   */
  public String normalizeContainerId(String containerId) {
    return containerId.startsWith(":") ? containerId.substring(1) : containerId;
  }

  /**
   * Listener when the value of the component has been changed
   */
  public void listener() {
    init(index);
  }

  public String getIndex() {
    return index;
  }

  public void setIndex(String index) {
    this.index = index;
  }



}
