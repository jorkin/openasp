package com.zfbots.ado;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import com.zfbots.util.Variant;

import oracle.jdbc.driver.OracleConnection;
import oracle.sql.BLOB;
import oracle.sql.CLOB;

public class Command
{
  private String sqlString;
  private String bmQueryStr = "";
  private String commandText;
  int commandType = 8;
  private Connection ownerCon = null;
  private PreparedStatement prepStmt = null;
  private CallableStatement callStmt = null;
  private int queryTimeout = 30;
  private boolean isExecuting = false;
  private boolean prepareStmt = false;
  private boolean prepared = false;
  private boolean freeOwnerCon = false;
  private String sortOrderStr = "";
  private int stmtType = 2;
  private Parameters params;
  private Vector paramValues = null;
  private static Properties props = new Properties();

  public Command()
  {
    initialize();
  }

  public Connection getActiveConnection()
  {
    return this.ownerCon;
  }

  public void setActiveConnection(Connection paramConnection)
    throws AdoError
  {
    freeResources();
    this.ownerCon = paramConnection;
    this.freeOwnerCon = false;
  }

  public void setActiveConnection(String paramString)
    throws AdoError
  {
    freeResources();
    try
    {
      this.ownerCon = new Connection();
      this.ownerCon.setConnectionString(paramString);
      this.ownerCon.open();
      this.freeOwnerCon = true;
    }
    catch (Exception localException)
    {
      throw new AdoError(localException, "Error creating connection.", 10001);
    }
  }

  public String getCommandText()
  {
    return this.commandText;
  }

  public void setCommandText(String paramString)
    throws AdoError
  {
    if (this.isExecuting)
      throwCannotSetValues("setCommandText");
    this.commandText = paramString;
    this.sqlString = null;
  }

  public int getCommandTimeout()
  {
    return this.queryTimeout;
  }

  public void setCommandTimeout(int paramInt)
    throws AdoError
  {
    if (paramInt < 0)
      throw new AdoError(null, "Command timeout value should be greater than or equal to 0", 20001);
    this.queryTimeout = paramInt;
  }

  public int getCommandType()
  {
    return this.commandType;
  }

  public void setCommandType(int paramInt)
    throws AdoError
  {
    if (paramInt == 0)
      return;
    if (this.isExecuting)
      throwCannotSetValues("setCommandType");
    switch (paramInt)
    {
    case 4:
      this.prepareStmt = true;
    case 1:
    case 2:
    case 1024:
    case 2048:
    case 4096:
    case 8192:
    case 16384:
    case 32768:
    case 65536:
    case 131072:
      this.commandType = paramInt;
      break;
    case 8:
    default:
      throw new AdoError(null, "unknown/unhandled command type", 20002);
    }
  }

  public Parameters getParameters()
  {
    if (this.params == null)
      this.params = new Parameters(this);
    return this.params;
  }

  public void setParameters(Parameters paramParameters)
  {
    this.params = paramParameters;
    if (paramParameters != null)
      this.params.setCommand(this);
  }

  public boolean getPrepared()
  {
    return this.prepareStmt;
  }

  public void setPrepared(boolean paramBoolean)
  {
    this.prepareStmt = paramBoolean;
  }

  public Properties getProperties()
  {
    return props;
  }

  public Recordset execute(Variant paramVariant, Vector paramVector, int paramInt)
    throws AdoError
  {
    setCommandType(paramInt);
    if (paramVector != null)
      this.paramValues = paramVector;
    Recordset localRecordset = execute();
    this.paramValues = null;
    if ((paramVariant != null) && (localRecordset != null))
      paramVariant.set(localRecordset.getRecordsAffected());
    return localRecordset;
  }

  public Recordset execute(Variant paramVariant, int paramInt)
    throws AdoError
  {
    setCommandType(paramInt);
    Recordset localRecordset = execute();
    if ((paramVariant != null) && (localRecordset != null))
      paramVariant.set(localRecordset.getRecordsAffected());
    return localRecordset;
  }

