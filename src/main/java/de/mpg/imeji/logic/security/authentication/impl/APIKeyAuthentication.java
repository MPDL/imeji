package de.mpg.imeji.logic.security.authentication.impl;

import java.net.URI;

import org.apache.log4j.Logger;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.lang.JoseException;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.exceptions.InactiveAuthenticationError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.security.authentication.Authentication;
import de.mpg.imeji.logic.security.authentication.ImejiRsaKeys;
import de.mpg.imeji.logic.user.controller.UserBusinessController;
import de.mpg.imeji.logic.vo.User;

/**
 * {@link Authentication} with API Key
 *
 * @author bastiens
 *
 */
public class APIKeyAuthentication implements Authentication {
  private static final Logger LOGGER = Logger.getLogger(APIKeyAuthentication.class);
  private String key;

  public APIKeyAuthentication(String key) {
    this.key = key;
  }

  @Override
  public User doLogin() throws AuthenticationError {
    try {
      UserBusinessController controller = new UserBusinessController();
      User user = controller.retrieve(URI.create(consumeJsonWebToken(key)), Imeji.adminUser);
      if (!user.isActive()) {
        throw new InactiveAuthenticationError(
            "Not active user: please activate your account with the limk sent after your registration");
      } else if (key.equals(user.getApiKey())) {
        return user;
      }
    } catch (Exception e) {
      LOGGER.error("Invalid Key authorization");
    }
    LOGGER.error(
        "Error APIKeyAuthentication user could not be authenticated with provided credentials");
    throw new AuthenticationError("Invalid Key authorization");
  }

  /**
   * Generate a JSON Web Token, with Informations about the user and the expiration date
   *
   * @param userId
   * @param expirationTimeMinutesInTheFuture
   * @return
   * @throws JoseException
   */
  public static String generateKey(URI userId, int expirationTimeMinutesInTheFuture)
      throws JoseException {
    return produceJsonWebToken(userId, expirationTimeMinutesInTheFuture);
  }

  /**
   * Use https://bitbucket.org/b_c/jose4j/wiki/Home to create JSON Web Token
   *
   * @param userId
   * @param expirationTimeMinutesInTheFuture
   * @return
   * @throws JoseException
   */
  private static String produceJsonWebToken(URI userId, int expirationTimeMinutesInTheFuture)
      throws JoseException {
    // Create the Claims, which will be the content of the JWT
    JwtClaims claims = new JwtClaims();
    // claims.setIssuer("Issuer"); // who creates the token and signs it
    // claims.setAudience(userId.toString()); // to whom the token is intended to be sent
    // time when the token will expire in minutes
    claims.setExpirationTimeMinutesInTheFuture(expirationTimeMinutesInTheFuture);
    claims.setGeneratedJwtId(); // a unique identifier for the token
    claims.setIssuedAtToNow(); // when the token was issued/created (now)
    claims.setSubject(userId.toString()); // the subject/principal is whom the token is about
    // A JWT is a JWS and/or a JWE with JSON claims as the payload.
    // In this example it is a JWS so we create a JsonWebSignature object.
    JsonWebSignature jws = new JsonWebSignature();
    // The payload of the JWS is JSON content of the JWT Claims
    jws.setPayload(claims.toJson());
    // The JWT is signed using the private key
    jws.setKey(ImejiRsaKeys.getRsaJsonWebKey().getPrivateKey());
    jws.setKeyIdHeaderValue(ImejiRsaKeys.getRsaJsonWebKey().getKeyId());
    // Set the signature algorithm on the JWT/JWS that will integrity protect the claims
    jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
    // Sign the JWS and produce the compact serialization or the complete JWT/JWS
    // representation, which is a string consisting of three dot ('.') separated
    // base64url-encoded parts in the form Header.Payload.Signature
    // If you wanted to encrypt it, you can simply set this jwt as the payload
    // of a JsonWebEncryption object and set the cty (Content Type) header to "jwt".
    return jws.getCompactSerialization();
  }


  /**
   * Use https://bitbucket.org/b_c/jose4j/wiki/Home to consume Json Web token
   *
   * @param token
   * @return
   * @throws JoseException
   */
  public static String consumeJsonWebToken(String token) throws JoseException {
    JwtConsumer jwtConsumer = new JwtConsumerBuilder().setRequireExpirationTime()
        .setAllowedClockSkewInSeconds(30).setRequireSubject()
        .setVerificationKey(ImejiRsaKeys.getRsaJsonWebKey().getKey()).build();
    try {
      JwtClaims jwtClaims = jwtConsumer.processToClaims(token);
      return jwtClaims.getSubject();
    } catch (InvalidJwtException | MalformedClaimException e) {
      LOGGER.error("Wrong APi Key!", e);
    }
    return null;

  }

  @Override
  public String getUserLogin() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getUserPassword() {
    // TODO Auto-generated method stub
    return null;
  }

}
