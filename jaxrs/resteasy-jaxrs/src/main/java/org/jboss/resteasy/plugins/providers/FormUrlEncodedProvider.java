package org.jboss.resteasy.plugins.providers;

import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.jboss.resteasy.util.Encode;
import org.jboss.resteasy.util.FindAnnotation;
import org.jboss.resteasy.util.NoContent;

import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.Consumes;
import javax.ws.rs.Encoded;
import javax.ws.rs.Produces;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Provider
@Produces("application/x-www-form-urlencoded")
@Consumes("application/x-www-form-urlencoded")
@ConstrainedTo(RuntimeType.CLIENT)
public class FormUrlEncodedProvider implements MessageBodyReader<MultivaluedMap>, MessageBodyWriter<MultivaluedMap>
{
   private final static Logger logger = Logger.getLogger(FormUrlEncodedProvider.class);
   public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return MultivaluedMap.class.equals(type);
      /*
      if (!MultivaluedMap.class.isAssignableFrom(type)) return false;
      if (genericType == null) return true;

      if (!(genericType instanceof ParameterizedType)) return false;
      ParameterizedType params = (ParameterizedType) genericType;
      if (params.getActualTypeArguments().length != 2) return false;
      return params.getActualTypeArguments()[0].equals(String.class) && params.getActualTypeArguments()[1].equals(String.class);
      */
   }

   public MultivaluedMap readFrom(Class<MultivaluedMap> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException
   {
      if (NoContent.isContentLengthZero(httpHeaders)) return null;
      boolean encoded = FindAnnotation.findAnnotation(annotations, Encoded.class) != null;
      if (encoded) return parseForm(entityStream);
      else return Encode.decode(parseForm(entityStream));
   }

   public static MultivaluedMap<String, String> parseForm(InputStream entityStream)
           throws IOException
   {
      char[] buffer = new char[100];
      StringBuffer buf = new StringBuffer();
      BufferedReader reader = new BufferedReader(new InputStreamReader(entityStream));

      int wasRead = 0;
      do
      {
         wasRead = reader.read(buffer, 0, 100);
         if (wasRead > 0) buf.append(buffer, 0, wasRead);
      } while (wasRead > -1);

      String form = buf.toString();

      MultivaluedMap<String, String> formData = new MultivaluedMapImpl<String, String>();
      if ("".equals(form)) return formData;

      String[] params = form.split("&");

      for (String param : params)
      {
         if (param.indexOf('=') >= 0)
         {
            String[] nv = param.split("=");
            String val = nv.length > 1 ? nv[1] : "";
            formData.add(nv[0], val);
         }
         else
         {
            formData.add(param, "");
         }
      }
      return formData;
   }

   public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return MultivaluedMap.class.isAssignableFrom(type);
      /*
      if (!MultivaluedMap.class.isAssignableFrom(type)) return false;
      if (genericType == null) return true;

      if (!(genericType instanceof ParameterizedType)) return false;
      ParameterizedType params = (ParameterizedType) genericType;
      if (params.getActualTypeArguments().length != 2) return false;
      return params.getActualTypeArguments()[0].equals(String.class) && params.getActualTypeArguments()[1].equals(String.class);
      */
   }

   public long getSize(MultivaluedMap stringStringMultivaluedMap, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return -1;
   }

   public void writeTo(MultivaluedMap data, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException
   {
      MultivaluedMap<String, String> formData = (MultivaluedMap<String, String>)data;
      boolean encoded = FindAnnotation.findAnnotation(annotations, Encoded.class) != null;
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      OutputStreamWriter writer = new OutputStreamWriter(baos, "UTF-8");

      boolean first = true;
      for (Map.Entry<String, List<String>> entry : formData.entrySet())
      {
         String encodedName = entry.getKey();
         if (!encoded) encodedName = URLEncoder.encode(entry.getKey(), "UTF-8");

         for (String value : entry.getValue())
         {
            if (first) first = false;
            else writer.write("&");
            if (!encoded)
            {
               value = URLEncoder.encode(value, "UTF-8");
            }
            writer.write(encodedName);
            writer.write("=");
            writer.write(value);
         }
         writer.flush();
      }

      byte[] bytes = baos.toByteArray();
      //logger.info("*** FORM PROVIDER WRITING: " + new String(bytes));

//      httpHeaders.putSingle(HttpHeaderNames.CONTENT_LENGTH, Integer.toString(bytes.length));
      entityStream.write(bytes);

   }

}