  public Recordset execute()
    throws AdoError
  {
    if (this.ownerCon == null)
      throw new AdoError("You need to set the connection before calling: Execute", 20003);
    if (!this.ownerCon.isConnected())
      throw new AdoError(null, "You must have an open connection before calling: Execute", 10009);
    buildSqlStatement();
    prepareStatement();
    return execStmt();
  }

  public void setSortOrder(String paramString)
  {
    this.sortOrderStr = paramString;
  }

  public String getSQLStatement()
    throws AdoError
  {
    if ((this.sqlString == null) || (this.sqlString.length() == 0))
      buildSqlStatement();
    return this.sqlString;
  }

  void setParamValues(PreparedStatement paramPreparedStatement, int paramInt1, Variant paramVariant, int paramInt2)
    throws AdoError
  {
    paramInt1++;
    try
    {
      if (paramVariant.getVarType() == 1)
      {
        paramPreparedStatement.setNull(paramInt1, paramInt2);
      }
      else
      {
        Object localObject1;
        switch (paramInt2)
        {
        case -5:
          paramPreparedStatement.setLong(paramInt1, paramVariant.toLong());
          break;
        case -3:
        case -2:
          paramPreparedStatement.setBytes(paramInt1, (byte[])paramVariant.toObject());
          break;
        case -7:
          paramPreparedStatement.setBoolean(paramInt1, paramVariant.toBoolean());
          break;
        case 1:
        case 12:
          paramPreparedStatement.setString(paramInt1, paramVariant.toString());
          break;
        case 2:
        case 3:
          if ((this.ownerCon.getWorkArounds() & 0x2) == 2)
            paramPreparedStatement.setDouble(paramInt1, paramVariant.toDouble());
          else
            paramPreparedStatement.setBigDecimal(paramInt1, paramVariant.toDecimal());
          break;
        case 6:
        case 8:
          paramPreparedStatement.setDouble(paramInt1, paramVariant.toDouble());
          break;
        case 4:
          paramPreparedStatement.setInt(paramInt1, paramVariant.toInt());
          break;
        case -4:
          localObject1 = AdoUtil.getStream(paramVariant);
          if (localObject1 == null)
            paramPreparedStatement.setNull(paramInt1, paramInt2);
          else
            paramPreparedStatement.setBinaryStream(paramInt1, (InputStream)localObject1, ((InputStream)localObject1).available());
          break;
        case -1:
          localObject1 = new Variant(0);
          Reader localObject2 = getReader(paramVariant, (Variant)localObject1);
          if (localObject2 == null)
            paramPreparedStatement.setNull(paramInt1, paramInt2);
          else
            paramPreparedStatement.setCharacterStream(paramInt1, (Reader)localObject2, ((Variant)localObject1).toInt());
          break;
        case 2004:
          setBlob(paramPreparedStatement, paramInt1, paramVariant, paramInt2);
          break;
        case 2005:
          setClob(paramPreparedStatement, paramInt1, paramVariant, paramInt2);
          break;
        case 0:
          paramPreparedStatement.setObject(paramInt1, (Object)null);
          break;
        case 1111:
          paramPreparedStatement.setObject(paramInt1, paramVariant.toObject());
          break;
        case 7:
          paramPreparedStatement.setFloat(paramInt1, paramVariant.toFloat());
          break;
        case 5:
          paramPreparedStatement.setShort(paramInt1, paramVariant.toShort());
          break;
        case 9:
        case 91:
          paramPreparedStatement.setDate(paramInt1, AdoUtil.getDate(paramVariant));
          break;
        case 10:
        case 92:
          paramPreparedStatement.setTime(paramInt1, AdoUtil.getTime(paramVariant));
          break;
        case 11:
        case 93:
          paramPreparedStatement.setTimestamp(paramInt1, AdoUtil.getTimestamp(paramVariant));
          break;
        case -6:
          paramPreparedStatement.setByte(paramInt1, paramVariant.toByte());
          break;
        default:
          throw new AdoError(null, "Unknown SQL Type : " + paramInt2, 9001);
        }
      }
    }
    catch (AdoError localAdoError)
    {
      Object localObject2;
      if (localAdoError.getNumber() == 30009)
      {
        localObject2 = localAdoError.getMessage();
        localObject2 = (String)localObject2 + " Parameter number is: " + paramInt1;
        localAdoError.setErrorMsg((String)localObject2);
      }
      throw localAdoError;
    }
    catch (SQLException localSQLException)
    {
      throw new AdoError(localSQLException, "Error setting value for parameter: " + paramInt1, 20004);
    }
    catch (Exception localException)
    {
      throw new AdoError(localException, "Error during data type conversion.", 30002);
    }
  }

