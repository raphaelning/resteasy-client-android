package org.jboss.resteasy.spi.metadata;

import org.jboss.resteasy.annotations.Body;
import org.jboss.resteasy.annotations.Form;
import org.jboss.resteasy.annotations.Suspend;
import org.jboss.resteasy.specimpl.ResteasyUriBuilder;
import org.jboss.resteasy.util.IsHttpMethod;
import org.jboss.resteasy.util.MethodHashing;
import org.jboss.resteasy.util.PickConstructor;
import org.jboss.resteasy.util.Types;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.jboss.resteasy.util.FindAnnotation.findAnnotation;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResourceBuilder
{
   public static class ResourceClassBuilder
   {
      final ResourceClass resourceClass;
      List<FieldParameter> fields = new ArrayList<FieldParameter>();
      List<SetterParameter> setters = new ArrayList<SetterParameter>();
      List<ResourceMethod> resourceMethods = new ArrayList<ResourceMethod>();
      List<ResourceLocator> resourceLocators = new ArrayList<ResourceLocator>();

      public ResourceClassBuilder(Class<?> root, String path)
      {
         this.resourceClass = new ResourceClass(root, path);
      }

      public ResourceMethodBuilder method(Method method)
      {
         return new ResourceMethodBuilder(this, method, method);
      }

      public ResourceMethodBuilder method(Method method, Method annotatedMethod)
      {
         return new ResourceMethodBuilder(this, method, annotatedMethod);
      }

      public ResourceLocatorBuilder locator(Method method)
      {
         return new ResourceLocatorBuilder(this, method, method);
      }

      public ResourceLocatorBuilder locator(Method method, Method annotatedMethod)
      {
         return new ResourceLocatorBuilder(this, method, annotatedMethod);
      }

      public FieldParameterBuilder field(Field field)
      {
         FieldParameter param = new FieldParameter(resourceClass, field);
         return new FieldParameterBuilder(this, param);
      }

      public SetterParameterBuilder setter(Method method)
      {
         SetterParameter param = new SetterParameter(resourceClass, method, method);
         return new SetterParameterBuilder(this, param);
      }

      public ResourceConstructorBuilder constructor(Constructor constructor)
      {
         return new ResourceConstructorBuilder(this, constructor);
      }

      public ResourceClass buildClass()
      {
         resourceClass.fields = fields.toArray(new FieldParameter[fields.size()]);
         resourceClass.setters = setters.toArray(new SetterParameter[setters.size()]);
         resourceClass.resourceMethods = resourceMethods.toArray(new ResourceMethod[resourceMethods.size()]);
         resourceClass.resourceLocators = resourceLocators.toArray(new ResourceLocator[resourceLocators.size()]);

         return resourceClass;
      }
   }

   public static class ParameterBuilder<T extends ParameterBuilder>
   {
      final Parameter parameter;

      public ParameterBuilder(Parameter parameter)
      {
         this.parameter = parameter;
      }

      public T type(Class<?> type)
      {
         parameter.type = type;
         return (T)this;
      }

      public T genericType(Type type)
      {
         parameter.genericType = type;
         return (T)this;
      }

      public T type(GenericType type)
      {
         parameter.type = type.getRawType();
         parameter.genericType = type.getType();
         return (T)this;
      }

      public T beanParam()
      {
         parameter.paramType = Parameter.ParamType.BEAN_PARAM;
         return (T)this;
      }

      public T context()
      {
         parameter.paramType = Parameter.ParamType.CONTEXT;
         return (T)this;
      }

      public T messageBody()
      {
         parameter.paramType = Parameter.ParamType.MESSAGE_BODY;
         return (T)this;
      }

      public T encoded()
      {
         parameter.encoded = true;
         return (T)this;
      }

      public T defaultValue(String defaultValue)
      {
         parameter.defaultValue = defaultValue;
         return (T)this;
      }

      public T cookieParam(String name)
      {
         parameter.paramType = Parameter.ParamType.COOKIE_PARAM;
         parameter.paramName = name;
         return (T)this;
      }

      public T formParam(String name)
      {
         parameter.paramType = Parameter.ParamType.FORM_PARAM;
         parameter.paramName = name;
         return (T)this;
      }

      /**
       * Resteasy @Form specific injection parameter
       *
       * @param prefix
       * @return
       */
      public T form(String prefix)
      {
         parameter.paramType = Parameter.ParamType.FORM;
         parameter.paramName = prefix;
         return (T)this;
      }

      /**
       * Resteasy @Form specific injection parameter
       *
       * @return
       */
      public T form()
      {
         parameter.paramType = Parameter.ParamType.FORM;
         parameter.paramName = "";
         return (T)this;
      }


      public T headerParam(String name)
      {
         parameter.paramType = Parameter.ParamType.HEADER_PARAM;
         parameter.paramName = name;
         return (T)this;
      }

      public T matrixParam(String name)
      {
         parameter.paramType = Parameter.ParamType.MATRIX_PARAM;
         parameter.paramName = name;
         return (T)this;
      }

      public T pathParam(String name)
      {
         parameter.paramType = Parameter.ParamType.PATH_PARAM;
         parameter.paramName = name;
         return (T)this;
      }

      public T queryParam(String name)
      {
         parameter.paramType = Parameter.ParamType.QUERY_PARAM;
         parameter.paramName = name;
         return (T)this;
      }

      public T fromAnnotations()
      {
         Annotation[] annotations = parameter.getAnnotations();
         AccessibleObject injectTarget = parameter.getAccessibleObject();
         Class<?> type = parameter.getResourceClass().getClazz();

         parameter.encoded = findAnnotation(annotations, Encoded.class) != null || injectTarget.isAnnotationPresent(Encoded.class) || type.isAnnotationPresent(Encoded.class);
         DefaultValue defaultValue = findAnnotation(annotations, DefaultValue.class);
         if (defaultValue != null) parameter.defaultValue = defaultValue.value();

         QueryParam query;
         HeaderParam header;
         MatrixParam matrix;
         PathParam uriParam;
         CookieParam cookie;
         FormParam formParam;
         Form form;
         Suspend suspend;
         Suspended suspended;


         if ((query = findAnnotation(annotations, QueryParam.class)) != null)
         {
            parameter.paramType = Parameter.ParamType.QUERY_PARAM;
            parameter.paramName = query.value();
         }
         else if ((header = findAnnotation(annotations, HeaderParam.class)) != null)
         {
            parameter.paramType = Parameter.ParamType.HEADER_PARAM;
            parameter.paramName = header.value();
         }
         else if ((formParam = findAnnotation(annotations, FormParam.class)) != null)
         {
            parameter.paramType = Parameter.ParamType.FORM_PARAM;
            parameter.paramName = formParam.value();
         }
         else if ((cookie = findAnnotation(annotations, CookieParam.class)) != null)
         {
            parameter.paramType = Parameter.ParamType.COOKIE_PARAM;
            parameter.paramName = cookie.value();
         }
         else if ((uriParam = findAnnotation(annotations, PathParam.class)) != null)
         {
            parameter.paramType = Parameter.ParamType.PATH_PARAM;
            parameter.paramName = uriParam.value();
         }
         else if ((form = findAnnotation(annotations, Form.class)) != null)
         {
            parameter.paramType = Parameter.ParamType.FORM;
            parameter.paramName = form.prefix();
         }
         else if (findAnnotation(annotations, BeanParam.class) != null)
         {
            parameter.paramType = Parameter.ParamType.BEAN_PARAM;
         }
         else if ((matrix = findAnnotation(annotations, MatrixParam.class)) != null)
         {
            parameter.paramType = Parameter.ParamType.MATRIX_PARAM;
            parameter.paramName = matrix.value();
         }
         else if ((suspend = findAnnotation(annotations, Suspend.class)) != null)
         {
            parameter.paramType = Parameter.ParamType.SUSPEND;
            parameter.suspendTimeout = suspend.value();
         }
         else if (findAnnotation(annotations, Context.class) != null)
         {
            parameter.paramType = Parameter.ParamType.CONTEXT;
         }
         else if ((suspended = findAnnotation(annotations, Suspended.class)) != null)
         {
            parameter.paramType = Parameter.ParamType.SUSPENDED;
         }
         else if (javax.ws.rs.container.AsyncResponse.class.isAssignableFrom(type))
         {
            parameter.paramType = Parameter.ParamType.SUSPENDED;
         }
         else if (findAnnotation(annotations, Body.class) != null)
         {
            parameter.paramType = Parameter.ParamType.MESSAGE_BODY;
         }
         else
         {
            parameter.paramType = Parameter.ParamType.UNKNOWN;
         }
         return (T)this;
      }
   }

   public static class ConstructorParameterBuilder extends ParameterBuilder<ConstructorParameterBuilder>
   {
      final ResourceConstructorBuilder constructor;
      final ConstructorParameter param;

      public ConstructorParameterBuilder(ResourceConstructorBuilder builder, ConstructorParameter param)
      {
         super(param);
         this.constructor = builder;
         this.param = param;
      }

      public ConstructorParameterBuilder param(int i)
      {
         return constructor.param(i);
      }

      public ResourceClassBuilder buildConstructor()
      {
         return constructor.buildConstructor();
      }

   }


   public static class LocatorMethodParameterBuilder<T extends LocatorMethodParameterBuilder> extends ParameterBuilder<T>
   {
      final ResourceLocatorBuilder locator;
      final MethodParameter param;

      public LocatorMethodParameterBuilder(ResourceLocatorBuilder method, MethodParameter param)
      {
         super(param);
         this.locator = method;
         this.param = param;
      }

      public T param(int i)
      {
         return (T)locator.param(i);
      }

      public ResourceClassBuilder buildMethod()
      {
         return locator.buildMethod();
      }

   }

   public static class ResourceMethodParameterBuilder extends LocatorMethodParameterBuilder<ResourceMethodParameterBuilder>
   {
      final ResourceMethodBuilder method;

      public ResourceMethodParameterBuilder(ResourceMethodBuilder method, MethodParameter param)
      {
         super(method, param);
         this.method = method;
      }

      public ResourceMethodParameterBuilder suspended()
      {
         method.method.asynchronous = true;
         parameter.paramType = Parameter.ParamType.SUSPENDED;
         return this;
      }

      public ResourceMethodParameterBuilder suspend(long timeout)
      {
         method.method.asynchronous = true;
         parameter.paramType = Parameter.ParamType.SUSPEND;
         parameter.suspendTimeout = timeout;
         return this;
      }

      @Override
      public ResourceMethodParameterBuilder fromAnnotations()
      {
         super.fromAnnotations();
         if (param.paramType == Parameter.ParamType.SUSPEND || param.paramType == Parameter.ParamType.SUSPENDED)
         {
            method.method.asynchronous = true;
         }
         else if (param.paramType == Parameter.ParamType.UNKNOWN)
         {
            param.paramType = Parameter.ParamType.MESSAGE_BODY;
         }
         return this;
      }
   }

   public static class ResourceConstructorBuilder
   {

      ResourceConstructor constructor;
      ResourceClassBuilder resourceClassBuilder;

      public ResourceConstructorBuilder(ResourceClassBuilder resourceClassBuilder, Constructor constructor)
      {
         this.resourceClassBuilder = resourceClassBuilder;
         this.constructor = new ResourceConstructor(resourceClassBuilder.resourceClass, constructor);
      }
      public ConstructorParameterBuilder param(int i)
      {
         return new ConstructorParameterBuilder(this, constructor.getParams()[i]);
      }

      public ResourceClassBuilder buildConstructor()
      {
         resourceClassBuilder.resourceClass.constructor = constructor;
         return resourceClassBuilder;
      }
   }

   public static class ResourceLocatorBuilder<T extends ResourceLocatorBuilder>
   {

      ResourceLocator locator;
      ResourceClassBuilder resourceClassBuilder;

      ResourceLocatorBuilder()
      {
      }

      public ResourceLocatorBuilder(ResourceClassBuilder resourceClassBuilder, Method method, Method annotatedMethod)
      {
         this.resourceClassBuilder = resourceClassBuilder;
         this.locator = new ResourceLocator(resourceClassBuilder.resourceClass, method, annotatedMethod);
      }

      public T returnType(Class<?> type)
      {
         locator.returnType = type;
         return (T)this;
      }

      public T genericReturnType(Type type)
      {
         locator.genericReturnType = type;
         return (T)this;
      }

      public T returnType(GenericType type)
      {
         locator.returnType = type.getRawType();
         locator.genericReturnType = type.getType();
         return (T)this;
      }

      public LocatorMethodParameterBuilder param(int i)
      {
         return new LocatorMethodParameterBuilder(this, locator.getParams()[i]);
      }

      public ResourceClassBuilder buildMethod()
      {
         ResteasyUriBuilder builder = new ResteasyUriBuilder();
         if (locator.resourceClass.path != null) builder.path(locator.resourceClass.path);
         if (locator.path != null) builder.path(locator.path);
         String pathExpression = builder.getPath();
         if (pathExpression == null)
            pathExpression = "";
         locator.fullpath = pathExpression;
         if (locator.resourceClass.getClazz().isAnonymousClass())
         {
            locator.getMethod().setAccessible(true);
         }
         resourceClassBuilder.resourceLocators.add(locator);
         return resourceClassBuilder;
      }

      public T path(String path)
      {
         locator.path = path;
         return (T)this;
      }
   }

   public static class ResourceMethodBuilder extends ResourceLocatorBuilder<ResourceMethodBuilder>
   {
      ResourceMethod method;

      ResourceMethodBuilder(ResourceClassBuilder resourceClassBuilder, Method method, Method annotatedMethod)
      {
         this.method = new ResourceMethod(resourceClassBuilder.resourceClass, method, annotatedMethod);
         this.locator = this.method;
         this.resourceClassBuilder = resourceClassBuilder;
      }

      public ResourceMethodBuilder httpMethod(String httpMethod)
      {
         method.httpMethods.add(httpMethod);
         return this;
      }

      public ResourceMethodBuilder get()
      {
         method.httpMethods.add(HttpMethod.GET);
         return this;
      }

      public ResourceMethodBuilder put()
      {
         method.httpMethods.add(HttpMethod.PUT);
         return this;
      }

      public ResourceMethodBuilder post()
      {
         method.httpMethods.add(HttpMethod.POST);
         return this;
      }

      public ResourceMethodBuilder delete()
      {
         method.httpMethods.add(HttpMethod.DELETE);
         return this;
      }

      public ResourceMethodBuilder options()
      {
         method.httpMethods.add(HttpMethod.OPTIONS);
         return this;
      }

      public ResourceMethodBuilder head()
      {
         method.httpMethods.add(HttpMethod.HEAD);
         return this;
      }

      public ResourceMethodBuilder produces(MediaType... produces)
      {
         method.produces = produces;
         return this;
      }

      public ResourceMethodBuilder produces(String... produces)
      {
         MediaType[] types = parseMediaTypes(produces);
         method.produces = types;
         return this;
      }

      protected MediaType[] parseMediaTypes(String[] produces)
      {
         List<MediaType> mediaTypes = new ArrayList<MediaType>();
         for (String produce : produces)
         {
            String[] split = produce.split(",");
            for (String s : split) mediaTypes.add(MediaType.valueOf(s));
         }
         MediaType[] types = new MediaType[mediaTypes.size()];
         types = mediaTypes.toArray(types);
         return types;
      }

      public ResourceMethodBuilder consumes(MediaType... consumes)
      {
         method.consumes = consumes;
         return this;
      }

      public ResourceMethodBuilder consumes(String... consumes)
      {
         MediaType[] types = parseMediaTypes(consumes);
         method.consumes = types;
         return this;
      }

      public ResourceMethodParameterBuilder param(int i)
      {
         return new ResourceMethodParameterBuilder(this, locator.getParams()[i]);
      }

      public ResourceClassBuilder buildMethod()
      {
         ResteasyUriBuilder builder = new ResteasyUriBuilder();
         if (method.resourceClass.path != null) builder.path(method.resourceClass.path);
         if (method.path != null) builder.path(method.path);
         String pathExpression = builder.getPath();
         if (pathExpression == null)
            pathExpression = "";
         method.fullpath = pathExpression;
         if (method.resourceClass.getClazz().isAnonymousClass())
         {
            method.getMethod().setAccessible(true);
         }
         resourceClassBuilder.resourceMethods.add(method);
         return resourceClassBuilder;
      }
   }

   public static class FieldParameterBuilder extends ParameterBuilder<FieldParameterBuilder>
   {
      FieldParameter field;
      ResourceClassBuilder resourceClassBuilder;
      FieldParameterBuilder(ResourceClassBuilder resourceClassBuilder, FieldParameter parameter)
      {
         super(parameter);
         this.field = parameter;
         this.resourceClassBuilder = resourceClassBuilder;
      }

      public ResourceClassBuilder buildField()
      {
         field.field.setAccessible(true);
         resourceClassBuilder.fields.add(field);
         return resourceClassBuilder;
      }
   }

   public static class SetterParameterBuilder extends ParameterBuilder<SetterParameterBuilder>
   {
      SetterParameter setter;
      ResourceClassBuilder resourceClassBuilder;
      SetterParameterBuilder(ResourceClassBuilder resourceClassBuilder, SetterParameter parameter)
      {
         super(parameter);
         this.setter = parameter;
         this.resourceClassBuilder = resourceClassBuilder;
      }

      public ResourceClassBuilder buildSetter()
      {
         setter.setter.setAccessible(true);
         resourceClassBuilder.setters.add(setter);
         return resourceClassBuilder;
      }
   }


   public static ResourceClassBuilder rootResource(Class<?> root)
   {
      return new ResourceClassBuilder(root, "/");
   }

   public static ResourceClassBuilder rootResource(Class<?> root, String path)
   {
      return new ResourceClassBuilder(root, path);
   }

   public static ResourceClassBuilder locator(Class<?> root)
   {
      return new ResourceClassBuilder(root, null);
   }


   /**
    * Picks a constructor from an annotated resource class based on spec rules
    *
    * @param annotatedResourceClass
    * @return
    */
   public static ResourceConstructor constructor(Class<?> annotatedResourceClass)
   {
      Constructor constructor = PickConstructor.pickPerRequestConstructor(annotatedResourceClass);
      if (constructor == null)
      {
         throw new RuntimeException("Could not find constructor for class: " + annotatedResourceClass.getName());
      }
      ResourceConstructorBuilder builder = rootResource(annotatedResourceClass).constructor(constructor);
      if (constructor.getParameterTypes() != null)
      {
         for (int i = 0; i < constructor.getParameterTypes().length; i++) builder.param(i).fromAnnotations();
      }
      return builder.buildConstructor().buildClass().getConstructor();
   }

   /**
    * Build metadata from annotations on classes and methods
    *
    * @return
    */
   public static ResourceClass rootResourceFromAnnotations(Class<?> clazz)
   {
      return fromAnnotations(false, clazz);
   }

   public static ResourceClass locatorFromAnnotations(Class<?> clazz)
   {
      return fromAnnotations(true, clazz);
   }

   private static final String WELD_PROXY_INTERFACE_NAME = "org.jboss.weld.bean.proxy.ProxyObject";

   /**
    * Whether the given class is a proxy created by Weld or not. This is
    * the case if the given class implements the interface
    * {@code org.jboss.weld.bean.proxy.ProxyObject}.
    *
    * @param clazz the class of interest
    *
    * @return {@code true} if the given class is a Weld proxy,
    * {@code false} otherwise
    */
   private static boolean isWeldProxy(Class<?> clazz) {
      for ( Class<?> implementedInterface : clazz.getInterfaces() ) {
         if ( implementedInterface.getName().equals( WELD_PROXY_INTERFACE_NAME ) ) {
            return true;
         }
      }

      return false;
   }

   private static ResourceClass fromAnnotations(boolean isLocator, Class<?> clazz)
   {
      // stupid hack for Weld as it loses generic type information, but retains annotations.
      if (!clazz.isInterface() && clazz.getSuperclass() != null && !clazz.getSuperclass().equals(Object.class) && isWeldProxy(clazz))
      {
         clazz = clazz.getSuperclass();
      }


      ResourceClassBuilder builder = null;
      if (isLocator) builder = locator(clazz);
      else
      {
         Path path = clazz.getAnnotation(Path.class);
         if (path == null) builder = rootResource(clazz, null);
         else builder = rootResource(clazz, path.value());
      }
      for (Method method : clazz.getMethods())
      {
         if(!method.isSynthetic() && !method.getDeclaringClass().equals(Object.class))
            processMethod(isLocator, builder, clazz, method);

      }
      if (!clazz.isInterface())
      {
         processFields(builder, clazz);
      }
      processSetters(builder, clazz);
      return builder.buildClass();
   }

   private static Method findAnnotatedInterfaceMethod(Class<?> root, Class<?> iface, Method implementation)
   {
      for (Method method : iface.getMethods())
      {
         if (method.isSynthetic()) continue;

         if (!method.getName().equals(implementation.getName())) continue;
         if (method.getParameterTypes().length != implementation.getParameterTypes().length) continue;

         Method actual = Types.getImplementingMethod(root, method);
         if (!actual.equals(implementation)) continue;

         if (method.isAnnotationPresent(Path.class) || IsHttpMethod.getHttpMethods(method) != null)
            return method;

      }
      for (Class<?> extended : iface.getInterfaces())
      {
         Method m = findAnnotatedInterfaceMethod(root, extended, implementation);
         if(m != null)
            return m;
      }
      return null;
   }

   private static Method findAnnotatedMethod(Class<?> root, Method implementation)
   {
      // check the method itself
      if (implementation.isAnnotationPresent(Path.class) || IsHttpMethod.getHttpMethods(implementation) != null)
         return implementation;

      if (implementation.isAnnotationPresent(Produces.class)
              || implementation.isAnnotationPresent(Consumes.class))
      {
         // completely abort this method
         return null;
      }

      // Per http://download.oracle.com/auth/otn-pub/jcp/jaxrs-1.0-fr-oth-JSpec/jaxrs-1.0-final-spec.pdf
      // Section 3.2 Annotation Inheritance

      // Check possible superclass declarations
      for (Class<?> clazz = implementation.getDeclaringClass().getSuperclass(); clazz != null; clazz = clazz.getSuperclass())
      {
         try
         {
            Method method = clazz.getDeclaredMethod(implementation.getName(), implementation.getParameterTypes());
            if (method.isAnnotationPresent(Path.class) || IsHttpMethod.getHttpMethods(method) != null)
               return method;
            if (method.isAnnotationPresent(Produces.class)
                    || method.isAnnotationPresent(Consumes.class))
            {
               // completely abort this method
               return null;
            }
         }
         catch (NoSuchMethodException e)
         {
            // ignore
         }
      }

      // Not found yet, so next check ALL interfaces from the root,
      // but ensure no redefinition by peer interfaces (ambiguous) to preserve logic found in
      // original implementation
      for (Class<?> clazz = root; clazz != null; clazz = clazz.getSuperclass())
      {
         Method method = null;
         for (Class<?> iface : clazz.getInterfaces())
         {
            Method m = findAnnotatedInterfaceMethod(root, iface, implementation);
            if (m != null)
            {
               if(method != null && !m.equals(method))
                  throw new RuntimeException("Ambiguous inherited JAX-RS annotations applied to method: " + implementation);
               method = m;
            }
         }
         if (method != null)
            return method;
      }
      return null;
   }

   protected static void processFields(ResourceClassBuilder resourceClassBuilder, Class<?> root)
   {
      do
      {
         processDeclaredFields(resourceClassBuilder, root);
         root = root.getSuperclass();
      } while (root.getSuperclass() != null && !root.getSuperclass().equals(Object.class));
   }

   protected static void processSetters(ResourceClassBuilder resourceClassBuilder, Class<?> root)
   {
      HashSet<Long> hashes = new HashSet<Long>();
      do
      {
         processDeclaredSetters(resourceClassBuilder, root, hashes);
         root = root.getSuperclass();
      } while (root != null && !root.equals(Object.class));
   }

   protected static void processDeclaredFields(ResourceClassBuilder resourceClassBuilder, Class<?> root)
   {
      for (Field field : root.getDeclaredFields())
      {
         FieldParameterBuilder builder = resourceClassBuilder.field(field).fromAnnotations();
         if (builder.field.paramType == Parameter.ParamType.MESSAGE_BODY && !field.isAnnotationPresent(Body.class)) continue;
         if (builder.field.paramType == Parameter.ParamType.UNKNOWN) continue;
         builder.buildField();
      }
   }
   protected static void processDeclaredSetters(ResourceClassBuilder resourceClassBuilder, Class<?> root, Set<Long> visitedHashes)
   {
      for (Method method : root.getDeclaredMethods())
      {
         if (!method.getName().startsWith("set")) continue;
         if (method.getParameterTypes().length != 1) continue;
         long hash = 0;
         try
         {
            hash = MethodHashing.methodHash(method);
         }
         catch (Exception e)
         {
            throw new RuntimeException(e);
         }
         if (!Modifier.isPrivate(method.getModifiers()) && visitedHashes.contains(hash)) continue;
         visitedHashes.add(hash);
         SetterParameterBuilder builder = resourceClassBuilder.setter(method).fromAnnotations();
         if (builder.setter.paramType == Parameter.ParamType.MESSAGE_BODY && !method.isAnnotationPresent(Body.class)) continue;
         if (builder.setter.paramType == Parameter.ParamType.UNKNOWN) continue;
         builder.buildSetter();
      }
   }

   protected static void processMethod(boolean isLocator, ResourceClassBuilder resourceClassBuilder, Class<?> root, Method implementation)
   {
      Method method = findAnnotatedMethod(root, implementation);
      if (method != null)
      {
         Set<String> httpMethods = IsHttpMethod.getHttpMethods(method);

         ResourceLocatorBuilder resourceLocatorBuilder;

         if (httpMethods == null)
         {
            resourceLocatorBuilder = resourceClassBuilder.locator(implementation, method);
         }
         else
         {
            ResourceMethodBuilder resourceMethodBuilder = resourceClassBuilder.method(implementation, method);
            resourceLocatorBuilder = resourceMethodBuilder;

            for (String httpMethod : httpMethods)
            {
               if (httpMethod.equalsIgnoreCase(HttpMethod.GET)) resourceMethodBuilder.get();
               else if (httpMethod.equalsIgnoreCase(HttpMethod.PUT)) resourceMethodBuilder.put();
               else if (httpMethod.equalsIgnoreCase(HttpMethod.POST)) resourceMethodBuilder.post();
               else if (httpMethod.equalsIgnoreCase(HttpMethod.DELETE)) resourceMethodBuilder.delete();
               else if (httpMethod.equalsIgnoreCase(HttpMethod.OPTIONS)) resourceMethodBuilder.options();
               else if (httpMethod.equalsIgnoreCase(HttpMethod.HEAD)) resourceMethodBuilder.head();
               else resourceMethodBuilder.httpMethod(httpMethod);
            }
            Produces produces = method.getAnnotation(Produces.class);
            if (produces == null) produces = resourceClassBuilder.resourceClass.getClazz().getAnnotation(Produces.class);
            if (produces == null) produces = method.getDeclaringClass().getAnnotation(Produces.class);
            if (produces != null) resourceMethodBuilder.produces(produces.value());

            Consumes consumes = method.getAnnotation(Consumes.class);
            if (consumes == null) consumes = resourceClassBuilder.resourceClass.getClazz().getAnnotation(Consumes.class);
            if (consumes == null) consumes = method.getDeclaringClass().getAnnotation(Consumes.class);
            if (consumes != null) resourceMethodBuilder.consumes(consumes.value());
         }
         Path methodPath = method.getAnnotation(Path.class);
         if (methodPath != null) resourceLocatorBuilder.path(methodPath.value());
         for (int i = 0; i < resourceLocatorBuilder.locator.params.length; i++)
         {
            resourceLocatorBuilder.param(i).fromAnnotations();
         }
         resourceLocatorBuilder.buildMethod();
      }
   }





}
