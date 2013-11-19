package org.jboss.resteasy.skeleton.key.as7;

import org.apache.catalina.connector.Request;
import org.jboss.logging.Logger;
import org.jboss.resteasy.skeleton.key.RSATokenVerifier;
import org.jboss.resteasy.skeleton.key.ResourceMetadata;
import org.jboss.resteasy.skeleton.key.SkeletonKeyPrincipal;
import org.jboss.resteasy.skeleton.key.SkeletonKeySession;
import org.jboss.resteasy.skeleton.key.VerificationException;
import org.jboss.resteasy.skeleton.key.representations.SkeletonKeyToken;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CatalinaBearerTokenAuthenticator
{
   protected ResourceMetadata resourceMetadata;
   protected boolean challenge;
   protected Logger log = Logger.getLogger(CatalinaBearerTokenAuthenticator.class);
   protected String tokenString;
   protected SkeletonKeyToken token;
   private Principal principal;
   protected boolean propagateToken;

   public CatalinaBearerTokenAuthenticator(ResourceMetadata resourceMetadata, boolean propagateToken, boolean challenge)
   {
      this.resourceMetadata = resourceMetadata;
      this.challenge = challenge;
      this.propagateToken = propagateToken;
   }

   public ResourceMetadata getResourceMetadata()
   {
      return resourceMetadata;
   }

   public String getTokenString()
   {
      return tokenString;
   }

   public SkeletonKeyToken getToken()
   {
      return token;
   }

   public Principal getPrincipal()
   {
      return principal;
   }

   public boolean login(Request request, HttpServletResponse response) throws LoginException, IOException
   {
      String authHeader = request.getHeader("Authorization");
      if (authHeader == null)
      {
         if (challenge)
         {
            challengeResponse(response, null, null);
            return false;
         }
         else
         {
            return false;
         }
      }

      String[] split = authHeader.trim().split("\\s+");
      if (split == null || split.length != 2) challengeResponse(response, null, null);
      if (!split[0].equalsIgnoreCase("Bearer")) challengeResponse(response, null, null);


      tokenString = split[1];

      try
      {
         token = RSATokenVerifier.verifyToken(tokenString, resourceMetadata);
      }
      catch (VerificationException e)
      {
         log.error("Failed to verify token", e);
         challengeResponse(response, "invalid_token", e.getMessage());
      }
      boolean verifyCaller = false;
      Set<String> roles = null;
      if (resourceMetadata.getResourceName() != null)
      {
         SkeletonKeyToken.Access access = token.getResourceAccess(resourceMetadata.getResourceName());
         if (access != null) roles = access.getRoles();
         verifyCaller = token.isVerifyCaller(resourceMetadata.getResourceName());
      }
      else
      {
         verifyCaller = token.isVerifyCaller();
         SkeletonKeyToken.Access access = token.getRealmAccess();
         if (access != null) roles = access.getRoles();
      }
      String surrogate = null;
      if (verifyCaller)
      {
         if (token.getTrustedCertificates() == null || token.getTrustedCertificates().size() == 0)
         {
            response.sendError(400);
            throw new LoginException("No trusted certificates in token");
         }
         // for now, we just make sure JBoss Web did two-way SSL
         // assume JBoss Web verifies the client cert
         X509Certificate[] chain = request.getCertificateChain();
         if (chain == null || chain.length == 0)
         {
            response.sendError(400);
            throw new LoginException("No certificates provided by jboss web to verify the caller");
         }
         surrogate = chain[0].getSubjectX500Principal().getName();
      }
      SkeletonKeyPrincipal skeletonKeyPrincipal = new SkeletonKeyPrincipal(token.getPrincipal(), surrogate);
      principal = new CatalinaSecurityContextHelper().createPrincipal(request.getContext().getRealm(), skeletonKeyPrincipal, roles);
      request.setUserPrincipal(principal);
      request.setAuthType("OAUTH_BEARER");
      if (propagateToken)
      {
         SkeletonKeySession skSession = new SkeletonKeySession(tokenString, resourceMetadata);
         request.setAttribute(SkeletonKeySession.class.getName(), skSession);
         ResteasyProviderFactory.pushContext(SkeletonKeySession.class, skSession);
      }

      return true;
   }


   protected void challengeResponse(HttpServletResponse response, String error, String description) throws LoginException
   {
      StringBuilder header = new StringBuilder("Bearer realm=\"");
      header.append(resourceMetadata.getRealm()).append("\"");
      if (error != null)
      {
         header.append(", error=\"").append(error).append("\"");
      }
      if (description != null)
      {
         header.append(", error_description=\"").append(description).append("\"");
      }
      response.setHeader("WWW-Authenticate", header.toString());
      try
      {
         response.sendError(401);
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
      throw new LoginException("Challenged");
   }
}
