package org.jboss.resteasy.test.smoke;

import org.junit.Assert;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Path("/")
public class SimpleResource
{

   private final static String encodedPart = "foo+bar%20gee@foo.com";
   private final static String decodedPart = "foo+bar gee@foo.com";
   private final static String queryDecodedPart = "foo bar gee@foo.com";

   @GET
   @Path("basic")
   @Produces("text/plain")
   public String getBasic()
   {
      System.out.println("getBasic()");
      return "basic";
   }

   @PUT
   @Path("basic")
   @Consumes("text/plain")
   public void putBasic(String body)
   {
      System.out.println(body);
   }

   @GET
   @Path("queryParam")
   @Produces("text/plain")
   public String getQueryParam(@QueryParam("param")String param)
   {
      return param;
   }

   @GET
   @Path("matrixParam")
   @Produces("text/plain")
   public String getMatrixParam(@MatrixParam("param")String param)
   {
      return param;
   }

   @GET
   @Path("uriParam/{param}")
   @Produces("text/plain")
   public int getUriParam(@PathParam("param")int param)
   {
      return param;
   }

   @POST
   @Path("formtestit")
   @Produces("text/plain")
   public String postForm(@FormParam("value") String value, @Context HttpHeaders headers)
   {
      System.out.println(headers.getRequestHeaders().getFirst("content-type"));
      System.out.println("HERE!!!");
      if (value == null) throw new RuntimeException("VALUE WAS NULL");
      System.out.println(value);
      return value;
   }


   @PUT
   @Path("formtestit")
   @Produces("text/plain")
   public String putForm(@FormParam("value") String value, @Context HttpHeaders headers)
   {
      System.out.println(headers.getRequestHeaders().getFirst("content-type"));
      System.out.println("HERE!!!");
      if (value == null) throw new RuntimeException("VALUE WAS NULL");
      System.out.println(value);
      return value;
   }

   @GET
   @Path("xml")
   @Produces("application/xml")
   public Customer getCustomer()
   {
      return new Customer("Bill Burke");
   }

   @Path("/simple/{bar}")
   @GET
   public String get(@PathParam("bar") String pathParam, @QueryParam("foo") String queryParam)
   {
      Assert.assertEquals(decodedPart, pathParam);
      Assert.assertEquals(queryDecodedPart, queryParam);
      return pathParam;
   }
}