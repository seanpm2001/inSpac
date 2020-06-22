package com.thoughtworks.provider.singpass;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.thoughtworks.provider.singpass.utils.PrivateKeyUtils;
import java.io.IOException;
import java.security.PrivateKey;
import java.text.ParseException;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.JsonWebToken;

/** @author yuexie.zhou */
public class SingpassIdentityProvider extends OIDCIdentityProvider {

  public static final String EMAIL_HOST = "@placeholder.com";

  public SingpassIdentityProvider(KeycloakSession session, SingpassIdentityProviderConfig config) {
    super(session, config);
  }

  @Override
  protected JsonWebToken validateToken(String encodedIdToken, boolean ignoreAudience) {
    if (encodedIdToken == null) {
      throw new IdentityBrokerException("No token from server.");
    }

    JsonWebToken token;
    try {
      JWEObject jweObject = JWEObject.parse(encodedIdToken);
      PrivateKey privateKey = PrivateKeyUtils.parsePrivateKey(getSingpassConfig().getPrivateKey());
      jweObject.decrypt(new RSADecrypter(privateKey));

      JWSInput jws = new JWSInput(jweObject.getPayload().toString());
      if (!verify(jws)) {
        throw new IdentityBrokerException("token signature validation failed");
      }
      token = jws.readJsonContent(JsonWebToken.class);
    } catch (JWSInputException e) {
      throw new IdentityBrokerException("Invalid token", e);
    } catch (ParseException e) {
      throw new IdentityBrokerException("JWE parse failed", e);
    } catch (JOSEException e) {
      throw new IdentityBrokerException("JWE decrypt failed", e);
    } catch (IOException e) {
      throw new IdentityBrokerException("Read private key failed", e);
    }

    String iss = token.getIssuer();

    if (!token.isActive(getConfig().getAllowedClockSkew())) {
      throw new IdentityBrokerException("Token is no longer valid");
    }

    if (!ignoreAudience && !token.hasAudience(getConfig().getClientId())) {
      throw new IdentityBrokerException("Wrong audience from token.");
    }

    String trustedIssuers = getConfig().getIssuer();

    if (trustedIssuers != null) {
      String[] issuers = trustedIssuers.split(",");

      for (String trustedIssuer : issuers) {
        if (iss != null && iss.equals(trustedIssuer.trim())) {
          return token;
        }
      }

      throw new IdentityBrokerException(
          "Wrong issuer from token. Got: " + iss + " expected: " + getConfig().getIssuer());
    }

    return token;
  }

  @Override
  protected BrokeredIdentityContext extractIdentity(
      AccessTokenResponse tokenResponse, String accessToken, JsonWebToken idToken) {
    String id = idToken.getSubject();
    BrokeredIdentityContext identity = new BrokeredIdentityContext(id);

    String[] splitPart = id.split(",");

    String nric;
    String uuid;
    String email;

    boolean subOnlyHaveUUID = splitPart.length == 1;
    if (subOnlyHaveUUID) {
      nric = "none";
      uuid = splitPart[0].split("=")[1];
      email = uuid + EMAIL_HOST;
    } else {
      nric = splitPart[0].split("=")[1];
      uuid = splitPart[1].split("=")[1];
      email = nric + EMAIL_HOST;
    }

    identity.getContextData().put(VALIDATED_ID_TOKEN, idToken);

    identity.setId(id);
    identity.setFirstName(nric);
    identity.setLastName(uuid);
    identity.setEmail(email);
    identity.setBrokerUserId(getConfig().getAlias() + "." + id);
    identity.setUsername(nric);

    if (tokenResponse != null && tokenResponse.getSessionState() != null) {
      identity.setBrokerSessionId(getConfig().getAlias() + "." + tokenResponse.getSessionState());
    }
    if (tokenResponse != null)
      identity.getContextData().put(FEDERATED_ACCESS_TOKEN_RESPONSE, tokenResponse);
    if (tokenResponse != null) processAccessTokenResponse(identity, tokenResponse);

    return identity;
  }

  private SingpassIdentityProviderConfig getSingpassConfig() {
    return (SingpassIdentityProviderConfig) super.getConfig();
  }
}
