package com.zfbots.util;

public class Financial
{
  public static double sln(double paramDouble1, double paramDouble2, double paramDouble3)
  {
    return (paramDouble1 - paramDouble2) / paramDouble3;
  }

  public static double syd(double paramDouble1, double paramDouble2, double paramDouble3, double paramDouble4)
  {
    double d = paramDouble3 * (paramDouble3 + 1.0D) / 2.0D;
    return (paramDouble1 - paramDouble2) * (paramDouble3 - paramDouble4 + 1.0D) / d;
  }

  public static double ddb(double paramDouble1, double paramDouble2, double paramDouble3, double paramDouble4, double paramDouble5)
  {
    if (paramDouble5 <= 0.0D)
      paramDouble5 = 2.0D;
    if (paramDouble4 >= paramDouble3)
      return 0.0D;
    double d1 = 0.0D;
    for (int i = 1; i <= paramDouble4 - 1.0D; i++)
      d1 += (paramDouble1 - d1) * paramDouble5 / paramDouble3;
    double d2 = (paramDouble1 - d1) * paramDouble5 / paramDouble3;
    if (d2 + d1 > paramDouble1 - paramDouble2)
      d2 = paramDouble1 - paramDouble2 - d1;
    if (d2 < 0.0D)
      d2 = 0.0D;
    return d2;
  }

  public static double db(double paramDouble1, double paramDouble2, double paramDouble3, int paramInt1, int paramInt2)
  {
    double d1 = 1.0D - Math.pow(paramDouble2 / paramDouble1, 1.0D / paramDouble3);
    d1 = MoreMath.round(d1, 3.0D);
    if (paramInt2 <= 0)
      paramInt2 = 12;
    int i = (int)paramDouble3;
    if (paramInt2 < 12)
      i++;
    if (paramInt1 > i)
      return 0.0D;
    double d2 = paramDouble1 * d1 * paramInt2 / 12.0D;
    if (paramInt1 == 1)
      return d2;
    double d3 = d2;
    for (int j = 2; j <= paramInt1 - 1; j++)
      d3 += (paramDouble1 - d3) * d1;
    if (paramInt1 == i)
      return (paramDouble1 - d3) * d1 * (12 - paramInt2) / 12.0D;
    double d4 = (paramDouble1 - d3) * d1;
    if (d4 + d3 > paramDouble1 - paramDouble2)
      d4 = paramDouble1 - paramDouble2 - d3;
    if (d4 < 0.0D)
      d4 = 0.0D;
    return d4;
  }

  public static double pmt(double paramDouble1, double paramDouble2, double paramDouble3, double paramDouble4, int paramInt)
  {
    double d1 = Math.pow(1.0D + paramDouble1, paramDouble2);
    double d2 = paramDouble3 * paramDouble1 * d1 / (d1 - 1.0D);
    d2 += paramDouble4 * paramDouble1 / (d1 - 1.0D);
    if (paramInt > 0)
      d2 /= (1.0D + paramDouble1);
    return -d2;
  }

  public static double pv(double paramDouble1, double paramDouble2, double paramDouble3, double paramDouble4, int paramInt)
  {
    double d1 = Math.pow(1.0D + paramDouble1, paramDouble2);
    double d2 = paramInt > 0 ? paramDouble1 * (d1 - 1.0D) : 0.0D;
    double d3 = paramDouble3 * (d1 - 1.0D + d2) / paramDouble1 / d1;
    d3 += paramDouble4 / d1;
    return -d3;
  }

  public static double fv(double paramDouble1, double paramDouble2, double paramDouble3, double paramDouble4, int paramInt)
  {
    double d1 = Math.pow(1.0D + paramDouble1, paramDouble2);
    double d2 = paramDouble3 * (d1 - 1.0D) / paramDouble1;
    if (paramInt > 0)
      d2 *= (1.0D + paramDouble1);
    d2 += paramDouble4 * d1;
    return -d2;
  }

  public static double nper(double paramDouble1, double paramDouble2, double paramDouble3, double paramDouble4, int paramInt)
  {
    double d1 = paramDouble2 * (1.0D + paramDouble1 * paramInt) / paramDouble1;
    double d2 = Math.log((d1 - paramDouble4) / (d1 + paramDouble3)) / Math.log(1.0D + paramDouble1);
    return d2;
  }
}