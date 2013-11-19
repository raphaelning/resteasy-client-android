package org.jboss.resteasy.plugins.cache.server;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.resteasy.spi.ResteasyConfiguration;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import java.io.IOException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServerCacheFeature implements Feature
{
   protected ServerCache cache;

   public ServerCacheFeature()
   {
   }

   public ServerCacheFeature(ServerCache cache)
   {
      this.cache = cache;
   }

   @Override
   public boolean configure(FeatureContext configurable)
   {
      ServerCache cache = getCache(configurable);
      if (cache == null) return false;
      configurable.register(new ServerCacheHitFilter(cache));
      configurable.register(new ServerCacheInterceptor(cache));
      return true;
   }

   protected ResteasyConfiguration getResteasyConfiguration()
   {
      return ResteasyProviderFactory.getContextData(ResteasyConfiguration.class);
   }

   protected String getConfigProperty(String name)
   {
      ResteasyConfiguration config = getResteasyConfiguration();
      if (config == null) return null;
      return config.getParameter(name);

   }

   protected ServerCache getCache(Configurable configurable)
   {
      if (this.cache != null) return this.cache;
      ServerCache cache = (ServerCache)configurable.getConfiguration().getProperty(ServerCache.class.getName());
      if (cache != null) return cache;
      cache = getXmlCache(configurable);
      if (cache != null) return cache;
      return getDefaultCache();
   }

   protected ServerCache getDefaultCache()
   {
      EmbeddedCacheManager manager = new DefaultCacheManager();
      manager.defineConfiguration("custom-cache", new ConfigurationBuilder()
              .eviction().strategy(EvictionStrategy.LIRS).maxEntries(100)
              .build());
      Cache<Object, Object> c = manager.getCache("custom-cache");
      return new InfinispanCache(c);
   }

   protected ServerCache getXmlCache(Configurable configurable)
   {
      String path = (String)configurable.getConfiguration().getProperty("server.request.cache.infinispan.config.file");
      if (path == null) path = getConfigProperty("server.request.cache.infinispan.config.file");
      if (path == null) return null;

      String name = (String)configurable.getConfiguration().getProperty("server.request.cache.infinispan.cache.name");
      if (name == null) name = getConfigProperty("server.request.cache.infinispan.cache.name");
      if (name == null) throw new RuntimeException("need to specify server.request.cache.infinispan.cache.name");

      try
      {
         Cache<Object, Object> c = new DefaultCacheManager(path).getCache(name);
         return new InfinispanCache(c);
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
   }
}
