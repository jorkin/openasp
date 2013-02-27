package com.zfbots.asp;

import java.util.Enumeration;
import java.util.Hashtable;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpUtils;

public class AspResponse
{
  public static int getCookieKeyCount(HttpServletRequest paramHttpServletRequest)
  {
    return 1;
  }

  public static String createQueryString(Hashtable paramHashtable)
  {
    Enumeration localEnumeration = paramHashtable.keys();
    StringBuffer localStringBuffer = new StringBuffer();
    while (localEnumeration.hasMoreElements())
    {
      Object localObject1 = localEnumeration.nextElement();
      if (localStringBuffer.length() > 0)
        localStringBuffer.append("&");
      localStringBuffer.append(localObject1);
      localStringBuffer.append("=");
      Object localObject2 = paramHashtable.get(localObject1);
      if ((localObject2 instanceof String[]))
      {
        localStringBuffer.append(((String[])localObject2)[0]);
        continue;
      }
      localStringBuffer.append(localObject2);
    }
    return localStringBuffer.toString();
  }

  public static Cookie getCookie(Cookie[] paramArrayOfCookie, String paramString)
  {
    Cookie localCookie = AspRequest.getCookie(paramArrayOfCookie, paramString);
    if (localCookie == null)
      localCookie = new Cookie(paramString, null);
    return localCookie;
  }

  public static void setCookieValue(Cookie paramCookie, String paramString1, String paramString2, HttpServletResponse paramHttpServletResponse)
  {
    if (paramCookie == null)
      paramCookie = new Cookie(null, null);
    if (paramString1 == null)
    {
      paramCookie.setValue(paramString2);
    }
    else
    {
      String str = paramCookie.getValue();
      Hashtable localHashtable = str == null ? new Hashtable() : HttpUtils.parseQueryString(str);
      localHashtable.put(paramString1, paramString2);
      paramCookie.setValue(createQueryString(localHashtable));
    }
    paramHttpServletResponse.addCookie(paramCookie);
  }
}