/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.beans;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;

@ManagedBean(name = "MessagesBean")
@RequestScoped
public class MessagesBean {
  /**
   * Return the messages
   * 
   * @return
   */
  public List<FacesMessage> getMessages() {
    return FacesContext.getCurrentInstance().getMessageList();
  }
}
