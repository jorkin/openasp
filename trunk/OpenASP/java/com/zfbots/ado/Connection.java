package com.zfbots.ado;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Vector;
import javax.swing.JOptionPane;

import com.zfbots.util.HashList;
import com.zfbots.util.Variant;


public class Connection
{
  private int commandTimeout = 30;
  private int connectionTimeout = 15;
  private int isoLevel = 4096;
  private String urlName = "";
  private String userId = null;
  private String password = null;
  private static final String version = "1.0";
  private Properties props = new Properties();
  private boolean connected = false;
  private java.sql.Connection con = null;
  private java.sql.Connection secondCon = null;
  private boolean inTransaction = false;
  private int transactionLevel = 0;
  private boolean amIBusy = false;
  private static PrintWriter logWriter = null;
  int numActiveStatements = 0;
  private String cursorName = "cursor";
  private int cursorCounter = 0;
  private Vector recsets = null;
  private Vector typeInfos = null;
  static final String CON = "con";
  static int counter = 0;
  String conName;
  String secondConName;
  private int workarounds = 0;
  private static HashList conList = new HashList();

  public Connection()
  {
  }

  Connection(Connection paramConnection, boolean paramBoolean)
    throws AdoError
  {
    this();
    this.commandTimeout = paramConnection.commandTimeout;
    this.connectionTimeout = paramConnection.connectionTimeout;
    this.urlName = paramConnection.urlName;
    this.userId = paramConnection.userId;
    this.password = paramConnection.password;
    if (paramBoolean)
    {
      this.con = connect();
      this.conName = ("con" + ++counter);
      println("opening con " + this.conName);
      AdoUtil.copyProps(paramConnection.props, this.props);
      this.connected = true;
    }
  }

  public static Connection openConnection(String paramString1, String paramString2, String paramString3, String paramString4)
    throws AdoError
  {
    AdoUtil.registerDriver(paramString4);
    Connection localConnection = getConnection(paramString1);
    if (localConnection == null)
    {
      localConnection = new Connection();
      localConnection.open(paramString1, paramString2, paramString3);
      conList.add(localConnection, paramString1);
    }
    else if (!localConnection.isConnected())
    {
      localConnection.open(paramString1, paramString2, paramString3);
    }
    return localConnection;
  }

  public static Connection getConnection(String paramString)
    throws AdoError
  {
    Object localObject = conList.get(paramString);
    if ((localObject instanceof Connection))
      return (Connection)localObject;
    return null;
  }

  public static Connection getConnection(int paramInt)
  {
    Object localObject = conList.get(paramInt);
    if ((localObject instanceof Connection))
      return (Connection)localObject;
    return null;
  }

  public static Connection getLastConnection()
  {
    return getConnection(conList.size() - 1);
  }

  public static int getConnectionCount()
  {
    return conList.size();
  }

  public int getConnectionTimeout()
  {
    return this.connectionTimeout;
  }

  public void setConnectionTimeout(int paramInt)
    throws AdoError
  {
    if (!this.connected)
    {
      if (paramInt < 1)
        throw new AdoError("Connection timeout value should be greater than 0", 10019);
      this.connectionTimeout = paramInt;
    }
    else
    {
      throw new AdoError("you already have an open connection for this object.setConnectionTimeout", 10008);
    }
  }

  public int getCommandTimeout()
  {
    return this.commandTimeout;
  }

  public void setCommandTimeout(int paramInt)
  {
    this.commandTimeout = paramInt;
  }

  public String getConnectionString()
  {
    return this.urlName;
  }

  public void setConnectionString(String paramString)
    throws AdoError
  {
    if (!this.connected)
      setURLName(paramString, null, null);
    else
      throw new AdoError("you already have an open connection for this object.setConnectionString", 10008);
  }

  public void setConnectionString(String paramString1, String paramString2, String paramString3)
    throws AdoError
  {
    if (!this.connected)
      setURLName(paramString1, paramString2, paramString3);
    else
      throw new AdoError("you already have an open connection for this object.setConnectionString", 10008);
  }

  public int getIsolationLevel()
  {
    return this.isoLevel;
  }

  public void setIsolationLevel(int paramInt)
  {
    this.isoLevel = paramInt;
  }

  public Properties getProperties()
  {
    return this.props;
  }

  public String getVersion()
  {
    return "1.0";
  }

  public boolean isBusy()
  {
    return this.amIBusy;
  }

