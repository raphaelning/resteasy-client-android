package org.jboss.resteasy.test.nextgen.encoding;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/test")
public interface TestClient
{
   @GET
   @Produces("text/plain")
   @Path("/path-param/{pathParam}")
   public Response getPathParam(@PathParam("pathParam") String pathParam);



   @GET
   @Produces("text/plain")
   @Path("/query-param")
   public Response getQueryParam(@QueryParam("queryParam") String queryParam);
}
