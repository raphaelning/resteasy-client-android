package org.jboss.resteasy.test.finegrain;

import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.test.BaseResourceTest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import java.util.List;
import java.util.Locale;

import static org.jboss.resteasy.test.TestPortProvider.generateURL;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AcceptLanguagesTest extends BaseResourceTest
{
   @Path("/lang")
   public static class Accept
   {

      @GET
      @Produces("text/plain")
      public String get(@Context HttpHeaders headers)
      {
         // en-US;q=0,en;q=0.8,de-AT,de;q=0.9
         List<Locale> accepts = headers.getAcceptableLanguages();
         for (Locale locale : accepts)
         {
            System.out.println(locale);
         }
         Assert.assertEquals(accepts.get(0).toString(), "de_AT");
         Assert.assertEquals(accepts.get(1).toString(), "de");
         Assert.assertEquals(accepts.get(2).toString(), "en");
         Assert.assertEquals(accepts.get(3).toString(), "en_US");
         return "hello";
      }
   }

   @BeforeClass
   public static void setUp() throws Exception
   {
      deployment.getRegistry().addPerRequestResource(Accept.class);
   }

   @Test
   public void testMe() throws Exception
   {
      ClientRequest request = new ClientRequest(generateURL("/lang"));
      request.header("Accept-Language", "en-US;q=0,en;q=0.8,de-AT,de;q=0.9");
      Assert.assertEquals(request.get().getStatus(), 200);

   }

}