  public boolean isInTransaction()
  {
    return this.inTransaction;
  }

  public int beginTrans()
    throws AdoError
  {
    if (!this.connected)
      ThrowNotConnected("BeginTrans");
    Boolean localBoolean = (Boolean)this.props.get("TransactionSupport");
    if (!localBoolean.booleanValue())
      throw new AdoError("The backend database does not support transactions.", 10002);
    if (this.inTransaction)
      throw new AdoError("Nested Transactions are not supported.  You need to commit/rollback the previous transaction before beginning a new one.", 10021);
    try
    {
      this.con.setTransactionIsolation(AdoUtil.getJDBCIsolationLevel(this.isoLevel));
      this.con.setAutoCommit(false);
      this.inTransaction = true;
    }
    catch (SQLException localSQLException)
    {
      throw new AdoError(localSQLException, "error starting the transaction.", 10003);
    }
    return ++this.transactionLevel;
  }

  public void commitTrans()
    throws AdoError
  {
    if (!this.connected)
      ThrowNotConnected("CommitTrans");
    if (!this.inTransaction)
      ThrowNotInTransaction("CommitTrans");
    try
    {
      this.con.commit();
      this.transactionLevel -= 1;
      if (this.transactionLevel == 0)
      {
        this.inTransaction = false;
        this.con.setAutoCommit(true);
      }
      notifyRecsets(true);
    }
    catch (SQLException localSQLException)
    {
      AdoError localAdoError = new AdoError();
      localAdoError.setErrorMsg("error while committing transaction.");
      localAdoError.setErrorNumber(10004);
      localAdoError.setNativeAdoError(localSQLException);
    }
  }

  public void rollbackTrans()
    throws AdoError
  {
    if (!this.connected)
      ThrowNotConnected("RollbackTrans");
    if (!this.inTransaction)
      ThrowNotInTransaction("RollbackTrans");
    try
    {
      this.con.rollback();
      this.transactionLevel -= 1;
      if (this.transactionLevel == 0)
      {
        this.inTransaction = false;
        this.con.setAutoCommit(true);
      }
      notifyRecsets(false);
    }
    catch (SQLException localSQLException)
    {
      AdoError localAdoError = new AdoError();
      localAdoError.setErrorMsg("error while rolling back transaction.");
      localAdoError.setErrorNumber(10005);
      localAdoError.setNativeAdoError(localSQLException);
    }
  }

  public void close()
    throws AdoError
  {
    println("number of active statements are : " + this.numActiveStatements);
    if (this.connected)
    {
      if (this.inTransaction)
        throw new AdoError("You need to complete the current transaction before closing the connection.", 10006);
      try
      {
        closeAllRecsets();
        if (this.secondCon != null)
        {
          this.secondCon.close();
          println("closed second con " + this.secondConName);
          this.secondCon = null;
        }
        this.con.close();
        println("closed con " + this.conName);
        this.con = null;
        this.connected = false;
      }
      catch (SQLException localSQLException)
      {
        throw new AdoError(localSQLException, "cannot close the connection.", 10007);
      }
    }
  }

  public void useConnection(java.sql.Connection paramConnection)
    throws AdoError
  {
    if (this.connected)
      throw new AdoError("you already have an open connection for this object.", 10008);
    this.con = paramConnection;
    this.conName = ("pooledcon" + ++counter);
    println("using con " + this.conName);
    initializeConnectionProps();
    this.connected = true;
  }

  public void open()
    throws AdoError
  {
    if (this.connected)
      throw new AdoError("you already have an open connection for this object.", 10008);
    this.con = connect();
    this.conName = ("con" + ++counter);
    println("opening con " + this.conName);
    initializeConnectionProps();
    this.connected = true;
  }

  public void open(String paramString)
    throws AdoError
  {
    if (this.connected)
      throw new AdoError("you already have an open connection for this object.", 10008);
    setURLName(paramString, null, null);
    open();
  }

  public void open(String paramString1, String paramString2)
    throws AdoError
  {
    if (this.connected)
      throw new AdoError("you already have an open connection for this object.", 10008);
    setURLName(paramString1, paramString2, null);
    open();
  }

  public void open(String paramString1, String paramString2, String paramString3)
    throws AdoError
  {
    if (this.connected)
      throw new AdoError("you already have an open connection for this object.", 10008);
    setURLName(paramString1, paramString2, paramString3);
    open();
  }

