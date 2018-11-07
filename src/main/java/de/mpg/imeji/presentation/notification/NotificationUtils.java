package de.mpg.imeji.presentation.notification;

import java.util.Locale;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.notification.email.EmailMessages;
import de.mpg.imeji.logic.notification.email.EmailService;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * Created by vlad on 13.03.15.
 */
public class NotificationUtils {
	private static final Logger LOGGER = LogManager.getLogger(NotificationUtils.class);
	private static final EmailService emailClient = new EmailService();

	private NotificationUtils() {
		// private constructor...
	}

	/**
	 * Send account activation email to current admin
	 */
	public static void sendActivationNotification(User user, Locale locale, boolean invitation) {
		try {
			// send to support
			emailClient.sendMail(Imeji.CONFIG.getContactEmail(), null,
					EmailMessages.getEmailOnAccountActivation_Subject(user, locale),
					EmailMessages.getEmailOnAccountActivation_Body(user, locale));
		} catch (final Exception e) {
			BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("error", locale) + ": Account activation email not sent");
			LOGGER.info("Error sending account activation email", e);
		}
	}

}
