package org.jboss.resteasy.test.client;

import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.conn.ClientConnectionManager;
import ch.boye.httpclientandroidlib.conn.params.ConnManagerParams;
import ch.boye.httpclientandroidlib.conn.scheme.PlainSocketFactory;
import ch.boye.httpclientandroidlib.conn.scheme.Scheme;
import ch.boye.httpclientandroidlib.conn.scheme.SchemeRegistry;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.impl.conn.tsccm.ThreadSafeClientConnManager;
import ch.boye.httpclientandroidlib.params.BasicHttpParams;
import ch.boye.httpclientandroidlib.params.HttpParams;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.ClientResponseFailure;
import org.jboss.resteasy.client.ProxyFactory;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.jboss.resteasy.spi.NoLogWebApplicationException;
import org.jboss.resteasy.test.BaseResourceTest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Test connection cleanup
 *
 * Modified for Android by Raphael Ning.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ApacheHttpClient4Test extends BaseResourceTest
{

   public static class MyResourceImpl implements MyResource
   {
      public String get()
      {
         return "hello world";
      }

      public String error()
      {
         throw new NoLogWebApplicationException(404);
      }
   }

   @Path("/test")
   public static interface MyResource
   {
      @GET
      @Produces("text/plain")
      public String get();

      @GET
      @Path("error")
      @Produces("text/plain")
      String error();
   }

   @BeforeClass
   public static void setUp() throws Exception
   {
      addPerRequestResource(MyResourceImpl.class);
   }

   private AtomicLong counter = new AtomicLong();

   @Test
   public void testConnectionCleanupGC() throws Exception
   {
      final ApacheHttpClient4Executor executor = createClient();
      counter.set(0);


      Thread[] threads = new Thread[3];

      for (int i = 0; i < 3; i++)
      {
         threads[i] = new Thread()
         {
            @Override
            public void run()
            {
               for (int j = 0; j < 10; j++)
               {
                  runit(executor, false);
                  System.gc();
               }
            }
         };
      }

      for (int i = 0; i < 3; i++) threads[i].start();
      for (int i = 0; i < 3; i++) threads[i].join();

      Assert.assertEquals(30l, counter.get());
   }

   @Test
   public void testConnectionCleanupManual() throws Exception
   {
      final ApacheHttpClient4Executor executor = createClient();
      counter.set(0);


      Thread[] threads = new Thread[3];

      for (int i = 0; i < 3; i++)
      {
         threads[i] = new Thread()
         {
            @Override
            public void run()
            {
               for (int j = 0; j < 10; j++)
               {
                  runit(executor, true);
               }
            }
         };
      }

      for (int i = 0; i < 3; i++) threads[i].start();
      for (int i = 0; i < 3; i++) threads[i].join();

      Assert.assertEquals(30l, counter.get());
   }

   @Test
   public void testConnectionCleanupProxy() throws Exception
   {
      final ApacheHttpClient4Executor executor = createClient();
      final MyResource proxy = ProxyFactory.create(MyResource.class, "http://localhost:8081", executor);
      counter.set(0);


      Thread[] threads = new Thread[3];


      for (int i = 0; i < 3; i++)
      {
         threads[i] = new Thread()
         {
            @Override
            public void run()
            {
               for (int j = 0; j < 10; j++)
               {
                  System.out.println("calling proxy");
                  String str = proxy.get();
                  System.out.println("returned: " + str);
                  Assert.assertEquals("hello world", str);
                  counter.incrementAndGet();
               }
            }
         };
      }

      for (int i = 0; i < 3; i++) threads[i].start();
      for (int i = 0; i < 3; i++) threads[i].join();

      Assert.assertEquals(30l, counter.get());
   }

   @Test
   public void testConnectionCleanupErrorGC() throws Exception
   {
      final ApacheHttpClient4Executor executor = createClient();
      final MyResource proxy = ProxyFactory.create(MyResource.class, "http://localhost:8081", executor);
      counter.set(0);


      Thread[] threads = new Thread[3];


      for (int i = 0; i < 3; i++)
      {
         threads[i] = new Thread()
         {
            @Override
            public void run()
            {
               for (int j = 0; j < 10; j++)
               {
                  System.out.println("calling proxy");
                  callProxy(proxy);
                  System.gc();
                  System.out.println("returned");
               }
            }
         };
      }

      for (int i = 0; i < 3; i++) threads[i].start();
      for (int i = 0; i < 3; i++) threads[i].join();

      Assert.assertEquals(30l, counter.get());
   }

   @Test
   public void testConnectionCleanupErrorNoGC() throws Exception
   {
      final ApacheHttpClient4Executor executor = createClient();
      final MyResource proxy = ProxyFactory.create(MyResource.class, "http://localhost:8081", executor);
      counter.set(0);


      Thread[] threads = new Thread[3];


      for (int i = 0; i < 3; i++)
      {
         threads[i] = new Thread()
         {
            @Override
            public void run()
            {
               for (int j = 0; j < 10; j++)
               {
                  System.out.println("calling proxy");
                  String str = null;
                  try
                  {
                     str = proxy.error();
                  }
                  catch (ClientResponseFailure e)
                  {
                     Assert.assertEquals(e.getResponse().getStatus(), 404);
                     e.getResponse().releaseConnection();
                     counter.incrementAndGet();
                  }
                  System.out.println("returned");
               }
            }
         };
      }

      for (int i = 0; i < 3; i++) threads[i].start();
      for (int i = 0; i < 3; i++) threads[i].join();

      Assert.assertEquals(30l, counter.get());
   }

   private void callProxy(MyResource proxy)
   {
      String str = null;
      try
      {
         str = proxy.error();
      }
      catch (ClientResponseFailure e)
      {
         Assert.assertEquals(e.getResponse().getStatus(), 404);
         counter.incrementAndGet();
      }
   }


   private ApacheHttpClient4Executor createClient()
   {
      HttpParams params = new BasicHttpParams();
      ConnManagerParams.setMaxTotalConnections(params, 3);
      ConnManagerParams.setTimeout(params, 1000);

      // Create and initialize scheme registry
      SchemeRegistry schemeRegistry = new SchemeRegistry();
      schemeRegistry.register(
              new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));

      // Create an HttpClient with the ThreadSafeClientConnManager.
      // This connection manager must be used if more than one thread will
      // be using the HttpClient.
      ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
      HttpClient httpClient = new DefaultHttpClient(cm, params);

      final ApacheHttpClient4Executor executor = new ApacheHttpClient4Executor(httpClient);
      return executor;
   }

   private void runit(ApacheHttpClient4Executor executor, boolean release)
   {
      ClientRequest request = executor.createRequest("http://localhost:8081/test");
      ClientResponse response = null;
      try
      {
         System.out.println("get");
         response = request.get();
         Assert.assertEquals(200, response.getStatus());
         //Assert.assertEquals("hello world", response.getEntity(String.class));
         System.out.println("ok");
         if (release) response.releaseConnection();
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
      counter.incrementAndGet();
   }
}
