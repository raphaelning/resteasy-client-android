package org.jboss.resteasy.test.regression;

import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.test.BaseResourceTest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static org.jboss.resteasy.test.TestPortProvider.generateBaseUrl;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Regression657 extends BaseResourceTest
{

   public interface Foo<T>
   {
      @GET
      public T getFoo(@HeaderParam("foo") String val);
   }

   @Path("blah")
   public static class ImpFoo implements Foo
   {
      @Override
      public Object getFoo(String val)
      {
         return "hello";
      }
   }


   public static class OhaUserModel
   {
      private String username;

      public OhaUserModel(String username)
      {
         this.username = username;
      }

      @Override
      public String toString()
      {
         return username;
      }
   }

   public interface BaseCrudService<T>
   {

      @GET
      @Path("/content/{id}")
      @Produces(MediaType.APPLICATION_JSON)
      public T getContent(
              @PathParam("id")
              String id);

      @POST
      @Path("/add")
      @Produces(MediaType.APPLICATION_JSON)
      @Consumes(MediaType.APPLICATION_JSON)
      public T add(T object);

      @GET
      @Path("/all")
      @Produces(MediaType.APPLICATION_JSON)
      public List<T> get();

      @PUT
      @Path("/update")
      @Produces(MediaType.APPLICATION_JSON)
      @Consumes(MediaType.APPLICATION_JSON)
      public T update(T object);

      @DELETE
      @Path("/delete/{id}")
      @Produces(MediaType.TEXT_PLAIN)
      public Boolean delete(
              @PathParam("id")
              String id);
   }

   @Path("/platform")
   public interface PlatformServiceLocator
   {

      @Path("/users/{user}")
      public UserRestService getUserService(
              @HeaderParam("entity")
              String entity,
              @HeaderParam("ticket")
              String ticket,
              @PathParam("user")
              String userId
      );
   }


   @Path("/users")
   public interface UserRestService extends BaseUserService
   {

      @GET
      @Path("/content/{id}")
      @Produces(MediaType.APPLICATION_JSON)
      public OhaUserModel getContent(
              @PathParam("id")
              String id);

      @POST
      @Path("/add")
      @Produces(MediaType.APPLICATION_JSON)
      @Consumes(MediaType.APPLICATION_JSON)
      public OhaUserModel add(OhaUserModel object);

      @GET
      @Path("/all")
      @Produces(MediaType.APPLICATION_JSON)
      public List<OhaUserModel> get();

      @PUT
      @Path("/update")
      @Produces(MediaType.APPLICATION_JSON)
      @Consumes(MediaType.APPLICATION_JSON)
      public OhaUserModel update(OhaUserModel object);

      @DELETE
      @Path("/delete/{id}")
      @Produces(MediaType.TEXT_PLAIN)
      public Boolean delete(
              @PathParam("id")
              String id);


      @GET
      @Path("/getbynamesurname/{name}/{surname}")
      @Produces(MediaType.APPLICATION_JSON)
      public List<OhaUserModel> getByNameSurname(
              @PathParam("name")
              String name,
              @PathParam("surname")
              String surname
      );

      @GET
      @Path("/getuserbymail/{mail}")
      @Produces(MediaType.APPLICATION_JSON)
      public OhaUserModel getUserByMail(
              @PathParam("mail")
              String mail
      );


      @POST
      @Path("/update/{id}")
      @Produces(MediaType.TEXT_PLAIN)
      //@Consumes(MediaType.TEXT_PLAIN)
      public Boolean update(
              @PathParam("id")
              String id,
              @QueryParam("adaId")
              String adaId,
              @QueryParam("name")
              String name,
              @QueryParam("surname")
              String surname,
              @QueryParam("address")
              String address,
              @QueryParam("city")
              String city,
              @QueryParam("country")
              String country,
              @QueryParam("zipcode")
              String zipcode,
              @QueryParam("email")
              String email,
              @QueryParam("phone")
              String phone,
              @QueryParam("phone")
              String timezone);

      @POST
      @Path("/updatepassword/{username}")
      @Produces(MediaType.TEXT_PLAIN)
      @Consumes(MediaType.APPLICATION_JSON)
      public Boolean updatePassword(
              @PathParam("username")
              String username,
              List<String> passwords);


      @POST
      @Path("/createuser")
      @Produces(MediaType.APPLICATION_JSON)
      public Boolean create(
              @QueryParam("email")
              String email,
              @QueryParam("password")
              String password,
              @QueryParam("username")
              String username);

      @GET
      @Path("/show-help/{user}")
      @Produces(MediaType.TEXT_PLAIN)
      public Boolean showHelp(
              @PathParam("user")
              long userId);

      @PUT
      @Path("/show-help/{user}/{show}")
      @Produces(MediaType.TEXT_PLAIN)
      public Boolean setShowHelp(
              @PathParam("user")
              long userId,
              @PathParam("show")
              boolean showHelp);


      @GET
      @Path("/create-jabber")
      @Produces(MediaType.TEXT_PLAIN)
      public void createJabberAccounts();

   }

   public interface BaseUserService extends BaseCrudService<OhaUserModel>
   {


      @GET
      @Produces("text/plain")
      @Path("data/ada/{user}")
      public OhaUserModel getUserDataByAdaId(
              @PathParam("user")
              String adaId);
   }


   public static class PlatformServiceLocatorImpl implements PlatformServiceLocator
   {
      @Override
      public UserRestService getUserService(String entity, String ticket, String userId)
      {
         return new UserRestService()
         {
            @Override
            public List<OhaUserModel> getByNameSurname(@PathParam("name") String name, @PathParam("surname") String surname)
            {
               return null;
            }

            @Override
            public OhaUserModel getUserByMail(@PathParam("mail") String mail)
            {
               return null;
            }

            @Override
            public Boolean update(@PathParam("id") String id, @QueryParam("adaId") String adaId, @QueryParam("name") String name, @QueryParam("surname") String surname, @QueryParam("address") String address, @QueryParam("city") String city, @QueryParam("country") String country, @QueryParam("zipcode") String zipcode, @QueryParam("email") String email, @QueryParam("phone") String phone, @QueryParam("phone") String timezone)
            {
               return null;
            }

            @Override
            public Boolean updatePassword(@PathParam("username") String username, List<String> passwords)
            {
               return null;
            }

            @Override
            public Boolean create(@QueryParam("email") String email, @QueryParam("password") String password, @QueryParam("username") String username)
            {
               return null;
            }

            @Override
            public Boolean showHelp(@PathParam("user") long userId)
            {
               return null;
            }

            @Override
            public Boolean setShowHelp(@PathParam("user") long userId, @PathParam("show") boolean showHelp)
            {
               return null;
            }

            @Override
            public void createJabberAccounts()
            {
            }

            @Override
            public OhaUserModel getContent(String id)
            {
               return null;
            }

            @Override
            public OhaUserModel add(OhaUserModel object)
            {
               return null;
            }

            @Override
            public List<OhaUserModel> get()
            {
               return null;
            }

            @Override
            public OhaUserModel update(OhaUserModel object)
            {
               return null;
            }

            @Override
            public Boolean delete(String id)
            {
               return null;
            }

            @Override
            public OhaUserModel getUserDataByAdaId(String adaId)
            {
               return new OhaUserModel("bill");
            }
         };
      }
   }

   @BeforeClass
   public static void setup() throws Exception
   {
      addPerRequestResource(ImpFoo.class); /* this is the actual reproduction of the problem */
      addPerRequestResource(PlatformServiceLocatorImpl.class);
   }

   @Test
   public void test657() throws Exception
   {
      ClientRequest request = new ClientRequest(generateBaseUrl() + "/platform/users/89080/data/ada/jsanchez110");
      String s = request.getTarget(String.class);
      Assert.assertEquals(s, "bill");

   }

}
