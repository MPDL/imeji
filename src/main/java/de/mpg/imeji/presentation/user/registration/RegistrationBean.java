package de.mpg.imeji.presentation.user.registration;

import java.io.IOException;
import java.util.Calendar;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.AlreadyExistsException;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.notification.email.EmailMessages;
import de.mpg.imeji.logic.notification.email.EmailService;
import de.mpg.imeji.logic.security.registration.RegistrationService;
import de.mpg.imeji.logic.security.sharing.invitation.InvitationService;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.security.LoginBean;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.util.DateHelper;

/**
 * Bean for registration workflow
 *
 * @author makarenko (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "RegistrationBean")
@ViewScoped
public class RegistrationBean extends SuperBean {
	private static final long serialVersionUID = -993770106648303808L;
	private static final Logger LOGGER = LogManager.getLogger(RegistrationBean.class);
	private User user = new User();
	private boolean isInvited = false;
	private boolean termsAccepted = StringHelper.isNullOrEmptyTrim(Imeji.CONFIG.getTermsOfUse())
			&& StringHelper.isNullOrEmptyTrim(Imeji.CONFIG.getTermsOfUseUrl());
	@ManagedProperty(value = "#{LoginBean}")
	private LoginBean loginBean;
	private final RegistrationService registrationService = new RegistrationService();

	@PostConstruct
	public void init() {
		this.user.setPerson(ImejiFactory.newPerson());
		this.user.setEmail(UrlHelper.getParameterValue("login"));
		this.isInvited = checkInvitations();
		if (hasValidToken()) {
			activateUser();
		}
	}

	/**
	 * Activate the user, login with this user and redirect to the resetpassword
	 * page
	 */
	private void activateUser() {
		String token = UrlHelper.getParameterValue("token");
		try {
			User user = registrationService.activate(registrationService.retrieveByToken(token));
			loginBean.getSessionBean().setUser(user);
			sendRegistrationNotification(user);
			redirect(getNavigation().getHomeUrl() + "/pwdreset?from=registration");
		} catch (Exception e) {
			LOGGER.error("Error activating user", e);
			BeanHelper.error("Error during user activation");
		}
	}

	/**
	 * True if a valid token is set in the url
	 * 
	 * @return
	 */
	private boolean hasValidToken() {
		String token = UrlHelper.getParameterValue("token");
		if (token != null) {
			try {
				registrationService.retrieveByToken(token);
				return true;
			} catch (Exception e) {
				// invalid token
			}
		}
		return false;

	}

	/**
	 * Trigger registration
	 * 
	 * @throws IOException
	 */
	public void register() throws IOException {
		/*
		 * Removed due to DSGVO if (!termsAccepted) {
		 * BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage(
		 * "error_accept_terms_of_use", getLocale())); return; }
		 */
		String passwordUrl = null;
		boolean registration_success = false;
		try {
			passwordUrl = getNavigation().getRegistrationUrl() + "?token="
					+ registrationService.register(user).getToken();
			registration_success = true;
		} catch (final UnprocessableError e) {
			BeanHelper.error(e, getLocale());
			LOGGER.error("error registering user", e);
		} catch (final AlreadyExistsException e) {
			BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_user_already_exists", getLocale()));
			LOGGER.error("error registering user", e);
		} catch (final Exception e) {
			BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_during_user_registration", getLocale()));
			LOGGER.error("error registering user", e);
		}
		if (registration_success) {
			BeanHelper.cleanMessages();
			BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("sending_registration_email", getLocale()));
			Calendar expirationDate = DateHelper.getCurrentDate();
			expirationDate.add(Calendar.DAY_OF_MONTH, Integer.valueOf(Imeji.CONFIG.getRegistrationTokenExpiry()));
			sendRegistrationEmail(passwordUrl, DateHelper.printDateWithTime(expirationDate));
			if (FacesContext.getCurrentInstance().getMessageList().size() > 1) {
				BeanHelper.cleanMessages();
				BeanHelper.info(
						"User account has been registered, but verification email could not be sent! Please contact service administrators!");
			}
			redirect(getNavigation().getHomeUrl());
		}
	}

	/**
	 * True if the user registering has been invited
	 *
	 * @return
	 * @throws ImejiException
	 */
	private boolean checkInvitations() {
		try {
			return !new InvitationService().retrieveInvitationOfUser(user.getEmail()).isEmpty();
		} catch (final ImejiException e) {
			LOGGER.error("Error checking user invitations", e);
			return false;
		}
	}

	/**
	 * Send registration email
	 */
	private void sendRegistrationEmail(String url, String exprirationDate) {
		final EmailService emailClient = new EmailService();
		try {
			// send to requester
			emailClient.sendMail(getUser().getEmail(), Imeji.CONFIG.getEmailServerSender(),
					EmailMessages.getEmailOnRegistrationRequest_Subject(getLocale()),
					EmailMessages.getEmailOnRegistrationRequest_Body(getUser(), url, Imeji.CONFIG.getContactEmail(),
							exprirationDate, getLocale(), getNavigation().getRegistrationUrl()));
		} catch (final Exception e) {
			LOGGER.error("Error sending email", e);
			BeanHelper.error("Error: Email not sent");
		}
	}

	/**
	 * Send registration email
	 */
	private void sendRegistrationNotification(User user) {
		final EmailService emailClient = new EmailService();
		try {
			// send to requester
			emailClient.sendMail(Imeji.CONFIG.getContactEmail(), Imeji.CONFIG.getEmailServerSender(),
					EmailMessages.getEmailOnAccountActivation_Subject(user, getLocale()),
					EmailMessages.getEmailOnAccountActivation_Body(user, getLocale()));
		} catch (final Exception e) {
			LOGGER.error("Error sending email", e);
			BeanHelper.error("Error: Email not sent");
		}
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public boolean isRegistrationEnabled() {
		return Imeji.CONFIG.isRegistrationEnabled() || isInvited;
	}

	public boolean isTermsAccepted() {
		return termsAccepted;
	}

	public void setTermsAccepted(boolean termsAccepted) {
		this.termsAccepted = termsAccepted;
	}

	public LoginBean getLoginBean() {
		return loginBean;
	}

	public void setLoginBean(LoginBean loginBean) {
		this.loginBean = loginBean;
	}
}
