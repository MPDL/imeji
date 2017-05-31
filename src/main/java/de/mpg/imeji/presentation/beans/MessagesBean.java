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

  public String getDetails(FacesMessage msg) {
    return msg.getDetail()
        .replaceAll("XXX_PWDRESET_START_XXX", "<a onclick=\"openDialog('testDialog');\">")
        .replaceAll("XXX_PWDRESET_END_XXX", "</a>");
  }
}
