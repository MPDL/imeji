package de.mpg.imeji.presentation.beans;

import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.bean.ManagedBean;
import jakarta.faces.context.FacesContext;

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

  public String getDetails(FacesMessage msg) {
    return msg.getDetail();
  }
}
