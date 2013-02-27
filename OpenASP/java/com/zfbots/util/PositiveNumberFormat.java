package com.zfbots.util;

import java.text.DecimalFormat;
import java.text.ParseException;

public class PositiveNumberFormat extends DecimalFormat
{
  public PositiveNumberFormat()
  {
  }

  public PositiveNumberFormat(String paramString)
  {
    super(paramString);
  }

  public Object parseObject(String paramString)
    throws ParseException
  {
    Object localObject = super.parseObject(paramString);
    if (((localObject instanceof Number)) && (((Number)localObject).doubleValue() < 0.0D))
    {
      int i = paramString.indexOf('-');
      if (i < 0)
        i = 0;
      throw new ParseException("Must be a positive number: " + paramString, i);
    }
    return localObject;
  }
}