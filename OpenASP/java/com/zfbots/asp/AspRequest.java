package com.zfbots.asp;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpUtils;

public class AspRequest
{
  static Vector variables = null;

  public static int getCookieKeyCount(Cookie paramCookie)
  {
    return 1;
  }

  public static String getCookieKey(Cookie paramCookie, int paramInt)
  {
    return null;
  }

  public static String getCookieValue(Cookie paramCookie, String paramString)
  {
    if (paramCookie != null)
    {
      if (paramString == null)
        return paramCookie.getValue();
      String str = paramCookie.getValue();
      Hashtable localHashtable = HttpUtils.parseQueryString(str);
      Object localObject = localHashtable.get(paramString);
      if ((localObject instanceof String[]))
        return ((String[])localObject)[0];
      if (localObject != null)
        return localObject.toString();
    }
    return "";
  }

  public static Cookie getCookie(Cookie[] paramArrayOfCookie, String paramString)
  {
    if (paramArrayOfCookie == null)
      return null;
    for (int i = 0; i < paramArrayOfCookie.length; i++)
    {
      Cookie localCookie = paramArrayOfCookie[i];
      if (paramString.equals(localCookie.getName()))
        return localCookie;
    }
    return null;
  }

  public static int getQueryStringCount(HttpServletRequest paramHttpServletRequest)
  {
	  int i=0;
    Enumeration localEnumeration = paramHttpServletRequest.getParameterNames();
    for (i = 0; localEnumeration.hasMoreElements(); i++)
      localEnumeration.nextElement();
    return i;
  }

  /** @deprecated */
  public static String getQueryString(int paramInt, HttpServletRequest paramHttpServletRequest)
  {
    return getQueryName(paramInt, paramHttpServletRequest);
  }

  public static String getQueryName(int paramInt, HttpServletRequest paramHttpServletRequest)
  {
    Enumeration localEnumeration = paramHttpServletRequest.getParameterNames();
    for (int i = 0; localEnumeration.hasMoreElements(); i++)
    {
      String str = localEnumeration.nextElement().toString();
      if (i == paramInt)
        return str;
    }
    return "";
  }

  public static String getQueryString(String paramString, HttpServletRequest paramHttpServletRequest)
  {
    if (paramString == null)
      return paramHttpServletRequest.getQueryString();
    String[] arrayOfString = paramHttpServletRequest.getParameterValues(paramString);
    if (arrayOfString == null)
      return "";
    StringBuffer localStringBuffer = new StringBuffer();
    for (int i = 0; i < arrayOfString.length; i++)
    {
      if (i != 0)
        localStringBuffer.append(", ");
      localStringBuffer.append(arrayOfString[i]);
    }
    return localStringBuffer.toString();
  }

  public static String getQueryString(String paramString, int paramInt, HttpServletRequest paramHttpServletRequest)
  {
    if (paramString == null)
      return paramHttpServletRequest.getQueryString();
    if (paramInt < 0)
      return getQueryString(paramString, paramHttpServletRequest);
    String[] arrayOfString = paramHttpServletRequest.getParameterValues(paramString);
    if (arrayOfString == null)
      return "";
    return arrayOfString[paramInt];
  }

  public static int getQueryStringValueCount(String paramString, HttpServletRequest paramHttpServletRequest)
  {
    if (paramString == null)
      return 0;
    String[] arrayOfString = paramHttpServletRequest.getParameterValues(paramString);
    if (arrayOfString == null)
      return 0;
    return arrayOfString.length;
  }

  public static int getFormElementCount(HttpServletRequest paramHttpServletRequest)
  {
    return getQueryStringCount(paramHttpServletRequest);
  }

  /** @deprecated */
  public static String getFormElement(int paramInt, HttpServletRequest paramHttpServletRequest)
  {
    return getQueryName(paramInt, paramHttpServletRequest);
  }

  public static String getFormElementName(int paramInt, HttpServletRequest paramHttpServletRequest)
  {
    return getFormElementName(paramInt, paramHttpServletRequest);
  }

  public static String getFormElement(String paramString, HttpServletRequest paramHttpServletRequest)
  {
    return getQueryString(paramString, paramHttpServletRequest);
  }

  public static String getFormElement(String paramString, int paramInt, HttpServletRequest paramHttpServletRequest)
  {
    return getQueryString(paramString, paramInt, paramHttpServletRequest);
  }

  public static int getFormElementCount(String paramString, HttpServletRequest paramHttpServletRequest)
  {
    return getQueryStringValueCount(paramString, paramHttpServletRequest);
  }

  public static int getVariableCount(HttpServletRequest paramHttpServletRequest)
  {
    if (variables == null)
      initVariables();
    return variables.size();
  }

  /** @deprecated */
  public static String getVariable(int paramInt, HttpServletRequest paramHttpServletRequest)
  {
    return getQueryName(paramInt, paramHttpServletRequest);
  }

  public static String getVariableName(int paramInt, HttpServletRequest paramHttpServletRequest)
  {
    if (variables == null)
      initVariables();
    return (String)variables.elementAt(paramInt);
  }

  public static String getVariable(String paramString, HttpServletRequest paramHttpServletRequest)
  {
    return getVariable(paramString, -1, paramHttpServletRequest);
  }

