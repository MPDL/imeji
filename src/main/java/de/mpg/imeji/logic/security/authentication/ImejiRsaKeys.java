package de.mpg.imeji.logic.security.authentication;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;

import org.jose4j.base64url.Base64Url;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.keys.RsaKeyUtil;
import org.jose4j.lang.JoseException;

import de.mpg.imeji.logic.util.StringHelper;

/**
 * RSA Keys for imeji
 *
 * @author bastiens
 *
 */
public class ImejiRsaKeys {
  private static RsaJsonWebKey rsaJsonWebKey;
  private static final int BITS_NUMBER = 2048;

  /**
   * Initialize the RSA Key
   *
   * @param publicKeyJson
   * @throws JoseException
   * @throws InvalidKeySpecException
   * @throws NoSuchAlgorithmException
   */
  public static void init(String publicKeyJson, String privateKeyString)
      throws JoseException, NoSuchAlgorithmException, InvalidKeySpecException {
    if (!StringHelper.isNullOrEmptyTrim(publicKeyJson)
        && !StringHelper.isNullOrEmptyTrim(privateKeyString)) {
      rsaJsonWebKey = parseJson(publicKeyJson);
      rsaJsonWebKey.setPrivateKey(generatePrivateKey(privateKeyString));
    } else {
      rsaJsonWebKey = RsaJwkGenerator.generateJwk(BITS_NUMBER);
    }
  }

  /**
   * Generate a new Private Key out of th private key String
   *
   * @param rsaJsonWebKey
   * @param privateKeyString
   * @return
   * @throws NoSuchAlgorithmException
   * @throws InvalidKeySpecException
   */
  private static PrivateKey generatePrivateKey(String privateKeyString)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    KeyFactory keyFactory = KeyFactory.getInstance(RsaKeyUtil.RSA);
    KeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64Url.decode(privateKeyString));
    return keyFactory.generatePrivate(privateKeySpec);
  }

  private static RsaJsonWebKey parseJson(String rsaJsonWebKeyJson) throws JoseException {
    return (RsaJsonWebKey) JsonWebKey.Factory.newJwk(rsaJsonWebKeyJson);
  }

  public static String getPublicKeyJson() {
    return rsaJsonWebKey.toJson();
  }

  public static String getPrivateKeyString() {
    return Base64Url.encode(rsaJsonWebKey.getRsaPrivateKey().getEncoded());
  }

  /**
   * Get the RSA Keys
   *
   * @return
   * @throws JoseException
   */
  public static RsaJsonWebKey getRsaJsonWebKey() throws JoseException {
    return rsaJsonWebKey;
  }
}
