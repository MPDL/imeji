package de.mpg.imeji.test.rest.resources.test;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by vlad on 24.03.15.
 */
public class BugsTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(BugsTest.class);

	/**
	 * See bug https://github.com/MPDL-Innovations/imeji/issues/42
	 */
	@Test
	public void notificationUrlEscapingTest_bug42() {

		// String templ =
		// "Dear XXX_USER_NAME_XXX,\\r\\n\\r\\nThe zipped items have been downloaded by
		// user
		// \"XXX_ACTOR_NAME_XXX\": XXX_ACTOR_EMAIL_XXX\\r\\nat
		// XXX_TIME_XXX.\\r\\nXXX_ITEMS_DOWNLOADED_XXX\\r\\n\\r\\nThe items have been
		// found by query:
		// XXX_QUERY_URL_XXX\\r\\n\\r\\nWARNING: This is an automatically generated
		// email, please do not
		// reply!";
		// String url =
		// "http://dev-faces.mpdl.mpg.de/imeji/collection/SoL9eg21DU7tlwAg/browse?q="
		// +
		// "NOTstatement==\"http://dev-faces.mpdl.mpg.de/imeji/statement/UkSj3SDNS0DYMvDA\"
		// "
		// + "NOT AND
		// statement==\"http://dev-faces.mpdl.mpg.de/imeji/statement/G0rE6dg5KMTLLVIe";
		// String expected =
		// "Dear User Name,\\r\\n\\r\\nThe zipped items have been downloaded by user
		// \"non_logged_in_user\": \\r\\nat Tue Mar 24 17:44:40 CET
		// 2015.\\r\\nhttp://dev-faces.mpdl.mpg.de/imeji/collection/SoL9eg21DU7tlwAg/browse?q=NOTstatement==%22http://dev-faces.mpdl.mpg.de/imeji/statement/UkSj3SDNS0DYMvDA%22%20NOT%20AND%20statement==%22http://dev-faces.mpdl.mpg.de/imeji/statement/G0rE6dg5KMTLLVIe\\r\\n\\r\\nThe
		// items have been found by query:
		// http://dev-faces.mpdl.mpg.de/imeji/collection/SoL9eg21DU7tlwAg/browse?q=NOTstatement==%22http://dev-faces.mpdl.mpg.de/imeji/statement/UkSj3SDNS0DYMvDA%22%20NOT%20AND%20statement==%22http://dev-faces.mpdl.mpg.de/imeji/statement/G0rE6dg5KMTLLVIe\\r\\n\\r\\nWARNING:
		// This is an automatically generated email, please do not reply!";
		// User mockedToUser = mock(User.class);
		// Person mockedPerson = mock(Person.class);
		// when(mockedToUser.getPerson()).thenReturn(mockedPerson);
		// when(mockedPerson.getCompleteName()).thenReturn("User Name");
		// assertThat(
		// EmailMessages.getEmailOnZipDownload_Body(mockedToUser, null,
		// UrlHelper.encodeQuery(url),
		// url, Locale.ENGLISH).replaceAll("at.*\\d{4}\\.", ""),
		// equalTo(expected.replaceAll("at.*\\d{4}\\.", "")));

	}

}
