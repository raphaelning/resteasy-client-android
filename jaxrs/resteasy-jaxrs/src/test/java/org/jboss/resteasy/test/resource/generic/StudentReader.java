package org.jboss.resteasy.test.resource.generic;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
@Consumes("application/student")
public class StudentReader implements MessageBodyReader<Student>
{

   public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return true;
   }

   public Student readFrom(Class<Student> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException
   {
      BufferedReader br = null;
      try
      {
         br = new BufferedReader(new InputStreamReader(entityStream));
         return new Student(br.readLine());
      }
      catch (Exception e)
      {
         throw new RuntimeException("Unable to parse student.", e);
      }
   }
}
