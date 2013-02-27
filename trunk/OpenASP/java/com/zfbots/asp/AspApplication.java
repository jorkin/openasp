package com.zfbots.asp;

import javax.servlet.http.HttpServletRequest;

import com.zfbots.util.HashList;
import com.zfbots.util.Variant;


public class AspApplication
{
  private static Object locked = null;
  private static HashList values = new HashList();

  public static int getVariableCount()
  {
    return values.size();
  }

  public static Variant getVariable(String paramString)
  {
    Object localObject = values.get(paramString);
    return localObject == null ? new Variant("") : new Variant(localObject);
  }

  public static Variant getVariable(int paramInt)
  {
    Object localObject = values.get(paramInt);
    return localObject == null ? new Variant("") : new Variant(localObject);
  }

  public static void setVariable(String paramString, Variant paramVariant, HttpServletRequest paramHttpServletRequest)
  {
    if ((locked == null) || (paramHttpServletRequest.getSession() == locked))
      values.add(paramVariant == null ? null : paramVariant.toObject(), paramString);
  }

  public static void removeVariable(String paramString, HttpServletRequest paramHttpServletRequest)
  {
    if ((locked == null) || (paramHttpServletRequest.getSession() == locked))
      values.remove(paramString);
  }

  public static void removeAllVariables(HttpServletRequest paramHttpServletRequest)
  {
    if ((locked == null) || (paramHttpServletRequest.getSession() == locked))
      values.clear();
  }

  public static void Lock(HttpServletRequest paramHttpServletRequest)
  {
    locked = paramHttpServletRequest.getSession();
  }

  public static void UnLock(HttpServletRequest paramHttpServletRequest)
  {
    if (paramHttpServletRequest.getSession() == locked)
      locked = null;
  }
}