package com.zfbots.util;

import java.util.Enumeration;

public class StringSplitter
  implements Enumeration
{
  private int currentPosition = 0;
  private int newPosition = -1;
  private int maxPosition;
  private String str;
  private String delimiter;
  private boolean delimChanged = false;
  private int delimLen;

  public StringSplitter(String paramString1, String paramString2)
  {
    this.str = paramString1;
    this.maxPosition = paramString1.length();
    this.delimiter = paramString2;
    this.delimLen = paramString2.length();
  }

  public StringSplitter(String paramString)
  {
    this(paramString, " ");
  }

  private int skipDelimiters(int paramInt)
  {
    if (this.delimiter == null)
      throw new NullPointerException();
    int i = paramInt;
    int j = 0;
    for (j = 0; (j < this.delimLen) && (i + j < this.maxPosition) && (this.str.charAt(i + j) == this.delimiter.charAt(j)); j++);
    if (j == this.delimLen)
      i += this.delimLen;
    return i;
  }

  private int scanToken(int paramInt)
  {
    int i = paramInt;
    int j = this.delimLen > 0 ? this.delimiter.charAt(0) : 0;
    while (i < this.maxPosition)
    {
      int k = this.str.charAt(i);
      if (k == j)
      {
        int m = 0;
        for (int n = 1; (n < this.delimLen) && (i + n < this.maxPosition); n++)
        {
          if (this.str.charAt(i + n) == this.delimiter.charAt(n))
            continue;
          m = 1;
          break;
        }
        if (m == 0)
          break;
      }
      i++;
    }
    return i;
  }

  public boolean hasMoreTokens()
  {
    this.newPosition = skipDelimiters(this.currentPosition);
    return (this.newPosition < this.maxPosition) || (this.newPosition > this.currentPosition);
  }

  public String nextToken()
  {
    this.currentPosition = ((this.newPosition >= 0) && (!this.delimChanged) ? this.newPosition : skipDelimiters(this.currentPosition));
    this.delimChanged = false;
    this.newPosition = -1;
    if (this.currentPosition >= this.maxPosition)
      return "";
    int i = this.currentPosition;
    this.currentPosition = scanToken(this.currentPosition);
    return this.str.substring(i, this.currentPosition);
  }

  public String nextToken(String paramString)
  {
    this.delimiter = paramString;
    this.delimLen = paramString.length();
    this.delimChanged = true;
    return nextToken();
  }

  public boolean hasMoreElements()
  {
    return hasMoreTokens();
  }

  public Object nextElement()
  {
    return nextToken();
  }

  public int countTokens()
  {
    int i = 0;
    int j = this.currentPosition;
    while (j < this.maxPosition)
    {
      j = skipDelimiters(j);
      if (j >= this.maxPosition)
        break;
      j = scanToken(j);
      i++;
    }
    return i;
  }
}