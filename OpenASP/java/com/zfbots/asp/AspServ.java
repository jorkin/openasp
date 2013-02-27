package com.zfbots.asp;

import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AspServ
{
  private static ThreadLocal out = new ThreadLocal();
  private static ThreadLocal response = new ThreadLocal();
  private static ThreadLocal request = new ThreadLocal();

  public static void init(HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse, PrintWriter paramPrintWriter)
  {
    request.set(paramHttpServletRequest);
    response.set(paramHttpServletResponse);
    out.set(paramPrintWriter);
  }

  public static HttpServletRequest request()
  {
    return (HttpServletRequest)request.get();
  }

  public static HttpServletResponse response()
  {
    return (HttpServletResponse)response.get();
  }

  public static PrintWriter out()
  {
    return (PrintWriter)out.get();
  }
}