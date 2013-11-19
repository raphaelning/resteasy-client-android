package org.jboss.resteasy.plugins.guice;

import static org.jboss.resteasy.test.TestPortProvider.generateBaseUrl;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Module;

import org.jboss.resteasy.client.ProxyFactory;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.test.EmbeddedContainer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class GuiceContextTest
{
   private static Dispatcher dispatcher;

   @BeforeClass
   public static void beforeClass() throws Exception
   {
      dispatcher = EmbeddedContainer.start().getDispatcher();
   }

   @AfterClass
   public static void afterClass() throws Exception
   {
      EmbeddedContainer.stop();
   }

   @Test
   public void testMethodInjection()
   {
      final Module module = new Module()
      {
         @Override
         public void configure(final Binder binder)
         {
            binder.bind(MethodTestResource.class);
         }
      };
      final ModuleProcessor processor = new ModuleProcessor(dispatcher.getRegistry(), dispatcher.getProviderFactory());
      processor.processInjector(Guice.createInjector(module));
      final TestResource resource = ProxyFactory.create(TestResource.class, generateBaseUrl());
      Assert.assertEquals("method", resource.getName());
      dispatcher.getRegistry().removeRegistrations(MethodTestResource.class);
   }

   @Test
   public void testFieldInjection()
   {
      final Module module = new Module()
      {
         @Override
         public void configure(final Binder binder)
         {
            binder.bind(FieldTestResource.class);
         }
      };
      final ModuleProcessor processor = new ModuleProcessor(dispatcher.getRegistry(), dispatcher.getProviderFactory());
      processor.processInjector(Guice.createInjector(module));
      final TestResource resource = ProxyFactory.create(TestResource.class, generateBaseUrl());
      Assert.assertEquals("field", resource.getName());
      dispatcher.getRegistry().removeRegistrations(FieldTestResource.class);
   }

   //@Test // not (yet) supprted
   public void testConstructorInjection()
   {
      final Module module = new Module()
      {
         @Override
         public void configure(final Binder binder)
         {
            binder.bind(ConstructorTestResource.class);
         }
      };
      final ModuleProcessor processor = new ModuleProcessor(dispatcher.getRegistry(), dispatcher.getProviderFactory());
      processor.processInjector(Guice.createInjector(module));
      final TestResource resource = ProxyFactory.create(TestResource.class, generateBaseUrl());
      Assert.assertEquals("constructor", resource.getName());
      dispatcher.getRegistry().removeRegistrations(ConstructorTestResource.class);
   }

   @Path("test")
   public interface TestResource
   {
      @GET
      public String getName();
   }

   @Path("test")
   public static class MethodTestResource
   {
      @GET
      public String getName(final @Context UriInfo uriInfo)
      {
         Assert.assertNotNull(uriInfo);
         return "method";
      }
   }

   @Path("test")
   public static class FieldTestResource
   {
      private
      @Context
      UriInfo uriInfo;

      @GET
      public String getName()
      {
         Assert.assertNotNull(uriInfo);
         return "field";
      }
   }

   @Path("test")
   public static class ConstructorTestResource
   {
      private final UriInfo uriInfo;

      public ConstructorTestResource(@Context final UriInfo uriInfo)
      {
         this.uriInfo = uriInfo;
      }

      @GET
      public String getName()
      {
         Assert.assertNotNull(uriInfo);
         return "field";
      }
   }
}