package com.zfbots.util;

public class MoreMath
{
  public static double log(double paramDouble1, double paramDouble2)
  {
    if (paramDouble2 <= 0.0D)
      paramDouble2 = 10.0D;
    double d = Math.log(paramDouble1) / Math.log(paramDouble2);
    if (d == 0.0D)
      return 0.0D;
    return d;
  }

  public static double log10(double paramDouble)
  {
    return Math.log(paramDouble) / Math.log(10.0D);
  }

  public static double acosh(double paramDouble)
  {
    return Math.log(paramDouble + Math.sqrt(paramDouble * paramDouble - 1.0D));
  }

  public static double asinh(double paramDouble)
  {
    return Math.log(paramDouble + Math.sqrt(paramDouble * paramDouble + 1.0D));
  }

  public static double atanh(double paramDouble)
  {
    return Math.log(Math.sqrt((1.0D + paramDouble) / (1.0D - paramDouble)));
  }

  public static double cosh(double paramDouble)
  {
    return (Math.exp(paramDouble) + Math.exp(-paramDouble)) / 2.0D;
  }

  public static double sinh(double paramDouble)
  {
    return (Math.exp(paramDouble) - Math.exp(-paramDouble)) / 2.0D;
  }

  public static double tanh(double paramDouble)
  {
    double d1 = Math.exp(paramDouble);
    double d2 = Math.exp(-paramDouble);
    return (d1 - d2) / (d1 + d2);
  }

  public static int factorial(int paramInt)
  {
    int i = 0;
    if (paramInt <= 0)
      i = 1;
    else
      i = paramInt * factorial(paramInt - 1);
    return i;
  }

  public static int permutations(int paramInt1, int paramInt2)
  {
    return factorial(paramInt1) / factorial(paramInt1 - paramInt2);
  }

  public static int combinations(int paramInt1, int paramInt2)
  {
    return factorial(paramInt1) / (factorial(paramInt1 - paramInt2) * factorial(paramInt2));
  }

  public static int mod(int paramInt1, int paramInt2)
  {
    return Math.abs(paramInt1 % paramInt2);
  }

  public static int quotient(int paramInt1, int paramInt2)
  {
    return paramInt1 % paramInt2;
  }

  public static int random(int paramInt1, int paramInt2)
  {
    return (int)(Math.random() * (paramInt2 - paramInt1 + 1)) + paramInt1;
  }

  public static double ceil(double paramDouble1, double paramDouble2)
  {
    return Math.ceil(paramDouble1 / paramDouble2) * paramDouble2;
  }

  public static double floor(double paramDouble1, double paramDouble2)
  {
    return Math.floor(paramDouble1 / paramDouble2) * paramDouble2;
  }

  public static double round(double paramDouble1, double paramDouble2)
  {
    if (paramDouble1 < 0.0D)
    {
      if (paramDouble2 == 0.0D)
        return Math.ceil(paramDouble1 - 0.5D);
      double d = Math.pow(10.0D, paramDouble2);
      return Math.ceil(paramDouble1 * d - 0.5D) / d;
    }
    if (paramDouble2 == 0.0D)
      return Math.floor(paramDouble1 + 0.5D);
    double d = Math.pow(10.0D, paramDouble2);
    return Math.floor(paramDouble1 * d + 0.5D) / d;
  }

  public static double roundUp(double paramDouble1, double paramDouble2)
  {
    if (paramDouble1 < 0.0D)
    {
      if (paramDouble2 == 0.0D)
        return Math.floor(paramDouble1);
      double d = Math.pow(10.0D, paramDouble2);
      return Math.floor(paramDouble1 * d) / d;
    }
    if (paramDouble2 == 0.0D)
      return Math.ceil(paramDouble1);
    double d = Math.pow(10.0D, paramDouble2);
    return Math.ceil(paramDouble1 * d) / d;
  }

  public static double roundDown(double paramDouble1, double paramDouble2)
  {
    if (paramDouble1 < 0.0D)
    {
      if (paramDouble2 == 0.0D)
        return Math.ceil(paramDouble1);
      double d = Math.pow(10.0D, paramDouble2);
      return Math.ceil(paramDouble1 * d) / d;
    }
    if (paramDouble2 == 0.0D)
      return Math.floor(paramDouble1);
    double d = Math.pow(10.0D, paramDouble2);
    return Math.floor(paramDouble1 * d) / d;
  }

  public static double odd(double paramDouble)
  {
    double d = Math.ceil(paramDouble);
    if (d % 2.0D == 0.0D)
      d += 1.0D;
    return d;
  }

  public static double even(double paramDouble)
  {
    double d = Math.ceil(paramDouble);
    d += d % 2.0D;
    return d;
  }

  public static int sign(double paramDouble)
  {
    return paramDouble > 0.0D ? 1 : paramDouble == 0.0D ? 0 : -1;
  }
}