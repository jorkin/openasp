package com.zfbots.asp;

import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.zfbots.util.Variant;


public class AspSession
{
  public static int getValueCount(HttpServletRequest paramHttpServletRequest)
  {
	  int i = 0;
    Enumeration localEnumeration = paramHttpServletRequest.getSession().getAttributeNames();
    for (i = 0; localEnumeration.hasMoreElements(); i++)
      localEnumeration.nextElement();
    return i;
  }

  public static String getValue(int paramInt, HttpServletRequest paramHttpServletRequest)
  {
    Enumeration localEnumeration = paramHttpServletRequest.getSession().getAttributeNames();
    for (int i = 0; localEnumeration.hasMoreElements(); i++)
    {
      String str = localEnumeration.nextElement().toString();
      if (i == paramInt)
        return str;
    }
    return "";
  }

  public static Variant getValue(String paramString, HttpServletRequest paramHttpServletRequest)
  {
    return new Variant(paramHttpServletRequest.getSession().getAttribute(paramString));
  }
}