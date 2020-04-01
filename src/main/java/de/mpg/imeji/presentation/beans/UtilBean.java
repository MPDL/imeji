package de.mpg.imeji.presentation.beans;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.faces.bean.ManagedBean;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.config.ImejiProperties;

@ManagedBean(name = "UtilBean")
@RequestScoped
public class UtilBean {


  public String toJavascriptArray(List<?> arr) {
    if (arr != null) {
      StringBuffer sb = new StringBuffer();
      sb.append("[");
      for (int i = 0; i < arr.size(); i++) {
        sb.append("\"").append(arr.get(i)).append("\"");
        if (i + 1 < arr.size()) {
          sb.append(",");
        }
      }
      sb.append("]");
      return sb.toString();
    } else
      return "Null";
  }


  public String getBaseUri() {
    return Imeji.PROPERTIES.getBaseURI();
  }

  public String cleanHtml(String s) {
    return Jsoup.clean(s, Whitelist.basic());
  }

}