  public Recordset openRecordset(String paramString, int paramInt1, int paramInt2)
    throws AdoError
  {
    Recordset localRecordset = new Recordset();
    localRecordset.open(paramString, this, paramInt1, paramInt2, 1);
    return localRecordset;
  }

  public Recordset execute(String paramString)
    throws AdoError
  {
    return execute(paramString, null, 1);
  }

  public Recordset execute(String paramString, Variant paramVariant)
    throws AdoError
  {
    return execute(paramString, paramVariant, 1);
  }

  public Recordset execute(String paramString, Variant paramVariant, int paramInt)
    throws AdoError
  {
    if (!this.connected)
      throw new AdoError("You must have an open connection before calling: Execute", 10009);
    Command localCommand = new Command();
    localCommand.setCommandText(paramString);
    localCommand.setCommandType(paramInt);
    localCommand.setCommandTimeout(this.commandTimeout);
    localCommand.setActiveConnection(this);
    return localCommand.execute(paramVariant, paramInt);
  }

  public int executeSQL(String paramString)
    throws SQLException
  {
    if (!this.connected)
      throw new SQLException("You must have an open connection before calling: Execute");
    Statement localStatement = this.con.createStatement();
    localStatement.execute(paramString);
    int i = localStatement.getUpdateCount();
    localStatement.close();
    return i;
  }

  public boolean isConnected()
  {
    return this.connected;
  }

  public int getMaxStatements()
  {
    Integer localInteger = (Integer)this.props.get("MaxStatements");
    return localInteger.intValue();
  }

  public String getIdentifierQuoteString()
  {
    return (String)this.props.get("IdentifierQuoteString");
  }

  public String getCatalogSeparator()
  {
    return (String)this.props.get("CatalogSeparator");
  }

  public boolean isPositionedDeleteSupported()
  {
    return ((Boolean)this.props.get("SupportsPosDelete")).booleanValue();
  }

  public boolean isPositionedUpdateSupported()
  {
    return ((Boolean)this.props.get("SupportsPosUpdate")).booleanValue();
  }

  public Vector getRecordsets()
  {
    return this.recsets;
  }

  public int getWorkArounds()
  {
    return this.workarounds;
  }

  public void setWorkArounds(int paramInt)
  {
    this.workarounds = paramInt;
  }

  public static void setLogWriter(PrintWriter paramPrintWriter)
  {
    logWriter = paramPrintWriter;
    DriverManager.setLogWriter(paramPrintWriter);
  }

  public static PrintWriter getLogWriter()
  {
    return logWriter;
  }

  protected void finalize()
    throws Throwable
  {
    super.finalize();
    conList.remove(this.urlName);
    if ((this.connected) && (this.con != null))
    {
      if (this.inTransaction)
        try
        {
          rollbackTrans();
        }
        catch (AdoError localAdoError1)
        {
        }
      try
      {
        close();
      }
      catch (AdoError localAdoError2)
      {
      }
    }
  }

  public java.sql.Connection getConnection()
  {
    return this.con;
  }

  java.sql.Connection getSecondConnection()
    throws AdoError
  {
    if (this.secondCon == null)
    {
      this.secondCon = connect();
      this.secondConName = ("con" + ++counter);
      println("opening second con " + this.secondConName);
    }
    return this.secondCon;
  }

  public String getUserId()
  {
    return this.userId;
  }

  public String getPassword()
  {
    return this.password;
  }

  int getActiveStatements()
  {
    return this.numActiveStatements;
  }

  private void setURLName(String paramString1, String paramString2, String paramString3)
  {
    this.urlName = paramString1;
    if (paramString2 != null)
      this.userId = paramString2;
    if (paramString3 != null)
      this.password = paramString3;
  }

