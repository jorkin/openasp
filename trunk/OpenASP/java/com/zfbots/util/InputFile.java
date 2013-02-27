package com.zfbots.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.StringTokenizer;

public class InputFile extends RandomAccessFile
{
  private static final int hashChar = 35;
  private static final int quoteChar = 34;
  private StringTokenizer tok = null;

  public InputFile(String paramString)
    throws FileNotFoundException
  {
    super(paramString, "r");
  }

  private String nextToken()
    throws IOException
  {
    if (this.tok == null)
      this.tok = new StringTokenizer(readLine(), ",");
    if (this.tok.hasMoreTokens())
      return this.tok.nextToken();
    this.tok = null;
    return nextToken();
  }

  public Variant input()
    throws IOException
  {
    String str = nextToken().trim();
    if (str.charAt(0) == '"')
      return new Variant(str.substring(1, str.length() - 1));
    if (str.charAt(0) == '#')
    {
      str = str.substring(1, str.length() - 1);
      if (str.toLowerCase().equals("true"))
        return new Variant(true);
      if (str.toLowerCase().equals("false"))
        return new Variant(false);
      if (str.toLowerCase().equals("null"))
        return new Variant().setNull();
      try
      {
        return new Variant(DateTime.toDate(str));
      }
      catch (Exception localException)
      {
        throw new IOException(localException + "\n" + "Error: parsing string: " + str);
      }
    }
    return new Variant(str);
  }

  public String inputLine()
    throws IOException
  {
    return readLine();
  }

  public String input(int paramInt)
    throws IOException
  {
    byte[] arrayOfByte = new byte[paramInt];
    read(arrayOfByte, 0, paramInt);
    return new String(arrayOfByte);
  }
}