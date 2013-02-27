package com.zfbots.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class OutputFile extends FileOutputStream
{
  private static final int hashChar = 35;
  private static final int quoteChar = 34;
  private static final int commaChar = 44;
  private static final int spaceChar = 32;
  private char[] buff = new char[256];
  private int col = 1;
  private int filelen = 0;

  public OutputFile(String paramString)
    throws FileNotFoundException
  {
    super(paramString);
  }

  public OutputFile(String paramString, boolean paramBoolean)
    throws FileNotFoundException
  {
    super(paramString, paramBoolean);
  }

  public void println()
    throws IOException
  {
    write(13);
    write(10);
  }

  public void print(int paramInt)
    throws IOException
  {
    writeString(Integer.toString(paramInt));
  }

  public void print(double paramDouble)
    throws IOException
  {
    writeString(Double.toString(paramDouble));
  }

  public void print(String paramString)
    throws IOException
  {
    writeString(paramString);
  }

  public void print(boolean paramBoolean)
    throws IOException
  {
    writeString(paramBoolean ? "True" : "False");
  }

  public void print(Variant paramVariant)
    throws IOException
  {
    writeString(paramVariant.toString());
  }

  public void writeValue(int paramInt)
    throws IOException
  {
    writeDelimeted(Integer.toString(paramInt), 0);
  }

  public void writeValue(double paramDouble)
    throws IOException
  {
    writeDelimeted(Double.toString(paramDouble), 0);
  }

  public void writeValue(String paramString)
    throws IOException
  {
    writeDelimeted(paramString, 34);
  }

  public void writeValue(boolean paramBoolean)
    throws IOException
  {
    writeDelimeted(paramBoolean ? "True" : "False", 35);
  }

  public void writeValue(Variant paramVariant)
    throws IOException
  {
    if (paramVariant.isNumeric())
    {
      writeDelimeted(paramVariant.toString(), 0);
    }
    else
    {
      int i = paramVariant.getVarType();
      int j = 35;
      if (i == 15)
        j = 34;
      writeDelimeted(paramVariant.toString(), j);
    }
  }

  public void spc(int paramInt)
    throws IOException
  {
    for (int i = 0; i < paramInt; i++)
      write(32);
  }

  public void tab()
    throws IOException
  {
    int i = 14 - (this.col - 1) % 14;
    spc(i);
  }

  public void tab(int paramInt)
    throws IOException
  {
    if (paramInt < 1)
      paramInt = 1;
    if (paramInt < this.col)
    {
      println();
      spc(paramInt);
    }
    else
    {
      spc(paramInt - this.col);
    }
  }

  private void writeDelimeted(String paramString, int paramInt)
    throws IOException
  {
    if (this.col != 1)
      write(44);
    if (paramInt != 0)
      write(paramInt);
    writeString(paramString);
    if (paramInt != 0)
      write(paramInt);
  }

  private void writeString(String paramString)
    throws IOException
  {
    int i = paramString.length();
    char[] arrayOfChar;
    if (i < 256)
    {
      arrayOfChar = this.buff;
      paramString.getChars(0, i, arrayOfChar, 0);
    }
    else
    {
      arrayOfChar = paramString.toCharArray();
    }
    int j = 0;
    for (int k = 0; k < i; k++)
    {
      if ((arrayOfChar[k] & 0xFF00) == 0)
        continue;
      j = 1;
    }
    for (int k = 0; k < i; k++)
    {
      int m = arrayOfChar[k];
      if (j != 0)
        write(m >>> 8 & 0xFF);
      write(m & 0xFF);
    }
  }

  public void write(int paramInt)
    throws IOException
  {
    super.write(paramInt);
    this.filelen += 1;
    if (paramInt != 10)
      this.col += 1;
    if (paramInt == 13)
      this.col = 1;
  }

  public int length()
  {
    return this.filelen;
  }
}