  private java.sql.Connection connect()
    throws AdoError
  {
    java.sql.Connection localConnection = null;
    try
    {
      DriverManager.setLoginTimeout(this.connectionTimeout);
      try
      {
        if ((this.userId == null) && (this.password == null))
          localConnection = DriverManager.getConnection(this.urlName);
        else
          localConnection = DriverManager.getConnection(this.urlName, this.userId, this.password);
        if (getConnection(this.urlName) == null)
          conList.add(this, this.urlName);
      }
      catch (SQLException localSQLException1)
      {
        if (localSQLException1.getErrorCode() == -3810)
        {
          String str = "error connecting to data source '" + this.urlName + "'\n  " + localSQLException1.getMessage();
          if (str.indexOf("Access") > 0)
          {
            str = str + "\n\nMake sure you close the Access database before\ntrying to run the application from Java";
            System.out.println(str);
            try
            {
              JOptionPane.showMessageDialog(null, str, "Connection Error", 0);
            }
            catch (Exception localException)
            {
            }
            System.exit(0);
          }
          throw localSQLException1;
        }
        if ((localSQLException1.getErrorCode() == 1017) || (this.userId == null) || (this.userId.equals("")))
          try
          {
            System.out.println("invalid login: " + localSQLException1.getErrorCode() + ": " + localSQLException1);
            localConnection = DriverManager.getConnection(this.urlName, this.userId, this.password);
          }
          catch (Throwable localThrowable)
          {
            throw localSQLException1;
          }
        if (localConnection == null)
          throw localSQLException1;
      }
      try
      {
        localConnection.setAutoCommit(true);
      }
      catch (SQLException localSQLException2)
      {
        setMetaDataError("setAutoCommit", localSQLException2);
      }
    }
    catch (SQLException localSQLException3)
    {
      throw new AdoError(localSQLException3, "Error connecting to data source: " + this.urlName, 10010);
    }
    return localConnection;
  }

  private String makeString(String paramString)
  {
    if (paramString == null)
      return "";
    return paramString;
  }

