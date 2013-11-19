package org.jboss.resteasy.client.jaxrs.internal;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Locale;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientInvocationBuilder implements Invocation.Builder
{
   protected ClientInvocation invocation;

   public ClientInvocationBuilder(ResteasyClient client, URI uri, ClientConfiguration configuration)
   {
      invocation = new ClientInvocation(client, uri, new ClientRequestHeaders(configuration), configuration);
   }

   public ClientInvocation getInvocation()
   {
      return invocation;
   }

   public ClientRequestHeaders getHeaders()
   {
      return invocation.headers;
   }

   @Override
   public Invocation.Builder accept(String... mediaTypes)
   {
      getHeaders().accept(mediaTypes);
      return this;
   }

   @Override
   public Invocation.Builder accept(MediaType... mediaTypes)
   {
      getHeaders().accept(mediaTypes);
      return this;
   }

   @Override
   public Invocation.Builder acceptLanguage(Locale... locales)
   {
      getHeaders().acceptLanguage(locales);
      return this;
   }

   @Override
   public Invocation.Builder acceptLanguage(String... locales)
   {
      getHeaders().acceptLanguage(locales);
      return this;
   }

   @Override
   public Invocation.Builder acceptEncoding(String... encodings)
   {
      getHeaders().acceptEncoding(encodings);
      return this;
   }

   @Override
   public Invocation.Builder cookie(Cookie cookie)
   {
      getHeaders().cookie(cookie);
      return this;
   }

   @Override
   public Invocation.Builder cookie(String name, String value)
   {
      getHeaders().cookie(new Cookie(name, value));
      return this;
   }

   @Override
   public Invocation.Builder cacheControl(CacheControl cacheControl)
   {
      getHeaders().cacheControl(cacheControl);
      return this;
   }

   @Override
   public Invocation.Builder header(String name, Object value)
   {
      getHeaders().header(name, value);
      return this;
   }

   @Override
   public Invocation.Builder headers(MultivaluedMap<String, Object> headers)
   {
      getHeaders().setHeaders(headers);
      return this;
   }

   @Override
   public Invocation build(String method)
   {
      invocation.setMethod(method);
      return invocation;
   }

   @Override
   public Invocation build(String method, Entity<?> entity)
   {
      invocation.setMethod(method);
      invocation.setEntity(entity);
      return invocation;
   }

   @Override
   public Invocation buildGet()
   {
      return build(HttpMethod.GET);
   }

   @Override
   public Invocation buildDelete()
   {
      return build(HttpMethod.DELETE);
   }

   @Override
   public Invocation buildPost(Entity<?> entity)
   {
      return build(HttpMethod.POST, entity);
   }

   @Override
   public Invocation buildPut(Entity<?> entity)
   {
      return build(HttpMethod.PUT, entity);
   }

   @Override
   public AsyncInvoker async()
   {
      return new AsynchronousInvoke(invocation);
   }

   @Override
   public Response get()
   {
      return buildGet().invoke();
   }

   @Override
   public <T> T get(Class<T> responseType)
   {
      return buildGet().invoke(responseType);
   }

   @Override
   public <T> T get(GenericType<T> responseType)
   {
      return buildGet().invoke(responseType);
   }

   @Override
   public Response put(Entity<?> entity)
   {
      return buildPut(entity).invoke();
   }

   @Override
   public <T> T put(Entity<?> entity, Class<T> responseType)
   {
      return buildPut(entity).invoke(responseType);
   }

   @Override
   public <T> T put(Entity<?> entity, GenericType<T> responseType)
   {
      return buildPut(entity).invoke(responseType);
   }

   @Override
   public Response post(Entity<?> entity)
   {
      return buildPost(entity).invoke();
   }

   @Override
   public <T> T post(Entity<?> entity, Class<T> responseType)
   {
      return buildPost(entity).invoke(responseType);
   }

   @Override
   public <T> T post(Entity<?> entity, GenericType<T> responseType)
   {
      return buildPost(entity).invoke(responseType);
   }

   @Override
   public Response delete()
   {
      return buildDelete().invoke();
   }

   @Override
   public <T> T delete(Class<T> responseType)
   {
      return buildDelete().invoke(responseType);
   }

   @Override
   public <T> T delete(GenericType<T> responseType)
   {
      return buildDelete().invoke(responseType);
   }

   @Override
   public Response head()
   {
      return build(HttpMethod.HEAD).invoke();
   }

   @Override
   public Response options()
   {
      return build(HttpMethod.OPTIONS).invoke();
   }

   @Override
   public <T> T options(Class<T> responseType)
   {
      return build(HttpMethod.OPTIONS).invoke(responseType);
  }

   @Override
   public <T> T options(GenericType<T> responseType)
   {
      return build(HttpMethod.OPTIONS).invoke(responseType);
   }

   @Override
   public Response trace()
   {
      return build("TRACE").invoke();
   }

   @Override
   public <T> T trace(Class<T> responseType)
   {
      return build("TRACE").invoke(responseType);
   }

   @Override
   public <T> T trace(GenericType<T> responseType)
   {
      return build("TRACE").invoke(responseType);
   }

   @Override
   public Response method(String name)
   {
      return build(name).invoke();
   }

   @Override
   public <T> T method(String name, Class<T> responseType)
   {
      return build(name).invoke(responseType);
   }

   @Override
   public <T> T method(String name, GenericType<T> responseType)
   {
      return build(name).invoke(responseType);
   }

   @Override
   public Response method(String name, Entity<?> entity)
   {
      return build(name, entity).invoke();
   }

   @Override
   public <T> T method(String name, Entity<?> entity, Class<T> responseType)
   {
      return build(name, entity).invoke(responseType);
   }

   @Override
   public <T> T method(String name, Entity<?> entity, GenericType<T> responseType)
   {
      return build(name, entity).invoke(responseType);
   }

   @Override
   public Invocation.Builder property(String name, Object value)
   {
      invocation.property(name, value);
      return this;
   }
}
