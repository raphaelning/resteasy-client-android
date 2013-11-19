package org.jboss.resteasy.core;

import org.jboss.resteasy.client.core.ClientErrorInterceptor;
import org.jboss.resteasy.client.exception.mapper.ClientExceptionMapper;
import org.jboss.resteasy.core.interception.ClientResponseFilterRegistry;
import org.jboss.resteasy.core.interception.ContainerRequestFilterRegistry;
import org.jboss.resteasy.core.interception.ContainerResponseFilterRegistry;
import org.jboss.resteasy.core.interception.InterceptorRegistry;
import org.jboss.resteasy.core.interception.JaxrsInterceptorRegistry;
import org.jboss.resteasy.core.interception.ReaderInterceptorRegistry;
import org.jboss.resteasy.core.interception.WriterInterceptorRegistry;
import org.jboss.resteasy.spi.ConstructorInjector;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.InjectorFactory;
import org.jboss.resteasy.spi.ProviderFactoryDelegate;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.StringConverter;
import org.jboss.resteasy.spi.StringParameterUnmarshaller;
import org.jboss.resteasy.spi.interception.ClientExecutionInterceptor;
import org.jboss.resteasy.util.ThreadLocalStack;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.RuntimeDelegate;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Allow applications to push/pop provider factories onto the stack
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ThreadLocalResteasyProviderFactory extends ResteasyProviderFactory implements ProviderFactoryDelegate
{
   private static final ThreadLocalStack<ResteasyProviderFactory> delegate = new ThreadLocalStack<ResteasyProviderFactory>();

   private ResteasyProviderFactory defaultFactory;


   public ThreadLocalResteasyProviderFactory(ResteasyProviderFactory defaultFactory)
   {
      this.defaultFactory = defaultFactory;
   }

   public ResteasyProviderFactory getDelegate()
   {
      ResteasyProviderFactory factory = delegate.get();
      if (factory == null) return defaultFactory;
      return factory;
   }

   @Override
   protected void initialize()
   {

   }

   @Override
   public MediaType getConcreteMediaTypeFromMessageBodyWriters(Class type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return super.getConcreteMediaTypeFromMessageBodyWriters(type, genericType, annotations, mediaType);
   }

   @Override
   public HeaderDelegate getHeaderDelegate(Class<?> aClass)
   {
      return getDelegate().getHeaderDelegate(aClass);
   }

   @Override
   public <T> T injectedInstance(Class<? extends T> clazz, HttpRequest request, HttpResponse response)
   {
      return getDelegate().injectedInstance(clazz, request, response);
   }

   @Override
   public void injectProperties(Object obj, HttpRequest request, HttpResponse response)
   {
      getDelegate().injectProperties(obj, request, response);
   }

   public static void push(ResteasyProviderFactory factory)
   {
      delegate.push(factory);
   }

   public static void pop()
   {
      delegate.pop();
   }

   @Override
   public ContainerResponseFilterRegistry getContainerResponseFilterRegistry()
   {
      return getDelegate().getContainerResponseFilterRegistry();
   }

   @Override
   public ReaderInterceptorRegistry getServerReaderInterceptorRegistry()
   {
      return getDelegate().getServerReaderInterceptorRegistry();
   }

   @Override
   public Variant.VariantListBuilder createVariantListBuilder()
   {
      return getDelegate().createVariantListBuilder();
   }

   @Override
   public List<ContextResolver> getContextResolvers(Class<?> clazz, MediaType type)
   {
      return getDelegate().getContextResolvers(clazz, type);
   }

   @Override
   public boolean isBuiltinsRegistered()
   {
      return getDelegate().isBuiltinsRegistered();
   }

   @Override
   public <T extends Throwable> ClientExceptionMapper<T> getClientExceptionMapper(Class<T> type)
   {
      return getDelegate().getClientExceptionMapper(type);
   }

   @Override
   public Set<Class<?>> getFeatureClasses()
   {
      return getDelegate().getFeatureClasses();
   }

   @Override
   public void setBuiltinsRegistered(boolean builtinsRegistered)
   {
      getDelegate().setBuiltinsRegistered(builtinsRegistered);
   }

   @Override
   public ResteasyProviderFactory register(Class<?> providerClass)
   {
      return getDelegate().register(providerClass);
   }

   @Override
   public Set<DynamicFeature> getClientDynamicFeatures()
   {
      return getDelegate().getClientDynamicFeatures();
   }

   @Override
   public void addClientExceptionMapper(Class<? extends ClientExceptionMapper<?>> providerClass)
   {
      getDelegate().addClientExceptionMapper(providerClass);
   }

   @Override
   public ResteasyProviderFactory register(Class<?> componentClass, Map<Class<?>, Integer> contracts)
   {
      return getDelegate().register(componentClass, contracts);
   }

   @Override
   public Collection<Feature> getEnabledFeatures()
   {
      return getDelegate().getEnabledFeatures();
   }

   @Override
   public Response.ResponseBuilder createResponseBuilder()
   {
      return getDelegate().createResponseBuilder();
   }

   @Override
   public void registerProviderInstance(Object provider)
   {
      getDelegate().registerProviderInstance(provider);
   }

   @Override
   public void addClientExceptionMapper(ClientExceptionMapper<?> provider, Type exceptionType)
   {
      getDelegate().addClientExceptionMapper(provider, exceptionType);
   }

   @Override
   public StringConverter getStringConverter(Class<?> clazz)
   {
      return getDelegate().getStringConverter(clazz);
   }

   @Override
   public <T> StringParameterUnmarshaller<T> createStringParameterUnmarshaller(Class<T> clazz)
   {
      return getDelegate().createStringParameterUnmarshaller(clazz);
   }

   @Override
   public Set<Object> getFeatureInstances()
   {
      return getDelegate().getFeatureInstances();
   }

   @Override
   public void addClientExceptionMapper(ClientExceptionMapper<?> provider)
   {
      getDelegate().addClientExceptionMapper(provider);
   }

   @Override
   public void setInjectorFactory(InjectorFactory injectorFactory)
   {
      getDelegate().setInjectorFactory(injectorFactory);
   }

   @Override
   public Set<Object> getInstances()
   {
      return getDelegate().getInstances();
   }

   @Override
   public boolean isRegistered(Object component)
   {
      return getDelegate().isRegistered(component);
   }

   @Override
   public ResteasyProviderFactory register(Class<?> componentClass, int priority)
   {
      return getDelegate().register(componentClass, priority);
   }

   @Override
   public <T> ContextResolver<T> getContextResolver(Class<T> contextType, MediaType mediaType)
   {
      return getDelegate().getContextResolver(contextType, mediaType);
   }

   @Override
   public InterceptorRegistry<ClientExecutionInterceptor> getClientExecutionInterceptorRegistry()
   {
      return getDelegate().getClientExecutionInterceptorRegistry();
   }

   @Override
   public <T> MessageBodyReader<T> getMessageBodyReader(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return getDelegate().getMessageBodyReader(type, genericType, annotations, mediaType);
   }

   @Override
   public void addClientErrorInterceptor(ClientErrorInterceptor handler)
   {
      getDelegate().addClientErrorInterceptor(handler);
   }

   @Override
   public void registerProvider(Class provider, Integer priorityOverride, boolean isBuiltin, Map<Class<?>, Integer> contracts)
   {
      getDelegate().registerProvider(provider, priorityOverride, isBuiltin, contracts);
   }

   @Override
   public Map<Class<?>, Map<Class<?>, Integer>> getClassContracts()
   {
      return getDelegate().getClassContracts();
   }

   @Override
   public ContainerRequestFilterRegistry getContainerRequestFilterRegistry()
   {
      return getDelegate().getContainerRequestFilterRegistry();
   }

   @Override
   public ResteasyProviderFactory register(Object component, Map<Class<?>, Integer> contracts)
   {
      return getDelegate().register(component, contracts);
   }

   @Override
   public boolean isRegisterBuiltins()
   {
      return getDelegate().isRegisterBuiltins();
   }

   @Override
   public ReaderInterceptorRegistry getClientReaderInterceptorRegistry()
   {
      return getDelegate().getClientReaderInterceptorRegistry();
   }

   @Override
   public void setRegisterBuiltins(boolean registerBuiltins)
   {
      getDelegate().setRegisterBuiltins(registerBuiltins);
   }

   @Override
   public ResteasyProviderFactory register(Object component, int priority)
   {
      return getDelegate().register(component, priority);
   }

   @Override
   public void registerProvider(Class provider, boolean isBuiltin)
   {
      getDelegate().registerProvider(provider, isBuiltin);
   }

   @Override
   public Collection<String> getPropertyNames()
   {
      return getDelegate().getPropertyNames();
   }

   @Override
   public void addClientExceptionMapper(ClientExceptionMapper<?> provider, Class<?> providerClass)
   {
      getDelegate().addClientExceptionMapper(provider, providerClass);
   }

   @Override
   public void insertInterceptorPrecedenceAfter(String after, String newPrecedence)
   {
      getDelegate().insertInterceptorPrecedenceAfter(after, newPrecedence);
   }

   @Override
   public ResteasyProviderFactory register(Object provider)
   {
      return getDelegate().register(provider);
   }

   @Override
   public <T> ConstructorInjector createConstructorInjector(Class<? extends T> clazz)
   {
      return getDelegate().createConstructorInjector(clazz);
   }

   @Override
   public <T> T createProviderInstance(Class<? extends T> clazz)
   {
      return getDelegate().createProviderInstance(clazz);
   }

   @Override
   public boolean isRegistered(Class<?> componentClass)
   {
      return getDelegate().isRegistered(componentClass);
   }

   @Override
   public void insertInterceptorPrecedenceBefore(String before, String newPrecedence)
   {
      getDelegate().insertInterceptorPrecedenceBefore(before, newPrecedence);
   }

   @Override
   public <T> T createEndpoint(Application applicationConfig, Class<T> endpointType) throws IllegalArgumentException, UnsupportedOperationException
   {
      return getDelegate().createEndpoint(applicationConfig, endpointType);
   }

   @Override
   public Map<String, Object> getMutableProperties()
   {
      return getDelegate().getMutableProperties();
   }

   @Override
   public Set<DynamicFeature> getServerDynamicFeatures()
   {
      return getDelegate().getServerDynamicFeatures();
   }

   @Override
   public boolean isEnabled(Feature feature)
   {
      return getDelegate().isEnabled(feature);
   }

   @Override
   public Object getProperty(String name)
   {
      return getDelegate().getProperty(name);
   }

   @Override
   public WriterInterceptorRegistry getServerWriterInterceptorRegistry()
   {
      return getDelegate().getServerWriterInterceptorRegistry();
   }

   @Override
   public ResteasyProviderFactory setProperties(Map<String, ?> properties)
   {
      return getDelegate().setProperties(properties);
   }

   @Override
   public List<ClientErrorInterceptor> getClientErrorInterceptors()
   {
      return getDelegate().getClientErrorInterceptors();
   }

   @Override
   public void injectProperties(Class declaring, Object obj)
   {
      getDelegate().injectProperties(declaring, obj);
   }

   @Override
   public UriBuilder createUriBuilder()
   {
      return getDelegate().createUriBuilder();
   }

   @Override
   public ResteasyProviderFactory register(Class<?> componentClass, Class<?>... contracts)
   {
      return getDelegate().register(componentClass, contracts);
   }

   @Override
   public <T> T injectedInstance(Class<? extends T> clazz)
   {
      return getDelegate().injectedInstance(clazz);
   }

   @Override
   public void appendInterceptorPrecedence(String precedence)
   {
      getDelegate().appendInterceptorPrecedence(precedence);
   }

   @Override
   public ResteasyProviderFactory getParent()
   {
      return getDelegate().getParent();
   }

   @Override
   public RuntimeType getRuntimeType()
   {
      return getDelegate().getRuntimeType();
   }

   @Override
   public void injectProperties(Object obj)
   {
      getDelegate().injectProperties(obj);
   }

   @Override
   public ResteasyProviderFactory property(String name, Object value)
   {
      return getDelegate().property(name, value);
   }

   @Override
   public WriterInterceptorRegistry getClientWriterInterceptorRegistry()
   {
      return getDelegate().getClientWriterInterceptorRegistry();
   }

   @Override
   public InjectorFactory getInjectorFactory()
   {
      return getDelegate().getInjectorFactory();
   }

   @Override
   public Map<Class<?>, Integer> getContracts(Class<?> componentClass)
   {
      return getDelegate().getContracts(componentClass);
   }

   @Override
   public ParamConverter getParamConverter(Class clazz, Type genericType, Annotation[] annotations)
   {
      return getDelegate().getParamConverter(clazz, genericType, annotations);
   }

   @Override
   public ClientResponseFilterRegistry getClientResponseFilters()
   {
      return getDelegate().getClientResponseFilters();
   }

   @Override
   public ResteasyProviderFactory register(Object component, Class<?>... contracts)
   {
      return getDelegate().register(component, contracts);
   }

   @Override
   public Set<Class<?>> getClasses()
   {
      return getDelegate().getClasses();
   }

   @Override
   public boolean isEnabled(Class<? extends Feature> featureClass)
   {
      return getDelegate().isEnabled(featureClass);
   }

   @Override
   public void registerProvider(Class provider)
   {
      getDelegate().registerProvider(provider);
   }

   @Override
   public void addHeaderDelegate(Class clazz, HeaderDelegate header)
   {
      getDelegate().addHeaderDelegate(clazz, header);
   }

   @Override
   public void registerProviderInstance(Object provider, Map<Class<?>, Integer> contracts, Integer defaultPriority, boolean builtIn)
   {
      getDelegate().registerProviderInstance(provider, contracts, defaultPriority, builtIn);
   }

   @Override
   public void addStringParameterUnmarshaller(Class<? extends StringParameterUnmarshaller> provider)
   {
      getDelegate().addStringParameterUnmarshaller(provider);
   }

   @Override
   public Set<Class<?>> getProviderClasses()
   {
      return getDelegate().getProviderClasses();
   }

   @Override
   public String toString(Object object, Class clazz, Type genericType, Annotation[] annotations)
   {
      return getDelegate().toString(object, clazz, genericType, annotations);
   }

   @Override
   public JaxrsInterceptorRegistry<ClientRequestFilter> getClientRequestFilters()
   {
      return getDelegate().getClientRequestFilters();
   }

   @Override
   public Map<String, Object> getProperties()
   {
      return getDelegate().getProperties();
   }

   @Override
   public String toHeaderString(Object object)
   {
      return getDelegate().toHeaderString(object);
   }

   @Override
   public Link.Builder createLinkBuilder()
   {
      return getDelegate().createLinkBuilder();
   }

   @Override
   public Set<Object> getProviderInstances()
   {
      return getDelegate().getProviderInstances();
   }

   @Override
   public Configuration getConfiguration()
   {
      return getDelegate().getConfiguration();
   }

   @Override
   public <T> MessageBodyWriter<T> getMessageBodyWriter(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return getDelegate().getMessageBodyWriter(type, genericType, annotations, mediaType);
   }

   @Override
   public <T extends Throwable> ExceptionMapper<T> getExceptionMapper(Class<T> type)
   {
      return getDelegate().getExceptionMapper(type);
   }

   @Override
   public Map<Class<?>, ExceptionMapper> getExceptionMappers()
   {
      return getDelegate().getExceptionMappers();
   }

   @Override
   public <T> HeaderDelegate<T> createHeaderDelegate(Class<T> tClass)
   {
      return getDelegate().createHeaderDelegate(tClass);
   }

   @Override
   public <T> MessageBodyWriter<T> getClientMessageBodyWriter(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return getDelegate().getClientMessageBodyWriter(type, genericType, annotations, mediaType);
   }

   @Override
   public <T> MessageBodyReader<T> getClientMessageBodyReader(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return getDelegate().getClientMessageBodyReader(type, genericType, annotations, mediaType);
   }

   @Override
   public <T> MessageBodyReader<T> getServerMessageBodyReader(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return getDelegate().getServerMessageBodyReader(type, genericType, annotations, mediaType);
   }

   @Override
   public <T> MessageBodyWriter<T> getServerMessageBodyWriter(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return getDelegate().getServerMessageBodyWriter(type, genericType, annotations, mediaType);
   }
}