  private void initializeConnectionProps()
    throws AdoError
  {
    Assert(this.con != null, "connection is null in initializeConnectionProps");
    try
    {
      DatabaseMetaData localDatabaseMetaData = this.con.getMetaData();
      String str1 = "";
      try
      {
        if ((getWorkArounds() & 0x8) != 8)
          str1 = this.con.getCatalog();
      }
      catch (SQLException localSQLException2)
      {
      }
      this.props.put("CurrentCatalog", makeString(str1));
      int i = 0;
      try
      {
        i = localDatabaseMetaData.getMaxConnections();
      }
      catch (SQLException localSQLException3)
      {
        setMetaDataError("ActiveSessions", localSQLException3);
      }
      this.props.put("ActiveSessions", new Integer(i));
      int j = 1;
      try
      {
        j = localDatabaseMetaData.isCatalogAtStart() ? 1 : 2;
      }
      catch (SQLException localSQLException4)
      {
        setMetaDataError("CatalogLocation", localSQLException4);
      }
      this.props.put("CatalogLocation", new Integer(j));
      String str2 = "";
      try
      {
        str2 = localDatabaseMetaData.getCatalogTerm();
      }
      catch (SQLException localSQLException5)
      {
        setMetaDataError("CatalogTerm", localSQLException5);
      }
      this.props.put("CatalogTerm", makeString(str2));
      int k = 0;
      try
      {
        if (localDatabaseMetaData.supportsCatalogsInDataManipulation())
          k |= 1;
        if (localDatabaseMetaData.supportsCatalogsInTableDefinitions())
          k |= 2;
        if (localDatabaseMetaData.supportsCatalogsInIndexDefinitions())
          k |= 4;
        if (localDatabaseMetaData.supportsCatalogsInPrivilegeDefinitions())
          k |= 8;
      }
      catch (SQLException localSQLException6)
      {
        setMetaDataError("CatalogUsage", localSQLException6);
      }
      this.props.put("CatalogUsage", new Integer(k));
      int m = 0;
      try
      {
        m = localDatabaseMetaData.nullPlusNonNullIsNull() ? 1 : 2;
      }
      catch (SQLException localSQLException7)
      {
        setMetaDataError("ConcatNullBehavior", localSQLException7);
      }
      this.props.put("ConcatNullBehavior", new Integer(m));
      String str3 = "";
      try
      {
        str3 = localDatabaseMetaData.getURL();
      }
      catch (SQLException localSQLException8)
      {
        setMetaDataError("DataSourceName", localSQLException8);
      }
      this.props.put("DataSourceName", makeString(str3));
      boolean bool1 = false;
      try
      {
        bool1 = localDatabaseMetaData.isReadOnly();
      }
      catch (SQLException localSQLException9)
      {
        setMetaDataError("DataSourceReadOnly", localSQLException9);
      }
      this.props.put("DataSourceReadOnly", new Boolean(bool1));
      String str4 = "";
      try
      {
        str4 = localDatabaseMetaData.getDatabaseProductName();
      }
      catch (SQLException localSQLException10)
      {
        setMetaDataError("DBMSName", localSQLException10);
      }
      this.props.put("DBMSName", makeString(str4));
      String str5 = "";
      try
      {
        str5 = localDatabaseMetaData.getDatabaseProductVersion();
      }
      catch (SQLException localSQLException11)
      {
        setMetaDataError("DBMSVer", localSQLException11);
      }
      this.props.put("DBMSVer", makeString(str5));
      int n = 0;
      try
      {
        if (localDatabaseMetaData.supportsGroupByUnrelated())
          n = 3;
        else if (localDatabaseMetaData.supportsGroupByBeyondSelect())
          n = 2;
        else if (localDatabaseMetaData.supportsGroupBy())
          n = 1;
      }
      catch (SQLException localSQLException12)
      {
        setMetaDataError("GroupBy", localSQLException12);
      }
      this.props.put("GroupBy", new Integer(n));
      this.props.put("HeterogeneousTables", new Integer(0));
      int i1 = 0;
      try
      {
        if (localDatabaseMetaData.storesLowerCaseIdentifiers())
          i1 = 2;
        else if (localDatabaseMetaData.storesUpperCaseIdentifiers())
          i1 = 1;
        else if (localDatabaseMetaData.storesMixedCaseIdentifiers())
          i1 = 4;
        else if (localDatabaseMetaData.supportsMixedCaseIdentifiers())
          i1 = 3;
      }
      catch (SQLException localSQLException13)
      {
        setMetaDataError("IdentifierCase", localSQLException13);
      }
      this.props.put("IdentifierCase", new Integer(i1));
      int i2 = 0;
      try
      {
        if (localDatabaseMetaData.storesLowerCaseQuotedIdentifiers())
          i2 = 2;
        else if (localDatabaseMetaData.storesUpperCaseQuotedIdentifiers())
          i2 = 1;
        else if (localDatabaseMetaData.storesMixedCaseQuotedIdentifiers())
          i2 = 4;
        else if (localDatabaseMetaData.supportsMixedCaseQuotedIdentifiers())
          i2 = 3;
      }
      catch (SQLException localSQLException14)
      {
        setMetaDataError("QuotedIdentifierCase", localSQLException14);
      }
      this.props.put("QuotedIdentifierCase", new Integer(i2));
      int i3 = 1;
      this.props.put("LockModes", new Integer(i3));
      int i4 = 0;
      try
      {
        i4 = localDatabaseMetaData.getMaxIndexLength();
      }
      catch (SQLException localSQLException15)
      {
        setMetaDataError("MaxIndexSize", localSQLException15);
      }
      this.props.put("MaxIndexSize", new Integer(i4));
      int i5 = 0;
      try
      {
        i5 = localDatabaseMetaData.getMaxRowSize();
      }
      catch (SQLException localSQLException16)
      {
        setMetaDataError("MaxRowSize", localSQLException16);
      }
      this.props.put("MaxRowSize", new Integer(i5));
      boolean bool2 = false;
      try
      {
        bool2 = localDatabaseMetaData.doesMaxRowSizeIncludeBlobs();
      }
      catch (SQLException localSQLException17)
      {
        setMetaDataError("MaxRowSizeIncludesBLOB", localSQLException17);
      }
      this.props.put("MaxRowSizeIncludesBLOB", new Boolean(bool2));
      int i6 = 0;
      try
      {
        i6 = localDatabaseMetaData.getMaxTablesInSelect();
      }
      catch (SQLException localSQLException18)
      {
        setMetaDataError("MaxTablesInSelect", localSQLException18);
      }
      this.props.put("MaxTablesInSelect", new Integer(i6));
      this.props.put("MultipleUpdate", new Boolean(false));
      int i7 = 1;
      try
      {
        if (localDatabaseMetaData.nullsAreSortedAtStart())
          i7 = 4;
        else if (localDatabaseMetaData.nullsAreSortedHigh())
          i7 = 2;
        else if (localDatabaseMetaData.nullsAreSortedLow())
          i7 = 4;
      }
      catch (SQLException localSQLException19)
      {
        setMetaDataError("NullCollation", localSQLException19);
      }
      this.props.put("NullCollation", new Integer(i7));
      boolean bool3 = false;
      try
      {
        bool3 = localDatabaseMetaData.supportsOrderByUnrelated();
      }
      catch (SQLException localSQLException20)
      {
        setMetaDataError("OrderByColumnsInSelect", localSQLException20);
      }
      this.props.put("OrderByColumnsInSelect", new Boolean(bool3));
      int i8 = 1;
      try
      {
        if (localDatabaseMetaData.supportsOpenStatementsAcrossCommit())
          i8 = 2;
      }
      catch (SQLException localSQLException21)
      {
        setMetaDataError("PrepareCommitBehavior", localSQLException21);
      }
      this.props.put("PrepareCommitBehavior", new Integer(i8));
      int i9 = 1;
      int i10 = 0;
      if (i10 != 0)
        try
        {
          if (localDatabaseMetaData.supportsOpenStatementsAcrossRollback())
            i9 = 2;
        }
        catch (SQLException localSQLException22)
        {
          setMetaDataError("PrepareAbortBehavior", localSQLException22);
        }
      this.props.put("PrepareAbortBehavior", new Integer(i9));
      String str6 = "";
      try
      {
        str6 = localDatabaseMetaData.getProcedureTerm();
      }
      catch (SQLException localSQLException23)
      {
        setMetaDataError("ProcedureTerm", localSQLException23);
      }
      this.props.put("ProcedureTerm", makeString(str6));
      String str7 = "";
      try
      {
        str7 = localDatabaseMetaData.getDriverName();
      }
      catch (SQLException localSQLException24)
      {
        setMetaDataError("ProviderName", localSQLException24);
      }
      this.props.put("ProviderName", makeString(str7));
      String str8 = "";
      try
      {
        str8 = localDatabaseMetaData.getDriverVersion();
      }
      catch (SQLException localSQLException25)
      {
        setMetaDataError("ProviderVersion", localSQLException25);
      }
      this.props.put("ProviderVersion", makeString(str8));
      String str9 = "";
      try
      {
        str9 = localDatabaseMetaData.getSchemaTerm();
      }
      catch (SQLException localSQLException26)
      {
        setMetaDataError("SchemaTerm", localSQLException26);
      }
      this.props.put("SchemaTerm", makeString(str9));
      int i11 = 0;
      try
      {
        if (localDatabaseMetaData.supportsSchemasInDataManipulation())
          i11 |= 1;
        if (localDatabaseMetaData.supportsSchemasInTableDefinitions())
          i11 |= 2;
        if (localDatabaseMetaData.supportsSchemasInIndexDefinitions())
          i11 |= 4;
        if (localDatabaseMetaData.supportsSchemasInPrivilegeDefinitions())
          i11 |= 8;
      }
      catch (SQLException localSQLException27)
      {
        setMetaDataError("SchemaUsage", localSQLException27);
      }
      this.props.put("SchemaUsage", new Integer(i11));
      int i12 = 0;
      try
      {
        if ((!localDatabaseMetaData.supportsANSI92EntryLevelSQL()) || ((!localDatabaseMetaData.supportsANSI92IntermediateSQL()) || ((!localDatabaseMetaData.supportsANSI92FullSQL()) || ((!localDatabaseMetaData.supportsMinimumSQLGrammar()) || ((!localDatabaseMetaData.supportsCoreSQLGrammar()) || (localDatabaseMetaData.supportsExtendedSQLGrammar()))))));
        if (!localDatabaseMetaData.supportsLikeEscapeClause());
      }
      catch (SQLException localSQLException28)
      {
      }
      this.props.put("SQLSupport", new Integer(i12));
      int i13 = 0;
      try
      {
        if (localDatabaseMetaData.supportsCorrelatedSubqueries())
          i13 |= 1;
        if (localDatabaseMetaData.supportsSubqueriesInComparisons())
          i13 |= 2;
        if (localDatabaseMetaData.supportsSubqueriesInExists())
          i13 |= 4;
        if (localDatabaseMetaData.supportsSubqueriesInIns())
          i13 |= 8;
        if (localDatabaseMetaData.supportsSubqueriesInQuantifieds())
          i13 |= 16;
      }
      catch (SQLException localSQLException29)
      {
        setMetaDataError("SubQueries", localSQLException29);
      }
      this.props.put("SubQueries", new Integer(i13));
      this.isoLevel = -1;
      try
      {
        int i14 = this.con.getTransactionIsolation();
        this.isoLevel = AdoUtil.getAdoIsolationLevel(i14);
      }
      catch (SQLException localSQLException30)
      {
      }
      this.props.put("TableTerm", "TABLE");
      String str10 = "";
      try
      {
        str10 = localDatabaseMetaData.getUserName();
      }
      catch (SQLException localSQLException31)
      {
        setMetaDataError("UserName", localSQLException31);
      }
      this.props.put("UserName", makeString(str10));
      boolean bool4 = false;
      try
      {
        bool4 = localDatabaseMetaData.supportsTransactions();
      }
      catch (SQLException localSQLException32)
      {
        setMetaDataError("TransactionSupport", localSQLException32);
      }
      this.props.put("TransactionSupport", new Boolean(bool4));
      this.props.put("CommandSupport", new Boolean(true));
      int i15 = 1;
      try
      {
        i15 = localDatabaseMetaData.getMaxStatements();
      }
      catch (SQLException localSQLException33)
      {
        setMetaDataError("MaxStatements", localSQLException33);
      }
      this.props.put("MaxStatements", new Integer(i15));
      String str11 = "\"";
      try
      {
        str11 = localDatabaseMetaData.getIdentifierQuoteString();
        if (str11 == null)
          str11 = "\"";
      }
      catch (SQLException localSQLException34)
      {
        setMetaDataError("IdentifierQuoteString", localSQLException34);
      }
      this.props.put("IdentifierQuoteString", str11);
      String str12 = ".";
      this.props.put("CatalogSeparator", str12);
      boolean bool5 = false;
      try
      {
        bool5 = localDatabaseMetaData.isCatalogAtStart();
      }
      catch (SQLException localSQLException35)
      {
        setMetaDataError("IsCatalogAtStart", localSQLException35);
      }
      this.props.put("IsCatalogAtStart", new Boolean(bool5));
      boolean bool6 = false;
      try
      {
        bool6 = localDatabaseMetaData.supportsPositionedUpdate();
      }
      catch (SQLException localSQLException36)
      {
        setMetaDataError("SupportsPosUpdate", localSQLException36);
      }
      this.props.put("SupportsPosUpdate", new Boolean(bool6));
      boolean bool7 = false;
      try
      {
        bool7 = localDatabaseMetaData.supportsPositionedDelete();
      }
      catch (SQLException localSQLException37)
      {
        setMetaDataError("SupportsPosDelete", localSQLException37);
      }
      this.props.put("SupportsPosDelete", new Boolean(bool7));
    }
    catch (SQLException localSQLException1)
    {
      throw new AdoError(localSQLException1, "Error getting capabilities for this connection: ", 10011);
    }
  }

