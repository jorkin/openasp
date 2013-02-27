package com.zfbots.vb;

import java.io.PrintStream;

import com.zfbots.ado.AdoError;
import com.zfbots.ado.Field;
import com.zfbots.ado.Fields;
import com.zfbots.ado.Recordset;


public class DataChange
{
  public static void initialize(DataBound paramDataBound, DataChangeEvent paramDataChangeEvent, Recordset paramRecordset)
  {
    DataBoundRouter.dbRouter.setInitializing(true);
    String str = paramDataBound.getDataField();
    if ((str == null) || (paramRecordset.isEOF()))
      paramDataChangeEvent.setValue(null);
    else
      try
      {
        Field localField = paramRecordset.getFields().getField(str);
        if (localField != null)
          paramDataChangeEvent.setValue(localField.getValue());
      }
      catch (AdoError localAdoError)
      {
        if (localAdoError.getNumber() != 30011)
          System.out.println("DataChange.initialize(" + str + "): " + localAdoError);
      }
    paramDataBound.dataChanged(paramDataChangeEvent);
    DataBoundRouter.dbRouter.setInitializing(false);
  }
}