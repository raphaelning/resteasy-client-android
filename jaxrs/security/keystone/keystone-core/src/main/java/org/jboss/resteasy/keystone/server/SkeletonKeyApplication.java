package org.jboss.resteasy.keystone.server;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.resteasy.keystone.model.Mappers;
import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.spi.ResteasyConfiguration;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Context;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SkeletonKeyApplication
{
   public static final String SKELETON_KEY_INFINISPAN_CONFIG_FILE = "skeleton.key.infinispan.config.file";
   public static final String SKELETON_KEY_INFINISPAN_CACHE_NAME = "skeleton.key.infinispan.cache.name";
   Configurable configurable;
   protected Set<Object> singletons = new HashSet<Object>();

   protected PrivateKey privateKey;
   protected X509Certificate certificate;
   protected RolesService roles;
   protected ProjectsService projects;
   protected UsersService users;
   protected TokenService tokenService;
   protected Cache cache;
   protected Logger logger = Logger.getLogger(SkeletonKeyApplication.class);

   public SkeletonKeyApplication(@Context Configurable confgurable)
   {
      this.configurable = confgurable;
      String exp = getConfigProperty("skeleton.key.token.expiration");
      String unit = getConfigProperty("skeleton.key.token.expiration.unit");
      long expiration = (exp == null) ? 30 : Long.parseLong(exp);

      Mappers.registerContextResolver(confgurable);

      cache = findCache();

      users = new UsersService(cache);
      singletons.add(users);
      roles = new RolesService(cache);
      singletons.add(roles);
      projects = new ProjectsService(cache, users, roles);
      singletons.add(projects);
      TimeUnit timeUnit = (unit == null) ? TimeUnit.MINUTES : TimeUnit.valueOf(unit);
      tokenService = new TokenService(cache, expiration, timeUnit, projects, users);
      singletons.add(tokenService);
      initKeyPair();

      singletons.add(new ServerTokenAuthFilter(tokenService));

   }

   protected void initKeyPair()
   {
      try
      {
         String keyStoreFile = getConfigProperty("skeleton.key.keyStoreFile");
         String keyStorePassword = getConfigProperty("skeleton.key.keyStorePassword");
         String alias = getConfigProperty("skeleton.key.alias");
         KeyStore keyStore = null;
         if (keyStoreFile == null)
         {
            String path = getConfigProperty("skeleton.key.keyStorePath");
            if (path == null)
            {
               logger.warn("No key store provided.");
               return;
            }
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
            if (is == null)
            {
               throw new RuntimeException("Keystore path invalid: " + path);
            }
            keyStore = KeyStore.getInstance("jks");
            keyStore.load(is, keyStorePassword.toCharArray());
         }
         else
         {
            keyStore = KeyStore.getInstance("jks");
            keyStore.load(new FileInputStream(keyStoreFile), keyStorePassword.toCharArray());
         }

         privateKey = (PrivateKey)keyStore.getKey(alias, keyStorePassword.toCharArray());
         if (privateKey == null) throw new RuntimeException("Private Key is null");
         certificate = (X509Certificate)keyStore.getCertificate(alias);
         if (certificate == null) throw new RuntimeException("Certificate is null");
         tokenService.setPrivateKey(privateKey);
         tokenService.setCertificate(certificate);
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   public RolesService getRoles()
   {
      return roles;
   }

   public ProjectsService getProjects()
   {
      return projects;
   }

   public UsersService getUsers()
   {
      return users;
   }

   public TokenService getTokenService()
   {
      return tokenService;
   }

   public Set<Object> getSingletons()
   {
      return singletons;
   }

   protected ResteasyConfiguration getResteasyConfiguration()
   {
      return ResteasyProviderFactory.getContextData(ResteasyConfiguration.class);
   }

   protected String getConfigProperty(String name)
   {
      String val = (String)configurable.getConfiguration().getProperty(name);
      if (val != null) return val;
      ResteasyConfiguration config = getResteasyConfiguration();
      if (config == null) return null;
      return config.getParameter(name);

   }

   public Cache getCache()
   {
      return cache;
   }

   protected Cache findCache()
   {
      Cache cache = (Cache)configurable.getConfiguration().getProperty("skeleton.key.cache");
      if (cache != null) return cache;
      cache = getXmlCache();
      if (cache != null) return cache;
      return getDefaultCache();
   }

   protected Cache getDefaultCache()
   {
      EmbeddedCacheManager manager = new DefaultCacheManager();
      manager.defineConfiguration("custom-cache", new ConfigurationBuilder()
              .eviction().strategy(EvictionStrategy.NONE).maxEntries(1000)
              .build());
      return manager.getCache("custom-cache");
   }

   protected Cache getXmlCache()
   {
      String path = getConfigProperty(SKELETON_KEY_INFINISPAN_CONFIG_FILE);
      if (path == null) return null;

      String name = getConfigProperty(SKELETON_KEY_INFINISPAN_CACHE_NAME);
      if (name == null) throw new RuntimeException("need to specify skeleton.key.infinispan.cache.name");

      try
      {
         return new DefaultCacheManager(path).getCache(name);
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
   }

}