  private void loadTypeInfos()
    throws SQLException
  {
    DatabaseMetaData localDatabaseMetaData = this.con.getMetaData();
    ResultSet localResultSet = localDatabaseMetaData.getTypeInfo();
    this.typeInfos = new Vector(localResultSet.getMetaData().getColumnCount());
    while (localResultSet.next())
      try
      {
        TypeInfo localTypeInfo = new TypeInfo(localResultSet);
        this.typeInfos.addElement(localTypeInfo);
      }
      catch (Exception localException)
      {
      }
    localResultSet.close();
  }

  TypeInfo getTypeInfo(int paramInt)
    throws SQLException
  {
    TypeInfo localTypeInfo = null;
    if (this.typeInfos == null)
      loadTypeInfos();
    int i = 0;
    for (int j = 0; (j < this.typeInfos.size()) && (i == 0); j++)
    {
      if (this.typeInfos.elementAt(j) == null)
        continue;
      localTypeInfo = (TypeInfo)this.typeInfos.elementAt(j);
      if (localTypeInfo.dataType != paramInt)
        continue;
      i = 1;
    }
    if (i == 0)
      localTypeInfo = null;
    return localTypeInfo;
  }

  void addRecset(Recordset paramRecordset)
  {
    if (this.recsets == null)
      this.recsets = new Vector(5);
    this.recsets.addElement(paramRecordset);
  }

