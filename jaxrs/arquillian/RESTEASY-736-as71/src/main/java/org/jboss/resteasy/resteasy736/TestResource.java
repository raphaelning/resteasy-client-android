package org.jboss.resteasy.resteasy736;

import org.jboss.resteasy.annotations.Suspend;
import org.jboss.resteasy.spi.AsynchronousResponse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 *
 * Copyright Aug 15, 2012
 */
@Path("/")
@Produces("text/plain")
public class TestResource
{
   @GET
   @Path("test")
   public void test(final @Suspend(5000) AsynchronousResponse response)
   {
      Thread t = new Thread()
      {
         @Override
         public void run()
         {
            try
            {
               System.out.println("TestResource: async thread started");
               Thread.sleep(10000);
               Response jaxrs = Response.ok("test").type(MediaType.TEXT_PLAIN).build();
               response.setResponse(jaxrs);
               System.out.println("TestResource: async thread finished");
            }
            catch (Exception e)
            {
               e.printStackTrace();
            }
         }
      };
      t.start();
   }
}
