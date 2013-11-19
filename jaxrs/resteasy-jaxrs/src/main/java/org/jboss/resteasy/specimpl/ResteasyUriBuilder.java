package org.jboss.resteasy.specimpl;

import org.jboss.resteasy.util.Encode;
import org.jboss.resteasy.util.PathHelper;

import javax.ws.rs.Path;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResteasyUriBuilder extends UriBuilder
{

   private String host;
   private String scheme;
   private int port = -1;

   private String userInfo;
   private String path;
   private String query;
   private String fragment;
   private String ssp;
   private String authority;


   public UriBuilder clone()
   {
      ResteasyUriBuilder impl = new ResteasyUriBuilder();
      impl.host = host;
      impl.scheme = scheme;
      impl.port = port;
      impl.userInfo = userInfo;
      impl.path = path;
      impl.query = query;
      impl.fragment = fragment;
      impl.ssp = ssp;
      impl.authority = authority;

      return impl;
   }

   public static final Pattern opaqueUri = Pattern.compile("^([^:/?#]+):([^/].*)");
   public static final Pattern hierarchicalUri = Pattern.compile("^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?");
   private static final Pattern hostPortPattern = Pattern.compile("([^/:]+):(\\d+)");

   public static boolean compare(String s1, String s2)
   {
      if (s1 == s2) return true;
      if (s1 == null || s2 == null) return false;
      return s1.equals(s2);
   }

   public static URI relativize(URI from, URI to)
   {
      if (!compare(from.getScheme(), to.getScheme())) return to;
      if (!compare(from.getHost(), to.getHost())) return to;
      if (from.getPort() != to.getPort()) return to;
      if (from.getPath() == null && to.getPath() == null) return URI.create("");
      else if (from.getPath() == null) return URI.create(to.getPath());
      else if (to.getPath() == null) return to;


      String fromPath = from.getPath();
      if (fromPath.startsWith("/")) fromPath = fromPath.substring(1);
      String[] fsplit = fromPath.split("/");
      String toPath = to.getPath();
      if (toPath.startsWith("/")) toPath = toPath.substring(1);
      String[] tsplit = toPath.split("/");

      int f = 0;

      for (;f < fsplit.length && f < tsplit.length; f++)
      {
         if (!fsplit[f].equals(tsplit[f])) break;
      }

      UriBuilder builder = UriBuilder.fromPath("");
      for (int i = f; i < fsplit.length; i++) builder.path("..");
      for (int i = f; i < tsplit.length; i++) builder.path(tsplit[i]);
      return builder.build();
   }

   /**
    * You may put path parameters anywhere within the uriTemplate except port
    *
    * @param uriTemplate
    * @return
    */
   public static UriBuilder fromTemplate(String uriTemplate)
   {
      ResteasyUriBuilder impl = new ResteasyUriBuilder();
      impl.uriTemplate(uriTemplate);
      return impl;
   }

   /**
    * You may put path parameters anywhere within the uriTemplate except port
    *
    * @param uriTemplate
    * @return
    */
   public UriBuilder uriTemplate(String uriTemplate)
   {
      if (uriTemplate == null) throw new IllegalArgumentException("uriTemplate parameter is null");
      Matcher opaque = opaqueUri.matcher(uriTemplate);
      if (opaque.matches())
      {
         this.authority = null;
         this.host = null;
         this.port = -1;
         this.userInfo = null;
         this.query = null;
         this.scheme = opaque.group(1);
         this.ssp = opaque.group(2);
         return this;
      }
      else
      {
         Matcher match = hierarchicalUri.matcher(uriTemplate);
         if (match.matches())
         {
            ssp = null;
            return parseHierarchicalUri(uriTemplate, match);
         }
      }
      throw new IllegalArgumentException("Illegal uri template: " + uriTemplate);
   }

   protected UriBuilder parseHierarchicalUri(String uriTemplate, Matcher match)
   {
      boolean scheme = match.group(2) != null;
      if (scheme) this.scheme = match.group(2);
      String authority = match.group(4);
      if (authority != null)
      {
         this.authority = null;
         String host = match.group(4);
         int at = host.indexOf('@');
         if (at > -1)
         {
            String user = host.substring(0, at);
            host = host.substring(at + 1);
            this. userInfo = user;
         }
         Matcher hostPortMatch = hostPortPattern.matcher(host);
         if (hostPortMatch.matches())
         {
            this.host = hostPortMatch.group(1);
            int val = 0;
            try {
               this.port = Integer.parseInt(hostPortMatch.group(2));
            }
            catch (NumberFormatException e) {
               throw new IllegalArgumentException("Illegal uri template: " + uriTemplate, e);
            }
         }
         else
         {
            this.host = host;
         }
      }
      if (match.group(5) != null)
      {
         String group = match.group(5);
         if (!scheme && !"".equals(group) && !group.startsWith("/") && group.indexOf(':') > -1) throw new IllegalArgumentException("Illegal uri template: " + uriTemplate);
         if (!"".equals(group)) replacePath(group);
      }
      if (match.group(7) != null) replaceQuery(match.group(7));
      if (match.group(9) != null) fragment(match.group(9));
      return this;
   }

   @Override
   public UriBuilder uri(String uriTemplate) throws IllegalArgumentException
   {
      return uriTemplate(uriTemplate);
   }

   @Override
   public UriBuilder uri(URI uri) throws IllegalArgumentException
   {
      if (uri == null) throw new IllegalArgumentException("URI was null");

      if (uri.getRawFragment() != null) fragment = uri.getRawFragment();

      if (uri.isOpaque())
      {
         scheme = uri.getScheme();
         ssp = uri.getRawSchemeSpecificPart();
         return this;
      }

      if (uri.getScheme() == null)
      {
         if (ssp != null)
         {
            if (uri.getRawSchemeSpecificPart() != null)
            {
               ssp = uri.getRawSchemeSpecificPart();
               return this;
            }
         }
      }
      else
      {
         scheme = uri.getScheme();
      }

      ssp = null;
      if (uri.getRawAuthority() != null) {
         if (uri.getRawUserInfo() == null && uri.getHost() == null && uri.getPort() == -1) {
            authority = uri.getRawAuthority();
            userInfo = null;
            host = null;
            port = -1;
         } else {
            authority = null;
            if (uri.getRawUserInfo() != null) {
               userInfo = uri.getRawUserInfo();
            }
            if (uri.getHost() != null) {
               host = uri.getHost();
            }
            if (uri.getPort() != -1) {
               port = uri.getPort();
            }
         }
      }

      if (uri.getRawPath() != null && uri.getRawPath().length() > 0) {
         path = uri.getRawPath();
      }
      if (uri.getRawQuery() != null && uri.getRawQuery().length() > 0) {
         query = uri.getRawQuery();
      }

      return this;
   }

   @Override
   public UriBuilder scheme(String scheme) throws IllegalArgumentException
   {
      this.scheme = scheme;
      return this;
   }

   @Override
   public UriBuilder schemeSpecificPart(String ssp) throws IllegalArgumentException
   {
      if (ssp == null) throw new IllegalArgumentException("schemeSpecificPart was null");

      StringBuilder sb = new StringBuilder();
      if (scheme != null) sb.append(scheme).append(':');
      if (ssp != null)
         sb.append(ssp);
      if (fragment != null && fragment.length() > 0) sb.append('#').append(fragment);
      URI uri = URI.create(sb.toString());

      if (uri.getRawSchemeSpecificPart() != null && uri.getRawPath() == null)
      {
         this.ssp = uri.getRawSchemeSpecificPart();
      }
      else
      {
         this.ssp = null;
         userInfo = uri.getRawUserInfo();
         host = uri.getHost();
         port = uri.getPort();
         path = uri.getRawPath();
         query = uri.getRawQuery();

      }
      return this;

   }

   @Override
   public UriBuilder userInfo(String ui)
   {
      this.userInfo = ui;
      return this;
   }

   @Override
   public UriBuilder host(String host) throws IllegalArgumentException
   {
      if (host != null && host.equals("")) throw new IllegalArgumentException("invalid host");
      this.host = host;
      return this;
   }

   @Override
   public UriBuilder port(int port) throws IllegalArgumentException
   {
      if (port < -1) throw new IllegalArgumentException("Invalid port value");
      this.port = port;
      return this;
   }

   protected static String paths(boolean encode, String basePath, String... segments)
   {
      String path = basePath;
      if (path == null) path = "";
      for (String segment : segments)
      {
         if ("".equals(segment)) continue;
         if (path.endsWith("/"))
         {
            if (segment.startsWith("/"))
            {
               segment = segment.substring(1);
               if ("".equals(segment)) continue;
            }
            if (encode) segment = Encode.encodePath(segment);
            path += segment;
         }
         else
         {
            if (encode) segment = Encode.encodePath(segment);
            if ("".equals(path))
            {
               path = segment;
            }
            else if (segment.startsWith("/"))
            {
               path += segment;
            }
            else
            {
               path += "/" + segment;
            }
         }

      }
      return path;
   }

   @Override
   public UriBuilder path(String segment) throws IllegalArgumentException
   {
      if (segment == null) throw new IllegalArgumentException("path was null");
      path = paths(true, path, segment);
      return this;
   }

   @SuppressWarnings("unchecked")
   @Override
   public UriBuilder path(Class resource) throws IllegalArgumentException
   {
      if (resource == null) throw new IllegalArgumentException("path was null");
      Path ann = (Path) resource.getAnnotation(Path.class);
      if (ann != null)
      {
         String[] segments = new String[]{ann.value()};
         path = paths(true, path, segments);
      }
      else
      {
         throw new IllegalArgumentException("Class must be annotated with @Path to invoke path(Class)");
      }
      return this;
   }

   @SuppressWarnings("unchecked")
   @Override
   public UriBuilder path(Class resource, String method) throws IllegalArgumentException
   {
      if (resource == null) throw new IllegalArgumentException("resource was null");
      if (method == null) throw new IllegalArgumentException("method was null");
      Method theMethod = null;
      for (Method m : resource.getMethods())
      {
         if (m.getName().equals(method))
         {
            if (theMethod != null && m.isAnnotationPresent(Path.class))
            {
               throw new IllegalArgumentException("there are two method named " + method);
            }
            if (m.isAnnotationPresent(Path.class)) theMethod = m;
         }
      }
      if (theMethod == null) throw new IllegalArgumentException("No @Path annotated method for " + resource.getName()+ "." +method);
      return path(theMethod);
   }

   @Override
   public UriBuilder path(Method method) throws IllegalArgumentException
   {
      if (method == null)
      {
         throw new IllegalArgumentException("method was null");
      }
      Path ann = method.getAnnotation(Path.class);
      if (ann != null)
      {
         path = paths(true, path, ann.value());
      }
      else
      {
         throw new IllegalArgumentException("method is not annotated with @Path");
      }
      return this;
   }

   @Override
   public UriBuilder replaceMatrix(String matrix) throws IllegalArgumentException
   {
      if (matrix == null) matrix = "";
      if (!matrix.startsWith(";")) matrix = ";" + matrix;
      matrix = Encode.encodePath(matrix);
      if (path == null)
      {
         path = matrix;
      }
      else
      {
         int start = path.lastIndexOf('/');
         if (start < 0) start = 0;
         int matrixIndex = path.indexOf(';', start);
         if (matrixIndex > -1) path = path.substring(0, matrixIndex) + matrix;
         else path += matrix;

      }
      return this;
   }

   @Override
   public UriBuilder replaceQuery(String query) throws IllegalArgumentException
   {
      if (query == null || query.length() == 0)
      {
         this.query = null;
         return this;
      }
      this.query = Encode.encodeQueryString(query);
      return this;
   }

   @Override
   public UriBuilder fragment(String fragment) throws IllegalArgumentException
   {
      if (fragment == null)
      {
         this.fragment = null;
         return this;
      }
      this.fragment = Encode.encodeFragment(fragment);
      return this;
   }

   /**
    * Only replace path params in path of URI.  This changes state of URIBuilder.
    *
    * @param name
    * @param value
    * @param isEncoded
    * @return
    */
   public UriBuilder substitutePathParam(String name, Object value, boolean isEncoded)
   {
      if (path != null)
      {
         StringBuffer buffer = new StringBuffer();
         replacePathParameter(name, value.toString(), isEncoded, path, buffer, false);
         path = buffer.toString();
      }
      return this;
   }

   @Override
   public URI buildFromMap(Map<String, ? extends Object> values) throws IllegalArgumentException, UriBuilderException
   {
      if (values == null) throw new IllegalArgumentException("values parameter is null");
      return buildUriFromMap(values, false, true);
   }

   @Override
   public URI buildFromEncodedMap(Map<String, ? extends Object> values) throws IllegalArgumentException, UriBuilderException
   {
      if (values == null) throw new IllegalArgumentException("values parameter is null");
      return buildUriFromMap(values, true, false);
   }

   @Override
   public URI buildFromMap(Map<String, ?> values, boolean encodeSlashInPath) throws IllegalArgumentException, UriBuilderException
   {
      if (values == null) throw new IllegalArgumentException("values parameter is null");
      return buildUriFromMap(values, false, encodeSlashInPath);
   }

   protected URI buildUriFromMap(Map<String, ? extends Object> paramMap, boolean fromEncodedMap, boolean encodeSlash) throws IllegalArgumentException, UriBuilderException
   {
      String buf = buildString(paramMap, fromEncodedMap, false, encodeSlash);
      try
      {
         return URI.create(buf);
      }
      catch (Exception e)
      {
         throw new RuntimeException("Failed to create URI: " + buf, e);
      }
   }

   private String buildString(Map<String, ? extends Object> paramMap, boolean fromEncodedMap, boolean isTemplate, boolean encodeSlash)
   {
      for (Map.Entry<String, ? extends Object> entry : paramMap.entrySet())
      {
         if (entry.getKey() == null) throw new IllegalArgumentException("map key is null");
         if (entry.getValue() == null) throw new IllegalArgumentException("map value is null");
      }
      StringBuffer buffer = new StringBuffer();

      if (scheme != null) replaceParameter(paramMap, fromEncodedMap, isTemplate, scheme, buffer, encodeSlash).append(":");
      if (ssp != null)
      {
         buffer.append(ssp);
      }
      else if (userInfo != null || host != null || port != -1)
      {
         buffer.append("//");
         if (userInfo != null) replaceParameter(paramMap, fromEncodedMap, isTemplate, userInfo, buffer, encodeSlash).append("@");
         if (host != null)
         {
            if ("".equals(host)) throw new UriBuilderException("empty host name");
            replaceParameter(paramMap, fromEncodedMap, isTemplate, host, buffer, encodeSlash);
         }
         if (port != -1) buffer.append(":").append(Integer.toString(port));
      }
      else if (authority != null)
      {
         buffer.append("//");
         replaceParameter(paramMap, fromEncodedMap, isTemplate, authority, buffer, encodeSlash);
      }
      if (path != null)
      {
         StringBuffer tmp = new StringBuffer();
         replaceParameter(paramMap, fromEncodedMap, isTemplate, path, tmp, encodeSlash);
         String tmpPath = tmp.toString();
         if (userInfo != null || host != null)
         {
            if (!tmpPath.startsWith("/")) buffer.append("/");
         }
         buffer.append(tmpPath);
      }
      if (query != null)
      {
         buffer.append("?");
         replaceQueryStringParameter(paramMap, fromEncodedMap, isTemplate, query, buffer);
      }
      if (fragment != null)
      {
         buffer.append("#");
         replaceParameter(paramMap, fromEncodedMap, isTemplate, fragment, buffer, encodeSlash);
      }
      return buffer.toString();
   }

   protected StringBuffer replacePathParameter(String name, String value, boolean isEncoded, String string, StringBuffer buffer, boolean encodeSlash)
   {
      Matcher matcher = createUriParamMatcher(string);
      while (matcher.find())
      {
         String param = matcher.group(1);
         if (!param.equals(name)) continue;
         if (!isEncoded)
         {
            if (encodeSlash) value = Encode.encodePath(value);
            else value = Encode.encodePathSegment(value);

         }
         else
         {
            value = Encode.encodeNonCodes(value);
         }
         // if there is a $ then we must backslash it or it will screw up regex group substitution
         value = value.replace("$", "\\$");
         matcher.appendReplacement(buffer, value);
      }
      matcher.appendTail(buffer);
      return buffer;
   }

   public static Matcher createUriParamMatcher(String string)
   {
      Matcher matcher = PathHelper.URI_PARAM_PATTERN.matcher(PathHelper.replaceEnclosedCurlyBraces(string));
      return matcher;
   }

   protected StringBuffer replaceParameter(Map<String, ? extends Object> paramMap, boolean fromEncodedMap, boolean isTemplate, String string, StringBuffer buffer, boolean encodeSlash)
   {
      Matcher matcher = createUriParamMatcher(string);
      while (matcher.find())
      {
         String param = matcher.group(1);
         Object valObj = paramMap.get(param);
         if (valObj == null  && !isTemplate)
         {
            throw new IllegalArgumentException("NULL value for template parameter: " + param);
         }
         else if (valObj == null && isTemplate)
         {
            matcher.appendReplacement(buffer, matcher.group());
            continue;
         }
         String value = valObj.toString();
         if (value != null)
         {
            if (!fromEncodedMap)
            {
               if (encodeSlash) value = Encode.encodePathSegmentAsIs(value);
               else value = Encode.encodePathAsIs(value);
            }
            else
            {
               if (encodeSlash) value = Encode.encodePathSegmentSaveEncodings(value);
               else value = Encode.encodePathSaveEncodings(value);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(value));
         }
         else
         {
            throw new IllegalArgumentException("path param " + param + " has not been provided by the parameter map");
         }
      }
      matcher.appendTail(buffer);
      return buffer;
   }

   protected StringBuffer replaceQueryStringParameter(Map<String, ? extends Object> paramMap, boolean fromEncodedMap, boolean isTemplate, String string, StringBuffer buffer)
   {
      Matcher matcher = createUriParamMatcher(string);
      while (matcher.find())
      {
         String param = matcher.group(1);
         Object valObj = paramMap.get(param);
         if (valObj == null  && !isTemplate)
         {
            throw new IllegalArgumentException("NULL value for template parameter: " + param);
         }
         else if (valObj == null && isTemplate)
         {
            matcher.appendReplacement(buffer, matcher.group());
            continue;
         }
         String value = valObj.toString();
         if (value != null)
         {
            if (!fromEncodedMap)
            {
               value = Encode.encodeQueryParamAsIs(value);
            }
            else
            {
               value = Encode.encodeQueryParamSaveEncodings(value);
            }
            matcher.appendReplacement(buffer, value);
         }
         else
         {
            throw new IllegalArgumentException("path param " + param + " has not been provided by the parameter map");
         }
      }
      matcher.appendTail(buffer);
      return buffer;
   }

   /**
    * Return a unique order list of path params
    *
    * @return
    */
   public List<String> getPathParamNamesInDeclarationOrder()
   {
      List<String> params = new ArrayList<String>();
      HashSet<String> set = new HashSet<String>();
      if (scheme != null) addToPathParamList(params, set, scheme);
      if (userInfo != null) addToPathParamList(params, set, userInfo);
      if (host != null) addToPathParamList(params, set, host);
      if (path != null) addToPathParamList(params, set, path);
      if (query != null) addToPathParamList(params, set, query);
      if (fragment != null) addToPathParamList(params, set, fragment);

      return params;
   }

   private void addToPathParamList(List<String> params, HashSet<String> set, String string)
   {
      Matcher matcher = PathHelper.URI_PARAM_PATTERN.matcher(PathHelper.replaceEnclosedCurlyBraces(string));
      while (matcher.find())
      {
         String param = matcher.group(1);
         if (set.contains(param)) continue;
         else
         {
            set.add(param);
            params.add(param);
         }
      }
   }

   @Override
   public URI build(Object... values) throws IllegalArgumentException, UriBuilderException
   {
      if (values == null) throw new IllegalArgumentException("values parameter is null");
      return buildFromValues(true, false, values);
   }

   protected URI buildFromValues(boolean encodeSlash, boolean encoded, Object... values)
   {
      List<String> params = getPathParamNamesInDeclarationOrder();
      if (values.length < params.size())
         throw new IllegalArgumentException("You did not supply enough values to fill path parameters");

      Map<String, Object> pathParams = new HashMap<String, Object>();


      for (int i = 0; i < params.size(); i++)
      {
         String pathParam = params.get(i);
         Object val = values[i];
         if (val == null) throw new IllegalArgumentException("A value was null");
         pathParams.put(pathParam, val.toString());
      }
      String buf = null;
      try
      {
         buf = buildString(pathParams, encoded, false, encodeSlash);
         return new URI(buf);
         //return URI.create(buf);
      }
      catch (Exception e)
      {
         throw new UriBuilderException("Failed to create URI: " + buf, e);
      }
   }

   @Override
   public UriBuilder matrixParam(String name, Object... values) throws IllegalArgumentException
   {
      if (name == null) throw new IllegalArgumentException("name parameter is null");
      if (values == null) throw new IllegalArgumentException("values parameter is null");
      if (path == null) path = "";
      for (Object val : values)
      {
         if (val == null) throw new IllegalArgumentException("null value");
         path += ";" + Encode.encodeMatrixParam(name) + "=" + Encode.encodeMatrixParam(val.toString());
      }
      return this;
   }

   private static final Pattern PARAM_REPLACEMENT = Pattern.compile("_resteasy_uri_parameter");

   @Override
   public UriBuilder replaceMatrixParam(String name, Object... values) throws IllegalArgumentException
   {
      if (name == null) throw new IllegalArgumentException("name parameter is null");
      if (path == null)
      {
         if (values != null && values.length > 0) return matrixParam(name, values);
         return this;
      }

      // remove all path param expressions so we don't accidentally start replacing within a regular expression
      ArrayList<String> pathParams = new ArrayList<String>();
      boolean foundParam = false;

      Matcher matcher = PathHelper.URI_TEMPLATE_PATTERN.matcher(PathHelper.replaceEnclosedCurlyBraces(path));
      StringBuffer newSegment = new StringBuffer();
      while (matcher.find())
      {
         foundParam = true;
         String group = matcher.group();
         pathParams.add(PathHelper.recoverEnclosedCurlyBraces(group));
         matcher.appendReplacement(newSegment, "_resteasy_uri_parameter");
      }
      matcher.appendTail(newSegment);
      path = newSegment.toString();

      // Find last path segment
      int start = path.lastIndexOf('/');
      if (start < 0) start = 0;

      int matrixIndex = path.indexOf(';', start);
      if (matrixIndex > -1)
      {

         String matrixParams = path.substring(matrixIndex + 1);
         path = path.substring(0, matrixIndex);
         MultivaluedMapImpl<String, String> map = new MultivaluedMapImpl<String, String>();

         String[] params = matrixParams.split(";");
         for (String param : params)
         {
            int idx = param.indexOf('=');
            if (idx < 0)
            {
               map.add(param, null);
            }
            else
            {
               String theName = param.substring(0, idx);
               String value = "";
               if (idx + 1 < param.length()) value = param.substring(idx + 1);
               map.add(theName, value);
            }
         }
         map.remove(name);
         for (String theName : map.keySet())
         {
            List<String> vals = map.get(theName);
            for (Object val : vals)
            {
               if (val == null) path += ";" + theName;
               else path += ";" + theName + "=" + val.toString();
            }
         }
      }
      if (values != null && values.length > 0) matrixParam(name, values);

      // put back all path param expressions
      if (foundParam)
      {
         matcher = PARAM_REPLACEMENT.matcher(path);
         newSegment = new StringBuffer();
         int i = 0;
         while (matcher.find())
         {
            matcher.appendReplacement(newSegment, pathParams.get(i++));
         }
         matcher.appendTail(newSegment);
         path = newSegment.toString();
      }
      return this;
   }

   /**
    * Called by ClientRequest.getUri() to add a query parameter for
    * {@code @QueryParam} parameters. We do not use UriBuilder.queryParam()
    * because
    * <ul>
    * <li> queryParam() supports URI template processing and this method must
    * always encode braces (for parameter substitution is not possible for
    * {@code @QueryParam} parameters).
    * <p/>
    * <li> queryParam() supports "contextual URI encoding" (i.e., it does not
    * encode {@code %} characters that are followed by two hex characters).
    * The JavaDoc for {@code @QueryParam.value()} explicitly states that
    * the value is specified in decoded format and that "any percent
    * encoded literals within the value will not be decoded and will
    * instead be treated as literal text". This means that it is an
    * explicit bug to perform contextual URI encoding of this method's
    * name parameter; hence, we must always encode said parameter. This
    * method also foregoes contextual URI encoding on this method's value
    * parameter because it represents arbitrary data passed to a
    * {@code QueryParam} parameter of a client proxy (since the client
    * proxy is nothing more than a transport layer, it should not be
    * "interpreting" such data; instead, it should faithfully transmit
    * this data over the wire).
    * </ul>
    *
    * @param name  the name of the query parameter.
    * @param value the value of the query parameter.
    * @return Returns this instance to allow call chaining.
    */
   public UriBuilder clientQueryParam(String name, Object value) throws IllegalArgumentException
   {
      if (name == null) throw new IllegalArgumentException("name parameter is null");
      if (value == null) throw new IllegalArgumentException("A passed in value was null");
      if (query == null) query = "";
      else query += "&";
      query += Encode.encodeQueryParamAsIs(name) + "=" + Encode.encodeQueryParamAsIs(value.toString());
      return this;
   }

   @Override
   public UriBuilder queryParam(String name, Object... values) throws IllegalArgumentException
   {
      if (name == null) throw new IllegalArgumentException("name parameter is null");
      if (values == null) throw new IllegalArgumentException("values parameter is null");
      for (Object value : values)
      {
         if (value == null) throw new IllegalArgumentException("A passed in value was null");
         if (query == null) query = "";
         else query += "&";
         query += Encode.encodeQueryParam(name) + "=" + Encode.encodeQueryParam(value.toString());
      }
      return this;
   }

   @Override
   public UriBuilder replaceQueryParam(String name, Object... values) throws IllegalArgumentException
   {
      if (name == null) throw new IllegalArgumentException("name parameter is null");
      if (query == null || query.equals(""))
      {
         if (values != null) return queryParam(name, values);
         return this;
      }

      String[] params = query.split("&");
      query = null;

      String replacedName = Encode.encodeQueryParam(name);


      for (String param : params)
      {
         int pos = param.indexOf('=');
         if (pos >= 0)
         {
            String paramName = param.substring(0, pos);
            if (paramName.equals(replacedName)) continue;
         }
         else
         {
            if (param.equals(replacedName)) continue;
         }
         if (query == null) query = "";
         else query += "&";
         query += param;
      }
      // don't set values if values is null
      if (values == null) return this;
      // don't set values if values is null
      if (values == null) return this;
      return queryParam(name, values);
   }

   public String getHost()
   {
      return host;
   }

   public String getScheme()
   {
      return scheme;
   }

   public int getPort()
   {
      return port;
   }

   public String getUserInfo()
   {
      return userInfo;
   }

   public String getPath()
   {
      return path;
   }

   public String getQuery()
   {
      return query;
   }

   public String getFragment()
   {
      return fragment;
   }

   @Override
   public UriBuilder segment(String... segments) throws IllegalArgumentException
   {
      if (segments == null) throw new IllegalArgumentException("segments parameter was null");
      for (String segment : segments)
      {
         if (segment == null) throw new IllegalArgumentException("A segment is null");
         path(Encode.encodePathSegment(segment));
      }
      return this;
   }

   @Override
   public URI buildFromEncoded(Object... values) throws IllegalArgumentException, UriBuilderException
   {
      if (values == null) throw new IllegalArgumentException("values param is null");
      return buildFromValues(false, true, values);
   }

   @Override
   public UriBuilder replacePath(String path)
   {
      if (path == null)
      {
         this.path = null;
         return this;
      }
      this.path = Encode.encodePath(path);
      return this;
   }

   @Override
   public URI build(Object[] values, boolean encodeSlashInPath) throws IllegalArgumentException, UriBuilderException
   {
      if (values == null) throw new IllegalArgumentException("values param is null");
      return buildFromValues(encodeSlashInPath, false, values);
   }

   @Override
   public String toTemplate()
   {
      return buildString(new HashMap<String, Object>(), true, true, true);
   }

   @Override
   public UriBuilder resolveTemplate(String name, Object value) throws IllegalArgumentException
   {
      if (name == null) throw new IllegalArgumentException("name param is null");
      if (value == null) throw new IllegalArgumentException("value param is null");
      HashMap<String, Object> vals = new HashMap<String, Object>();
      vals.put(name, value);
      return resolveTemplates(vals);
   }

   @Override
   public UriBuilder resolveTemplates(Map<String, Object> templateValues) throws IllegalArgumentException
   {
      if (templateValues == null) throw new IllegalArgumentException("templateValues param null");
      String str = buildString(templateValues, false, true, true);
      return fromTemplate(str);
   }

   @Override
   public UriBuilder resolveTemplate(String name, Object value, boolean encodeSlashInPath) throws IllegalArgumentException
   {
      if (name == null) throw new IllegalArgumentException("name param is null");
      if (value == null) throw new IllegalArgumentException("value param is null");
      HashMap<String, Object> vals = new HashMap<String, Object>();
      vals.put(name, value);
      String str = buildString(vals, false, true, encodeSlashInPath);
      return fromTemplate(str);
   }

   @Override
   public UriBuilder resolveTemplateFromEncoded(String name, Object value) throws IllegalArgumentException
   {
      if (name == null) throw new IllegalArgumentException("name param is null");
      if (value == null) throw new IllegalArgumentException("value param is null");
      HashMap<String, Object> vals = new HashMap<String, Object>();
      vals.put(name, value);
      String str = buildString(vals, true, true, true);
      return fromTemplate(str);
   }

   @Override
   public UriBuilder resolveTemplates(Map<String, Object> templateValues, boolean encodeSlashInPath) throws IllegalArgumentException
   {
      if (templateValues == null) throw new IllegalArgumentException("templateValues param null");
      String str = buildString(templateValues, false, true, encodeSlashInPath);
      return fromTemplate(str);
   }

   @Override
   public UriBuilder resolveTemplatesFromEncoded(Map<String, Object> templateValues) throws IllegalArgumentException
   {
      if (templateValues == null) throw new IllegalArgumentException("templateValues param null");
      String str = buildString(templateValues, true, true, true);
      return fromTemplate(str);
   }
}
