package org.jboss.resteasy.test.smoke;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SimpleSubresource
{

   @SuppressWarnings("unused")
   private String foo;

   @Context
   private UriInfo uriInfo;

   @GET
   @Path("basic")
   @Produces("text/plain")
   public String getBasic()
   {
      return "basic";
   }

   @Path("subresource")
   public SimpleSubresource getSubresource()
   {
      System.out.println("Subsubresource");
      return new SimpleSubresource();
   }

   @GET
   @Path("testContextParam")
   public void testContextParam()
   {
      if (uriInfo != null)
         throw new IllegalStateException("uriInfo is supposed to be null");
   }

}
