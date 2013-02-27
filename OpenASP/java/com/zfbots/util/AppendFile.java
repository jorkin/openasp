package com.zfbots.util;

import java.io.FileNotFoundException;

public class AppendFile extends OutputFile
{
  public AppendFile(String paramString)
    throws FileNotFoundException
  {
    super(paramString, true);
  }
}