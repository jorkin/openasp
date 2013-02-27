package com.zfbots.util;

import java.util.Arrays;

public class Statistics
{
  public static double sum(double[] paramArrayOfDouble)
  {
    if (paramArrayOfDouble.length == 0)
      return 0.0D;
    double d = 0.0D;
    for (int i = 0; i < paramArrayOfDouble.length; i++)
      d += paramArrayOfDouble[i];
    return d;
  }

  public static double sumSquares(double[] paramArrayOfDouble)
  {
    if (paramArrayOfDouble.length == 0)
      return 0.0D;
    double d = 0.0D;
    for (int i = 0; i < paramArrayOfDouble.length; i++)
      d += paramArrayOfDouble[i] * paramArrayOfDouble[i];
    return d;
  }

  public static double product(double[] paramArrayOfDouble)
  {
    if (paramArrayOfDouble.length == 0)
      return 0.0D;
    double d = 1.0D;
    for (int i = 0; i < paramArrayOfDouble.length; i++)
      d *= paramArrayOfDouble[i];
    return d;
  }

  public static double sumProduct(double[][] paramArrayOfDouble)
  {
    if (paramArrayOfDouble.length == 0)
      return 0.0D;
    int i = paramArrayOfDouble.length;
    int j = paramArrayOfDouble[0].length;
    double d1 = 0.0D;
    for (int k = 0; k < j; k++)
    {
      double d2 = 0.0D;
      for (int m = 0; m < i; m++)
        if (m == 0)
          d2 = paramArrayOfDouble[m][k];
        else
          d2 *= paramArrayOfDouble[m][k];
      d1 += d2;
    }
    return d1;
  }

  public static double min(double[] paramArrayOfDouble)
  {
    if (paramArrayOfDouble.length == 0)
      return 0.0D;
    double d = 1.7976931348623157E+308D;
    for (int i = 0; i < paramArrayOfDouble.length; i++)
    {
      if (paramArrayOfDouble[i] >= d)
        continue;
      d = paramArrayOfDouble[i];
    }
    return d;
  }

  public static double max(double[] paramArrayOfDouble)
  {
    if (paramArrayOfDouble.length == 0)
      return 0.0D;
    double d = 4.9E-324D;
    for (int i = 0; i < paramArrayOfDouble.length; i++)
    {
      if (paramArrayOfDouble[i] <= d)
        continue;
      d = paramArrayOfDouble[i];
    }
    return d;
  }

  public static double average(double[] paramArrayOfDouble)
  {
    return sum(paramArrayOfDouble) / paramArrayOfDouble.length;
  }

  public static double median(double[] paramArrayOfDouble)
  {
    if (paramArrayOfDouble.length == 0)
      return 0.0D;
    Arrays.sort(paramArrayOfDouble);
    if (paramArrayOfDouble.length % 2 == 0)
      return (paramArrayOfDouble[(paramArrayOfDouble.length / 2 - 1)] + paramArrayOfDouble[(paramArrayOfDouble.length / 2)]) / 2.0D;
    return paramArrayOfDouble[(paramArrayOfDouble.length / 2)];
  }

  public static int rank(double paramDouble, double[] paramArrayOfDouble, int paramInt)
  {
    if (paramArrayOfDouble.length == 0)
      return 0;
    Arrays.sort(paramArrayOfDouble);
    if (paramInt > 0)
      for (int i = 0; i < paramArrayOfDouble.length; i++)
        if (paramDouble == paramArrayOfDouble[i])
          return i + 1;
    for (int i = paramArrayOfDouble.length - 1; i >= 0; i--)
      if (paramDouble == paramArrayOfDouble[i])
        return i + 1;
    return 0;
  }

  public static double stdev(double[] paramArrayOfDouble)
  {
    return Math.sqrt(var(paramArrayOfDouble));
  }

  public static double stdevp(double[] paramArrayOfDouble)
  {
    return Math.sqrt(varp(paramArrayOfDouble));
  }

  public static double var(double[] paramArrayOfDouble)
  {
    double d1 = sum(paramArrayOfDouble);
    double d2 = sumSquares(paramArrayOfDouble);
    int i = paramArrayOfDouble.length;
    return (i * d2 - d1 * d1) / (i * (i - 1));
  }

  public static double varp(double[] paramArrayOfDouble)
  {
    double d1 = sum(paramArrayOfDouble);
    double d2 = sumSquares(paramArrayOfDouble);
    int i = paramArrayOfDouble.length;
    return (i * d2 - d1 * d1) / (i * i);
  }
}