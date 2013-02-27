package com.zfbots.asp;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.asp.AspWriter;

public class Asp
{
  private static ThreadLocal out = new ThreadLocal();
  private static ThreadLocal response = new ThreadLocal();
  private static ThreadLocal request = new ThreadLocal();

  public static void init(HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse, AspWriter paramJspWriter)
  {
    request.set(paramHttpServletRequest);
    response.set(paramHttpServletResponse);
    out.set(paramJspWriter);
  }

  public static HttpServletRequest request()
  {
    return (HttpServletRequest)request.get();
  }

  public static HttpServletResponse response()
  {
    return (HttpServletResponse)response.get();
  }

  public static AspWriter out()
  {
    return (AspWriter)out.get();
  }
}