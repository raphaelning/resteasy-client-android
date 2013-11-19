package org.jboss.resteasy.tests;

import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AsyncTest
{
   @Test
   public void testAsync() throws Exception
   {
      ClientRequest request = new ClientRequest("http://localhost:8080/cdi-test/jaxrs/async");
      ClientResponse<String> response = request.get(String.class);
      Assert.assertEquals(200, response.getStatus());
      Assert.assertEquals("hello", response.getEntity());
   }

   @Test
   public void testTimeout() throws Exception
   {
      ClientRequest request = new ClientRequest("http://localhost:8080/cdi-test/jaxrs/async/timeout");
      ClientResponse<String> response = request.get(String.class);
      Assert.assertEquals(503, response.getStatus());
   }
}