  private void setClob(PreparedStatement paramPreparedStatement, int paramInt1, Variant paramVariant, int paramInt2)
    throws IOException, SQLException
  {
    try
    {
      CLOB localCLOB = CLOB.createTemporary((OracleConnection)this.ownerCon.getConnection(), false, 10);
      Writer localObject = localCLOB.setCharacterStream(0L);
      ((Writer)localObject).write(paramVariant.toString());
      ((Writer)localObject).close();
      paramPreparedStatement.setClob(paramInt1, localCLOB);
      return;
    }
    catch (Exception localVariant)
    {
      Variant localVariant1 = new Variant(0);
      Object localObject = getReader(paramVariant, localVariant1);
      if (localObject == null)
        paramPreparedStatement.setNull(paramInt1, paramInt2);
      else
        paramPreparedStatement.setCharacterStream(paramInt1, (Reader)localObject, localVariant1.toInt());
    }
  }

  private void setBlob(PreparedStatement paramPreparedStatement, int paramInt1, Variant paramVariant, int paramInt2)
    throws IOException, SQLException
  {
    try
    {
      BLOB localBLOB = BLOB.createTemporary((OracleConnection)this.ownerCon.getConnection(), false, 10);
      OutputStream localOutputStream = localBLOB.setBinaryStream(0L);
      localOutputStream.write(paramVariant.toBytes());
      localOutputStream.close();
      paramPreparedStatement.setBlob(paramInt1, localBLOB);
      return;
    }
    catch (Exception localException)
    {
      System.out.println(localException);
      InputStream localInputStream = AdoUtil.getStream(paramVariant);
      if (localInputStream == null)
        paramPreparedStatement.setNull(paramInt1, paramInt2);
      else
        paramPreparedStatement.setBinaryStream(paramInt1, localInputStream, localInputStream.available());
    }
  }

  private static Reader getReader(Variant paramVariant1, Variant paramVariant2)
    throws IOException
  {
    Object localObject1 = null;
    Object localObject2 = paramVariant1.toObject();
    paramVariant2.set(0);
    if (localObject2 == null)
      return null;
    if ((localObject2 instanceof Reader))
      localObject1 = (Reader)localObject2;
    else if ((localObject2 instanceof InputStream))
      localObject1 = new InputStreamReader((InputStream)localObject2);
    if (localObject1 == null)
    {
      String str = paramVariant1.toString();
      paramVariant2.set(str.length());
      localObject1 = new StringReader(str);
    }
    else
    {
      ((Reader)localObject1).reset();
      long l = 0L;
      while (((Reader)localObject1).read() >= 0)
        l += 1L;
      paramVariant2.set(l);
      ((Reader)localObject1).reset();
    }
    return (Reader)localObject1;
  }

  void setCurrentParamValues()
    throws AdoError
  {
    int i = 0;
    if (this.paramValues != null)
      i = this.paramValues.size();
    if ((i > 0) && (getPrepared()) && (this.prepared))
    {
      if (this.params.getCount() < i)
        throw new AdoError("You need to set the parameters and their types before calling Execute", 20009);
      PreparedStatement localPreparedStatement = this.commandType == 4 ? this.callStmt : this.prepStmt;
      for (int j = 0; j < i; j++)
      {
        Variant localVariant = (Variant)this.paramValues.elementAt(j);
        int k = this.params.getParameter(j).getSqlType();
        if (k == 1111)
          throw new AdoError("Parameter type not set.", 40008);
        setParamValues(localPreparedStatement, j, localVariant, this.params.getParameter(j).getSqlType());
      }
    }
  }

