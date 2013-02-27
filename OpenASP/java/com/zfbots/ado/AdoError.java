package com.zfbots.ado;

import java.sql.SQLException;

import com.zfbots.util.RuntimeError;


public class AdoError extends RuntimeError
{
  private String sqlState = null;
  private String description = null;

  public AdoError()
  {
    this.source = "ADO";
  }

  public AdoError(Exception paramException, String paramString, int paramInt)
  {
    super(paramInt, "ADO", paramString);
    setNativeAdoError(paramException);
  }

  public AdoError(String paramString)
  {
    super(0, "ADO", paramString);
  }

  public AdoError(Exception paramException, String paramString)
  {
    super(0, "ADO", paramString);
    setNativeAdoError(paramException);
  }

  public AdoError(String paramString, int paramInt)
  {
    super(paramInt, "ADO", paramString);
  }

  public String getMessage()
  {
    if (this.description != null)
      return this.description;
    return super.getMessage();
  }

  public String getSQLState()
  {
    return this.sqlState;
  }

  public int getNativeError()
  {
    if ((this.nativeError instanceof SQLException))
      return ((SQLException)this.nativeError).getErrorCode();
    return 0;
  }

  public Throwable getNativeException()
  {
    return this.nativeError;
  }

  public String toString()
  {
    String str = super.toString();
    if (this.sqlState != null)
      str = str + "\r\n SQLState: " + this.sqlState;
    return str;
  }

  public void setErrorMsg(String paramString)
  {
    this.description = paramString;
  }

  public void setErrorNumber(int paramInt)
  {
    this.errorNumber = paramInt;
  }

  void setNativeAdoError(Exception paramException)
  {
    if ((paramException instanceof SQLException))
    {
      this.source = "SQL";
      SQLException localSQLException = (SQLException)paramException;
      this.errorNumber = localSQLException.getErrorCode();
      this.sqlState = localSQLException.getSQLState();
    }
    if ((this.description == null) || (this.description.length() == 0))
      this.description = paramException.getMessage();
    else
      this.description = (this.description + "\r\n" + paramException.getMessage());
    this.nativeError = paramException;
  }

  static void throwCursorNotOpen(String paramString)
    throws AdoError
  {
    throw new AdoError("You need to have an open cursor in order to call: " + paramString);
  }

  static void throwCursorNotOpenToDoOp(String paramString)
    throws AdoError
  {
    throw new AdoError("You need to have an open cursor in order to call: " + paramString);
  }

  static void throwNotSupported(String paramString)
    throws AdoError
  {
    throw new AdoError("Feature Not Supported: " + paramString);
  }
}
