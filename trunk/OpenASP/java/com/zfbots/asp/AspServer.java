package com.zfbots.asp;

import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AspServer
{
  public static void Execute(String paramString, HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse)
    throws ServletException, IOException
  {
    RequestDispatcher localRequestDispatcher = paramHttpServletRequest.getRequestDispatcher(paramString);
    localRequestDispatcher.include(paramHttpServletRequest, paramHttpServletResponse);
  }

  public static void Transfer(String paramString, HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse)
    throws ServletException, IOException
  {
    RequestDispatcher localRequestDispatcher = paramHttpServletRequest.getRequestDispatcher(paramString);
    localRequestDispatcher.forward(paramHttpServletRequest, paramHttpServletResponse);
  }
}