package org.jboss.resteasy.keystone.as7;

import org.apache.catalina.connector.Request;
import org.jboss.resteasy.keystone.core.UserPrincipal;
import org.jboss.resteasy.keystone.model.Access;
import org.jboss.resteasy.keystone.model.Role;
import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.security.smime.PKCS7SignatureInput;
import org.jboss.security.JSSESecurityDomain;
import org.jboss.security.SecurityConstants;
import org.jboss.security.SecurityDomain;
import org.jboss.security.SecurityUtil;
import org.jboss.security.SimpleGroup;
import org.jboss.security.SimplePrincipal;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Principal;
import java.security.acl.Group;
import java.security.cert.X509Certificate;
import java.util.Map;

/**
 * Keystone Access token protocol
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SignedSkeletonKeyStoneLoginModule extends JBossWebAuthLoginModule
{
   private static final Logger log = Logger.getLogger(SignedSkeletonKeyStoneLoginModule.class);
   private static final String SECURITY_DOMAIN = "securityDomain";
   protected String projectId;
   protected String skeletonKeyCertificateAlias;
   protected Access access;
   /** The SecurityDomain to obtain the KeyStore/TrustStore from */
   private Object domain = null;

   @Override
   public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options)
   {
      super.initialize(subject, callbackHandler, sharedState, options);

      projectId = (String) options.get("projectId");
      skeletonKeyCertificateAlias = (String)options.get("skeleton.key.certificate.alias");
      // Get the security domain and default to "other"
      String sd = (String) options.get(SECURITY_DOMAIN);
      log.error("Security Domain: " + sd);
      sd = SecurityUtil.unprefixSecurityDomain(sd);
      if (sd == null)
         sd = "other";

      try
      {
         Object tempDomain = new InitialContext().lookup(SecurityConstants.JAAS_CONTEXT_ROOT + sd);
         if (tempDomain instanceof SecurityDomain)
         {
            domain = tempDomain;
         }
         else {
            tempDomain = new InitialContext().lookup(SecurityConstants.JAAS_CONTEXT_ROOT + sd + "/jsse");
            if (tempDomain instanceof JSSESecurityDomain) {
               domain = tempDomain;
            }
            else
            {
               log.error("The JSSE security domain " + sd + " is not valid. All authentication using this login module will fail!");
            }
         }
      }
      catch (NamingException e)
      {
         log.error("Unable to find the securityDomain named: " + sd, e);
      }
   }

   @Override
   protected boolean login(Request request, HttpServletResponse response) throws LoginException
   {
      String tokenHeader = request.getHeader("X-Auth-Signed-Token");
      if (tokenHeader == null) return false; // throw new LoginException("No X-Auth-Signed-Token");
      // if we don't have a trust store, we'll just use the key store.
      KeyStore keyStore = null;
      if( domain != null )
      {
         if (domain instanceof SecurityDomain)
         {
            keyStore = ((SecurityDomain) domain).getKeyStore();
         }
         else
         if (domain instanceof JSSESecurityDomain)
         {
            keyStore = ((JSSESecurityDomain) domain).getKeyStore();
         }
      }
      if (keyStore == null) throw new LoginException("No trust store found");
      X509Certificate certificate = null;
      try
      {
         certificate = (X509Certificate)keyStore.getCertificate(skeletonKeyCertificateAlias);
      }
      catch (KeyStoreException e)
      {
         throw new LoginException("Could not get certificate from keyStore");
      }
      try
      {
         PKCS7SignatureInput input = new PKCS7SignatureInput(tokenHeader);
         if (input.verify(certificate) == false) throw new LoginException("Bad Signature");
         access = (Access)input.getEntity(Access.class, MediaType.APPLICATION_JSON_TYPE);

      }
      catch (LoginException le)
      {
         throw le;
      }
      catch (Exception e)
      {
         throw new LoginException("Bad Token");
      }

      if (access.getToken().expired())
      {
         throw new LoginException("Token expired");
      }
      if (!projectId.equals(access.getToken().getProject().getId()))
      {
         throw new LoginException("Token project id doesn't match");
      }

      this.loginOk = true;
      return true;
   }

   @Override
   protected Principal getIdentity()
   {
      Principal principal = new UserPrincipal(access.getUser());
      return principal;
   }

   @Override
   protected Group[] getRoleSets() throws LoginException
   {
      SimpleGroup roles = new SimpleGroup("Roles");
      Group[] roleSets = {roles};
      for (Role role : access.getUser().getRoles())
      {
         roles.addMember(new SimplePrincipal(role.getName()));
      }
      return roleSets;
   }
}