  public static String getVariable(String paramString, int paramInt, HttpServletRequest paramHttpServletRequest)
  {
    String str1 = paramString.toString().toUpperCase();
    if (str1.startsWith("HTTP_"))
      return paramHttpServletRequest.getHeader(str1.substring(5).replace('_', '-'));
    if ((str1.equals("ALL_HTTP")) || (str1.equals("ALL_RAW")))
    {
      int i = 0;
      if (str1.equals("ALL_HTTP"))
        i = 1;
      StringBuffer localStringBuffer = new StringBuffer();
      Enumeration localEnumeration = paramHttpServletRequest.getHeaderNames();
      while (localEnumeration.hasMoreElements())
      {
        String str2 = localEnumeration.nextElement().toString();
        if (i != 0)
        {
          localStringBuffer.append("HTTP_");
          localStringBuffer.append(str2.replace('-', '_').toUpperCase());
        }
        else
        {
          localStringBuffer.append(str2);
        }
        localStringBuffer.append(":");
        localStringBuffer.append(paramHttpServletRequest.getHeader(str2));
        localStringBuffer.append(" ");
      }
      return localStringBuffer.toString();
    }
    if (str1.equals("URL"))
      return paramHttpServletRequest.getRequestURI();
    if (str1.indexOf('_') > 0)
    {
      if (str1.equals("AUTH_TYPE"))
        return paramHttpServletRequest.getAuthType();
      if (str1.equals("CONTENT_LENGTH"))
        return String.valueOf(paramHttpServletRequest.getContentLength());
      if (str1.equals("CONTENT_TYPE"))
        return paramHttpServletRequest.getContentType();
      if (str1.equals("PATH_INFO"))
        return paramHttpServletRequest.getPathInfo();
      if (str1.equals("PATH_TRANSLATED"))
        return paramHttpServletRequest.getPathTranslated();
      if (str1.equals("QUERY_STRING"))
        return paramHttpServletRequest.getQueryString();
      if (str1.equals("REMOTE_ADDR"))
        return paramHttpServletRequest.getRemoteAddr();
      if (str1.equals("REMOTE_HOST"))
        return paramHttpServletRequest.getRemoteHost();
      if (str1.equals("REMOTE_USER"))
        return paramHttpServletRequest.getRemoteUser();
      if (str1.equals("REQUEST_METHOD"))
        return paramHttpServletRequest.getMethod();
      if (str1.equals("SCRIPT_NAME"))
        return paramHttpServletRequest.getServletPath();
      if (str1.equals("SERVER_NAME"))
        return paramHttpServletRequest.getServerName();
      if (str1.equals("SERVER_PORT"))
        return String.valueOf(paramHttpServletRequest.getServerPort());
      if (str1.equals("SERVER_PROTOCOL"))
        return paramHttpServletRequest.getProtocol();
      if (str1.equals("GATEWAY_INTERFACE"))
        return "CGI/1.1";
    }
    return getQueryString(paramString, paramInt, paramHttpServletRequest);
  }

  private static void initVariables()
  {
    variables = new Vector(50);
    variables.addElement("ALL_HTTP");
    variables.addElement("ALL_RAW");
    variables.addElement("APPL_MD_PATH");
    variables.addElement("APPL_PHYSICAL_PATH");
    variables.addElement("AUTH_PASSWORD");
    variables.addElement("AUTH_TYPE");
    variables.addElement("AUTH_USER");
    variables.addElement("CERT_COOKIE");
    variables.addElement("CERT_FLAGS");
    variables.addElement("CERT_ISSUER");
    variables.addElement("CERT_KEYSIZE");
    variables.addElement("CERT_SECRETKEYSIZE");
    variables.addElement("CERT_SERIALNUMBER");
    variables.addElement("CERT_SERVER_ISSUER");
    variables.addElement("CERT_SERVER_SUBJECT");
    variables.addElement("CERT_SUBJECT");
    variables.addElement("CONTENT_LENGTH");
    variables.addElement("CONTENT_TYPE");
    variables.addElement("GATEWAY_INTERFACE");
    variables.addElement("HTTPS");
    variables.addElement("HTTPS_KEYSIZE");
    variables.addElement("HTTPS_SECRETKEYSIZE");
    variables.addElement("HTTPS_SERVER_ISSUER");
    variables.addElement("HTTPS_SERVER_SUBJECT");
    variables.addElement("INSTANCE_ID");
    variables.addElement("INSTANCE_META_PATH");
    variables.addElement("LOCAL_ADDR");
    variables.addElement("LOGON_USER");
    variables.addElement("PATH_INFO");
    variables.addElement("PATH_TRANSLATED");
    variables.addElement("QUERY_STRING");
    variables.addElement("REMOTE_ADDR");
    variables.addElement("REMOTE_HOST");
    variables.addElement("REMOTE_USER");
    variables.addElement("REQUEST_METHOD");
    variables.addElement("SCRIPT_NAME");
    variables.addElement("SERVER_NAME");
    variables.addElement("SERVER_PORT");
    variables.addElement("SERVER_PORT_SECURE");
    variables.addElement("SERVER_PROTOCOL");
    variables.addElement("SERVER_SOFTWARE");
    variables.addElement("URL");
    variables.addElement("HTTP_ACCEPT");
    variables.addElement("HTTP_ACCEPT_LANGUAGE");
    variables.addElement("HTTP_CONNECTION");
    variables.addElement("HTTP_HOST");
    variables.addElement("HTTP_REFERER");
    variables.addElement("HTTP_USER_AGENT");
    variables.addElement("HTTP_COOKIE");
    variables.addElement("HTTP_ACCEPT_ENCODING");
    variables.addElement("HTTP_ACCEPT_CHARSET");
  }
}