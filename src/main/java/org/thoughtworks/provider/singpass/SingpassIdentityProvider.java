package org.thoughtworks.provider.singpass;

import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.JsonWebToken;

/** @author yuexie.zhou */
public class SingpassIdentityProvider extends OIDCIdentityProvider {

  private static final Logger log = Logger.getLogger(SingpassIdentityProvider.class);

  public SingpassIdentityProvider(KeycloakSession session, SingpassIdentityProviderConfig config) {
    super(session, config);

    System.out.println("SingpassIdentityProvider: ");
    System.out.println(config.getPrivateKey());
    System.out.println(getConfig().toString());
  }

  @Override
  protected JsonWebToken validateToken(String encodedIdToken, boolean ignoreAudience) {
    if (encodedIdToken == null) {
      throw new IdentityBrokerException("No token from server.");
    }

    JsonWebToken token = new JsonWebToken();

    log.debug(getSingpassConfig().getPrivateKey());

    return token;
  }

  private SingpassIdentityProviderConfig getSingpassConfig() {
    return (SingpassIdentityProviderConfig) super.getConfig();
  }
}