  void setParamValues()
    throws AdoError
  {
    if (this.paramValues != null)
    {
      setCurrentParamValues();
    }
    else if (this.params != null)
    {
      int i = this.params.getCount();
      if ((i > 0) && (getPrepared()) && (this.prepared))
      {
        PreparedStatement localPreparedStatement = this.commandType == 4 ? this.callStmt : this.prepStmt;
        for (int j = 0; j < i; j++)
        {
          Parameter localParameter = this.params.getParameter(j);
          int k = localParameter.getDirection();
          if ((k == 1) || (k == 3))
          {
            Variant localVariant = localParameter.getValue();
            setParamValues(localPreparedStatement, j, localVariant, localParameter.getSqlType());
          }
          if ((k != 2) && (k != 4) && (k != 3))
            continue;
          if (this.commandType != 4)
            throw new AdoError("Cannot set direction to return value unless the command type is adCmdStoredProc.", 20005);
          try
          {
            this.callStmt.registerOutParameter(j + 1, localParameter.getSqlType());
          }
          catch (SQLException localSQLException)
          {
            throw new AdoError("Error registering outParam for parameter: " + (j + 1), 20006);
          }
        }
      }
    }
  }

  public int getIndexOf(String paramString)
    throws AdoError
  {
    int i = 0;
    if ((this.sqlString == null) || (this.sqlString.length() == 0))
      buildSqlStatement();
    if ((this.ownerCon == null) || (!this.ownerCon.isConnected()))
      throw new AdoError("You must have an open connection before calling: buildSqlStatement", 10009);
    String str1 = this.ownerCon.getIdentifierQuoteString();
    String str2 = " \t\n\r" + str1;
    int j = 0;
    int k = 0;
    String str3 = "";
    StringTokenizer localStringTokenizer = new StringTokenizer(this.sqlString, str2, true);
    while ((localStringTokenizer.hasMoreTokens()) && (j == 0))
    {
      str3 = localStringTokenizer.nextToken();
      if ((k == 0) && (str3.equalsIgnoreCase(paramString)))
      {
        j = 1;
        continue;
      }
      if (str3.equals(str1))
        k = k == 0 ? 1 : 0;
      i += str3.length();
    }
    if (j == 0)
      i = -1;
    return i;
  }

  int getFromClauseOffset()
    throws AdoError
  {
    return getIndexOf("FROM");
  }

  int getWhereClauseOffset()
    throws AdoError
  {
    return getIndexOf("WHERE");
  }

  int getOrderByOffset()
    throws AdoError
  {
    return getIndexOf("ORDER");
  }

  int getGroupByOffset()
    throws AdoError
  {
    return getIndexOf("GROUP");
  }

  String getWhereClause()
    throws AdoError
  {
    int i = getWhereClauseOffset();
    String str = "";
    if (i != -1)
      str = this.sqlString.substring(i);
    return str;
  }

  String getOrderByClause()
    throws AdoError
  {
    int i = getOrderByOffset();
    String str = "";
    if (i != -1)
      str = this.sqlString.substring(i);
    return str;
  }

  public String getTableName()
    throws AdoError
  {
    int i = getFromClauseOffset();
    if (i != -1)
    {
      String str = this.sqlString.substring(i + 4).trim();
      StringTokenizer localStringTokenizer = new StringTokenizer(str);
      if (str.charAt(0) == '[')
        return localStringTokenizer.nextToken("[]");
      return localStringTokenizer.nextToken();
    }
    return null;
  }

  public String getSqlWithNewFilter(Recordset paramRecordset, String paramString1, String paramString2)
    throws AdoError
  {
    Field localField = paramRecordset.getField(paramString1);
    if (!localField.isNumericType())
      paramString2 = "'" + paramString2 + "'";
    int i = getIndexOf("where");
    String str1 = this.sqlString.trim();
    if (i == -1)
    {
      String str2 = getTableName();
      int j = getIndexOf("FROM");
      if (j != -1)
        str1 = str1.substring(0, j) + "From " + str2;
    }
    else
    {
      str1 = str1.substring(0, i);
    }
    if (str1.endsWith(";"))
      str1 = str1.substring(0, str1.length() - 1);
    String str2 = this.ownerCon.getIdentifierQuoteString();
    str1 = str1 + " Where " + str2 + paramString1 + str2 + "=" + paramString2;
    return str1;
  }