  void removeRecset(Recordset paramRecordset)
  {
    this.recsets.removeElement(paramRecordset);
  }

  java.sql.Connection getDriverConnection()
    throws AdoError
  {
    int i = getMaxStatements();
    java.sql.Connection localConnection;
    if ((i == 0) || (i > this.numActiveStatements))
      localConnection = getConnection();
    else
      localConnection = getSecondConnection();
    return localConnection;
  }

  synchronized void incrementActiveStatements()
  {
    this.numActiveStatements += 1;
  }

  synchronized void decrementActiveStatements()
  {
    this.numActiveStatements -= 1;
  }

  Statement createStatement()
    throws AdoError
  {
    Statement localStatement = null;
    try
    {
      java.sql.Connection localConnection = getDriverConnection();
      localStatement = localConnection.createStatement();
      incrementActiveStatements();
    }
    catch (SQLException localSQLException)
    {
      throw new AdoError(localSQLException, "error creating new statement.", 10012);
    }
    return localStatement;
  }

  PreparedStatement prepareStatement(String paramString)
    throws AdoError
  {
    PreparedStatement localPreparedStatement = null;
    try
    {
      java.sql.Connection localConnection = getDriverConnection();
      localPreparedStatement = localConnection.prepareStatement(paramString);
      incrementActiveStatements();
    }
    catch (SQLException localSQLException)
    {
      throw new AdoError(localSQLException, "error preparing statement: " + paramString, 10013);
    }
    return localPreparedStatement;
  }

