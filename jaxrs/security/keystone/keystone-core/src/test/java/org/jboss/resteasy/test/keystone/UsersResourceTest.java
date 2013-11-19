package org.jboss.resteasy.test.keystone;

import junit.framework.Assert;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.keystone.model.User;
import org.jboss.resteasy.keystone.server.SkeletonKeyApplication;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.test.EmbeddedContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Set;

import static org.jboss.resteasy.test.TestPortProvider.generateURL;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UsersResourceTest
{
   private static ResteasyDeployment deployment;

   public static class SApp extends Application
   {
      SkeletonKeyApplication app;

      public SApp(@Context Configurable confgurable)
      {
         this.app = new SkeletonKeyApplication(confgurable);
      }



      @Override
      public Set<Object> getSingletons()
      {
         return app.getSingletons();
      }
   }

   @BeforeClass
   public static void before() throws Exception
   {
      deployment = new ResteasyDeployment();
      deployment.setApplicationClass(SApp.class.getName());
      EmbeddedContainer.start(deployment);
   }

   @AfterClass
   public static void after() throws Exception
   {
      EmbeddedContainer.stop();
   }

   @Test
   public void testUser()
   {
      String newUser = "{ \"user\" : { \"username\" : \"wburke\", \"name\" : \"Bill Burke\", \"email\" : \"bburke@redhat.com\", \"enabled\" : true, \"credentials\" : { \"password\" : \"geheim\" }} }";
      ResteasyClient client = new ResteasyClientBuilder().providerFactory(deployment.getProviderFactory())
                                                         .connectionPoolSize(100)
                                                         .maxPooledPerRoute(100).build();
      Response response = client.target(generateURL("/users")).request().post(Entity.json(newUser));
      Assert.assertEquals(response.getStatus(), 201);
      response.close();
      ResteasyWebTarget target = client.target(response.getLocation());
      String user = target.request().get(String.class);
      System.out.println(user);
      User u = target.request().get(User.class);
      System.out.println(u);
      Assert.assertEquals("wburke", u.getUsername());
      Assert.assertEquals("Bill Burke", u.getName());
      Assert.assertEquals("bburke@redhat.com", u.getEmail());
      Assert.assertTrue(u.getEnabled());
      u.setName("William Burke");
      Assert.assertEquals(target.request().put(Entity.json(u)).getStatus(), 204);
      u = target.request().get(User.class);
      Assert.assertEquals("William Burke", u.getName());
      Assert.assertEquals(target.request().delete().getStatus(), 204);
      response = target.request().get();
      Assert.assertEquals(404, response.getStatus());
      client.close();
   }
   @Test
   public void testUserId()
   {
      String newUser = "{ \"user\" : { \"id\" : \"5\", \"username\" : \"wburke\", \"name\" : \"Bill Burke\", \"email\" : \"bburke@redhat.com\", \"enabled\" : true, \"credentials\" : { \"password\" : \"geheim\" }} }";
      ResteasyClient client = new ResteasyClientBuilder().providerFactory(deployment.getProviderFactory()).build();
      Response response = client.target(generateURL("/users")).request().post(Entity.json(newUser));
      Assert.assertEquals(response.getStatus(), 201);
      response.close();
      ResteasyWebTarget target = client.target(response.getLocation());
      String user = target.request().get(String.class);
      System.out.println(user);
      User u = target.request().get(User.class);
      System.out.println(u);
      Assert.assertEquals("5", u.getId());
      Assert.assertEquals("wburke", u.getUsername());
      Assert.assertEquals("Bill Burke", u.getName());
      Assert.assertEquals("bburke@redhat.com", u.getEmail());
      Assert.assertTrue(u.getEnabled());
      u.setName("William Burke");
      Assert.assertEquals(target.request().put(Entity.json(u)).getStatus(), 204);
      u = target.request().get(User.class);
      Assert.assertEquals("William Burke", u.getName());
      Assert.assertEquals(target.request().delete().getStatus(), 204);
      response = target.request().get();
      Assert.assertEquals(404, response.getStatus());
      client.close();
   }
}
