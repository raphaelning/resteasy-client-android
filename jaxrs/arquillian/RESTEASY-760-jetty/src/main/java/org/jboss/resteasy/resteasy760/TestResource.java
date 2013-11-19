package org.jboss.resteasy.resteasy760;

import javax.ws.rs.Consumes;
import javax.ws.rs.Encoded;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 *
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @author Achim Bitzer
 * @version $Revision: 1.1 $
 *
 * Copyright Aug 15, 2012
 */
@Path("/")
@Produces("text/plain")
public class TestResource
{
   @PUT
   @Path("put/noquery")
   @Consumes("application/x-www-form-urlencoded")
   public String noQueryPut(@FormParam("formParam") String formParam)
   {
      return formParam;
   }
   
   @PUT
   @Path("put/noquery/encoded")
   @Consumes("application/x-www-form-urlencoded")
   public String noQueryPutEncoded(@Encoded @FormParam("formParam") String formParam)
   {
      return formParam;
   }
   
   @POST
   @Path("post/noquery")
   @Consumes("application/x-www-form-urlencoded")
   public String noQueryPost(@FormParam("formParam") String formParam)
   {
      return formParam;
   }
   
   @POST
   @Path("post/noquery/encoded")
   @Consumes("application/x-www-form-urlencoded")
   public String noQueryPostEncoded(@Encoded @FormParam("formParam") String formParam)
   {
      return formParam;
   }
   
   @PUT
   @Path("put/query")
   @Consumes("application/x-www-form-urlencoded")
   public String queryPut(@FormParam("formParam") String formParam)
   {
      return formParam;
   }
   
   @PUT
   @Path("put/query/encoded")
   @Consumes("application/x-www-form-urlencoded")
   public String queryPutEncoded(@Encoded @FormParam("formParam") String formParam)
   {
      return formParam;
   }
   
   @POST
   @Path("post/query")
   @Consumes("application/x-www-form-urlencoded")
   public String queryPost(@FormParam("formParam") String formParam)
   {
      return formParam;
   }
   
   @POST
   @Path("post/query/encoded")
   @Consumes("application/x-www-form-urlencoded")
   public String queryPostEncoded(@Encoded @FormParam("formParam") String formParam)
   {
      return formParam;
   }
}
