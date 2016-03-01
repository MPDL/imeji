/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.user.util;

import static com.google.common.base.Strings.isNullOrEmpty;
import static de.mpg.imeji.presentation.beans.ConfigurationBean.getEmailServerEnableAuthenticationStatic;
import static de.mpg.imeji.presentation.beans.ConfigurationBean.getEmailServerPasswordStatic;
import static de.mpg.imeji.presentation.beans.ConfigurationBean.getEmailServerPortStatic;
import static de.mpg.imeji.presentation.beans.ConfigurationBean.getEmailServerSenderStatic;
import static de.mpg.imeji.presentation.beans.ConfigurationBean.getEmailServerStatic;
import static de.mpg.imeji.presentation.beans.ConfigurationBean.getEmailServerUserStatic;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.log4j.Logger;

import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;

/**
 * Client to send email
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class EmailClient {
  private static final Logger LOGGER = Logger.getLogger(EmailClient.class);

  /**
   * Is true if the Email is valid
   */
  public static boolean isValidEmail(String email) {
    String regexEmailMatch = "([^.@]+)(\\.[^.@]+)*@([^.@]+\\.)+([^.@]+)";
    return email.matches(regexEmailMatch);
  }

  /**
   * Send an email according to the properties define in imeji.properties
   */
  public void sendMail(String to, String from, String subject, String message) throws IOException,
      URISyntaxException {
    sendMail(to, from, null, subject, message);
  }

  /**
   * Send an email according to the properties define in imeji.properties
   */
  public void sendMail(String to, String from, String[] replyTo, String subject, String message)
      throws IOException, URISyntaxException {
    String emailUser = getEmailServerUserStatic();
    String password = getEmailServerPasswordStatic();
    String server = getEmailServerStatic();
    String port = getEmailServerPortStatic();
    if (isNullOrEmpty(port))
      port = "25";
    String auth = Boolean.toString(getEmailServerEnableAuthenticationStatic());
    String sender = getEmailServerSenderStatic();
    if (from != null) {
      sender = from;
    }
    String[] recipientsAdress = {to};
    sendMail(server, port, auth, emailUser, password, sender, recipientsAdress, null, null,
        replyTo, subject, message);
  }

  /**
   * Send an email according to the properties define in imeji.properties
   */

  /**
   * Send an email according to the properties define in imeji.properties
   */

  /**
   * Send an email
   */
  public String sendMail(String smtpHost, String port, String withAuth, String usr, String pwd,
      String senderAddress, String[] recipientsAddresses, String[] recipientsCCAddresses,
      String[] recipientsBCCAddresses, String[] replytoAddresses, String subject, String text) {
    LOGGER.debug("EmailHandlingBean sendMail...");
    String status = "not sent";
    String to = "";
    try {
      // Setup mail server
      Properties props = System.getProperties();
      props.put("mail.smtp.host", smtpHost);
      props.put("mail.smtp.auth", withAuth);
      props.put("mail.smtp.starttls.enable", "true");
      props.put("mail.smtp.port", port);
      // Get a mail session with authentication
      MailAuthenticator authenticator = new MailAuthenticator(usr, pwd);
      Session mailSession = Session.getInstance(props, authenticator);
      // Define a new mail message
      Message message = new MimeMessage(mailSession);
      message.setFrom(new InternetAddress(senderAddress));
      // add TO recipients
      for (String ra : recipientsAddresses) {
        if (ra != null && !ra.trim().equals("")) {
          message.addRecipient(Message.RecipientType.TO, new InternetAddress(ra));
          to = ra;
          LOGGER.debug(">>> recipientTO: " + ra);
        }
      }
      // add CC recipients
      if (recipientsCCAddresses != null)
        for (String racc : recipientsCCAddresses) {
          if (racc != null && !racc.trim().equals("")) {
            message.addRecipient(Message.RecipientType.CC, new InternetAddress(racc));
            LOGGER.debug(">>> recipientCC  " + racc);
          }
        }
      // add BCC recipients
      if (recipientsBCCAddresses != null)
        for (String rabcc : recipientsBCCAddresses) {
          if (rabcc != null && !rabcc.trim().equals("")) {
            message.addRecipient(Message.RecipientType.BCC, new InternetAddress(rabcc));
            LOGGER.debug(">>> recipientBCC  " + rabcc);
          }
        }
      // add replyTo
      if (replytoAddresses != null) {
        InternetAddress[] adresses = new InternetAddress[recipientsAddresses.length];
        int i = 0;
        for (String a : replytoAddresses) {
          if (a != null && !a.trim().equals("")) {
            adresses[i] = new InternetAddress(a);
            i++;
            LOGGER.debug(">>> replyToaddress  " + a);
          }
        }
        if (i > 0)
          message.setReplyTo(adresses);
      }
      message.setSubject(subject);
      Date date = new Date();
      message.setSentDate(date);
      // Create a message part to represent the body text
      BodyPart messageBodyPart = new MimeBodyPart();
      messageBodyPart.setText(text);
      // use a MimeMultipart as we need to handle the file attachments
      Multipart multipart = new MimeMultipart();
      // add the message body to the mime message
      multipart.addBodyPart(messageBodyPart);
      // Put all message parts in the message
      message.setContent(multipart);
      LOGGER.debug("Transport will send now....  ");
      // Send the message
      Transport.send(message);
      status = "sent";
      LOGGER.debug("Email sent!");
    } catch (MessagingException e) {
      SessionBean sessionBean = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
      BeanHelper.error(sessionBean.getMessage("email_error").replace("XXX_USER_EMAIL_XXX", to)
          + ": " + e);
      LOGGER.error("Error in sendMail(...)", e);
    }
    return status;
  }

  /**
   * {@link Authenticator} for imeji
   *
   * @author saquet (initial creation)
   * @author $Author$ (last modification)
   * @version $Revision$ $LastChangedDate$
   */
  public class MailAuthenticator extends Authenticator {
    private final String user;
    private final String password;

    /**
     * Public constructor.
     */
    public MailAuthenticator(String usr, String pwd) {
      this.user = usr;
      this.password = pwd;
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
      PasswordAuthentication pwdAut = new PasswordAuthentication(this.user, this.password);
      return pwdAut;
    }
  }


}
