package com.zfbots.ado;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import com.zfbots.util.Variant;


public class Parameters
{
  private Vector params;
  private Command ownerCommand;

  public Parameters()
  {
    this(null);
  }

  public Parameters(Command paramCommand)
  {
    this(3, paramCommand);
  }

  public Parameters(int paramInt, Command paramCommand)
  {
    this.params = new Vector(paramInt);
    this.ownerCommand = paramCommand;
  }

  public void append(Parameter paramParameter)
  {
    this.params.addElement(paramParameter);
  }

  public int getCount()
  {
    return this.params.size();
  }

  public void remove(Variant paramVariant)
    throws AdoError
  {
    remove(getParameter(paramVariant));
  }

  public Parameter getParameter(Variant paramVariant)
    throws AdoError
  {
    Parameter localParameter = null;
    if (paramVariant.getVarType() == 15)
      localParameter = getParameter(paramVariant.toString());
    else if (paramVariant.isNumeric())
      localParameter = getParameter(paramVariant.toInt());
    else if (paramVariant.getVarType() == 12)
      localParameter = (Parameter)paramVariant.toObject();
    else
      throw new AdoError("Unknown/Unhandled variant type for this operation: getItem", 90001);
    if (localParameter == null)
      throw new AdoError("Parameter not found in the collection. ", 40005);
    return localParameter;
  }

  public void refresh()
    throws AdoError
  {
    Connection localConnection = this.ownerCommand.getActiveConnection();
    if ((localConnection == null) || (!localConnection.isConnected()))
      throw new AdoError("You must have an open connection before calling: Refresh", 10009);
    String str1 = this.ownerCommand.getProcName();
    java.sql.Connection localConnection1 = localConnection.getConnection();
    try
    {
      DatabaseMetaData localDatabaseMetaData = localConnection1.getMetaData();
      String str2 = localConnection1.getCatalog();
      ResultSet localResultSet = localDatabaseMetaData.getProcedureColumns(str2, null, str1, null);
      int i = 1;
      int j = 1;
      while (localResultSet.next())
        switch (localResultSet.getInt("COLUMN_TYPE"))
        {
        case 1:
          j = 1;
          break;
        case 2:
          j = 3;
          break;
        case 4:
          j = 2;
          break;
        case 5:
          j = 4;
          break;
        case 3:
          break;
        case 0:
        default:
          throw new AdoError("Error extracting parameter information", 40006);
          //Parameter localParameter = new Parameter();
          //localParameter.setSqlType(localResultSet.getInt(6));
          //localParameter.setDirection(j);
          //append(localParameter);
        }
      localResultSet.close();
    }
    catch (SQLException localSQLException)
    {
      throw new AdoError(localSQLException, "Error extracting parameter information", 40006);
    }
  }

  public void remove(Parameter paramParameter)
    throws AdoError
  {
    if (this.params.removeElement(paramParameter) != true)
      throw new AdoError("Parameter not found in the collection. ", 40005);
  }

  public Parameter getParameter(int paramInt)
    throws AdoError
  {
    if (paramInt >= this.params.size())
      throw new AdoError("Parameter array index out of bounds, array size: " + this.params.size(), 40007);
    return (Parameter)this.params.elementAt(paramInt);
  }

  public Parameter getParameter(String paramString)
    throws AdoError
  {
    int i = this.params.size();
    for (int j = 0; j < i; j++)
    {
      Parameter localParameter = (Parameter)this.params.elementAt(j);
      if (paramString.equalsIgnoreCase(localParameter.getName()))
        return localParameter;
    }
    throw new AdoError("Parameter not found in the collection. " + paramString, 40005);
  }

  void setCommand(Command paramCommand)
  {
    this.ownerCommand = paramCommand;
  }
}