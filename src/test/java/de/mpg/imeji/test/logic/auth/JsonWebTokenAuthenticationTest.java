package de.mpg.imeji.test.logic.auth;

import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.jose4j.lang.JoseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.mpg.imeji.logic.auth.ImejiRsaKeys;
import de.mpg.imeji.logic.auth.authentication.impl.APIKeyAuthentication;

public class JsonWebTokenAuthenticationTest {
  private static String privateKeyString;
  private static String publicKeyJson;
  private static String USER_ID = "user123";


  @Before
  public void init() throws JoseException, NoSuchAlgorithmException, InvalidKeySpecException {
    ImejiRsaKeys.init(null, null);
    privateKeyString = ImejiRsaKeys.getPrivateKeyString();
    publicKeyJson = ImejiRsaKeys.getPublicKeyJson();
  }


  @Test
  public void testTokenCreatinAndValidation() throws JoseException {
    String apiKey = APIKeyAuthentication.generateKey(URI.create(USER_ID), 100);
    String userId = APIKeyAuthentication.consumeJsonWebToken(apiKey);
    Assert.assertTrue(USER_ID + " != " + userId, USER_ID.toString().equals(userId));
  }


  @Test
  public void testTokenCreatinAndValidationAfterRestart()
      throws JoseException, NoSuchAlgorithmException, InvalidKeySpecException {
    // Generate APiKey
    String apiKey = APIKeyAuthentication.generateKey(URI.create(USER_ID), 100);
    // Restart
    ImejiRsaKeys.init(publicKeyJson, privateKeyString);
    // Check that the apiKey is still valid
    String userId = APIKeyAuthentication.consumeJsonWebToken(apiKey);
    Assert.assertTrue(USER_ID + " != " + userId, USER_ID.toString().equals(userId));
  }

}