  String getProcName()
    throws AdoError
  {
    String str1 = "";
    if (this.commandType != 4)
      throw new AdoError("Command type should be adCmdStoredProc for invoking this method.getProcName", 20007);
    if (this.commandText.length() == 0)
      throw new AdoError("Command text not set yet.", 20011);
    String str2 = this.ownerCon.getIdentifierQuoteString();
    String str3 = " \t\n\r" + str2 + "{}";
    StringTokenizer localStringTokenizer = new StringTokenizer(this.commandText, str3, true);
    String str4 = localStringTokenizer.nextToken();
    int i = 0;
    do
      if (str4.equalsIgnoreCase("Call"))
      {
        str1 = localStringTokenizer.nextToken("(}");
        i = 1;
      }
      else
      {
        str4 = localStringTokenizer.nextToken();
      }
    while ((localStringTokenizer.hasMoreTokens()) && (i == 0));
    if (i == 0)
      throw new AdoError("Missing call keyword in stored procedure call: " + this.commandText, 20010);
    str1 = str1.trim();
    int j = str1.length();
    if (str2.length() > 0)
    {
      int k = str2.charAt(0);
      int m = str1.charAt(0);
      int n = str1.charAt(j - 1);
      if ((m == k) && (n == k))
        str1 = str1.substring(1, j - 1);
    }
    return str1;
  }

  PreparedStatement getPreparedStmt()
    throws AdoError
  {
    prepareStatement();
    return this.prepStmt;
  }

  CallableStatement getCallableStmt()
    throws AdoError
  {
    prepareStatement();
    return this.callStmt;
  }

  void closePreparedStatement()
    throws AdoError
  {
    if (this.prepared)
    {
      if (this.commandType == 4)
      {
        this.ownerCon.closeStatement(this.callStmt);
        this.callStmt = null;
      }
      else
      {
        this.ownerCon.closeStatement(this.prepStmt);
        this.prepStmt = null;
      }
      this.prepared = false;
    }
  }

  int getStmtType()
    throws AdoError
  {
    if ((this.sqlString == null) || (this.sqlString.length() == 0))
      buildSqlStatement();
    return this.stmtType;
  }

  synchronized void setExecuting(boolean paramBoolean)
  {
    this.isExecuting = paramBoolean;
  }

  void freeResources()
    throws AdoError
  {
    closePreparedStatement();
    setExecuting(false);
    if (this.freeOwnerCon)
    {
      this.ownerCon.close();
      this.freeOwnerCon = false;
    }
  }

  private void buildSqlStatement()
    throws AdoError
  {
    if ((this.commandText == null) || (this.commandText.length() == 0))
      throw new AdoError("Command text not set yet.", 20011);
    switch (this.commandType)
    {
    case 8:
      AdoError.throwNotSupported("Command Type: " + this.commandType);
      break;
    case 1:
    case 4:
    case 1024:
    case 2048:
    case 4096:
    case 8192:
    case 16384:
    case 32768:
    case 65536:
    case 131072:
      this.sqlString = this.commandText;
      break;
    case 2:
      if ((this.ownerCon == null) || (!this.ownerCon.isConnected()))
        throw new AdoError("You must have an open connection before calling: buildSqlStatement", 10009);
      this.sqlString = "select * from ";
      Properties localProperties = this.ownerCon.getProperties();
      boolean bool1 = ((Boolean)localProperties.get("IsCatalogAtStart")).booleanValue();
      int i = ((Integer)localProperties.get("IdentifierCase")).intValue();
      String str1 = this.ownerCon.getCatalogSeparator();
      String str2 = this.ownerCon.getIdentifierQuoteString();
      boolean bool2 = false;
      QualifiedTableName localQualifiedTableName = new QualifiedTableName(this.commandText, str2, str1, i, bool1, bool2);
      this.sqlString += localQualifiedTableName.getExtendedTableName();
      appendSortOrder();
      break;
    default:
      throw new AdoError("unknown/unhandled command type", 20002);
    }
    setStmtType();
  }

