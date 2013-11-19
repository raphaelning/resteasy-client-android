package org.jboss.resteasy.test.client;

import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.core.executors.URLConnectionClientExecutor;
import org.jboss.resteasy.test.BaseResourceTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import static org.jboss.resteasy.test.TestPortProvider.generateURL;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UrlExecutorTest extends BaseResourceTest
{
   @Path("/test")
   public static class TestService
   {
      @POST
      @Path("{name}")
      @Produces("text/plain")
      @Consumes("text/plain")
      public String post(@PathParam("name") String id, String message)
      {
         System.out.println("Server!!!");
         return message + id;
      }
   }

   @Before
   public void setUp() throws Exception
   {
      addPerRequestResource(TestService.class);
   }

   @Test
   public void testStylesheet() throws Exception
   {
      URLConnectionClientExecutor executor = new URLConnectionClientExecutor();
      ClientRequest request = executor.createRequest(generateURL("/test/{name}"));
      request.body("text/plain", "Hello ").pathParameter("name", "Bill");
      String response = request.postTarget(String.class);
      System.out.println(response);
      Assert.assertEquals(response, "Hello Bill");
      response = request.postTarget(String.class);
      System.out.println(response);
      Assert.assertEquals(response, "Hello Bill");
      request.clear();
      request.body("text/plain", "Goodbye ").pathParameter("name", "Everyone");
      response = request.postTarget(String.class);
      System.out.println(response);
      Assert.assertEquals(response, "Goodbye Everyone");


   }
}
