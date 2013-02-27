package com.zfbots.vb;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.io.PrintStream;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.zfbots.ado.AdoError;
import com.zfbots.ado.Field;
import com.zfbots.ado.Fields;
import com.zfbots.ado.Recordset;
import com.zfbots.util.Err;


public class DataBoundRouter
  implements TextListener, ItemListener, ListSelectionListener
{
  public static DataBoundRouter dbRouter = new DataBoundRouter();
  private int initializing = 0;

  public boolean isInitializing()
  {
    return this.initializing > 0;
  }

  public void setInitializing(boolean paramBoolean)
  {
    if (paramBoolean)
      this.initializing += 1;
    else
      this.initializing -= 1;
    if (this.initializing < 0)
    {
      System.out.print("error: DataBoundRouter.initializing < 0: ");
      Err.printStackTrace();
    }
  }

  private void changeValue(DataBound paramDataBound)
  {
    if (this.initializing > 0)
      return;
    if (paramDataBound != null)
      try
      {
    	Recordset localObject = paramDataBound.getDataSource() != null ? paramDataBound.getDataSource().getRecordset() : null;
        if ((localObject != null) && (paramDataBound.getDataField() != null))
        {
          Field localField = localObject.getFields().getField(paramDataBound.getDataField());
          if (localField != null)
            localField.setValue(paramDataBound.getDataValue());
        }
      }
      catch (AdoError localAdoError)
      {
        System.out.println("DataBoundRouter.changeValue " + localAdoError);
      }
  }

  private void changeRecord(DataBound paramDataBound, int paramInt)
  {
  }

  public void textValueChanged(TextEvent paramTextEvent)
  {
    changeValue((DataBound)paramTextEvent.getSource());
  }

  public void itemStateChanged(ItemEvent paramItemEvent)
  {
    changeValue((DataBound)paramItemEvent.getSource());
  }

  public void valueChanged(ListSelectionEvent paramListSelectionEvent)
  {
    changeValue((DataBound)paramListSelectionEvent.getSource());
  }
}