  private void setStmtType()
    throws AdoError
  {
    StringTokenizer localStringTokenizer = new StringTokenizer(this.sqlString, " \r\n\t");
    String str = localStringTokenizer.nextToken();
    if (str.equalsIgnoreCase("SELECT"))
    {
      this.stmtType = 2;
      int i = getWhereClauseOffset();
      if (i != -1)
        this.stmtType |= 4;
      i = getGroupByOffset();
      if (i != -1)
        this.stmtType |= 8;
      i = getOrderByOffset();
      if (i != -1)
        this.stmtType |= 16;
    }
    else
    {
      this.stmtType = 1;
      if ((str.equalsIgnoreCase("EXEC")) || (str.equalsIgnoreCase("EXECUTE")))
      {
        this.commandType = 4;
        this.stmtType = 32;
      }
      else if (str.equalsIgnoreCase("CALL"))
      {
        this.commandType = 4;
        this.stmtType = 32;
      }
    }
  }

  private void prepareStatement()
    throws AdoError
  {
    if ((getPrepared()) && (!this.prepared))
      if (getCommandType() == 4)
      {
        if (!this.sqlString.startsWith("{call"))
        {
          StringBuffer localStringBuffer = new StringBuffer();
          localStringBuffer.append("{");
          int i = 0;
          int j = 0;
          if (this.params != null)
          {
            int k = this.params.getCount();
            for (int m = 0; m < k; m++)
            {
              Parameter localParameter = this.params.getParameter(m);
              if (localParameter == null)
                continue;
              if (localParameter.getDirection() == 4)
                i = 1;
              else
                j++;
            }
          }
          if (i != 0)
            localStringBuffer.append("? = ");
          localStringBuffer.append("call ");
          localStringBuffer.append(this.sqlString);
          if (j > 0)
            localStringBuffer.append("(");
          for (int k = 0; k < j; k++)
          {
            if (k > 0)
              localStringBuffer.append(",");
            localStringBuffer.append("?");
          }
          if (j > 0)
            localStringBuffer.append(")");
          localStringBuffer.append("}");
          this.sqlString = localStringBuffer.toString();
        }
        println("preparing call: " + this.sqlString);
        this.callStmt = this.ownerCon.prepareCall(this.sqlString);
        this.prepared = true;
      }
      else if ((getCommandType() == 1) || (getCommandType() == 2))
      {
        println("preparing statement: " + this.sqlString);
        this.prepStmt = this.ownerCon.prepareStatement(this.sqlString);
        this.prepared = true;
      }
  }

  private Recordset execStmt()
    throws AdoError
  {
    Recordset localRecordset = new Recordset();
    localRecordset.setSource(this);
    localRecordset.open();
    if (((this.stmtType & 0x2) != 2) && (this.commandType != 4) && (this.commandType != 1024) && (this.commandType != 2048) && (this.commandType != 4096) && (this.commandType != 8192) && (this.commandType != 65536) && (this.commandType != 131072) && (this.commandType != 32768) && (this.commandType != 16384))
      localRecordset.close();
    return localRecordset;
  }

  private void appendSortOrder()
  {
    if (this.sortOrderStr.length() > 0)
      this.sqlString = (this.sqlString + " order by " + this.sortOrderStr);
  }

  private void throwCannotSetValues(String paramString)
    throws AdoError
  {
    throw new AdoError("The cursor is already open.  You need to close the cursor before calling: " + paramString, 70001);
  }

  private void println(String paramString)
  {
    this.ownerCon.println(paramString);
  }

  private void initialize()
  {
    this.commandType = 8;
    this.queryTimeout = 30;
    this.stmtType = 2;
  }

  public void finalize()
  {
    try
    {
      freeResources();
    }
    catch (AdoError localAdoError)
    {
    }
  }

  public String toString()
  {
    return "Command[sql=" + this.sqlString + ",cmdType=" + this.commandType + ",con=" + this.ownerCon + "]";
  }

  static
  {
    props.put("ParametersSupported", new Boolean(true));
    props.put("PrepareSupported", new Boolean(true));
  }
}