package org.jboss.resteasy.util;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Cookie;

public class CookieParser {
   public static List<Cookie> parseCookies(String cookieHeader) {
      if (cookieHeader == null) throw new IllegalArgumentException("Cookie header value was null");
      try
      {
         List<Cookie> cookies = new ArrayList<Cookie>();

         int version = 0;
         String domain = null;
         String path = null;
         String cookieName = null;
         String cookieValue = null;

         String parts[] = cookieHeader.split("[;,]");
         for (String part : parts)
         {
            String nv[] = part.split("=", 2);
            String name = nv.length > 0 ? nv[0].trim() : "";
            String value = nv.length > 1 ? nv[1].trim() : "";
            if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1)
               value = value.substring(1, value.length() - 1);
            if (!name.startsWith("$"))
            {
               if (cookieName != null) {
                  cookies.add(new Cookie(cookieName, cookieValue, path, domain, version));
                  cookieName = cookieValue = path = domain = null;
               }

               cookieName = name;
               cookieValue = value;
            }
            else if (name.equalsIgnoreCase("$Version"))
            {
               version = Integer.parseInt(value);
            }
            else if (name.equalsIgnoreCase("$Path"))
            {
               path = value;
            }
            else if (name.equalsIgnoreCase("$Domain"))
            {
               domain = value;
            }
         }
         if (cookieName != null) {
            cookies.add(new Cookie(cookieName, cookieValue, path, domain, version));

         }
         return cookies;
      }
      catch (Exception ex)
      {
        throw new IllegalArgumentException("Failed to parse cookie string '" + cookieHeader + "'", ex);
      }
  }
}
