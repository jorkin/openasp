package com.zfbots.ado;

import java.util.StringTokenizer;

public class QualifiedTableName
{
  String catalogName = null;
  String ownerName = null;
  String tableName = null;
  String alias = null;
  String extendedTableName = null;
  String idQuoteStr = null;
  String catSeparator = null;
  int idCaseProp = 0;
  boolean isCatAtStart = false;

  public QualifiedTableName(String paramString1, String paramString2, String paramString3, int paramInt, boolean paramBoolean1, boolean paramBoolean2)
    throws AdoError
  {
    this.idQuoteStr = paramString2;
    this.catSeparator = paramString3;
    this.isCatAtStart = paramBoolean1;
    this.idCaseProp = paramInt;
    if (paramString1.indexOf("[") >= 0)
    {
      char c = '"';
      if ((paramString2 != null) && (paramString2.length() > 0))
        c = paramString2.charAt(0);
      paramString1 = paramString1.replace('[', c);
      paramString1 = paramString1.replace(']', c);
    }
    String str = paramString1;
    if (paramBoolean2)
      str = getStringWithoutAlias(paramString1);
    if (str.length() == 0)
      throw new AdoError("Missing table name in select statement.", 70032);
    int i = str.indexOf(paramString3);
    if (i != -1)
    {
      int j = str.indexOf(paramString3, i + 1);
      if (j != -1)
      {
        this.catalogName = getNameWithoutQuotes(str.substring(0, i));
        if (i + 1 < j)
          this.ownerName = getNameWithoutQuotes(str.substring(i + 1, j));
        else
          this.ownerName = null;
        if (str.length() > j + 1)
          this.tableName = getNameWithoutQuotes(str.substring(j + 1));
        else
          this.tableName = "";
      }
      else
      {
        this.ownerName = getNameWithoutQuotes(str.substring(0, i));
        this.tableName = getNameWithoutQuotes(str.substring(i + 1));
      }
    }
    else
    {
      this.tableName = getNameWithoutQuotes(str);
    }
    setExtendedTableName();
  }

  public String getCatalogName()
  {
    return this.catalogName;
  }

  public String getOwnerName()
  {
    return this.ownerName;
  }

  public String getTableName()
  {
    return this.tableName;
  }

  public String getExtendedTableName()
  {
    return this.extendedTableName;
  }

  public String getStringWithoutAlias(String paramString)
  {
    String str1 = this.idQuoteStr + " \r\n\t";
    paramString = paramString.trim();
    StringTokenizer localStringTokenizer = new StringTokenizer(paramString, str1, true);
    int i = 0;
    int j = 0;
    String str3 = "";
    while (localStringTokenizer.hasMoreTokens())
    {
      String str2 = localStringTokenizer.nextToken();
      int k = str2.charAt(0);
      if ((i == 0) && ((k == 32) || (k == 10) || (k == 13) || (k == 9)))
      {
        j = 1;
        continue;
      }
      if (j != 0)
      {
        this.alias = str2;
        continue;
      }
      if (str2.equals(this.idQuoteStr))
        i = i == 0 ? 1 : 0;
      str3 = str3 + str2;
    }
    return str3;
  }

  private String getTokenWithCorrectCase(String paramString, int paramInt)
  {
    switch (paramInt)
    {
    case 2:
      paramString = paramString.toLowerCase();
      break;
    case 1:
      paramString = paramString.toUpperCase();
      break;
    case 3:
      break;
    case 4:
    }
    return paramString;
  }

  String getNameWithoutQuotes(String paramString)
  {
    String str = paramString;
    if ((this.idQuoteStr != null) && (this.idQuoteStr.length() > 0))
    {
      int i = 0;
      int j = 0;
      int k = paramString.length();
      int m = this.idQuoteStr.charAt(0);
      int n = paramString.charAt(0);
      if (n == m)
        i = 1;
      n = paramString.charAt(k - 1);
      if (n == m)
        j = 1;
      if ((i != 0) && (j != 0))
        str = paramString.substring(1, k - 1);
      else
        str = getTokenWithCorrectCase(paramString, this.idCaseProp);
    }
    return str;
  }

  void setExtendedTableName()
  {
    this.extendedTableName = "";
    if ((this.catalogName != null) && (this.catalogName.length() > 0))
    {
      this.extendedTableName += this.idQuoteStr;
      this.extendedTableName += this.catalogName;
      this.extendedTableName += this.idQuoteStr;
      this.extendedTableName += this.catSeparator;
    }
    if ((this.ownerName != null) && (this.ownerName.length() > 0))
    {
      this.extendedTableName += this.idQuoteStr;
      this.extendedTableName += this.ownerName;
      this.extendedTableName += this.idQuoteStr;
      this.extendedTableName += this.catSeparator;
    }
    else if ((this.catalogName != null) && (this.catalogName.length() > 0))
    {
      this.extendedTableName += this.catSeparator;
    }
    this.extendedTableName += this.idQuoteStr;
    this.extendedTableName += this.tableName;
    this.extendedTableName += this.idQuoteStr;
  }
}