package org.jboss.resteasy.test.finegrain.methodparams;

import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.test.EmbeddedContainer;
import org.jboss.resteasy.util.HttpResponseCodes;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import static org.jboss.resteasy.test.TestPortProvider.generateURL;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class EncodedParamsTest
{
   private static Dispatcher dispatcher;

   @BeforeClass
   public static void before() throws Exception
   {
   }

   @AfterClass
   public static void after() throws Exception
   {
   }

   @Path("/encodedParam")
   public static class SimpleResource
   {
      @GET
      public String get(@QueryParam("hello world") int num, @QueryParam("stuff") @Encoded String stuff,
                        @QueryParam("stuff") String unStuff)
      {
         Assert.assertEquals(5, num);
         System.out.println("Hello " + num + " times");

         Assert.assertEquals("hello%20world", stuff);
         Assert.assertEquals("hello world", unStuff);
         return "HELLO";
      }

      @GET
      @Path("/{param}")
      public String goodbye(@PathParam("param") @Encoded String stuff)
      {
         System.out.println("Goodbye");
         Assert.assertEquals("hello%20world", stuff);
         return "GOODBYE";
      }
   }

   @Path("/encodedMethod")
   public static class SimpleResource2
   {
      @GET
      @Encoded
      public String get(@QueryParam("stuff") String stuff)
      {
         Assert.assertEquals("hello%20world", stuff);
         return "HELLO";
      }

      @GET
      @Encoded
      @Path("/{param}")
      public String goodbye(@PathParam("param") String stuff)
      {
         System.out.println("Goodbye");
         Assert.assertEquals("hello%20world", stuff);
         return "GOODBYE";
      }
   }

   private void _test(String path)
   {
      try
      {
         ClientRequest request = new ClientRequest(generateURL(path));
         ClientResponse<?> response = request.get();
         Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
         response.releaseConnection();
      } catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   @Test
   public void testEncoded() throws Exception
   {
      dispatcher = EmbeddedContainer.start().getDispatcher();
      try
      {
         dispatcher.getRegistry().addPerRequestResource(SimpleResource.class);
         dispatcher.getRegistry().addPerRequestResource(SimpleResource2.class);
         _test("/encodedParam?hello%20world=5&stuff=hello%20world");
         _test("/encodedParam/hello%20world");
         _test("/encodedMethod?hello%20world=5&stuff=hello%20world");
         _test("/encodedMethod/hello%20world");
      }
      finally
      {
         EmbeddedContainer.stop();
      }
   }
}