  CallableStatement prepareCall(String paramString)
    throws AdoError
  {
    CallableStatement localCallableStatement = null;
    try
    {
      java.sql.Connection localConnection = getDriverConnection();
      localCallableStatement = localConnection.prepareCall(paramString);
      incrementActiveStatements();
    }
    catch (SQLException localSQLException)
    {
      throw new AdoError(localSQLException, "error creating call statement: " + paramString, 10014);
    }
    return localCallableStatement;
  }

  public DatabaseMetaData getMetaData()
    throws AdoError
  {
    DatabaseMetaData localDatabaseMetaData = null;
    try
    {
      java.sql.Connection localConnection = getDriverConnection();
      localDatabaseMetaData = localConnection.getMetaData();
    }
    catch (SQLException localSQLException)
    {
      throw new AdoError(localSQLException, "error creating database metadata.", 10015);
    }
    return localDatabaseMetaData;
  }

  void closeStatement(Statement paramStatement)
    throws AdoError
  {
    try
    {
      paramStatement.close();
      decrementActiveStatements();
      paramStatement = null;
    }
    catch (SQLException localSQLException)
    {
      throw new AdoError(localSQLException, "error closing statement.", 10016);
    }
  }

  String getNextCursorName()
  {
    this.cursorCounter += 1;
    return this.cursorName + this.cursorCounter;
  }

  void println(String paramString)
  {
    if (logWriter != null)
      logWriter.println(paramString);
  }

  private void notifyRecsets(boolean paramBoolean)
    throws AdoError
  {
    if (this.recsets != null)
    {
      int i = this.recsets.size();
      AdoError localObject = null;
      for (int j = 0; j < i; j++)
      {
        Recordset localRecordset = (Recordset)this.recsets.elementAt(j);
        try
        {
          if (paramBoolean)
            localRecordset.transCommitted();
          else
            localRecordset.transRolledback();
        }
        catch (AdoError localAdoError)
        {
          localObject = localAdoError;
        }
      }
      if (localObject != null)
        throw localObject;
    }
  }

  private void closeAllRecsets()
    throws AdoError
  {
    if (this.recsets != null)
    {
      int i = this.recsets.size();
      AdoError localObject = null;
      for (int j = 0; j < i; j++)
      {
        Recordset localRecordset = (Recordset)this.recsets.elementAt(j);
        try
        {
          localRecordset.close();
        }
        catch (AdoError localAdoError)
        {
          localObject = localAdoError;
        }
      }
      if (localObject != null)
        throw localObject;
    }
  }

  private void ThrowNotConnected(String paramString)
    throws AdoError
  {
    throw new AdoError("You must have an open connection before calling: " + paramString, 10009);
  }

  private void ThrowNotInTransaction(String paramString)
    throws AdoError
  {
    throw new AdoError("You need to be in a transaction in order to call: " + paramString, 10017);
  }

  private void setMetaDataError(String paramString, SQLException paramSQLException)
    throws AdoError
  {
    throw new AdoError(paramSQLException, "Error getting capabilities for this connection: " + paramString, 10011);
  }

  private void Assert(boolean paramBoolean, String paramString)
  {
    if (!paramBoolean)
      println(paramString);
  }

  public String toString()
  {
    return "Connection[url=" + this.urlName + ",userId=" + this.userId + (this.connected ? ",connected" : "") + "]";
  }

  static
  {
    try
    {
      logWriter = DriverManager.getLogWriter();
    }
    catch (Exception localException)
    {
    }
  }
}