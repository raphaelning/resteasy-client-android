package org.jboss.resteasy.test.nextgen.security.doseta;

import org.jboss.resteasy.annotations.security.doseta.After;
import org.jboss.resteasy.annotations.security.doseta.Signed;
import org.jboss.resteasy.annotations.security.doseta.Verify;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.security.doseta.DKIMSignature;
import org.jboss.resteasy.security.doseta.DosetaKeyRepository;
import org.jboss.resteasy.security.doseta.KeyRepository;
import org.jboss.resteasy.security.doseta.UnauthorizedSignatureException;
import org.jboss.resteasy.security.doseta.Verification;
import org.jboss.resteasy.security.doseta.Verifier;
import org.jboss.resteasy.spi.MarshalledEntity;
import org.jboss.resteasy.test.BaseResourceTest;
import org.jboss.resteasy.test.TestPortProvider;
import org.jboss.resteasy.util.Base64;
import org.jboss.resteasy.util.ParameterParser;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.HashMap;

import static org.jboss.resteasy.test.TestPortProvider.generateBaseUrl;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SigningTest extends BaseResourceTest
{
   public static KeyPair keys;
   public static DosetaKeyRepository repository;
   public static PrivateKey badKey;
   public static ResteasyClient client;

   @Test
   public void testMe() throws Exception
   {
      URL url = Thread.currentThread().getContextClassLoader().getResource("dns/zones");
      System.out.println(url.getFile());
   }

   @BeforeClass
   public static void setup() throws Exception
   {
      Logger.setLoggerType(Logger.LoggerType.JUL);
      repository = new DosetaKeyRepository();
      repository.setKeyStorePath("test.jks");
      repository.setKeyStorePassword("password");
      repository.setUseDns(false);
      repository.start();

      PrivateKey privateKey = repository.getKeyStore().getPrivateKey("test._domainKey.samplezone.org");
      if (privateKey == null) throw new Exception("Private Key is null!!!");
      PublicKey publicKey = repository.getKeyStore().getPublicKey("test._domainKey.samplezone.org");
      keys = new KeyPair(publicKey, privateKey);

      KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
      badKey = keyPair.getPrivate();


      dispatcher.getDefaultContextObjects().put(KeyRepository.class, repository);
      addPerRequestResource(SignedResource.class);
      client = new ResteasyClientBuilder().build();
   }

   @AfterClass
   public static void afterIt() throws Exception
   {
      client.close();
   }

   @Path("/signed")
   public static interface SigningProxy
   {
      @GET
      @Verify
      @Produces("text/plain")
      @Path("bad-signature")
      public String bad();

      @GET
      @Verify
      @Produces("text/plain")
      public String hello();

      @POST
      @Consumes("text/plain")
      @Signed(selector = "test", domain = "samplezone.org")
      public void postSimple(String input);
   }


   @Path("/signed")
   public static class SignedResource
   {
      @DELETE
      @Path("request-only")
      public Response deleteRequestOnly(@Context HttpHeaders headers,
                                   @Context UriInfo uriInfo,
                                   @HeaderParam(DKIMSignature.DKIM_SIGNATURE)  DKIMSignature signature)
      {
         Assert.assertNotNull(signature);
         System.out.println("Signature: " + signature);
         Verification verification = new Verification(keys.getPublic());
         verification.setBodyHashRequired(false);
         verification.getRequiredAttributes().put("method", "GET");
         verification.getRequiredAttributes().put("uri", uriInfo.getPath());
         try
         {
            verification.verify(signature, headers.getRequestHeaders(), null, keys.getPublic());
         }
         catch (SignatureException e)
         {
            throw new RuntimeException(e);
         }
         String token = signature.getAttributes().get("token");
         signature = new DKIMSignature();
         signature.setDomain("samplezone.org");
         signature.setSelector("test");
         signature.setPrivateKey(keys.getPrivate());
         signature.setBodyHashRequired(false);
         signature.getAttributes().put("token", token);

         return Response.ok().header(DKIMSignature.DKIM_SIGNATURE, signature).build();

      }

      @GET
      @Produces("text/plain")
      @Path("bad-signature")
      public Response badSignature() throws Exception
      {
         DKIMSignature signature = new DKIMSignature();
         signature.setDomain("samplezone.org");
         signature.setSelector("test");
         signature.sign(new HashMap(), "hello world".getBytes(), keys.getPrivate());

         byte[] sig = {0x0f, 0x03};
         String encodedBadSig = Base64.encodeBytes(sig);

         ParameterParser parser = new ParameterParser();
         String s = signature.toString();
         String header = parser.setAttribute(s.toCharArray(), 0, s.length(), ';', "b", encodedBadSig);

         signature.setSignature(sig);
         return Response.ok("hello world").header(DKIMSignature.DKIM_SIGNATURE, header).build();
      }

      @GET
      @Produces("text/plain")
      @Path("bad-hash")
      public Response badHash() throws Exception
      {
         DKIMSignature signature = new DKIMSignature();
         signature.setDomain("samplezone.org");
         signature.setSelector("test");
         signature.sign(new HashMap(), "hello world".getBytes(), keys.getPrivate());

         return Response.ok("hello").header(DKIMSignature.DKIM_SIGNATURE, signature.toString()).build();
      }

      @GET
      @Produces("text/plain")
      @Path("manual")
      public Response getManual()
      {
         DKIMSignature signature = new DKIMSignature();
         signature.setSelector("test");
         signature.setDomain("samplezone.org");
         Response.ResponseBuilder builder = Response.ok("hello");
         builder.header(DKIMSignature.DKIM_SIGNATURE, signature);
         return builder.build();
      }

      @GET
      @Path("header")
      @Produces("text/plain")
      public Response withHeader()
      {
         Response.ResponseBuilder builder = Response.ok("hello world");
         builder.header("custom", "value");
         DKIMSignature signature = new DKIMSignature();
         signature.setSelector("test");
         signature.setDomain("samplezone.org");
         signature.addHeader("custom");
         builder.header(DKIMSignature.DKIM_SIGNATURE, signature);
         return builder.build();
      }

      @GET
      @Signed(selector = "test", domain = "samplezone.org")
      @Produces("text/plain")
      public String hello()
      {
         return "hello world";
      }

      @POST
      @Consumes("text/plain")
      @Verify
      public void post(@HeaderParam(DKIMSignature.DKIM_SIGNATURE) DKIMSignature signature, String input)
      {
         Assert.assertNotNull(signature);
         Assert.assertEquals(input, "hello world");
      }

      @POST
      @Consumes("text/plain")
      @Path("verify-manual")
      public void verifyManual(@HeaderParam(DKIMSignature.DKIM_SIGNATURE) DKIMSignature signature, @Context HttpHeaders headers, MarshalledEntity<String> input) throws Exception
      {
         Assert.assertNotNull(signature);
         Assert.assertEquals(input.getEntity(), "hello world");

         signature.verify(headers.getRequestHeaders(), input.getMarshalledBytes(), keys.getPublic());
      }

      @GET
      @Signed(selector = "test", domain = "samplezone.org",
              timestamped = true)
      @Produces("text/plain")
      @Path("stamped")
      public String getStamp()
      {
         return "hello world";
      }

      @GET
      @Signed(selector = "test", domain = "samplezone.org",
              expires = @After(seconds = 1))
      @Produces("text/plain")
      @Path("expires-short")
      public String getExpiresShort()
      {
         return "hello world";
      }

      @GET
      @Signed(selector = "test", domain = "samplezone.org",
              expires = @After(minutes = 1))
      @Produces("text/plain")
      @Path("expires-minute")
      public String getExpiresMinute()
      {
         return "hello world";
      }

      @GET
      @Signed(selector = "test", domain = "samplezone.org",
              expires = @After(hours = 1))
      @Produces("text/plain")
      @Path("expires-hour")
      public String getExpiresHour()
      {
         return "hello world";
      }

      @GET
      @Signed(selector = "test", domain = "samplezone.org",
              expires = @After(days = 1))
      @Produces("text/plain")
      @Path("expires-day")
      public String getExpiresDay()
      {
         return "hello world";
      }

      @GET
      @Signed(selector = "test", domain = "samplezone.org",
              expires = @After(months = 1))
      @Produces("text/plain")
      @Path("expires-month")
      public String getExpiresMonth()
      {
         return "hello world";
      }

      @GET
      @Signed(selector = "test", domain = "samplezone.org",
              expires = @After(years = 1))
      @Produces("text/plain")
      @Path("expires-year")
      public String getExpiresYear()
      {
         return "hello world";
      }
   }

   @Test
   public void testRequestOnly() throws Exception
   {
      //ResteasyClient client = new ResteasyClient();
      WebTarget target = client.target(TestPortProvider.generateURL("/signed/request-only"));
      DKIMSignature contentSignature = new DKIMSignature();
      contentSignature.setDomain("samplezone.org");
      contentSignature.setSelector("test");
      contentSignature.setPrivateKey(keys.getPrivate());
      contentSignature.setBodyHashRequired(false);
      contentSignature.setAttribute("method", "GET");
      contentSignature.setAttribute("uri", "/signed/request-only");
      contentSignature.setAttribute("token", "1122");
      Response response = target.request().header(DKIMSignature.DKIM_SIGNATURE, contentSignature).delete();

      Assert.assertEquals(200, response.getStatus());
      String signatureHeader = (String)response.getHeaderString(DKIMSignature.DKIM_SIGNATURE);
      contentSignature = new DKIMSignature(signatureHeader);
      Verification verification = new Verification(keys.getPublic());
      verification.setBodyHashRequired(false);
      verification.getRequiredAttributes().put("token", "1122");
      verification.verify(contentSignature, response.getStringHeaders(), null, keys.getPublic());
      response.close();





   }


   @Test
   public void testSigningManual() throws Exception
   {
      //ResteasyClient client = new ResteasyClient();
      WebTarget target = client.target(TestPortProvider.generateURL("/signed"));
      Response response = target.request().get();
      Assert.assertEquals(200, response.getStatus());
      MarshalledEntity<String> marshalledEntity = response.readEntity(new GenericType<MarshalledEntity<String>>()
      {
      });
      Assert.assertEquals("hello world", marshalledEntity.getEntity());
      String signatureHeader = response.getHeaderString(DKIMSignature.DKIM_SIGNATURE);
      System.out.println(DKIMSignature.DKIM_SIGNATURE + ":  " + signatureHeader);

      Assert.assertNotNull(signatureHeader);

      DKIMSignature contentSignature = new DKIMSignature(signatureHeader);
      contentSignature.verify(response.getStringHeaders(), marshalledEntity.getMarshalledBytes(), keys.getPublic());
      response.close();
   }

   @Test
   public void testBasicVerification() throws Exception
   {
      //ResteasyClient client = new ResteasyClient();
      WebTarget target = client.target(TestPortProvider.generateURL("/signed"));
      DKIMSignature contentSignature = new DKIMSignature();
      contentSignature.setDomain("samplezone.org");
      contentSignature.setSelector("test");
      contentSignature.setPrivateKey(keys.getPrivate());
      Response response = target.request().header(DKIMSignature.DKIM_SIGNATURE, contentSignature)
                                          .post(Entity.text("hello world"));
      Assert.assertEquals(204, response.getStatus());
      response.close();
   }

   @Test
   public void testManualVerification() throws Exception
   {
      //ResteasyClient client = new ResteasyClient();
      WebTarget target = client.target(TestPortProvider.generateURL("/signed/verify-manual"));
      DKIMSignature contentSignature = new DKIMSignature();
      contentSignature.setDomain("samplezone.org");
      contentSignature.setSelector("test");
      contentSignature.setAttribute("code", "hello");
      contentSignature.setPrivateKey(keys.getPrivate());
      Response response = target.request().header(DKIMSignature.DKIM_SIGNATURE, contentSignature)
              .post(Entity.text("hello world"));
      Assert.assertEquals(204, response.getStatus());
      response.close();


   }

   @Test
   public void testBasicVerificationRepository() throws Exception
   {
      //ResteasyClient client = new ResteasyClient();
      WebTarget target = client.target(TestPortProvider.generateURL("/signed"));
      target.property(KeyRepository.class.getName(), repository);
      DKIMSignature contentSignature = new DKIMSignature();
      contentSignature.setSelector("test");
      contentSignature.setDomain("samplezone.org");
      Response response = target.request().header(DKIMSignature.DKIM_SIGNATURE, contentSignature)
              .post(Entity.text("hello world"));
      Assert.assertEquals(204, response.getStatus());
      response.close();
   }

   @Test
   public void testBasicVerificationBadSignature() throws Exception
   {
      //ResteasyClient client = new ResteasyClient();
      WebTarget target = client.target(TestPortProvider.generateURL("/signed"));
      DKIMSignature contentSignature = new DKIMSignature();
      contentSignature.setSelector("test");
      contentSignature.setDomain("samplezone.org");
      contentSignature.setPrivateKey(badKey);
      Response response = target.request().header(DKIMSignature.DKIM_SIGNATURE, contentSignature)
              .post(Entity.text("hello world"));
      Assert.assertEquals(401, response.getStatus());
      response.close();
   }

   @Test
   public void testBasicVerificationNoSignature() throws Exception
   {
      //ResteasyClient client = new ResteasyClient();
      WebTarget target = client.target(TestPortProvider.generateURL("/signed"));
      Response response = target.request().post(Entity.text("hello world"));
      Assert.assertEquals(401, response.getStatus());
      response.close();
   }

   @Test
   public void testTimestampSignature() throws Exception
   {
      DKIMSignature signature = new DKIMSignature();
      signature.setTimestamp();
      signature.setSelector("test");
      signature.setDomain("samplezone.org");
      signature.sign(new HashMap(), "hello world".getBytes(), keys.getPrivate());
      String sig = signature.toString();
      System.out.println(DKIMSignature.DKIM_SIGNATURE + ": " + sig);
      signature = new DKIMSignature(sig);

   }

   @Test
   public void testTimestamp() throws Exception
   {
      Verifier verifier = new Verifier();
      Verification verification = verifier.addNew();
      verification.setRepository(repository);
      verification.setStaleCheck(true);
      verification.setStaleSeconds(100);

      //ResteasyClient client = new ResteasyClient();
      WebTarget target = client.target(TestPortProvider.generateURL("/signed/stamped"));
      Invocation.Builder request = target.request();
      request.property(Verifier.class.getName(), verifier);
      Response response = request.get();
      System.out.println(response.getHeaderString(DKIMSignature.DKIM_SIGNATURE));
      Assert.assertEquals(200, response.getStatus());
      try
      {
         String output = response.readEntity(String.class);
      }
      catch (Exception e)
      {
         throw e;
      }
      response.close();


   }

   @Test
   public void testStaleTimestamp() throws Exception
   {
      Verifier verifier = new Verifier();
      Verification verification = verifier.addNew();
      verification.setRepository(repository);
      verification.setStaleCheck(true);
      verification.setStaleSeconds(1);

      //ResteasyClient client = new ResteasyClient();
      WebTarget target = client.target(TestPortProvider.generateURL("/signed/stamped"));
      Invocation.Builder request = target.request();
      request.property(Verifier.class.getName(), verifier);
      Response response = request.get();
      System.out.println(response.getHeaderString(DKIMSignature.DKIM_SIGNATURE));
      Assert.assertEquals(200, response.getStatus());
      Thread.sleep(1500);
      try
      {
         String output = response.readEntity(String.class);
         Assert.fail();
      }
      catch (ProcessingException pe)
      {
         UnauthorizedSignatureException e = (UnauthorizedSignatureException)pe.getCause();
         System.out.println("here");
         Assert.assertEquals("Failed to verify signatures:\r\n Signature is stale", e.getMessage());
      }
      response.close();


   }

   @Test
   public void testExpiresHour() throws Exception
   {
      Verifier verifier = new Verifier();
      Verification verification = verifier.addNew();
      verification.setRepository(repository);

      //ResteasyClient client = new ResteasyClient();
      WebTarget target = client.target(TestPortProvider.generateURL("/signed/expires-hour"));
      Invocation.Builder request = target.request();
      request.property(Verifier.class.getName(), verifier);
      Response response = request.get();
      System.out.println(response.getHeaderString(DKIMSignature.DKIM_SIGNATURE));
      Assert.assertEquals(200, response.getStatus());
      String output = response.readEntity(String.class);
      response.close();
   }

   @Test
   public void testExpiresMinutes() throws Exception
   {
      Verifier verifier = new Verifier();
      Verification verification = verifier.addNew();
      verification.setRepository(repository);

      //ResteasyClient client = new ResteasyClient();
      WebTarget target = client.target(TestPortProvider.generateURL("/signed/expires-minute"));
      Invocation.Builder request = target.request();
      request.property(Verifier.class.getName(), verifier);
      Response response = request.get();
      System.out.println(response.getHeaderString(DKIMSignature.DKIM_SIGNATURE));
      Assert.assertEquals(200, response.getStatus());
      String output = response.readEntity(String.class);
      response.close();
   }

   @Test
   public void testExpiresDays() throws Exception
   {
      Verifier verifier = new Verifier();
      Verification verification = verifier.addNew();
      verification.setRepository(repository);

      //ResteasyClient client = new ResteasyClient();
      WebTarget target = client.target(TestPortProvider.generateURL("/signed/expires-day"));
      Invocation.Builder request = target.request();
      request.property(Verifier.class.getName(), verifier);
      Response response = request.get();
      System.out.println(response.getHeaderString(DKIMSignature.DKIM_SIGNATURE));
      Assert.assertEquals(200, response.getStatus());
      String output = response.readEntity(String.class);
      response.close();
   }

   @Test
   public void testExpiresMonths() throws Exception
   {
      Verifier verifier = new Verifier();
      Verification verification = verifier.addNew();
      verification.setRepository(repository);

      //ResteasyClient client = new ResteasyClient();
      WebTarget target = client.target(TestPortProvider.generateURL("/signed/expires-month"));
      Invocation.Builder request = target.request();
      request.property(Verifier.class.getName(), verifier);
      Response response = request.get();
      System.out.println(response.getHeaderString(DKIMSignature.DKIM_SIGNATURE));
      Assert.assertEquals(200, response.getStatus());
      String output = response.readEntity(String.class);
      response.close();
   }

   @Test
   public void testExpiresYears() throws Exception
   {
      Verifier verifier = new Verifier();
      Verification verification = verifier.addNew();
      verification.setRepository(repository);

      //ResteasyClient client = new ResteasyClient();
      WebTarget target = client.target(TestPortProvider.generateURL("/signed/expires-year"));
      Invocation.Builder request = target.request();
      request.property(Verifier.class.getName(), verifier);
      Response response = request.get();
      System.out.println(response.getHeaderString(DKIMSignature.DKIM_SIGNATURE));
      Assert.assertEquals(200, response.getStatus());
      String output = response.readEntity(String.class);
      response.close();
   }

   @Test
   public void testExpiresFail() throws Exception
   {
      Verifier verifier = new Verifier();
      Verification verification = verifier.addNew();
      verification.setRepository(repository);

      //ResteasyClient client = new ResteasyClient();
      WebTarget target = client.target(TestPortProvider.generateURL("/signed/expires-short"));
      Invocation.Builder request = target.request();
      request.property(Verifier.class.getName(), verifier);
      Response response = request.get();
      System.out.println(response.getHeaderString(DKIMSignature.DKIM_SIGNATURE));
      Assert.assertEquals(200, response.getStatus());
      Thread.sleep(1500);
      try
      {
         String output = response.readEntity(String.class);
         throw new Exception("unreachable!");
      }
      catch (ProcessingException pe)
      {
         UnauthorizedSignatureException e = (UnauthorizedSignatureException)pe.getCause();
         Assert.assertEquals("Failed to verify signatures:\r\n Signature expired", e.getMessage());
      }
      response.close();


   }

   @Test
   public void testManualFail() throws Exception
   {
      KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
      kpg.initialize(1024);
      KeyPair keyPair = kpg.genKeyPair();

      Verifier verifier = new Verifier();
      Verification verification = verifier.addNew();
      verification.setKey(keyPair.getPublic());

      //ResteasyClient client = new ResteasyClient();
      WebTarget target = client.target(TestPortProvider.generateURL("/signed/manual"));
      Invocation.Builder request = target.request();
      request.property(Verifier.class.getName(), verifier);
      Response response = request.get();
      System.out.println(response.getHeaderString(DKIMSignature.DKIM_SIGNATURE));
      Assert.assertNotNull(response.getHeaderString(DKIMSignature.DKIM_SIGNATURE));
      Assert.assertEquals(200, response.getStatus());
      try
      {
         String output = response.readEntity(String.class);
         throw new Exception("unreachable!");
      }
      catch (ProcessingException pe)
      {
         UnauthorizedSignatureException e = (UnauthorizedSignatureException)pe.getCause();
         Assert.assertEquals("Failed to verify signatures:\r\n Failed to verify signature.", e.getMessage());
      }
      response.close();


   }

   @Test
   public void testManual() throws Exception
   {
      Verifier verifier = new Verifier();
      Verification verification = verifier.addNew();
      verification.setRepository(repository);

      //ResteasyClient client = new ResteasyClient();
      WebTarget target = client.target(TestPortProvider.generateURL("/signed/manual"));
      Invocation.Builder request = target.request();
      request.property(Verifier.class.getName(), verifier);
      Response response = request.get();
      System.out.println(response.getHeaderString(DKIMSignature.DKIM_SIGNATURE));
      Assert.assertNotNull(response.getHeaderString(DKIMSignature.DKIM_SIGNATURE));
      Assert.assertEquals(200, response.getStatus());
      String output = response.readEntity(String.class);
      Assert.assertEquals("hello", output);
      response.close();
   }

   @Test
   public void testManualWithHeader() throws Exception
   {
      Verifier verifier = new Verifier();
      Verification verification = verifier.addNew();
      verification.setRepository(repository);

      //ResteasyClient client = new ResteasyClient();
      WebTarget target = client.target(TestPortProvider.generateURL("/signed/header"));
      Invocation.Builder request = target.request();
      request.property(Verifier.class.getName(), verifier);
      Response response = request.get();
      System.out.println(response.getHeaderString(DKIMSignature.DKIM_SIGNATURE));
      Assert.assertNotNull(response.getHeaderString(DKIMSignature.DKIM_SIGNATURE));
      Assert.assertEquals(200, response.getStatus());
      String output = response.readEntity(String.class);
      Assert.assertEquals("hello world", output);
      response.close();
   }


   @Test
   public void testBadSignature() throws Exception
   {
      //ResteasyClient client = new ResteasyClient();
      WebTarget target = client.target(TestPortProvider.generateURL("/signed/bad-signature"));
      Response response = target.request().get();
      Assert.assertEquals(200, response.getStatus());
      String signatureHeader = response.getHeaderString(DKIMSignature.DKIM_SIGNATURE);
      Assert.assertNotNull(signatureHeader);
      System.out.println(DKIMSignature.DKIM_SIGNATURE + ":  " + signatureHeader);

      DKIMSignature contentSignature = new DKIMSignature(signatureHeader);

      MarshalledEntity<String> entity =  response.readEntity(new GenericType<MarshalledEntity<String>>(){});
      boolean failedVerification = false;

      try
      {
         contentSignature.verify(response.getStringHeaders(), entity.getMarshalledBytes(), keys.getPublic());
      }
      catch (SignatureException e)
      {
         failedVerification = true;
      }
      Assert.assertTrue(failedVerification);
      response.close();
   }

   @Test
   public void testBadHash() throws Exception
   {
      //ResteasyClient client = new ResteasyClient();
      WebTarget target = client.target(TestPortProvider.generateURL("/signed/bad-hash"));
      Response response = target.request().get();
      Assert.assertEquals(200, response.getStatus());
      String signatureHeader = response.getHeaderString(DKIMSignature.DKIM_SIGNATURE);
      Assert.assertNotNull(signatureHeader);
      System.out.println(DKIMSignature.DKIM_SIGNATURE + ":  " + signatureHeader);

      DKIMSignature contentSignature = new DKIMSignature(signatureHeader);

      MarshalledEntity<String> entity =  response.readEntity(new GenericType<MarshalledEntity<String>>(){});

      boolean failedVerification = false;
      try
      {
         contentSignature.verify(response.getStringHeaders(), entity.getMarshalledBytes(), keys.getPublic());
      }
      catch (SignatureException e)
      {
         failedVerification = true;
      }
      Assert.assertTrue(failedVerification);
      response.close();
   }

   @Test
   public void testProxy() throws Exception
   {
      //ResteasyClient client = new ResteasyClient();
      ResteasyWebTarget target = client.target(generateBaseUrl());
      target.property(KeyRepository.class.getName(), repository);
      SigningProxy proxy = target.proxy(SigningProxy.class);
      String output = proxy.hello();
      proxy.postSimple("hello world");

   }


   @Test
   public void testBadSignatureProxy() throws Exception
   {
      //ResteasyClient client = new ResteasyClient();
      ResteasyWebTarget target = client.target(generateBaseUrl());
      target.property(KeyRepository.class.getName(), repository);
      SigningProxy proxy = target.proxy(SigningProxy.class);
      try
      {
         String output = proxy.bad();
         throw new Exception("UNREACHABLE");
      }
      catch (ResponseProcessingException e)
      {
         System.out.println("*** cause ***: " + e.getCause().getClass().getName());
         //Assert.assertTrue(e.getCause() instanceof UnauthorizedSignatureException);
      }
   }

}
