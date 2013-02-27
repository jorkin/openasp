package com.zfbots.ado;

import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import com.zfbots.util.DateTime;
import com.zfbots.util.Err;
import com.zfbots.util.Obj;
import com.zfbots.util.Strings;
import com.zfbots.util.Variant;
import com.zfbots.vb.DataBound;
import com.zfbots.vb.DataChange;
import com.zfbots.vb.DataChangeEvent;
import com.zfbots.vb.DataSource;
import com.zfbots.vb.EventListenerList;


public class Recordset implements Runnable, DataSource {
	private static final int LT = 0;
	private static final int LE = 1;
	private static final int EQ = 2;
	private static final int GE = 3;
	private static final int GT = 4;
	private static final int NE = 5;
	private static final int LIKE = 6;
	private Hashtable dirtyRecords = null;
	private boolean batchUpdateMode = false;
	private static Hashtable sqlCommands;
	private boolean keepOpenAfterTransaction;
	private boolean isOpen;
	boolean isDisconnected;
	private boolean iCursorOpen;
	private boolean isBOF;
	private boolean isEOF;
	private boolean freeOwnerCon;
	private boolean freeOwnerCommand;
	private boolean nextRecordsetCreated;
	private boolean allowUpdateOnFirstTableInJoin;
	private int cacheSize;
	private int currentRowNum;
	private int cursorType;
	private int lockType;
	private int maxRecords;
	private int numColumns;
	private int recordCount;
	private int recordStatus;
	private int recPos;
	private Properties props;
	private Vector tableNames;
	private Recordset rsClone;
	private Connection ownerCon;
	private Command ownerCommand;
	private AdoCursor cursor;
	private Statement stmt;
	private ResultSet rs;
	private Fields fields;
	private Variant filter;
	private Thread recFetchThread;
	private Command keyCommand;
	private Recordset keyRs;
	private String keyCmdStr;
	private String whereClause;
	private Command userCommand;
	private Recordset userrs;
	private boolean keyAllFetched = true;
	private boolean stopKeyThread = false;
	private Thread keyThread;
	private int minRid;
	private int maxRid;
	private boolean canGoNow;
	private boolean fetchPrev;
	private int keyRecToFetch;
	static final int BUILD_SELECT = 0;
	static final int BUILD_WHERE = 1;
	static final int BUILD_FIRST_TABLE = 2;
	static final int USE_TABLE_NAME_ONLY = 4;
	private static String autoIncQuery;
	private int numRecsAffected = -1;
	Hashtable keyDict = null;
	private Object recFetchLock = new Object();
	private AdoError backgroundFetchError;
	private boolean bgErrorSet;
	protected EventListenerList listenerList = null;

	public Recordset() {
		initialize();
		this.props = new Properties();
		this.props.put("ReadOnly", new Boolean(true));
		this.props.put("BookmarkSupport", new Boolean(true));
		this.props.put("RequerySupport", new Boolean(true));
	}

	public Recordset(Recordset paramRecordset, boolean paramBoolean) {
		initialize(paramRecordset, paramBoolean);
	}

	public Recordset cloneRecordset() {
		this.rsClone = new Recordset(this, true);
		return this.rsClone;
	}

	public Recordset getLastClone() {
		if (this.rsClone == null)
			this.rsClone = cloneRecordset();
		return this.rsClone;
	}

	public int getAbsolutePosition() throws AdoError {
		throwBgError();
		if ((!this.isOpen) && (!this.isDisconnected))
			AdoError.throwCursorNotOpenToDoOp("getAbsolutePosition");
		checkAndThrowInvalidStmtType();
		int i = -1;
		int j = getRecordsFetched();
		if (j == 0)
			i = -1;
		else if ((this.isBOF) || (this.isEOF))
			i = this.recPos;
		else
			i = this.currentRowNum;
		return i;
	}

	public void setAbsolutePosition(int paramInt) throws AdoError {
		throwBgError();
		if (this.isOpen) {
			checkAndThrowInvalidStmtType();
			if ((this.cursorType == 0) && (paramInt < this.currentRowNum))
				throw new AdoError(
						"Invalid operation for cursor type: Forward Only Cursor",
						70002);
			if ((paramInt < 1) || (paramInt == -2) || (paramInt == -3)
					|| (paramInt == -1))
				throw new AdoError("Invalid row value :" + paramInt, 70003);
			synchronized (this.recFetchLock) {
				updateIfDirty();
				moveToPosition(paramInt);
			}
			updateDataBound(10);
		} else {
			AdoError.throwCursorNotOpen("setAbsolutePosition");
		}
	}

	private void moveToPosition(int paramInt) throws AdoError {
		this.currentRowNum = 0;
		this.isBOF = false;
		this.isEOF = false;
		moveRelative(paramInt, true);
	}

	public Connection getActiveConnection() {
		return this.ownerCon;
	}

	public void setActiveConnection(Variant paramVariant) throws AdoError {
		if (paramVariant.getVarType() == 15) {
			setActiveConnection(paramVariant.toString());
		} else {
			Object localObject = paramVariant.toObject();
			if ((localObject != null) && (!(localObject instanceof Connection)))
				throw new AdoError(
						"Unknown/Unhandled variant type for this operation: setActiveConnection",
						90001);
			setActiveConnection((Connection) localObject);
		}
	}

	public void setActiveConnection(Connection paramConnection) throws AdoError {
		if ((this.isOpen) && (paramConnection == null)) {
			int i = this.ownerCommand.getStmtType();
			if (((i & 0x2) == 2) || (i == 32))
				getRecordCount(true);
			freeResources();
			this.isDisconnected = true;
		}
		if (this.ownerCon != null)
			this.ownerCon.removeRecset(this);
		if (this.freeOwnerCon) {
			freeConnection();
			this.freeOwnerCon = false;
		}
		this.ownerCon = paramConnection;
		if (this.ownerCon != null) {
			this.ownerCon.addRecset(this);
			this.isDisconnected = false;
		}
		if ((this.ownerCommand != null)
				&& (this.ownerCommand.getActiveConnection() != paramConnection))
			this.ownerCommand.setActiveConnection(paramConnection);
	}

	public void setActiveConnection(String paramString) throws AdoError {
		setActiveConnection(paramString, null, null);
	}

	public void setActiveConnection(String paramString1, String paramString2,
			String paramString3) throws AdoError {
		Connection localConnection = null;
		try {
			localConnection = new Connection();
			localConnection.setConnectionString(paramString1, paramString2,
					paramString3);
			localConnection.open();
			setActiveConnection(localConnection);
			this.freeOwnerCon = true;
		} catch (AdoError localAdoError) {
			if (localConnection != null) {
				if (localConnection.isConnected())
					localConnection.close();
				localConnection = null;
			}
			throw localAdoError;
		}
	}

	/** @deprecated */
	public boolean getBOF() {
		return this.isBOF;
	}

	public boolean isBOF() {
		return this.isBOF;
	}

	public Variant getBookmark() throws AdoError {
		throwBgError();
		if ((!this.isOpen) && (!this.isDisconnected))
			AdoError.throwCursorNotOpenToDoOp("setBookmark");
		checkAndThrowInvalidStmtType();
		Variant localVariant = new Variant(this.currentRowNum);
		return localVariant;
	}

	public void setBookmark(Variant paramVariant) throws AdoError {
		int i = paramVariant.toInt();
		if (i < 1)
			return;
		throwBgError();
		checkAndThrowInvalidStmtType();
		if ((this.cursorType == 0) && (i < this.currentRowNum))
			throw new AdoError(
					"Invalid operation for cursor type: Forward Only Cursor",
					70002);
		if (isDeleted(i))
			throw new AdoError(
					"The requested row to go has since been deleted.", 70005);
		setAbsolutePosition(i);
	}

	public int getCursorType() {
		return this.cursorType;
	}

	public void setCursorType(int paramInt) throws AdoError {
		if (this.isOpen)
			throw new AdoError(
					"The cursor is already open.  You need to close the cursor before calling: setCursorType",
					70001);
		switch (paramInt) {
		case 0:
		case 1:
		case 3:
			this.cursorType = paramInt;
			break;
		case 2:
		default:
			this.cursorType = 1;
		}
	}

	/** @deprecated */
	public boolean getEOF() {
		return this.isEOF;
	}

	public boolean isEOF() {
		return this.isEOF;
	}

	public Fields getFields() throws AdoError {
		throwBgError();
		return this.fields;
	}

	public int getFieldCount() {
		if (this.fields != null)
			return this.fields.getCount();
		return 0;
	}

	public Field getField(Variant paramVariant) throws AdoError {
		throwBgError();
		return this.fields != null ? this.fields.getField(paramVariant) : null;
	}

	public Field getField(int paramInt) throws AdoError {
		throwBgError();
		return this.fields != null ? this.fields.getField(paramInt) : null;
	}

	public Field getField(String paramString) throws AdoError {
		throwBgError();
		return this.fields != null ? this.fields.getField(paramString, null)
				: null;
	}

	public void setFieldValue(Variant paramVariant, Field paramField)
			throws AdoError {
		getField(paramVariant).setValue(paramField);
	}

	public void setFieldValue(int paramInt, Field paramField) throws AdoError {
		getField(paramInt).setValue(paramField);
	}

	public void setFieldValue(String paramString, Field paramField)
			throws AdoError {
		getField(paramString).setValue(paramField);
	}

	public int getLockType() {
		return this.lockType;
	}

	public void setLockType(int paramInt) throws AdoError {
		if (this.isOpen)
			throw new AdoError(
					"The cursor is already open.  You need to close the cursor before calling: setLockType",
					70001);
		if (paramInt == 2)
			AdoError.throwNotSupported("Lock Pessimistic");
		this.lockType = paramInt;
		if (paramInt == 4)
			this.batchUpdateMode = true;
		else
			this.batchUpdateMode = false;
		if (paramInt == 1)
			this.props.put("ReadOnly", new Boolean(true));
		else
			this.props.put("ReadOnly", new Boolean(false));
	}

	public int getMaxRecords() {
		return this.maxRecords;
	}

	public void setMaxRecords(int paramInt) throws AdoError {
		if (this.isOpen)
			throw new AdoError(
					"The cursor is already open.  You need to close the cursor before calling: setMaxRecords",
					70001);
		if (paramInt < 0)
			throw new AdoError(
					"MaxRecords must be greater than or equal to 0.", 70037);
		this.maxRecords = paramInt;
		if (this.cursor != null)
			this.cursor.setMaxRecords(paramInt);
	}

	public Properties getProperties() {
		return this.props;
	}

	public int getRecordCount() {
		if (this.filter != null)
			return this.numRecsAffected;
		if (this.cursorType == 0)
			return getRecordCount(false);
		return getRecordCount(true);
	}

	public int getRecordCount(boolean paramBoolean) {
		if (this.filter != null)
			return this.numRecsAffected;
		if (!this.isOpen)
			return -1;
		if (!allRecsFetched())
			if (paramBoolean)
				try {
					recInCache(2147483647);
				} catch (Exception localException) {
					return -1;
				}
			else
				return -1;
		return getRecordsFetched();
	}

	public int getRecordsAffected() {
		return this.numRecsAffected;
	}

	public Command getSource() {
		return this.ownerCommand;
	}

	public void setSource(Variant paramVariant) throws AdoError {
		if (this.isOpen)
			throw new AdoError(
					"The cursor is already open.  You need to close the cursor before calling: setSource",
					70001);
		if (paramVariant.getVarType() == 15) {
			setSource(paramVariant.toString());
		} else {
			Object localObject = paramVariant.toObject();
			if ((localObject != null) && (!(localObject instanceof Command)))
				throw new AdoError(
						"Unknown/Unhandled variant type for this operation: ",
						90001);
			setSource((Command) localObject);
		}
	}

	public void setSource(Command paramCommand) throws AdoError {
		if (this.isOpen)
			throw new AdoError(
					"The cursor is already open.  You need to close the cursor before calling: setSource",
					70001);
		if ((paramCommand instanceof Command)) {
			if (this.freeOwnerCommand) {
				freeCommand();
				this.freeOwnerCommand = false;
			}
			this.ownerCommand = paramCommand;
			setActiveConnection(paramCommand.getActiveConnection());
		} else {
			throw new AdoError(
					"Unknown/Unhandled object type for this operation: ", 90002);
		}
	}

	public void setSource(String paramString) throws AdoError {
		if (this.isOpen)
			throw new AdoError(
					"The cursor is already open.  You need to close the cursor before calling: setSource",
					70001);
		this.ownerCommand = new Command();
		paramString = paramString.trim();
		this.ownerCommand.setCommandText(paramString);
		this.ownerCommand.setCommandType(1);
		this.ownerCommand.setActiveConnection(this.ownerCon);
		this.freeOwnerCommand = true;
	}

	public void addNew() throws AdoError {
		throwBgError();
		if (!this.isOpen)
			AdoError.throwCursorNotOpenToDoOp("AddNew");
		checkAndThrowInvalidStmtType();
		if (this.lockType == 1)
			throwCantUpdateOnReadOnly();
		updateIfDirty();
		setRecordStatus(1);
		clearFields();
		addRowInCache();
		updateDataBound(1);
	}

	public void addNew(Vector paramVector1, Vector paramVector2)
			throws AdoError {
		throwBgError();
		if (!this.isOpen)
			AdoError.throwCursorNotOpenToDoOp("AddNew");
		if (this.lockType == 1)
			throwCantUpdateOnReadOnly();
		checkAndThrowInvalidStmtType();
		clearFields();
		addRowInCache();
		setNewValues(paramVector1, paramVector2);
		setRecordStatus(1);
		update();
	}

	private void updateBatch_() throws AdoError {
		if (this.batchUpdateMode)
			addBatchUpdate();
		updateRowInCache(null);
	}

	private void updateIfDirty() throws AdoError {
		if ((this.recordStatus == 1) || (isDirty()))
			if ((this.ownerCon == null) || (this.batchUpdateMode))
				updateBatch_();
			else
				update();
	}

	public void update() throws AdoError {
		throwBgError();
		if (!this.isOpen)
			AdoError.throwCursorNotOpenToDoOp("Update");
		if (this.lockType == 1)
			throwCantUpdateOnReadOnly();
		checkAndThrowInvalidStmtType();
		if ((this.recordStatus == 8) && (!isDirty()))
			throw new AdoError("there's nothing to update.", 70008);
		if ((this.ownerCon == null) || (this.batchUpdateMode)
				|| (this.isDisconnected))
			updateBatch_();
		else
			update_();
		updateDataBound(3);
	}

	private void update_() throws AdoError {
		synchronized (this.recFetchLock) {
			if (this.recordStatus == 1) {
				addNewRecord();
				this.isEOF = (this.isBOF = false);
			} else {
				modifyRecord(false);
			}
		}
		setRecordStatus(8);
		clearFields();
	}

	public void update(Vector paramVector1, Vector paramVector2)
			throws AdoError {
		throwBgError();
		if (!this.isOpen)
			AdoError.throwCursorNotOpenToDoOp("AddNew");
		if (this.lockType == 1)
			throwCantUpdateOnReadOnly();
		checkAndThrowInvalidStmtType();
		setNewValues(paramVector1, paramVector2);
		update();
	}

	public void cancelUpdate() throws AdoError {
		throwBgError();
		if ((this.recordStatus == 2) || (this.recordStatus == 1) || (isDirty()))
			for (int i = 0; i < this.fields.getCount(); i++) {
				Field localField = this.fields.getField(i);
				if (!localField.isDirty())
					continue;
				localField.restoreOldValue();
			}
		int i = this.recordStatus;
		setRecordStatus(8);
		clearFields();
		if ((i == 1) && (this.currentRowNum > 0))
			this.cursor.removeFromCache(this.currentRowNum - 1);
		if ((i == 1) && (this.cursorType != 0) && (this.currentRowNum > 0))
			synchronized (this.recFetchLock) {
				fillRecordBuffer(true);
			}
	}

	public void delete() throws AdoError {
		throwBgError();
		if (!this.isOpen)
			AdoError.throwCursorNotOpenToDoOp("Delete");
		if (this.lockType == 1)
			throwCantUpdateOnReadOnly();
		checkAndThrowInvalidStmtType();
		setRecordStatus(4);
		if (this.batchUpdateMode) {
			addBatchUpdate();
			deleteRowInCache();
		} else {
			modifyRecord(true);
		}
	}

	public Variant[][] getRows(Variant paramVariant, Vector paramVector)
			throws AdoError {
		return getRows(-1, paramVariant, paramVector);
	}

	public Variant[][] getRows(int paramInt, Variant paramVariant,
			Vector paramVector) throws AdoError {
		throwBgError();
		checkAndThrowInvalidStmtType();
		return getRows_(paramInt, paramVariant, paramVector);
	}

	public void move(int paramInt, Variant paramVariant) throws AdoError {
		throwBgError();
		checkAndThrowInvalidStmtType();
		if (paramVariant != null)
			setBookmark(paramVariant);
		move_(paramInt);
		updateDataBound(10);
	}

	public void move(int paramInt) throws AdoError {
		throwBgError();
		if ((!this.isOpen) && (!this.isDisconnected))
			AdoError.throwCursorNotOpenToDoOp("Move");
		move_(paramInt);
		updateDataBound(10);
	}

	public void moveFirst() throws AdoError {
		throwBgError();
		moveFirst_();
		updateDataBound(12);
	}

	public void moveLast() throws AdoError {
		throwBgError();
		moveLast_();
		updateDataBound(15);
	}

	public void movePrevious() throws AdoError {
		throwBgError();
		movePrevious_();
		updateDataBound(14);
	}

	public void moveNext() throws AdoError {
		throwBgError();
		moveNext_();
		updateDataBound(13);
	}

	public void open() throws AdoError {
		throwBgError();
		execQuery();
		updateDataBound(7);
	}

	public void open(String paramString) throws AdoError {
		throwBgError();
		if (paramString != null)
			setSource(paramString);
		open();
	}

	public void open(Command paramCommand, Connection paramConnection,
			int paramInt1, int paramInt2) throws AdoError {
		throwBgError();
		if (paramCommand != null)
			setSource(paramCommand);
		if (paramConnection != null)
			setActiveConnection(paramConnection);
		setCursorType(paramInt1);
		setLockType(paramInt2);
		open();
	}

	public void open(Variant paramVariant1, Variant paramVariant2,
			int paramInt1, int paramInt2, int paramInt3) throws AdoError {
		throwBgError();
		if (paramVariant1 != null)
			setSource(paramVariant1);
		if (paramVariant2 != null)
			setActiveConnection(paramVariant2);
		setCursorType(paramInt1);
		setLockType(paramInt2);
		this.ownerCommand.setCommandType(paramInt3);
		open();
	}

	public void open(String paramString, Connection paramConnection,
			int paramInt1, int paramInt2, int paramInt3) throws AdoError {
		throwBgError();
		if (paramString != null)
			setSource(paramString);
		if (paramConnection != null)
			setActiveConnection(paramConnection);
		setCursorType(paramInt1);
		setLockType(paramInt2);
		this.ownerCommand.setCommandType(paramInt3);
		open();
	}

	public void open(String paramString1, String paramString2, int paramInt1,
			int paramInt2, int paramInt3) throws AdoError {
		throwBgError();
		if (paramString1 != null)
			setSource(paramString1);
		if (paramString2 != null)
			setActiveConnection(paramString2);
		setCursorType(paramInt1);
		setLockType(paramInt2);
		this.ownerCommand.setCommandType(paramInt3);
		open();
	}

	private static boolean conditionMet(Condition paramCondition,
			Variant paramVariant) {
		if (paramCondition.compareType == 6)
			return paramVariant.toString().toUpperCase()
					.matches(paramCondition.compareTo.toString());
		int i = Obj.compare(paramVariant, paramCondition.compareTo, false);
		switch (paramCondition.compareType) {
		case 0:
			return i < 0;
		case 1:
			return i <= 0;
		case 2:
			return i == 0;
		case 3:
			return i >= 0;
		case 4:
			return i > 0;
		case 5:
			return i != 0;
		}
		return false;
	}

	private boolean matches(Vector paramVector) throws AdoError {
		int i = paramVector.size();
		boolean j = true;
		for (int k = 0; k < i; k++) {
			Condition localCondition = (Condition) paramVector.get(k);
			boolean bool = conditionMet(localCondition,
					localCondition.field.getValue());
			if (localCondition.combineAsAnd) {
				if ((j != false) && (bool))
					j = true;
				else
					j = false;
			} else if ((j != false) || (bool))
				j = true;
			else
				j = false;
		}
		return j;
	}

	private Vector parseCriteria(String paramString) throws AdoError {
		paramString = paramString.replace('(', ' ');
		paramString = paramString.replace(')', ' ');
		Vector localVector = new Vector();
		String str1 = paramString.toUpperCase();
		int i = 0;
		String str2 = paramString;
		int j;
		int m;
		for (boolean bool = true;; bool = m == j) {
			j = str1.indexOf(" AND ", i);
			int k = str1.indexOf(" OR ", i);
			if ((j < 0) && (k < 0)) {
				localVector.add(parseCondition(str2, bool));
				break;
			}
			m = Math.min(j < 0 ? 999999 : j, k < 0 ? 999999 : k);
			str2 = paramString.substring(i, m);
			localVector.add(parseCondition(str2, bool));
			i = m + 4;
			str2 = paramString.substring(i);
		}
		return localVector;
	}

	private Condition parseCondition(String paramString, boolean paramBoolean)
			throws AdoError {
		Condition localCondition = new Condition();
		localCondition.combineAsAnd = paramBoolean;
		String str1 = "";
		Object localObject1 = "";
		String str2 = paramString.toUpperCase();
		int i = str2.indexOf(" LIKE ");
		Object localObject2;
		if (i > 0) {
			str1 = paramString.substring(0, i).trim();
			localObject1 = paramString.substring(i + 6).trim();
			localObject1 = ((String) localObject1).replace('_', '.');
			localObject1 = Strings.replace((String) localObject1, "*", ".*");
			localObject1 = Strings.replace((String) localObject1, "%", ".*");
			localObject1 = ((String) localObject1).toUpperCase();
			localCondition.compareType = 6;
		} else {
			localObject2 = new StringTokenizer(paramString, "<=>", true);
			if (((StringTokenizer) localObject2).hasMoreTokens()) {
				str1 = ((StringTokenizer) localObject2).nextToken();
				str1 = str1.replace('[', ' ');
				str1 = str1.replace(']', ' ').trim();
			}
			if (((StringTokenizer) localObject2).hasMoreTokens()) {
				String str3 = ((StringTokenizer) localObject2).nextToken()
						.trim();
				if (((StringTokenizer) localObject2).hasMoreTokens()) {
					String str4 = ((StringTokenizer) localObject2).nextToken()
							.trim();
					if ((str4.equals("=")) || (str4.equals(">")))
						str3 = str3 + str4;
					else
						localObject1 = str4;
				}
				if (str3.equals("<"))
					localCondition.compareType = 0;
				else if (str3.equals("<="))
					localCondition.compareType = 1;
				else if (str3.equals("="))
					localCondition.compareType = 2;
				else if (str3.equals(">="))
					localCondition.compareType = 3;
				else if (str3.equals(">"))
					localCondition.compareType = 4;
				else if (str3.equals("<>"))
					localCondition.compareType = 5;
			}
			if (((StringTokenizer) localObject2).hasMoreTokens())
				localObject1 = (String) localObject1
						+ ((StringTokenizer) localObject2).nextToken().trim();
		}
		localCondition.field = this.fields.getField(str1);
		localCondition.compareTo.set((String) localObject1);
		if (((String) localObject1).length() > 0) {
			if (((String) localObject1).charAt(0) == '#') {
				localObject2 = ((String) localObject1).substring(1,
						((String) localObject1).length() - 1);
				try {
					localCondition.compareTo.set(DateTime
							.toDate((String) localObject2));
				} catch (Exception localException) {
					throw new AdoError(localException, "Error parsing date: "
							+ (String) localObject2);
				}
			}
			if ((((String) localObject1).charAt(0) == '\'')
					&& (((String) localObject1).charAt(((String) localObject1)
							.length() - 1) == '\''))
				localCondition.compareTo.set(((String) localObject1).substring(
						1, ((String) localObject1).length() - 1));
		}
		if (localCondition.compareTo.isNumeric())
			localCondition.compareTo.set(localCondition.compareTo.toInt());
		return (Condition) (Condition) localCondition;
	}

	public boolean findFirst(String paramString) throws AdoError {
		if ((this.isEOF) && (this.isBOF))
			return false;
		moveFirst();
		return findNext(paramString);
	}

	public boolean findNext(String paramString) throws AdoError {
		if ((this.isEOF) && (this.isBOF))
			return false;
		updateIfDirty();
		Vector localVector = parseCriteria(paramString);
		while (!this.isEOF) {
			if (matches(localVector)) {
				updateDataBound(13);
				return true;
			}
			getNext();
		}
		updateDataBound(15);
		return false;
	}

	public boolean findPrevious(String paramString) throws AdoError {
		if ((this.isEOF) && (this.isBOF))
			return false;
		updateIfDirty();
		Vector localVector = parseCriteria(paramString);
		while (!this.isBOF) {
			if (matches(localVector)) {
				updateDataBound(14);
				return true;
			}
			getPrev();
		}
		updateDataBound(12);
		return false;
	}

	public boolean findLast(String paramString) throws AdoError {
		if ((this.isEOF) && (this.isBOF))
			return false;
		moveLast();
		return findPrevious(paramString);
	}

	public boolean find(String paramString, int paramInt1, int paramInt2,
			Variant paramVariant) throws AdoError {
		updateIfDirty();
		if (paramVariant != null)
			setBookmark(paramVariant);
		if (paramInt1 != 0)
			if ((paramInt1 > 0) && (!this.isEOF))
				moveRelative(paramInt1, false);
			else if ((paramInt1 < 0) && (!this.isBOF))
				moveRelative(paramInt1, false);
		if (paramInt2 >= 0)
			return findNext(paramString);
		return findPrevious(paramString);
	}

	public Variant getFilter() {
		return this.filter;
	}

	public void setFilter(Variant paramVariant) throws AdoError {
		this.filter = null;
		this.numRecsAffected = -1;
		Variant localVariant = paramVariant;
		if ((localVariant != null) && (localVariant.isNumeric())
				&& (localVariant.toInt() == 0))
			localVariant = null;
		if (localVariant == null) {
			this.cursor.setAllFilteredOut(false);
		} else {
			getRecordCount(true);
			Object localObject1;
			Object localObject2;
			if (localVariant.getVarType() == 15) {
				this.numRecsAffected = 0;
				localObject1 = localVariant.toString();
				if ((localObject1 == null)
						|| (((String) localObject1).length() == 0))
					return;
				localObject2 = parseCriteria(localVariant.toString());
				moveFirst_();
				while (!this.isEOF) {
					if (matches((Vector) localObject2)) {
						this.cursor.setFilteredOut(this.currentRowNum - 1,
								false);
						this.numRecsAffected += 1;
					} else {
						this.cursor
								.setFilteredOut(this.currentRowNum - 1, true);
					}
					getNext();
				}
			}
			if ((localVariant.isNumeric()) && (localVariant.toInt() == 1)) {
				this.numRecsAffected = 0;
				this.cursor.setAllFilteredOut(true);
				if (this.dirtyRecords != null) {
					this.numRecsAffected = this.dirtyRecords.size();
					localObject2 = this.dirtyRecords.elements();
					while (((Enumeration) localObject2).hasMoreElements()) {
						localObject1 = (DirtyRecord) ((Enumeration) localObject2)
								.nextElement();
						this.cursor.setFilteredOut(
								((DirtyRecord) localObject1).getRowNum() - 1,
								false);
					}
				}
			}
			this.filter = new Variant(localVariant);
			moveFirst_();
		}
		updateDataBound(7);
	}

	public void requery() throws AdoError {
		throwBgError();
		requery_();
		updateDataBound(7);
	}

	public static void setAutoIncrementQuery(String paramString) {
		autoIncQuery = paramString;
	}

	public void setAllowUpdateOnFirstTableInJoins(boolean paramBoolean) {
		this.allowUpdateOnFirstTableInJoin = paramBoolean;
	}

	public String getClipString() throws AdoError {
		throwBgError();
		return getClipString_(-1, null, null, null, null, null);
	}

	public String getClipString(int paramInt) throws AdoError {
		throwBgError();
		return getClipString_(paramInt, null, null, null, null, null);
	}

	public String getClipString(int paramInt, Variant paramVariant)
			throws AdoError {
		throwBgError();
		return getClipString_(paramInt, paramVariant, null, null, null, null);
	}

	public String getClipString(int paramInt, Variant paramVariant,
			Vector paramVector) throws AdoError {
		throwBgError();
		return getClipString_(paramInt, paramVariant, paramVector, null, null,
				null);
	}

	public String getClipString(int paramInt, Variant paramVariant,
			Vector paramVector, String paramString) throws AdoError {
		throwBgError();
		return getClipString_(paramInt, paramVariant, paramVector, paramString,
				null, null);
	}

	public String getClipString(int paramInt, Variant paramVariant,
			Vector paramVector, String paramString1, String paramString2)
			throws AdoError {
		throwBgError();
		return getClipString_(paramInt, paramVariant, paramVector,
				paramString1, paramString2, null);
	}

	public String getClipString(int paramInt, Variant paramVariant,
			Vector paramVector, String paramString1, String paramString2,
			String paramString3) throws AdoError {
		throwBgError();
		return getClipString_(paramInt, paramVariant, paramVector,
				paramString1, paramString2, paramString3);
	}

	public Recordset nextRecordset() throws AdoError {
		throwBgError();
		if (!this.isOpen)
			AdoError.throwCursorNotOpenToDoOp("NextRecordset");
		if ((this.ownerCommand == null) || (this.ownerCon == null))
			throw new AdoError(
					"Recordset is no longer associated with a connection/query",
					70009);
		if (!this.ownerCon.isConnected())
			throw new AdoError(
					"You must have an open connection before calling: ", 10009);
		checkAndThrowInvalidStmtType();
		if (this.nextRecordsetCreated)
			throw new AdoError(
					"NextRecordset can be called only once per Recordset.",
					70010);
		return getNextRecordset();
	}

	public Recordset getRecordset() {
		return this;
	}

	public void addDataBound(DataBound paramDataBound) {
		if (this.listenerList == null)
			this.listenerList = new EventListenerList();
		this.listenerList.remove(DataBound.class, paramDataBound);
		this.listenerList.add(DataBound.class, paramDataBound);
		DataChange.initialize(paramDataBound, new DataChangeEvent(this, 7),
				this);
	}

	public void removeDataBound(DataBound paramDataBound) {
		if (this.listenerList != null)
			this.listenerList.remove(DataBound.class, paramDataBound);
	}

	protected void updateDataBound(int paramInt) {
		if (this.listenerList == null)
			return;
		if (((this.isEOF) || (this.isBOF)) && ((!this.isEOF) || (!this.isBOF)))
			return;
		Object[] arrayOfObject = this.listenerList.getListenerList();
		DataChangeEvent localDataChangeEvent = null;
		for (int i = arrayOfObject.length - 2; i >= 0; i -= 2) {
			if (arrayOfObject[i] != DataBound.class)
				continue;
			if (localDataChangeEvent == null)
				localDataChangeEvent = new DataChangeEvent(this, paramInt);
			DataBound localDataBound = (DataBound) arrayOfObject[(i + 1)];
			DataChange.initialize(localDataBound, localDataChangeEvent, this);
		}
	}

	public Variant getValueAt(int paramInt1, int paramInt2) {
		try {
			if (paramInt1 == this.currentRowNum - 1)
				return getField(paramInt2).getValue();
			return this.cursor.getValueAt(paramInt1, paramInt2,
					this.filter != null);
		} catch (Exception localException) {
		}
		return null;
	}

	public void reExecuteQuery() throws AdoError {
		throwBgError();
		checkAndThrowInvalidStmtType();
		requery_();
	}

	public boolean isOpen() {
		return this.isOpen;
	}

	ResultSet getResultset() {
		return this.rs;
	}

	void setRecordCount(int paramInt) {
		this.recordCount = paramInt;
	}

	boolean isCursorOnValidRow() {
		boolean i = true;
		if ((!this.isOpen)
				|| ((this.recordStatus != 1) && ((this.isEOF) || (this.isBOF) || (isDeleted()))))
			i = false;
		return i;
	}

	void closeCursor() throws AdoError {
		println("in closeCursor");
		if (this.recFetchThread != null)
			abortRecFetchThread();
		if (this.keyThread != null)
			abortKeyThread();
		if (this.iCursorOpen) {
			if (this.rs != null) {
				try {
					this.rs.close();
				} catch (Exception localException1) {
				}
				this.rs = null;
			}
			if (this.stmt != null) {
				try {
					this.ownerCon.closeStatement(this.stmt);
				} catch (Exception localException2) {
				}
				this.stmt = null;
			}
			this.iCursorOpen = false;
		}
	}

	public void setKeepOpenAfterTransaction(boolean paramBoolean) {
		this.keepOpenAfterTransaction = paramBoolean;
	}

	void transCommitted() throws AdoError {
		if ((this.ownerCon == null) || (this.keepOpenAfterTransaction))
			return;
		Integer localInteger = (Integer) this.ownerCon.getProperties().get(
				"PrepareCommitBehavior");
		if (localInteger.intValue() == 1)
			freeResources();
	}

	void transRolledback() throws AdoError {
		if ((this.ownerCon == null) || (this.keepOpenAfterTransaction))
			return;
		Integer localInteger = (Integer) this.ownerCon.getProperties().get(
				"PrepareAbortBehavior");
		if (localInteger.intValue() == 1)
			freeResources();
	}

	private void freeKeyRsResources() throws AdoError {
		if (this.keyRs != null) {
			this.keyRs.close();
			this.keyRs = null;
			this.keyAllFetched = true;
		}
		if (this.keyCommand != null) {
			Connection localConnection = this.keyCommand.getActiveConnection();
			if ((localConnection != null) && (this.ownerCon != localConnection))
				localConnection.close();
			this.keyCommand = null;
		}
	}

	private void freeUserRsResources() throws AdoError {
		if (this.userrs != null) {
			this.userrs.close();
			this.userrs = null;
		}
		if (this.userCommand != null) {
			Connection localConnection = this.userCommand.getActiveConnection();
			if ((localConnection != null) && (this.ownerCon != localConnection))
				localConnection.close();
			this.userCommand = null;
		}
	}

	private void move_(int paramInt) throws AdoError {
		checkAndThrowInvalidStmtType();
		synchronized (this.recFetchLock) {
			if (paramInt != 0) {
				if ((this.cursorType == 0) && (paramInt < 0))
					throw new AdoError(
							"Invalid operation for cursor type: Forward Only Cursor",
							70002);
				updateIfDirty();
				moveRelative(paramInt, false);
			}
		}
	}

	private void moveFirst_() throws AdoError {
		synchronized (this.recFetchLock) {
			if ((!this.isOpen) && (!this.isDisconnected))
				AdoError.throwCursorNotOpenToDoOp("MoveFirst");
			checkAndThrowInvalidStmtType();
			updateIfDirty();
			getFirst();
		}
	}

	private void moveLast_() throws AdoError {
		synchronized (this.recFetchLock) {
			if ((!this.isOpen) && (!this.isDisconnected))
				AdoError.throwCursorNotOpenToDoOp("MoveLast");
			checkAndThrowInvalidStmtType();
			updateIfDirty();
			getLast();
		}
	}

	private void movePrevious_() throws AdoError {
		synchronized (this.recFetchLock) {
			if ((!this.isOpen) && (!this.isDisconnected))
				AdoError.throwCursorNotOpenToDoOp("MovePrevious");
			checkAndThrowInvalidStmtType();
			if (this.cursorType == 0)
				throw new AdoError(
						"Invalid operation for cursor type: Forward Only Cursor",
						70002);
			if (this.isBOF)
				throw new AdoError(
						"You cannot move back when the cursor is on BOF.",
						70012);
			updateIfDirty();
			getPrev();
		}
	}

	private void moveNext_() throws AdoError {
		synchronized (this.recFetchLock) {
			if ((!this.isOpen) && (!this.isDisconnected))
				AdoError.throwCursorNotOpenToDoOp("MoveNext");
			checkAndThrowInvalidStmtType();
			if (this.isEOF)
				throw new AdoError(
						"You cannot move forward when the cursor is on EOF.",
						70013);
			updateIfDirty();
			getNext();
		}
	}

	private String getClipString_(int paramInt, Variant paramVariant,
			Vector paramVector, String paramString1, String paramString2,
			String paramString3) throws AdoError {
		if ((!this.isOpen) && (!this.isDisconnected))
			AdoError.throwCursorNotOpenToDoOp("getClipString");
		checkAndThrowInvalidStmtType();
		String str1 = "";
		synchronized (this.recFetchLock) {
			if (paramVariant != null)
				setBookmark(paramVariant);
			if ((this.isBOF) || (this.isEOF))
				throw new AdoError(
						"You have to be positioned on a valid record when you call: getClipString",
						70004);
			int i = this.fields.getCount();
			if (paramVector != null)
				i = paramVector.size();
			char c1 = '\t';
			char c2 = '\r';
			String str2 = paramString1 != null ? paramString1 : String
					.valueOf(c1);
			String str3 = paramString2 != null ? paramString2 : String
					.valueOf(c2);
			String str4 = paramString3 != null ? paramString3 : "";
			for (int j = 0; (!this.isEOF)
					&& ((paramInt == -1) || (j < paramInt)); j++) {
				if (j > 0)
					str1 = str1 + str3;
				for (int k = 0; k < i; k++) {
					Field localField;
					if (paramVector != null)
						localField = this.fields.getField(paramVector
								.elementAt(k));
					else
						localField = this.fields.getField(k);
					if (k > 0)
						str1 = str1 + str2;
					if (localField.isNull())
						str1 = str1 + str4;
					else
						str1 = str1 + localField.getValue().toString();
				}
				getNext();
			}
		}
		return str1;
	}

	private Variant[][] getRows_(int paramInt, Variant paramVariant,
			Vector paramVector) throws AdoError {
		if ((!this.isOpen) && (!this.isDisconnected))
			AdoError.throwCursorNotOpenToDoOp("GetRows");
		checkAndThrowInvalidStmtType();
		Variant[][] arrayOfVariant = (Variant[][]) null;
		synchronized (this.recFetchLock) {
			if ((paramInt != -1) && (paramInt < 1))
				throw new AdoError(
						"Invalid value passed for numRecords in GetRows.",
						70014);
			if (paramVariant != null)
				setBookmark(paramVariant);
			if ((this.isBOF) || (this.isEOF))
				throw new AdoError(
						"You have to be positioned on a valid record when you call: GetRows",
						70004);
			int i = this.fields.getCount();
			if (paramVector != null)
				i = paramVector.size();
			int j = this.currentRowNum + paramInt;
			if (paramInt == -1)
				j = 2147483647;
			if ((!recInCache(j)) && (getRecordsFetched() == 0)) {
				this.isEOF = (this.isBOF = true);
				this.recPos = -3;
				this.currentRowNum = 0;
			}
			j = getRecordsFetched() - this.currentRowNum;
			if ((j < paramInt) || (paramInt == -1))
				j++;
			else if (j > paramInt)
				j = paramInt;
			arrayOfVariant = new Variant[i][j];
			for (int k = 0; (!this.isEOF) && (k < j); k++) {
				for (int m = 0; m < i; m++) {
					Field localField;
					if (paramVector != null)
						localField = this.fields.getField(paramVector
								.elementAt(m));
					else
						localField = this.fields.getField(m);
					Variant localVariant = localField.getValue();
					if (localVariant == null)
						localVariant = new Variant((Object) null, 1);
					arrayOfVariant[m][k] = localVariant;
				}
				getNext();
			}
		}
		return arrayOfVariant;
	}

	private void reExecQuery() throws AdoError {
		synchronized (this.recFetchLock) {
			try {
				int i = this.ownerCommand.getCommandType();
				switch (i) {
				case 1024:
					execGetTables();
					break;
				case 2048:
					execGetColumns();
					break;
				case 4096:
					execGetPrimaryKeys();
					break;
				case 8192:
					execGetForeignKeys();
					break;
				case 65536:
					execGetExportedKeys();
					break;
				case 131072:
					execGetCrossReference();
					break;
				case 16384:
					execGetUniqueIndexes();
					break;
				case 32768:
					execGetNonUniqueIndexes();
					break;
				default:
					if ((this.cursorType == 3) || (this.cursorType == 0)) {
						if (!this.ownerCommand.getPrepared())
							executeStmt(this.ownerCommand.getSQLStatement());
						else
							executePreparedStmt();
					} else
						executeKeyQuery();
				}
			} catch (SQLException localSQLException) {
				throw new AdoError(localSQLException, "Error executing query: "
						+ this.ownerCommand.getSQLStatement(), 70016);
			}
			prepareToFetch();
		}
	}

	private Recordset getNextRecordset() throws AdoError {
		checkAndThrowInvalidStmtType();
		this.nextRecordsetCreated = true;
		Statement localStatement = getStatement();
		Recordset localRecordset = null;
		try {
			if ((localStatement.getMoreResults())
					&& (localStatement.getUpdateCount() == -1)) {
				ResultSet localResultSet = localStatement.getResultSet();
				if (localResultSet != null) {
					localRecordset = new Recordset(this, false);
					localRecordset.rs = localResultSet;
					localRecordset.prepareToFetch();
				}
			}
		} catch (SQLException localSQLException) {
			throw new AdoError(localSQLException,
					"Error getting next recordset.", 0);
		}
		close();
		return localRecordset;
	}

	private void requery_() throws AdoError {
		synchronized (this.recFetchLock) {
			closeCursor();
			if (this.userrs != null)
				this.userrs.close();
			if (this.keyRs != null)
				this.keyRs.close();
			if (!this.isOpen)
				prepareQuery();
			try {
				reExecQuery();
			} catch (AdoError localAdoError) {
				this.ownerCommand.setExecuting(false);
				throw localAdoError;
			}
		}
	}

	public void resync() throws AdoError {
		throwBgError();
		checkAndThrowInvalidStmtType();
		synchronized (this.recFetchLock) {
			switch (this.cursorType) {
			case 3:
				reExecuteQuery();
				break;
			case 1:
				if (!this.isOpen) {
					requery();
				} else {
					int i = this.minRid;
					this.minRid = (this.maxRid = 0);
					recInCache(i);
				}
			case 0:
			case 2:
			}
		}
	}

	private PreparedStatement getPreparedStatement() throws AdoError {
		Object localObject = null;
		if (this.ownerCommand.getCommandType() == 4)
			localObject = this.ownerCommand.getCallableStmt();
		else
			localObject = this.ownerCommand.getPreparedStmt();
		return (PreparedStatement) localObject;
	}

	private Statement getStatement() throws AdoError {
		Object localObject = null;
		if (this.ownerCommand.getPrepared())
			localObject = getPreparedStatement();
		else
			localObject = this.stmt;
		return (Statement) localObject;
	}

	private void allocBuffers() throws AdoError {
		int i = getNumColumns();
		this.fields = new Fields(i);
		try {
			ResultSetMetaData localResultSetMetaData = this.rs.getMetaData();
			for (int j = 0; j < i; j++) {
				Field localField = new Field(this, localResultSetMetaData,
						j + 1);
				this.fields.add(localField);
			}
		} catch (Exception localException) {
			throw new AdoError(localException,
					"error getting recordset metadata.", 70018);
		}
	}

	private void allocBuffers(Fields paramFields) throws AdoError {
		int i = paramFields.getCount();
		this.fields = new Fields(i);
		for (int j = 0; j < i; j++) {
			Field localField1 = paramFields.getField(j);
			Field localField2 = new Field(this, localField1);
			this.fields.add(localField2);
		}
	}

	private void fetchRecords() throws AdoError {
		if (this.fields == null) {
			allocBuffers();
			this.cursor = new AdoCursor(this, this.fields);
			this.cursor.setMaxRecords(this.maxRecords);
		} else {
			this.cursor.reset();
		}
		this.cursor.setResultSet(this.rs);
		if ((this.recFetchThread == null)
				&& ((this.cursorType != 0) || (this.cacheSize > 0))) {
			this.recFetchThread = new Thread(this.cursor);
			if (this.recFetchThread != null)
				this.recFetchThread.setPriority(3);
			if (this.recFetchThread != null)
				this.recFetchThread.start();
			println("fetching records.");
		}
	}

	public int getRecordsFetched() {
		int i = 0;
		if ((this.cursorType == 1) && (this.keyRs != null))
			i = this.keyRs.getRecordsFetched();
		else if (this.cursor != null)
			i = this.cursor.getRecordsFetched();
		return i;
	}

	private boolean recInCache(int paramInt) throws AdoError {
		boolean bool = true;
		int i = this.ownerCommand.getStmtType();
		checkAndThrowInvalidStmtType();
		if (this.cursorType == 1) {
			if (((paramInt > this.maxRid) || (paramInt < this.minRid))
					&& ((paramInt < 1) || (this.keyRs == null) || (!fetchKeyRecords(paramInt))))
				bool = false;
		} else if (this.cursor != null)
			bool = this.cursor.getRecord(paramInt);
		else
			bool = false;
		return bool;
	}

	private void moveRelative(int paramInt, boolean paramBoolean)
			throws AdoError {
		int i = this.currentRowNum + paramInt;
		if (i < 1) {
			if ((this.isBOF) && (paramInt < 0))
				throw new AdoError(
						"You cannot move back when the cursor is on BOF.",
						70012);
			if (!paramBoolean) {
				this.isBOF = true;
				this.recPos = -2;
				this.currentRowNum = 0;
				return;
			}
			i = 1;
		} else if ((this.isEOF) && (paramInt > 0)) {
			throw new AdoError(
					"You cannot move forward when the cursor is on EOF.", 70013);
		}
		if (!recInCache(i)) {
			if (!paramBoolean) {
				this.isEOF = true;
				this.recPos = -3;
				this.currentRowNum = (getRecordsFetched() + 1);
				return;
			}
			if (getRecordsFetched() == 0) {
				this.isEOF = (this.isBOF = true);
				this.recPos = -3;
				this.currentRowNum = 0;
				return;
			}
			gotoLastRow();
		} else {
			gotoRow(i, paramInt >= 0);
		}
		if ((isDeleted()) && (!paramBoolean)) {
			if (paramInt < 0)
				getPrev();
			else
				getNext();
		} else {
			this.isBOF = false;
			this.isEOF = false;
			fillRecordBuffer(true);
		}
	}

	private void getNext() throws AdoError {
		if ((this.cursorType == 0) && (this.cacheSize == 0)) {
			this.isEOF = this.cursor.moveCursor();
			this.currentRowNum += 1;
			if (!this.isEOF) {
				fillRecordBuffer(false);
			} else {
				setAllRecsFetched(true);
				this.recPos = -3;
				this.recordCount = getRecordsFetched();
			}
		} else if (!recInCache(this.currentRowNum + 1)) {
			this.currentRowNum += 1;
			this.isEOF = true;
			this.recPos = -3;
		} else {
			this.isEOF = false;
			gotoNextRow();
			if (isDeleted()) {
				getNext();
			} else {
				this.isBOF = false;
				fillRecordBuffer(true);
			}
		}
	}

	private void getPrev() throws AdoError {
		if (!recInCache(this.currentRowNum - 1)) {
			this.currentRowNum = 0;
			this.isBOF = true;
			this.recPos = -2;
		} else {
			this.isBOF = false;
			gotoPreviousRow();
			if (isDeleted()) {
				getPrev();
			} else {
				this.isEOF = false;
				fillRecordBuffer(true);
			}
		}
	}

	private void getFirst() throws AdoError {
		if (!recInCache(1)) {
			this.isEOF = true;
			this.isBOF = true;
			this.recPos = -3;
			this.currentRowNum = -1;
			return;
		}
		this.isBOF = false;
		this.isEOF = false;
		gotoFirstRow();
		if (isDeleted()) {
			getNext();
			if (this.isEOF) {
				this.isBOF = true;
				this.recPos = -2;
				this.currentRowNum = 0;
			}
		} else {
			fillRecordBuffer(true);
		}
	}

	private void getLast() throws AdoError {
		if ((!recInCache(2147483647)) && (getRecordsFetched() == 0)) {
			this.isEOF = true;
			this.isBOF = true;
			this.recPos = -3;
			this.currentRowNum = 0;
			return;
		}
		this.isBOF = false;
		this.isEOF = false;
		gotoLastRow();
		if (isDeleted()) {
			getPrev();
			if (this.isBOF) {
				this.currentRowNum = 0;
				this.isEOF = true;
				this.recPos = -3;
			}
		} else {
			fillRecordBuffer(true);
		}
	}

	private void gotoNextRow() throws AdoError {
		this.currentRowNum += 1;
		if (this.filter != null) {
			int i = this.cursor.getRecordsFetched();
			while ((this.currentRowNum <= i)
					&& (this.cursor.isFilteredOut(this.currentRowNum - 1)))
				this.currentRowNum += 1;
			if (this.currentRowNum > i)
				this.isEOF = true;
		}
	}

	private void gotoPreviousRow() throws AdoError {
		this.currentRowNum -= 1;
		if (this.filter != null) {
			while ((this.currentRowNum > 0)
					&& (this.cursor.isFilteredOut(this.currentRowNum - 1)))
				this.currentRowNum -= 1;
			if (this.currentRowNum == 0)
				this.isBOF = true;
		}
	}

	private void gotoFirstRow() throws AdoError {
		this.currentRowNum = 1;
		if ((this.filter != null)
				&& (this.cursor.isFilteredOut(this.currentRowNum - 1)))
			gotoNextRow();
	}

	private void gotoLastRow() throws AdoError {
		this.currentRowNum = getRecordsFetched();
		if ((this.filter != null)
				&& (this.cursor.isFilteredOut(this.currentRowNum - 1)))
			gotoPreviousRow();
	}

	private void gotoRow(int paramInt, boolean paramBoolean) throws AdoError {
		this.currentRowNum = paramInt;
		if ((this.filter != null)
				&& (this.cursor.isFilteredOut(this.currentRowNum - 1)))
			if (paramBoolean)
				gotoNextRow();
			else
				gotoPreviousRow();
	}

	private boolean isDeleted() {
		boolean bool = false;
		synchronized (this.recFetchLock) {
			bool = isDeleted(this.currentRowNum);
		}
		return bool;
	}

	private boolean isDeleted(int paramInt) {
		boolean bool = false;
		if (this.cursorType == 1) {
			if (this.keyRs.cursor != null)
				bool = this.keyRs.cursor.isDeletedRecord(paramInt - 1);
		} else
			bool = this.cursor.isDeletedRecord(paramInt - 1);
		return bool;
	}

	private boolean isDirty() throws AdoError {
		if (this.fields != null)
			for (int i = 0; i < this.fields.getCount(); i++) {
				Field localField = this.fields.getField(i);
				if (localField.isDirty())
					return true;
			}
		return false;
	}

	private int getNumColumns() throws AdoError {
		if (!this.isOpen)
			AdoError.throwCursorNotOpenToDoOp("GetNumColumns");
		if (this.numColumns == -1)
			try {
				ResultSetMetaData localResultSetMetaData = this.rs
						.getMetaData();
				this.numColumns = localResultSetMetaData.getColumnCount();
				println("numColumns: " + this.numColumns);
			} catch (SQLException localSQLException) {
				throw new AdoError(localSQLException,
						"error getting recordset metadata.", 0);
			}
		return this.numColumns;
	}

	private void fillRecordBuffer(boolean paramBoolean) throws AdoError {
		int i = this.currentRowNum - 1;
		if (this.cursorType == 1) {
			i = i - this.minRid + 1;
			if (i > this.cacheSize) {
				String str = "internal error, recToFill is: " + i;
				str = str + " cache size is: " + this.cacheSize;
				throw new AdoError(str);
			}
		}
		this.cursor.fillRecordBuffer(i, paramBoolean);
	}

	private boolean allRecsFetched() {
		boolean bool = false;
		if (this.cursorType == 1)
			bool = this.keyRs.allRecsFetched();
		else if (this.cursor != null)
			bool = this.cursor.allRecsFetched();
		return bool;
	}

	private void setAllRecsFetched(boolean paramBoolean) {
		if (this.cursor != null)
			this.cursor.setAllRecsFetched(paramBoolean);
	}

	public int getRecordStatus() {
		return this.recordStatus;
	}

	private void setRecordStatus(int paramInt) {
		this.recordStatus = paramInt;
		if ((this.cursorType == 1) && (this.keyRs != null))
			this.keyRs.setRecordStatus(paramInt);
	}

	private void execQuery() throws AdoError {
		synchronized (this.recFetchLock) {
			prepareQuery();
			try {
				reExecQuery();
			} catch (AdoError localAdoError) {
				this.ownerCommand.setExecuting(false);
				throw localAdoError;
			}
		}
	}

	private void prepareQuery() throws AdoError {
		if (this.isOpen)
			throw new AdoError(
					"The cursor is already open.  You need to close the cursor before calling: exeQuery",
					70001);
		if ((this.ownerCommand == null) || (this.ownerCon == null)) {
			println("ownerquery or ownercon is null");
			throw new AdoError("Recordset source not set yet.", 70021);
		}
		try {
			int i = this.ownerCommand.getCommandType();
			if ((i == 8) || (i == -1))
				if (isSqlStatement(this.ownerCommand.getCommandText()))
					this.ownerCommand.setCommandType(1);
				else
					this.ownerCommand.setCommandType(2);
			this.ownerCommand.setExecuting(true);
			if (!this.ownerCon.isConnected()) {
				println("ownercon is not connected");
				throw new AdoError(
						"You must have an open connection before calling: Open",
						10009);
			}
			if (i == 1024) {
				if (this.cursorType == 1)
					throw new AdoError(
							"You cannot set the cursor type to adOpenKeyset for adCmdGetTables/adCmdGetColumns - you should set it to adOpenStatic",
							70031);
			} else if ((i == 2048) || (i == 4096) || (i == 8192)
					|| (i == 65536) || (i == 131072) || (i == 16384)
					|| (i == 32768)) {
				if (this.cursorType == 1)
					throw new AdoError(
							"You cannot set the cursor type to adOpenKeyset for adCmdGetTables/adCmdGetColumns - you should set it to adOpenStatic",
							70031);
			} else {
				int j = this.ownerCommand.getStmtType();
				if (this.cursorType == 1) {
					if ((j & 0x8) == 8)
						throw new AdoError(
								"You cannot set the cursor type to adOpenKeyset on a Group By query - you should set it to adOpenStatic",
								70028);
					if ((j & 0x2) != 2)
						throw new AdoError(
								"You cannot set the cursor type to adOpenKeyset on a non select stmt - you should set it to adOpenStatic type: "
										+ j
										+ " cmdtype: "
										+ this.ownerCommand.getCommandType()
										+ " sql: "
										+ this.ownerCommand.getSQLStatement(),
								70035);
				}
				if ((j & 0x2) == 2)
					getTableNames();
				if ((this.cursorType == 1) && (i != 4) && ((j & 0x8) == 0)) {
					if (this.tableNames == null)
						throw new AdoError(
								"Missing table name in select statement.",
								70032);
					prepareKeyQuery();
					prepareUserQuery();
				}
			}
		} catch (AdoError localAdoError) {
			this.ownerCommand.setExecuting(false);
			throw localAdoError;
		}
	}

	private void setOutputParamValues(CallableStatement paramCallableStatement)
			throws AdoError {
		Parameters localParameters = getSource().getParameters();
		if (localParameters != null) {
			int i = localParameters.getCount();
			for (int j = 0; j < i; j++) {
				Parameter localParameter = localParameters.getParameter(j);
				int k = localParameter.getDirection();
				if ((k != 2) && (k != 3) && (k != 4))
					continue;
				Variant localVariant = new Variant();
				localVariant.setNull();
				AdoUtil.getColValue(paramCallableStatement,
						localParameter.getSqlType(), j + 1, localVariant);
				localParameter.setValue(localVariant);
			}
		}
	}

	private void prepareToFetch() throws AdoError {
		println("in prepareToFetch()");
		this.iCursorOpen = true;
		this.isOpen = true;
		this.isEOF = false;
		int i = this.ownerCommand.getStmtType();
		int j = this.ownerCommand.getCommandType();
		if ((j == 4) && (this.rs == null)) {
			this.isEOF = (this.isBOF = true);
		} else if (((i & 0x2) == 2) || (j == 4) || (j == 1024) || (j == 4096)
				|| (j == 8192) || (j == 65536) || (j == 131072) || (j == 16384)
				|| (j == 32768) || (j == 2048)) {
			if (this.cursorType != 1) {
				if (this.rs == null)
					throw new AdoError(
							"JDBC driver returned null for ResultSet after executing the query.",
							70036);
				if ((this.cursorType != 0) && (this.cacheSize == 0))
					this.cacheSize = 5;
				fetchRecords();
			} else {
				fetchUserQuery();
			}
			println("calling getFirst()");
			try {
				getFirst();
			} catch (AdoError localAdoError) {
				this.isEOF = (this.isBOF = true);
				this.recPos = -3;
				this.currentRowNum = -1;
				Err.set(null);
			}
		}
	}

	private void getTableNames() throws AdoError {
		this.tableNames = new Vector(3);
		String str1 = this.ownerCommand.getSQLStatement();
		int i = this.ownerCommand.getFromClauseOffset();
		Properties localProperties = this.ownerCon.getProperties();
		boolean bool1 = ((Boolean) localProperties.get("IsCatalogAtStart"))
				.booleanValue();
		int j = ((Integer) localProperties.get("IdentifierCase")).intValue();
		String str2 = this.ownerCon.getCatalogSeparator();
		String str3 = this.ownerCon.getIdentifierQuoteString();
		String str4 = " \r\n\t," + str3;
		if (i == -1) {
			QualifiedTableName localObject = new QualifiedTableName("dual",
					str3, str2, j, bool1, false);
			this.tableNames.addElement(localObject);
			return;
		}
		Object localObject = new StringTokenizer(str1.substring(i), str4, true);
		((StringTokenizer) localObject).nextToken();
		String str6 = "";
		int k = 1;
		int m = 0;
		QualifiedTableName localQualifiedTableName = null;
		boolean bool2 = true;
		while ((((StringTokenizer) localObject).hasMoreTokens()) && (k != 0)) {
			String str5 = ((StringTokenizer) localObject).nextToken();
			if ((m == 0)
					&& ((str5.equalsIgnoreCase("WHERE"))
							|| (str5.equalsIgnoreCase("GROUP"))
							|| (str5.equalsIgnoreCase("HAVING")) || (str5
								.equalsIgnoreCase("ORDER")))) {
				k = 0;
				continue;
			}
			if ((m == 0) && (str5.equals(","))) {
				localQualifiedTableName = new QualifiedTableName(str6, str3,
						str2, j, bool1, bool2);
				this.tableNames.addElement(localQualifiedTableName);
				str6 = "";
				continue;
			}
			if (str5.equals(str3))
				m = m == 0 ? 1 : 0;
			str6 = str6 + str5;
		}
		localQualifiedTableName = new QualifiedTableName(str6, str3, str2, j,
				bool1, bool2);
		this.tableNames.addElement(localQualifiedTableName);
		localObject = null;
	}

	private void getTableNamesTheRightWayButDriversDontSupportThis()
			throws SQLException {
		this.tableNames = new Vector(3);
		ResultSetMetaData localResultSetMetaData = this.rs.getMetaData();
		for (int i = 1; i <= this.numColumns; i++) {
			String str1 = localResultSetMetaData.getTableName(i);
			if (this.tableNames.contains(str1))
				continue;
			String str2 = localResultSetMetaData.getCatalogName(i);
			String str3 = localResultSetMetaData.getSchemaName(i);
			this.tableNames.addElement(str1);
		}
	}

	private void setConnection(Command paramCommand) throws AdoError {
		Connection localConnection = this.ownerCon;
		int i = this.ownerCon.getMaxStatements();
		if ((i > 0) && (i <= this.ownerCon.getActiveStatements()))
			try {
				localConnection = new Connection(this.ownerCon, true);
			} catch (Exception localException) {
				throw new AdoError(localException,
						"Error creating a second connection.", 10020);
			}
		paramCommand.setActiveConnection(localConnection);
	}

	private void prepareKeyQuery() throws AdoError {
		String str = buildKeyQuery();
		this.keyCommand = new Command();
		setConnection(this.keyCommand);
		this.keyCommand.setCommandText(str);
		this.keyCommand.setCommandType(1);
		this.keyCommand.setPrepared(this.ownerCommand.getPrepared());
		Parameters localParameters = this.ownerCommand.getParameters();
		for (int i = 0; i < localParameters.getCount(); i++) {
			Parameter localParameter = localParameters.getParameter(i);
			this.keyCommand.getParameters().append(localParameter);
		}
		this.keyRs = new Recordset();
		this.keyRs.setMaxRecords(this.maxRecords);
		this.keyRs.setSource(this.keyCommand);
		this.keyRs.setCursorType(3);
		this.keyRs.setLockType(getLockType());
	}

	private void executeKeyQuery() throws AdoError {
		this.keyRs.open();
		this.keyAllFetched = true;
	}

	private void prepareUserQuery() throws AdoError {
		String str = new String(this.ownerCommand.getSQLStatement());
		int i = this.ownerCommand.getWhereClauseOffset();
		if (i == -1)
			i = this.ownerCommand.getOrderByOffset();
		if (i != -1)
			str = str.substring(0, i);
		str = str + " where ";
		str = str + this.whereClause;
		this.userCommand = new Command();
		setConnection(this.userCommand);
		this.userCommand.setPrepared(true);
		this.userCommand.setCommandText(str);
		this.userCommand.setCommandType(1);
		this.userrs = new Recordset();
		this.userrs.setSource(this.userCommand);
		this.userrs.setCursorType(3);
		this.userrs.setMaxRecords(this.maxRecords);
		this.userrs.setLockType(getLockType());
		this.cursor = new AdoCursor(this, this.fields);
		this.cursor.setMaxRecords(this.maxRecords);
	}

	private void createUserParams() throws AdoError {
		Fields localFields = this.keyRs.fields;
		this.userCommand.setParameters(null);
		Parameters localParameters = this.userCommand.getParameters();
		for (int i = 0; i < localFields.getCount(); i++) {
			Field localField = localFields.getField(i);
			Parameter localParameter = new Parameter();
			localParameter.setSqlType(localField.getSqlType());
			localParameters.append(localParameter);
		}
	}

	private void fetchUserQuery() throws AdoError {
		createUserParams();
		if (!fetchKeyRecords(1)) {
			this.cursor.setAllRecsFetched(true);
			try {
				if (this.ownerCommand.getPrepared())
					executePreparedStmt();
				else
					executeStmt(this.ownerCommand.getSQLStatement());
				this.isOpen = true;
				allocBuffers();
				this.cursor.setFields(this.fields);
				this.rs.close();
				this.rs = null;
			} catch (SQLException localSQLException) {
				throw new AdoError(localSQLException, "Error executing query: "
						+ this.ownerCommand.getSQLStatement(), 70016);
			}
		}
	}

	private synchronized boolean getKeyRecord(int paramInt) {
		boolean i = true;
		this.keyRecToFetch = paramInt;
		while ((this.keyRecToFetch == paramInt) && (!this.keyAllFetched))
			try {
				wait();
			} catch (InterruptedException localInterruptedException) {
			}
		if (this.maxRid < paramInt)
			i = false;
		return i;
	}

	private void abortRecFetchThread() {
		if (!this.cursor.allRecsFetched()) {
			this.cursor.setAbortQuery(true);
			try {
				this.recFetchThread.join();
			} catch (InterruptedException localInterruptedException) {
			}
		}
		this.recFetchThread = null;
	}

	private void abortKeyThread() {
		if (!this.keyAllFetched) {
			println("aborting key thread...");
			this.stopKeyThread = true;
			try {
				this.keyThread.join();
			} catch (InterruptedException localInterruptedException) {
			}
			println("aborted key thread.");
		}
		this.keyThread = null;
	}

	private boolean fetchKeyRecords(int paramInt) throws AdoError {
		println("in fetchKeyRecords, record to fetch is " + paramInt);
		boolean bool = true;
		if ((paramInt < this.minRid) && (paramInt != 1))
			this.fetchPrev = true;
		else
			this.fetchPrev = false;
		if ((!this.keyAllFetched) && (paramInt > this.maxRid)
				&& (paramInt < this.minRid + this.cacheSize)) {
			bool = getKeyRecord(paramInt);
		} else {
			abortKeyThread();
			int i = 0;
			println("calling setAbsolutePostion on the key cursor");
			this.keyRs.setAbsolutePosition(paramInt);
			i = this.keyRs.getAbsolutePosition();
			println("after getAbsolutePostion rowMovedTo is " + i);
			if ((i == paramInt)
					|| ((paramInt == 2147483647) && (!this.keyRs.isEOF()) && (!this.keyRs
							.isBOF()))) {
				if (this.keyRs.isDeleted()) {
					this.cursor.reset();
					this.cursor.addEmptyRow(this.fields);
					this.cursor.deleteRecord(0);
					this.minRid = i;
					this.maxRid = i;
				} else {
					this.minRid = i;
					this.maxRid = (i - 1);
					println("starting worker thread to fetch the key record...");
					startKeyThread();
					throwBgError();
				}
			} else
				bool = false;
		}
		return bool;
	}

	private synchronized void startKeyThread() {
		this.canGoNow = false;
		this.keyThread = new Thread(this);
		int i = Thread.currentThread().getPriority();
		this.keyThread.setPriority(i);
		this.keyAllFetched = false;
		this.keyThread.start();
		while (!this.canGoNow)
			try {
				println("waiting for notification from the key cursor");
				wait();
			} catch (InterruptedException localInterruptedException) {
			}
	}

	public void run() {
		this.cursor.reset();
		fetchFirst();
		fetchRest();
	}

	private synchronized void fetchFirst() {
		try {
			fetchRecords(1);
		} catch (AdoError localAdoError1) {
			println("AdoError in Recordset run " + localAdoError1.toString());
			setBackgroundFetchError(localAdoError1);
		} catch (Exception localException) {
			println("Exception while fetching the rest of the columns for the query.\r\n "
					+ localException.toString());
			AdoError localAdoError2 = new AdoError();
			localAdoError2
					.setErrorMsg("Caught Exception getting data for keyset cursor.");
			localAdoError2.setNativeAdoError(localException);
			setBackgroundFetchError(localAdoError2);
		} finally {
			if (!this.canGoNow) {
				this.canGoNow = true;
				notify();
			}
		}
	}

	private void fetchRest() {
		if (!this.cursor.allRecsFetched())
			try {
				int i = 0;
				while ((!this.keyRs.isEOF()) && (!this.keyRs.isBOF())
						&& (i < this.cacheSize - 1) && (!this.stopKeyThread)
						&& (!this.cursor.allRecsFetched()))
					i += fetchRecords(2);
			} catch (AdoError localAdoError1) {
				println("AdoError in Recordset run "
						+ localAdoError1.toString());
				setBackgroundFetchError(localAdoError1);
			} catch (Exception localException) {
				println("Exception in Recordset run "
						+ localException.toString());
				AdoError localAdoError2 = new AdoError();
				localAdoError2
						.setErrorMsg("Caught Exception getting data for keyset cursor.");
				localAdoError2.setNativeAdoError(localException);
				setBackgroundFetchError(localAdoError2);
			}
		notifyAllRecsFetched();
	}

	private synchronized void notifyAllRecsFetched() {
		this.keyAllFetched = true;
		this.stopKeyThread = false;
		notify();
	}

	private synchronized int fetchRecords(int paramInt) throws AdoError,
			Exception {
		println("in fetchRecords(int) - fetching the user columns in the background");
		int i = 0;
		try {
			while ((!this.keyRs.isEOF()) && (!this.keyRs.isBOF())
					&& (i < paramInt) && (!this.stopKeyThread)) {
				Fields localFields = this.keyRs.getFields();
				Parameters localParameters = this.userCommand.getParameters();
				for (int j = 0; j < localFields.getCount(); j++) {
					Field localField = localFields.getField(j);
					Parameter localParameter = localParameters.getParameter(j);
					localParameter.setValue(localField.getValue());
				}
				if (!this.userrs.isOpen) {
					println("opening cursor for user columns...");
					this.userrs.open();
					allocBuffers(this.userrs.fields);
					this.cursor.setFields(this.fields);
				} else {
					println("reexcuting the query to fetch user columns...");
					this.userrs.reExecuteQuery();
				}
				if (!this.userrs.isEOF()) {
					this.cursor.addToCache(this.userrs.fields);
					this.keyRs.moveNext();
				} else {
					println("record no longer found in the db.");
					this.cursor.addEmptyRow(this.fields);
					this.cursor.deleteRecord(i);
					if (this.fetchPrev)
						this.keyRs.movePrevious();
					else
						this.keyRs.moveNext();
				}
				i++;
				this.maxRid += 1;
				if (this.keyRecToFetch == 2147483647)
					continue;
				this.keyRecToFetch = 2147483647;
				notify();
			}
			if (this.keyRs.isEOF()) {
				this.cursor.setAllRecsFetched(true);
				if (this.keyRecToFetch != 2147483647) {
					println("notifying fetched key record: "
							+ this.keyRecToFetch);
					this.keyRecToFetch = 2147483647;
					notify();
				}
				this.keyRs.movePrevious();
			} else if (this.keyRs.isBOF()) {
				this.keyRs.moveNext();
			}
		} catch (AdoError localAdoError) {
			this.cursor.setAllRecsFetched(true);
			if (this.keyRecToFetch != 2147483647) {
				this.keyRecToFetch = 2147483647;
				notify();
			}
			throw localAdoError;
		} catch (Exception localException) {
			this.cursor.setAllRecsFetched(true);
			if (this.keyRecToFetch != 2147483647) {
				this.keyRecToFetch = 2147483647;
				notify();
			}
			throw localException;
		}
		return i;
	}

	private String getFromClause(Vector paramVector) {
		String str1 = " from ";
		String str2 = this.ownerCon.getIdentifierQuoteString();
		for (int i = 0; i < paramVector.size(); i++) {
			if (i > 0)
				str1 = str1 + ", ";
			QualifiedTableName localQualifiedTableName = (QualifiedTableName) paramVector
					.elementAt(i);
			str1 = str1 + localQualifiedTableName.getExtendedTableName();
			if (localQualifiedTableName.alias == null)
				continue;
			str1 = str1 + " " + localQualifiedTableName.alias;
		}
		return str1;
	}

	private String buildFromCols(Hashtable paramHashtable, int paramInt) {
		int i = 0;
		int j = 0;
		if ((paramInt & 0x2) > 0) {
			i = 1;
			j = 1;
			paramInt &= -3;
		}
		if ((paramInt & 0x4) > 0) {
			j = 1;
			paramInt &= -5;
		}
		String str1 = "";
		String str2 = this.ownerCon.getIdentifierQuoteString();
		String str3 = this.ownerCon.getCatalogSeparator();
		for (int k = 0; k < this.tableNames.size(); k++) {
			if (k > 0)
				if (paramInt == 0)
					str1 = str1 + ", ";
				else if (paramInt == 1)
					str1 = str1 + "and ";
			QualifiedTableName localQualifiedTableName = (QualifiedTableName) this.tableNames
					.elementAt(k);
			String str4 = j != 0 ? null : localQualifiedTableName.alias;
			if (str4 == null)
				str4 = str2 + localQualifiedTableName.tableName + str2;
			Vector localVector = (Vector) paramHashtable
					.get(localQualifiedTableName);
			if (localVector != null)
				for (int m = 0; m < localVector.size(); m++) {
					if (m > 0)
						if (paramInt == 0)
							str1 = str1 + ", ";
						else if (paramInt == 1)
							str1 = str1 + "and ";
					str1 = str1 + str4 + str3;
					str1 = str1 + str2 + (String) localVector.elementAt(m)
							+ str2;
					if (paramInt != 1)
						continue;
					str1 = str1 + " = ? ";
				}
			if (i != 0)
				break;
		}
		return str1;
	}

	private String buildKeyQuery() throws AdoError {
		int i = 0;
		Hashtable localHashtable = getKeyCols(this.tableNames);
		String str = "select ";
		this.whereClause = " ";
		str = str + buildFromCols(localHashtable, 0);
		str = str + getFromClause(this.tableNames);
		this.whereClause += buildFromCols(localHashtable, 1);
		int j = this.ownerCommand.getStmtType();
		if ((j & 0x4) == 4)
			str = str + " " + this.ownerCommand.getWhereClause();
		else if ((j & 0x10) == 16)
			str = str + " " + this.ownerCommand.getOrderByClause();
		println(str);
		println(this.whereClause);
		return str;
	}

	private Hashtable getKeyCols(Vector paramVector) throws AdoError {
		if (this.keyDict == null) {
			this.keyDict = new Hashtable(3);
			for (int i = 0; i < paramVector.size(); i++) {
				QualifiedTableName localQualifiedTableName = (QualifiedTableName) paramVector
						.elementAt(i);
				this.keyDict.put(
						localQualifiedTableName,
						buildKeyCols(localQualifiedTableName.catalogName,
								localQualifiedTableName.ownerName,
								localQualifiedTableName.tableName));
			}
		}
		return this.keyDict;
	}

	private Hashtable getSearchableCols(Vector paramVector) throws AdoError {
		Hashtable localHashtable = new Hashtable(3);
		for (int i = 0; i < paramVector.size(); i++) {
			QualifiedTableName localQualifiedTableName = (QualifiedTableName) paramVector
					.elementAt(i);
			localHashtable.put(
					localQualifiedTableName,
					getSearchableCols(localQualifiedTableName.catalogName,
							localQualifiedTableName.ownerName,
							localQualifiedTableName.tableName));
		}
		return localHashtable;
	}

	private Vector buildKeyCols(String paramString1, String paramString2,
			String paramString3) throws AdoError {
		Vector localVector = null;
		try {
			localVector = getPrimaryKeyCols(paramString1, paramString2,
					paramString3);
		} catch (Exception localException) {
		}
		if ((localVector == null) || (localVector.size() == 0))
			localVector = getIndexCols(paramString1, paramString2, paramString3);
		if ((localVector.size() == 0) && (this.cursorType == 1))
			throw new AdoError(
					"Either the table does not exist or there is no unique index in the table: "
							+ paramString3, 70020);
		return localVector;
	}

	private Vector getBestRowId(String paramString1, String paramString2,
			String paramString3) throws AdoError {
		Vector localVector = new Vector(2);
		try {
			DatabaseMetaData localDatabaseMetaData = this.ownerCon
					.getMetaData();
			ResultSet localResultSet = localDatabaseMetaData
					.getBestRowIdentifier(paramString1, paramString2,
							paramString3, 1, false);
			this.ownerCon.incrementActiveStatements();
			String str = null;
			int i = 1;
			while ((localResultSet.next()) && (i != 0)) {
				int k = localResultSet.getInt("SCOPE");
				if ((k != 1) && (k != 2)) {
					i = 0;
					continue;
				}
				str = localResultSet.getString("COLUMN_NAME");
				int j = localResultSet.getInt("DATA_TYPE");
				localVector.addElement(str);
			}
			localResultSet.close();
			if ((this.ownerCon.getWorkArounds() & 0x1) != 1)
				this.ownerCon.decrementActiveStatements();
		} catch (SQLException localSQLException) {
			throw new AdoError(localSQLException,
					"error getting recordset metadata.", 70018);
		}
		return localVector;
	}

	private Vector getSearchableCols(String paramString1, String paramString2,
			String paramString3) throws AdoError {
		Vector localVector = new Vector(2);
		try {
			DatabaseMetaData localDatabaseMetaData = this.ownerCon
					.getMetaData();
			this.rs = localDatabaseMetaData.getColumns(paramString1,
					paramString2, paramString3, null);
			while (this.rs.next()) {
				String str = "";
				int i = 0;
				str = this.rs.getString(4);
				i = this.rs.getInt(5);
				Field localField = null;
				try {
					localField = this.fields.getField(str);
				} catch (AdoError localAdoError) {
					if ((localAdoError.getNumber() != 30011)
							|| (this.cursorType != 1)) {
						localAdoError.setErrorNumber(70033);
						localAdoError
								.setErrorMsg("To update the record, you need to have the unique key columns or all the searchable columns in the select list.");
						throw localAdoError;
					}
				}
				if (!localField.isSearchable())
					continue;
				localVector.addElement(str);
			}
		} catch (SQLException localSQLException) {
			throw new AdoError(localSQLException, "getSearchableCols");
		}
		return localVector;
	}

	private Vector getPrimaryKeyCols(String paramString1, String paramString2,
			String paramString3) throws AdoError {
		Vector localVector = new Vector(2);
		try {
			DatabaseMetaData localDatabaseMetaData = this.ownerCon
					.getMetaData();
			ResultSet localResultSet = localDatabaseMetaData.getPrimaryKeys(
					paramString1, paramString2, paramString3);
			this.ownerCon.incrementActiveStatements();
			String str = null;
			while (localResultSet.next()) {
				str = localResultSet.getString(4);
				localVector.addElement(str);
			}
			localResultSet.close();
			this.ownerCon.decrementActiveStatements();
		} catch (SQLException localSQLException) {
			throw new AdoError(localSQLException,
					"error getting recordset metadata.", 70018);
		}
		return localVector;
	}

	private Vector getIndexCols(String paramString1, String paramString2,
			String paramString3) throws AdoError {
		if (this.ownerCon == null)
			return null;
		Vector localVector = new Vector(2);
		try {
			DatabaseMetaData localDatabaseMetaData = this.ownerCon
					.getMetaData();
			ResultSet localResultSet = localDatabaseMetaData.getIndexInfo(
					paramString1, paramString2, paramString3, true, true);
			this.ownerCon.incrementActiveStatements();
			Object localObject = null;
			String str1 = null;
			while (localResultSet.next()) {
				String str2 = localResultSet.getString(6);
				int i = localResultSet.getInt(7);
				str1 = localResultSet.getString(9);
				if (i != 0) {
					if ((localObject != null) && (!localObject.equals(str2)))
						break;
					localObject = str2;
					localVector.addElement(str1);
					continue;
				}
				localObject = null;
			}
			localResultSet.close();
			if ((this.ownerCon.getWorkArounds() & 0x1) != 1)
				this.ownerCon.decrementActiveStatements();
		} catch (SQLException localSQLException) {
			throw new AdoError(localSQLException,
					"error getting recordset metadata.", 70018);
		}
		return localVector;
	}

	private void execGetTables() throws SQLException {
		Properties localProperties = this.ownerCon.getProperties();
		String str = (String) localProperties.get("CurrentCatalog");
		DatabaseMetaData localDatabaseMetaData = this.ownerCon.getConnection()
				.getMetaData();
		StringTokenizer localStringTokenizer = new StringTokenizer(
				this.ownerCommand.getCommandText(), ",");
		int i = localStringTokenizer.countTokens();
		String[] arrayOfString = new String[i];
		int j = 0;
		while (localStringTokenizer.hasMoreTokens()) {
				try {
					arrayOfString[(j++)] = localStringTokenizer.nextToken();
				} catch (ArrayIndexOutOfBoundsException localArrayIndexOutOfBoundsException) {
				}
			}
		try {
			this.rs = localDatabaseMetaData.getTables(str, null, null,
					arrayOfString);
		} catch (SQLException localSQLException) {
			this.rs = localDatabaseMetaData.getTables(null, null, null,
					arrayOfString);
		}
	}

	private void checkCmdText(String paramString) throws AdoError {
		if ((paramString == null) || (paramString.length() == 0))
			throw new AdoError("Command text not set yet.", 20011);
	}

	private void execGetColumns() throws SQLException, AdoError {
		DatabaseMetaData localDatabaseMetaData = this.ownerCon.getConnection()
				.getMetaData();
		String str1 = this.ownerCommand.getCommandText();
		checkCmdText(str1);
		boolean bool1 = false;
		Properties localProperties = this.ownerCon.getProperties();
		String str2 = this.ownerCon.getCatalogSeparator();
		String str3 = this.ownerCon.getIdentifierQuoteString();
		boolean bool2 = ((Boolean) localProperties.get("IsCatalogAtStart"))
				.booleanValue();
		int i = ((Integer) localProperties.get("IdentifierCase")).intValue();
		QualifiedTableName localQualifiedTableName = new QualifiedTableName(
				str1, str3, str2, i, bool2, bool1);
		try {
			this.rs = localDatabaseMetaData.getColumns(
					localQualifiedTableName.catalogName,
					localQualifiedTableName.ownerName,
					localQualifiedTableName.tableName, null);
		} catch (SQLException localSQLException) {
			this.rs = localDatabaseMetaData.getColumns(null, null,
					localQualifiedTableName.tableName, null);
		}
	}

	private void execGetPrimaryKeys() throws SQLException, AdoError {
		DatabaseMetaData localDatabaseMetaData = this.ownerCon.getConnection()
				.getMetaData();
		String str1 = this.ownerCommand.getCommandText();
		checkCmdText(str1);
		StringTokenizer localStringTokenizer = new StringTokenizer(
				this.ownerCommand.getCommandText(), ", ");
		int i = localStringTokenizer.countTokens();
		String[] arrayOfString = new String[i];
		int j = 0;
		while (localStringTokenizer.hasMoreTokens())
				try {
					arrayOfString[(j++)] = localStringTokenizer.nextToken();
				} catch (ArrayIndexOutOfBoundsException localArrayIndexOutOfBoundsException) {
				}
		Properties localProperties = this.ownerCon.getProperties();
		String str2 = (String) localProperties.get("CurrentCatalog");
		if (arrayOfString.length == 1) {
			this.rs = localDatabaseMetaData.getPrimaryKeys(str2, null,
					arrayOfString[0]);
		} else if (arrayOfString.length == 2) {
			this.rs = localDatabaseMetaData.getPrimaryKeys(str2,
					arrayOfString[0], arrayOfString[1]);
		} else if (arrayOfString.length == 3) {
			str2 = null;
			if (arrayOfString[0].length() > 0)
				str2 = arrayOfString[0];
			this.rs = localDatabaseMetaData.getPrimaryKeys(str2,
					arrayOfString[1], arrayOfString[2]);
		} else {
			throw new AdoError(
					"adCmdGetPrimaryKeys call should have a tablename", 70038);
		}
	}

	private void execGetUniqueIndexes() throws SQLException, AdoError {
		execGetIndexes(true, true);
	}

	private void execGetNonUniqueIndexes() throws SQLException, AdoError {
		execGetIndexes(false, true);
	}

	private void execGetIndexes(boolean paramBoolean1, boolean paramBoolean2)
			throws SQLException, AdoError {
		DatabaseMetaData localDatabaseMetaData = this.ownerCon.getConnection()
				.getMetaData();
		String str1 = this.ownerCommand.getCommandText();
		checkCmdText(str1);
		StringTokenizer localStringTokenizer = new StringTokenizer(
				this.ownerCommand.getCommandText(), ", ");
		int i = localStringTokenizer.countTokens();
		String[] arrayOfString = new String[i];
		int j = 0;
		while(localStringTokenizer.hasMoreTokens())
				try {
					arrayOfString[(j++)] = localStringTokenizer.nextToken();
				} catch (ArrayIndexOutOfBoundsException localArrayIndexOutOfBoundsException) {
				}
		Properties localProperties = this.ownerCon.getProperties();
		String str2 = (String) localProperties.get("CurrentCatalog");
		if (arrayOfString.length == 1) {
			this.rs = localDatabaseMetaData.getIndexInfo(str2, null,
					arrayOfString[0], paramBoolean1, paramBoolean2);
		} else if (arrayOfString.length == 2) {
			this.rs = localDatabaseMetaData.getIndexInfo(str2,
					arrayOfString[0], arrayOfString[1], paramBoolean1,
					paramBoolean2);
		} else if (arrayOfString.length == 3) {
			str2 = null;
			if (arrayOfString[0].length() > 0)
				str2 = arrayOfString[0];
			this.rs = localDatabaseMetaData.getIndexInfo(str2,
					arrayOfString[1], arrayOfString[2], paramBoolean1,
					paramBoolean2);
		} else {
			throw new AdoError("adCmdGetIndexes call should have a tablename",
					70040);
		}
	}

	private void execGetForeignKeys() throws SQLException, AdoError {
		Properties localProperties = this.ownerCon.getProperties();
		String str1 = (String) localProperties.get("CurrentCatalog");
		DatabaseMetaData localDatabaseMetaData = this.ownerCon.getConnection()
				.getMetaData();
		String str2 = this.ownerCommand.getCommandText();
		checkCmdText(str2);
		StringTokenizer localStringTokenizer = new StringTokenizer(str2, ", ");
		int i = localStringTokenizer.countTokens();
		String[] arrayOfString = new String[i];
		int j = 0;
		while(localStringTokenizer.hasMoreTokens())
				try {
					arrayOfString[(j++)] = localStringTokenizer.nextToken();
				} catch (ArrayIndexOutOfBoundsException localArrayIndexOutOfBoundsException) {
				}
		if (arrayOfString.length == 0)
			this.rs = localDatabaseMetaData.getCrossReference(str1, null, null,
					str1, null, null);
		else if (arrayOfString.length == 1)
			this.rs = localDatabaseMetaData.getImportedKeys(str1, null,
					arrayOfString[0]);
		else if (arrayOfString.length == 2)
			this.rs = localDatabaseMetaData.getImportedKeys(str1,
					arrayOfString[0], arrayOfString[1]);
		else if (arrayOfString.length == 3)
			this.rs = localDatabaseMetaData.getImportedKeys(arrayOfString[0],
					arrayOfString[1], arrayOfString[2]);
		else
			throw new AdoError(
					"adCmdGetForeignKeys call should have a tablename", 70039);
	}

	private void execGetExportedKeys() throws SQLException, AdoError {
		Properties localProperties = this.ownerCon.getProperties();
		String str1 = (String) localProperties.get("CurrentCatalog");
		DatabaseMetaData localDatabaseMetaData = this.ownerCon.getConnection()
				.getMetaData();
		String str2 = this.ownerCommand.getCommandText();
		checkCmdText(str2);
		StringTokenizer localStringTokenizer = new StringTokenizer(str2, ", ");
		int i = localStringTokenizer.countTokens();
		String[] arrayOfString = new String[i];
		int j = 0;
		while(localStringTokenizer.hasMoreTokens())
				try {
					arrayOfString[(j++)] = localStringTokenizer.nextToken();
				} catch (ArrayIndexOutOfBoundsException localArrayIndexOutOfBoundsException) {
				}
		if (arrayOfString.length == 1)
			this.rs = localDatabaseMetaData.getExportedKeys(str1, null,
					arrayOfString[0]);
		else if (arrayOfString.length == 2)
			this.rs = localDatabaseMetaData.getExportedKeys(str1,
					arrayOfString[0], arrayOfString[1]);
		else if (arrayOfString.length == 3)
			this.rs = localDatabaseMetaData.getExportedKeys(arrayOfString[0],
					arrayOfString[1], arrayOfString[2]);
		else
			throw new AdoError(
					"adCmdGetForeignKeys call should have a tablename", 70039);
	}

	private void execGetCrossReference() throws SQLException, AdoError {
		Properties localProperties = this.ownerCon.getProperties();
		String str1 = (String) localProperties.get("CurrentCatalog");
		DatabaseMetaData localDatabaseMetaData = this.ownerCon.getConnection()
				.getMetaData();
		String str2 = this.ownerCommand.getCommandText();
		checkCmdText(str2);
		StringTokenizer localStringTokenizer = new StringTokenizer(str2, ", ");
		int i = localStringTokenizer.countTokens();
		String[] arrayOfString = new String[i];
		int j = 0;
		while(localStringTokenizer.hasMoreTokens())
				try {
					arrayOfString[(j++)] = localStringTokenizer.nextToken();
				} catch (ArrayIndexOutOfBoundsException localArrayIndexOutOfBoundsException) {
				}
		if (arrayOfString.length == 0)
			this.rs = localDatabaseMetaData.getCrossReference(str1, null, null,
					str1, null, null);
		else if (arrayOfString.length == 2)
			this.rs = localDatabaseMetaData.getCrossReference(str1, null,
					arrayOfString[0], str1, null, arrayOfString[1]);
		else if (arrayOfString.length == 4)
			this.rs = localDatabaseMetaData.getCrossReference(str1,
					arrayOfString[0], arrayOfString[1], str1, arrayOfString[2],
					arrayOfString[3]);
		else
			throw new AdoError(
					"adCmdGetForeignKeys call should have a tablename", 70039);
	}

	private void executePreparedStmt() throws AdoError, SQLException {
		println("preparing sql statement");
		PreparedStatement localPreparedStatement = getPreparedStatement();
		this.ownerCommand.setParamValues();
		try {
			localPreparedStatement.setMaxRows(this.maxRecords);
		} catch (Exception localException) {
			if (this.maxRecords > 0)
				localException.printStackTrace();
		}
		this.rs = null;
		println("executing prepared statement.");
		int i = this.ownerCommand.getStmtType();
		int j = this.ownerCommand.getCommandType();
		if (((i & 0x2) == 2) || (j == 4)) {
			boolean bool = localPreparedStatement.execute();
			if ((j == 4)
					&& ((localPreparedStatement instanceof CallableStatement)))
				setOutputParamValues((CallableStatement) localPreparedStatement);
			if (bool)
				this.rs = localPreparedStatement.getResultSet();
			this.numRecsAffected = -1;
		} else {
			this.numRecsAffected = localPreparedStatement.executeUpdate();
			this.rs = null;
		}
	}

	private void executeStmt(String paramString) throws AdoError {
		try {
			if (this.stmt == null) {
				if (this.ownerCon == null)
					throw new AdoError(
							"You must have a valid connection before accessing the database.",
							10009);
				this.stmt = this.ownerCon.createStatement();
			}
			try {
				this.stmt.setMaxRows(this.maxRecords);
			} catch (Exception localException) {
				if (this.maxRecords > 0)
					localException.printStackTrace();
			}
			println("executing sql statement: " + paramString);
			int i = this.ownerCommand.getStmtType();
			int j = this.ownerCommand.getCommandType();
			if (((i & 0x2) == 2) || (j == 4)) {
				this.rs = this.stmt.executeQuery(paramString);
				if ((j == 4) && ((this.stmt instanceof CallableStatement)))
					setOutputParamValues((CallableStatement) this.stmt);
				this.numRecsAffected = -1;
			} else {
				this.rs = null;
				this.numRecsAffected = this.stmt.executeUpdate(paramString);
				this.ownerCon.closeStatement(this.stmt);
				this.stmt = null;
			}
		} catch (SQLException localSQLException) {
			this.ownerCon.closeStatement(this.stmt);
			this.stmt = null;
			throw new AdoError(localSQLException, "Error executing query: "
					+ paramString, 70016);
		}
	}

	private void freeCommand() throws AdoError {
		this.ownerCommand.freeResources();
	}

	private void freeConnection() throws AdoError {
		this.ownerCon.removeRecset(this);
		this.ownerCon.close();
	}

	void freeResources() throws AdoError {
		if (this.recFetchThread != null)
			abortRecFetchThread();
		if (this.keyThread != null)
			abortKeyThread();
		if (this.ownerCommand != null)
			this.ownerCommand.setExecuting(false);
		closeCursor();
		freeKeyRsResources();
		freeUserRsResources();
		if (!this.nextRecordsetCreated) {
			if ((this.ownerCommand != null)
					&& (this.ownerCommand.getPrepared()))
				this.ownerCommand.closePreparedStatement();
			if (this.freeOwnerCommand)
				freeCommand();
			if (this.freeOwnerCon)
				freeConnection();
		}
	}

	public void close() throws AdoError {
		throwBgError();
		freeResources();
		this.isBOF = true;
		this.isEOF = true;
		this.recPos = -3;
		this.isOpen = false;
		this.currentRowNum = 0;
		this.numColumns = -1;
		this.recordCount = -1;
		setRecordStatus(8);
		this.cursor = null;
		this.fields = null;
		this.rs = null;
		setAllRecsFetched(false);
		this.tableNames = null;
		this.keyDict = null;
		this.keyCmdStr = null;
		this.minRid = 0;
		this.maxRid = 0;
		this.canGoNow = false;
	}

	private void initialize() {
		this.recordCount = -1;
		this.isOpen = false;
		this.iCursorOpen = false;
		this.isDisconnected = false;
		this.lockType = 1;
		this.cursorType = 0;
		this.isBOF = true;
		this.isEOF = true;
		this.recPos = -3;
		this.maxRecords = 0;
		this.cacheSize = 20;
		this.ownerCon = null;
		this.ownerCommand = null;
		this.numColumns = -1;
		this.stmt = null;
		this.rs = null;
		this.tableNames = null;
		this.fields = null;
		this.keyCommand = null;
		this.keyRs = null;
		this.keyCmdStr = null;
		this.whereClause = null;
		this.minRid = 0;
		this.maxRid = 0;
		this.keyRecToFetch = 2147483647;
		this.currentRowNum = 0;
		this.canGoNow = false;
		this.userCommand = null;
		this.userrs = null;
		this.keyDict = null;
		this.recFetchThread = null;
		setRecordStatus(8);
		this.filter = null;
		this.freeOwnerCon = false;
		this.freeOwnerCommand = false;
		this.cursor = null;
		this.nextRecordsetCreated = false;
		this.bgErrorSet = false;
		this.backgroundFetchError = null;
	}

	private void initialize(Recordset paramRecordset, boolean paramBoolean) {
		this.recordCount = paramRecordset.recordCount;
		this.lockType = paramRecordset.lockType;
		this.cursorType = paramRecordset.cursorType;
		this.maxRecords = paramRecordset.maxRecords;
		this.cacheSize = paramRecordset.cacheSize;
		this.ownerCon = paramRecordset.ownerCon;
		this.ownerCommand = paramRecordset.ownerCommand;
		this.props = new Properties();
		AdoUtil.copyProps(paramRecordset.props, this.props);
		this.freeOwnerCon = paramRecordset.freeOwnerCon;
		this.freeOwnerCommand = paramRecordset.freeOwnerCommand;
		this.minRid = 0;
		this.maxRid = 0;
		this.keyRecToFetch = 2147483647;
		this.currentRowNum = 0;
		this.canGoNow = false;
		this.recFetchThread = null;
		this.bgErrorSet = false;
		this.backgroundFetchError = null;
		setRecordStatus(8);
		if (paramBoolean) {
			this.isOpen = false;
			this.iCursorOpen = paramRecordset.iCursorOpen;
			this.isBOF = paramRecordset.isBOF;
			this.isEOF = paramRecordset.isEOF;
			this.recPos = paramRecordset.recPos;
			this.numColumns = paramRecordset.numColumns;
			this.stmt = paramRecordset.stmt;
			this.rs = paramRecordset.rs;
			this.tableNames = new Vector();
			for (int i = 0; i < paramRecordset.tableNames.size(); i++)
				this.tableNames.addElement(paramRecordset.tableNames
						.elementAt(i));
			this.fields = null;
			this.keyCommand = paramRecordset.keyCommand;
			this.keyRs = paramRecordset.keyRs;
			this.keyCmdStr = paramRecordset.keyCmdStr;
			this.whereClause = paramRecordset.whereClause;
			this.userCommand = paramRecordset.userCommand;
			this.userrs = paramRecordset.userrs;
			this.cursor = paramRecordset.cursor;
			this.nextRecordsetCreated = paramRecordset.nextRecordsetCreated;
			this.filter = paramRecordset.filter;
			this.keyDict = (paramRecordset.keyDict != null ? (Hashtable) paramRecordset.keyDict
					.clone() : null);
			try {
				execQuery();
			} catch (Exception localException) {
			}
		} else {
			this.isBOF = true;
			this.isEOF = true;
			this.recPos = -3;
			this.numColumns = -1;
			this.stmt = null;
			this.rs = null;
			this.tableNames = null;
			this.fields = null;
			this.keyCommand = null;
			this.keyRs = null;
			this.keyCmdStr = null;
			this.whereClause = null;
			this.userCommand = null;
			this.userrs = null;
			this.cursor = null;
			this.nextRecordsetCreated = false;
			this.filter = null;
		}
	}

	public void finalize() throws AdoError {
		freeResources();
	}

	private void clearFields() throws AdoError {
		for (int i = 0; i < this.fields.getCount(); i++) {
			Field localField = this.fields.getField(i);
			localField.clearDirtyFlag();
			localField.setInitialValue(new Variant().setNull());
		}
	}

	private void setNewValues(Vector paramVector1, Vector paramVector2)
			throws AdoError {
		for (int i = 0; i < paramVector1.size(); i++) {
			Field localField = this.fields.getField(paramVector1.elementAt(i));
			Object localObject = paramVector2.elementAt(i);
			if (!(localObject instanceof Variant))
				throw new AdoError(
						"Unknown/Unhandled object type for this operation: ",
						90002);
			localField.setValue((Variant) localObject);
		}
	}

	void addNewRecord() throws AdoError {
		if ((this.tableNames.size() != 1)
				&& (!this.allowUpdateOnFirstTableInJoin))
			throw new AdoError(
					"Currently you can do an insert only on queries with one table",
					70023);
		QualifiedTableName localQualifiedTableName = (QualifiedTableName) this.tableNames
				.elementAt(0);
		String str1 = localQualifiedTableName.getExtendedTableName();
		String str2 = createInsertStmt(str1, this.fields);
		PreparedStatement localPreparedStatement = null;
		println(str2);
		try {
			localPreparedStatement = this.ownerCon.prepareStatement(str2);
			int i = this.fields.getCount();
			int j = 0;
			int k = -1;
			Object localObject;
			for (int m = 0; m < i; m++) {
				localObject = this.fields.getField(m);
				if ((!((Field) localObject).isAutoInc())
						&& (((Field) localObject).isDirty())) {
					this.ownerCommand.setParamValues(localPreparedStatement,
							j++, ((Field) localObject).getValue(),
							((Field) localObject).getSqlType());
					println("  " + ((Field) localObject).getName() + "="
							+ ((Field) localObject).getValue());
				}
				if (!((Field) localObject).isAutoInc())
					continue;
				k = m;
			}
			localPreparedStatement.executeUpdate();
			this.ownerCon.closeStatement(localPreparedStatement);
			localPreparedStatement = null;
			if ((k >= 0) && (autoIncQuery != null)) {
				Statement localStatement = this.ownerCon.createStatement();
				localObject = localStatement.executeQuery(autoIncQuery);
				println(autoIncQuery);
				println("  autoInc Field: " + k);
				if (((ResultSet) localObject).next()) {
					int n = ((ResultSet) localObject).getInt(1);
					Field localField = this.fields.getField(k);
					localField.setInitialValue(new Variant(n));
					this.cursor.updateCacheForField(this.currentRowNum - 1,
							localField, k);
					println("  new ID: " + localField.getName() + "="
							+ localField.getValue());
				}
				localStatement.close();
			}
		} catch (SQLException localSQLException) {
			if (localPreparedStatement != null)
				this.ownerCon.closeStatement(localPreparedStatement);
			throw new AdoError(localSQLException, "Error executing query: "
					+ str2, 70016);
		}
	}

	String createInsertStmt(String paramString, Fields paramFields)
			throws AdoError {
		String str1 = this.ownerCon.getIdentifierQuoteString();
		String str2 = "insert into ";
		str2 = str2 + paramString;
		str2 = str2 + " ( ";
		String str3 = " ) values ( ";
		for (int i = 0; i < paramFields.getCount(); i++) {
			Field localField = paramFields.getField(i);
			if ((localField.isAutoInc()) || (!localField.isDirty()))
				continue;
			str2 = str2 + str1;
			str2 = str2 + localField.getName();
			str2 = str2 + str1;
			str3 = str3 + "?";
			str2 = str2 + ", ";
			str3 = str3 + ", ";
		}
		int i = str2.lastIndexOf(',');
		if (i == -1)
			throw new AdoError(
					"Either none of the fields were updated or the updated fields are not modifiable.",
					70030);
		str2 = str2.substring(0, i);
		i = str3.lastIndexOf(',');
		str3 = str3.substring(0, i);
		str2 = str2 + str3;
		str2 = str2 + " ) ";
		return str2;
	}

	synchronized void modifyRecord(boolean paramBoolean) throws AdoError {
		int i = 5;
		if (this.tableNames.size() > 1)
			if (this.allowUpdateOnFirstTableInJoin)
				i |= 2;
			else
				throw new AdoError(
						"Currently you can do an update only on queries with one table",
						70024);
		Hashtable localHashtable = getKeyCols(this.tableNames);
		QualifiedTableName localQualifiedTableName = (QualifiedTableName) this.tableNames
				.elementAt(0);
		Vector localVector = (Vector) localHashtable
				.get(localQualifiedTableName);
		if ((localVector == null) || (localVector.size() == 0)) {
			localHashtable = getSearchableCols(this.tableNames);
			localVector = (Vector) localHashtable.get(localQualifiedTableName);
			if ((localVector == null) || (localVector.size() == 0))
				throw new AdoError(
						"To update the record, you need to have the unique key columns or all the searchable columns in the select list.",
						70033);
		}
		String str1 = buildFromCols(localHashtable, i);
		String str2 = localQualifiedTableName.getExtendedTableName();
		Statement localStatement = null;
		PreparedStatement localPreparedStatement = null;
		String str3 = null;
		String str4 = "";
		if (paramBoolean)
			str4 = createDeleteStmt(str2);
		else
			str4 = createUpdateStmt(str2, this.fields);
		str4 = str4 + " where ";
		try {
			if (localStatement == null) {
				str4 = str4 + str1;
			} else {
				str4 = str4 + "current of ";
				str4 = str4 + str3;
			}
			localPreparedStatement = this.ownerCon.prepareStatement(str4);
			println(str4);
			int j = this.fields.getCount();
			int k = 0;
			if (!paramBoolean)
				for (int m = 0; m < j; m++) {
					Field localField = this.fields.getField(m);
					if (!localField.isDirty())
						continue;
					println("  " + localField.getName() + "="
							+ localField.getValue());
					this.ownerCommand
							.setParamValues(localPreparedStatement, k++,
									localField.getValue(),
									localField.getSqlType());
				}
			if (localStatement == null)
				fillWhereClauseValues(localPreparedStatement, k, localVector);
			int m = localPreparedStatement.executeUpdate();
			this.ownerCon.closeStatement(localPreparedStatement);
			localPreparedStatement = null;
			if (localStatement != null) {
				this.ownerCon.closeStatement(localStatement);
				localStatement = null;
			}
			if (m == 0) {
				clearDirtyFlag();
				throw new AdoError(
						paramBoolean ? "The record to be deleted is no longer in the database! You should Refresh the query to get the latest state of the db."
								: "The record to be updated is no longer in the database! You should Refresh the query to get the latest state of the db.",
						70026);
			}
			if (paramBoolean)
				deleteRowInCache();
			else
				updateRowInCache(localVector);
		} catch (SQLException localSQLException) {
			if (localPreparedStatement != null)
				this.ownerCon.closeStatement(localPreparedStatement);
			if (localStatement != null)
				this.ownerCon.closeStatement(localStatement);
			throw new AdoError(localSQLException, "Error executing query: "
					+ str4, 70016);
		}
	}

	private void fillWhereClauseValues(
			PreparedStatement paramPreparedStatement, int paramInt,
			Vector paramVector) throws AdoError {
		int i = paramVector.size();
		for (int j = 0; j < i; j++) {
			Field localField = null;
			try {
				localField = this.fields.getField((String) paramVector
						.elementAt(j));
			} catch (AdoError localAdoError) {
				if ((localAdoError.getNumber() != 30011)
						|| (this.cursorType != 1)) {
					localAdoError
							.setErrorMsg("To update the record, you need to have the unique key columns or all the searchable columns in the select list.");
					localAdoError.setErrorNumber(70033);
					throw localAdoError;
				}
			}
			if (localField == null) {
				this.keyRs.setAbsolutePosition(this.currentRowNum);
				Fields localFields = this.keyRs.getFields();
				localField = localFields.getField((String) paramVector
						.elementAt(j));
			}
			println("  where value: "
					+ (localField.isDirty() ? localField.getOldValue()
							: localField.getValue()));
			this.ownerCommand.setParamValues(paramPreparedStatement, j
					+ paramInt, localField.isDirty() ? localField.getOldValue()
					: localField.getValue(), localField.getSqlType());
		}
	}

	String createDeleteStmt(String paramString) throws AdoError {
		String str1 = this.ownerCon.getIdentifierQuoteString();
		String str2 = "delete from ";
		str2 = str2 + paramString;
		return str2;
	}

	String createUpdateStmt(String paramString, Fields paramFields)
			throws AdoError {
		String str1 = this.ownerCon.getIdentifierQuoteString();
		String str2 = "update ";
		str2 = str2 + paramString;
		str2 = str2 + " set ";
		for (int i = 0; i < paramFields.getCount(); i++) {
			Field localField = paramFields.getField(i);
			if (!localField.isDirty())
				continue;
			str2 = str2 + str1;
			str2 = str2 + localField.getName();
			str2 = str2 + str1;
			str2 = str2 + " = ?, ";
		}
		int i = str2.lastIndexOf(',');
		if (i == -1)
			throw new AdoError(
					"Either none of the fields were updated or the updated fields are not modifiable.",
					70030);
		str2 = str2.substring(0, i);
		return str2;
	}

	void executeUpdate(String paramString) throws AdoError {
		Statement localStatement = null;
		try {
			if (this.lockType != 4) {
				localStatement = this.ownerCon.createStatement();
				this.numRecsAffected = localStatement
						.executeUpdate(paramString);
				this.ownerCon.closeStatement(localStatement);
			}
		} catch (SQLException localSQLException1) {
			if (localStatement != null)
				try {
					localStatement.close();
				} catch (SQLException localSQLException2) {
				}
			throw new AdoError(localSQLException1, "Error executing query: "
					+ paramString, 70016);
		}
	}

	private boolean setKeyValues(Vector paramVector) throws AdoError {
		String str = "";
		boolean i = false;
		for (int j = 0; j < paramVector.size(); j++) {
			str = (String) paramVector.elementAt(j);
			Field localField1 = null;
			try {
				localField1 = this.fields.getField(str);
				if (localField1.isDirty()) {
					Field localField2 = this.keyRs.fields.getField(str);
					Variant localVariant = localField1.getValue();
					localField2.setValue(localVariant);
					i = true;
				}
			} catch (AdoError localAdoError) {
			}
		}
		return i;
	}

	private boolean autoIncColsInKey(Vector paramVector) throws AdoError {
		boolean i = false;
		String str = "";
		int j = 0;
		for (int k = 0; (k < paramVector.size()) && (i == false); k++) {
			str = (String) paramVector.elementAt(k);
			Field localField = this.fields.getField(str);
			if (!localField.isAutoInc())
				continue;
			i = true;
		}
		return i;
	}

	private void addRowInCache() throws AdoError {
		if (this.cursorType == 1) {
			Vector localVector = null;
			if (this.cursorType == 1) {
				Hashtable localHashtable = getKeyCols(this.tableNames);
				QualifiedTableName localQualifiedTableName = (QualifiedTableName) this.tableNames
						.elementAt(0);
				localVector = (Vector) localHashtable
						.get(localQualifiedTableName);
			}
			if (!autoIncColsInKey(localVector)) {
				setKeyValues(localVector);
				this.keyRs.cursor.addToCache(this.keyRs.fields);
				this.keyRs.clearDirtyFlag();
				this.currentRowNum = this.cursor.addToCache(this.fields);
			} else {
				this.currentRowNum -= 1;
			}
		} else if (this.cursorType == 3) {
			this.currentRowNum = this.cursor.addToCache(this.fields);
		}
	}

	private void deleteRowInCache() throws AdoError {
		if (this.cursorType == 1) {
			this.keyRs.cursor.deleteRecord(this.currentRowNum - 1);
			this.cursor.deleteRecord(this.currentRowNum - this.minRid);
		} else if (this.cursorType == 3) {
			this.cursor.deleteRecord(this.currentRowNum - 1);
		}
	}

	private void updateRowInCache(Vector paramVector) throws AdoError {
		if (this.cursorType == 1) {
			if (setKeyValues(paramVector))
				this.keyRs.cursor.updateCache(this.currentRowNum - 1,
						this.keyRs.fields);
			this.keyRs.clearDirtyFlag();
			this.cursor.updateCache(this.currentRowNum - this.minRid,
					this.fields);
		} else if (this.cursorType == 3) {
			this.cursor.updateCache(this.currentRowNum - 1, this.fields);
			if ((this.filter != null) && (this.filter.isNumeric())
					&& (this.filter.toInt() == 1))
				if (this.isDisconnected) {
					this.cursor.setFilteredOut(this.currentRowNum - 1, false);
					this.numRecsAffected += 1;
				} else {
					this.cursor.setFilteredOut(this.currentRowNum - 1, true);
					this.numRecsAffected -= 1;
				}
		}
		clearDirtyFlag();
	}

	private void clearDirtyFlag() throws AdoError {
		for (int i = 0; i < this.fields.getCount(); i++) {
			Field localField = this.fields.getField(i);
			localField.clearDirtyFlag();
		}
	}

	private final void throwCantUpdateOnReadOnly() throws AdoError {
		throw new AdoError(
				"You cannot insert/update/delete if the locktype is set as read only",
				70027);
	}

	private final void throwBgError() throws AdoError {
		if (this.bgErrorSet) {
			this.bgErrorSet = false;
			throw this.backgroundFetchError;
		}
	}

	private final void checkAndThrowInvalidStmtType() throws AdoError {
		if (this.ownerCommand == null)
			return;
		int i = this.ownerCommand.getCommandType();
		int j = this.ownerCommand.getStmtType();
		if (((j & 0x2) != 2) && (i != 4) && (i != 1024) && (i != 4096)
				&& (i != 8192) && (i != 65536) && (i != 131072) && (i != 16384)
				&& (i != 32768) && (i != 2048))
			throw new AdoError("Invalid operation for the command executed.",
					70034);
	}

	final void setBackgroundFetchError(AdoError paramAdoError) {
		this.backgroundFetchError = paramAdoError;
		this.bgErrorSet = true;
	}

	final void println(String paramString) {
		if (this.ownerCon != null)
			this.ownerCon.println(paramString);
	}

	public String toString() {
		String str = "Recordset[";
		if (this.isBOF)
			str = str + "BOF,";
		if (this.isEOF)
			str = str + "EOF,";
		if (this.isOpen)
			str = str + "Open,";
		if (this.isOpen)
			str = str + "filter=" + this.filter + ",";
		str = str + "recCount=" + this.recordCount + ",currentRowNum="
				+ this.currentRowNum + ",cursorType=" + this.cursorType
				+ ",lockType=" + this.lockType + " " + this.ownerCommand + "]";
		return str;
	}

	public static boolean isSqlStatement(String paramString) {
		StringTokenizer localStringTokenizer = new StringTokenizer(paramString,
				" \r\n\t");
		if (localStringTokenizer.hasMoreTokens()) {
			String str = localStringTokenizer.nextToken().trim().toLowerCase();
			return sqlCommands.containsKey(str);
		}
		return false;
	}

	static void init() {
		sqlCommands = new Hashtable();
		Boolean localBoolean = new Boolean(true);
		sqlCommands.put("select", localBoolean);
		sqlCommands.put("insert", localBoolean);
		sqlCommands.put("update", localBoolean);
		sqlCommands.put("delete", localBoolean);
		sqlCommands.put("commit", localBoolean);
		sqlCommands.put("rollback", localBoolean);
		sqlCommands.put("create", localBoolean);
		sqlCommands.put("drop", localBoolean);
		sqlCommands.put("alter", localBoolean);
		sqlCommands.put("grant", localBoolean);
		sqlCommands.put("revoke", localBoolean);
		sqlCommands.put("lock", localBoolean);
		sqlCommands.put("call", localBoolean);
		sqlCommands.put("exec", localBoolean);
		sqlCommands.put("execute", localBoolean);
	}

	public void cancelBatch() throws AdoError {
		if (this.dirtyRecords != null)
			this.dirtyRecords.clear();
		if ((this.filter != null) && (this.filter.isNumeric())
				&& (this.filter.toInt() == 1))
			this.cursor.setAllFilteredOut(false);
	}

	public void updateBatch() throws AdoError {
		if (this.batchUpdateMode) {
			int i = getAbsolutePosition();
			updateIfDirty();
			this.batchUpdateMode = false;
			Variant localVariant1 = getFilter();
			if (localVariant1 != null)
				setFilter(null);
			int j = this.dirtyRecords == null ? 0 : this.dirtyRecords.size();
			try {
				Enumeration localEnumeration = this.dirtyRecords.elements();
				while (localEnumeration.hasMoreElements()) {
					DirtyRecord localDirtyRecord = (DirtyRecord) localEnumeration
							.nextElement();
					moveToPosition(localDirtyRecord.getRowNum());
					fillRecordBuffer(true);
					int k = this.fields.getCount();
					for (int m = 0; m < k; m++) {
						Field localField = this.fields.getField(m);
						Variant localVariant2 = localField.getValue();
						localField.clearDirtyFlag();
						if (!localDirtyRecord.isFieldDirty(m))
							continue;
						localField.setInitialValue(localDirtyRecord
								.getOldValue(m));
						localField.setValue(localVariant2);
					}
					this.dirtyRecords.remove(new Integer(localDirtyRecord
							.getRowNum()));
					if (localDirtyRecord.isDeleted()) {
						delete();
						continue;
					}
					if (localDirtyRecord.isNew())
						setRecordStatus(1);
					else
						setRecordStatus(2);
					update_();
				}
			} catch (AdoError localAdoError) {
				throw localAdoError;
			} finally {
				this.batchUpdateMode = true;
				if (localVariant1 != null)
					setFilter(localVariant1);
				moveToPosition(i);
			}
		} else {
			update();
		}
	}

	private void addBatchUpdate() throws AdoError {
		if (this.dirtyRecords == null)
			this.dirtyRecords = new Hashtable();
		Integer localInteger = new Integer(this.currentRowNum);
		DirtyRecord localDirtyRecord = (DirtyRecord) this.dirtyRecords
				.get(localInteger);
		if (localDirtyRecord == null) {
			localDirtyRecord = new DirtyRecord(this.currentRowNum,
					getFieldCount());
			this.dirtyRecords.put(localInteger, localDirtyRecord);
		}
		if (this.recordStatus == 1)
			localDirtyRecord.setNew(true);
		else if (this.recordStatus == 4)
			localDirtyRecord.setDeleted(true);
		for (int i = 0; i < this.fields.getCount(); i++) {
			Field localField = this.fields.getField(i);
			if (localField.isDirty()) {
				Variant localVariant = localField.getOldValue();
				localField.clearDirtyFlag();
				localDirtyRecord.setFieldDirty(i, true);
				localDirtyRecord.setOldValue(i, localVariant);
			} else {
				if (localDirtyRecord.isFieldDirty(i))
					continue;
				localDirtyRecord.setOldValue(i, localField.getValue());
			}
		}
		setRecordStatus(8);
	}

	static {
		init();
		autoIncQuery = null;
	}

	private static class DirtyRecord {
		private static byte IS_DELETED = 1;
		private static byte IS_NEW = 2;
		private byte flags = 0;
		int rowNum;
		BitSet dirtyFields;
		Vector oldValues;

		DirtyRecord(int paramInt1, int paramInt2) {
			this.rowNum = paramInt1;
			this.dirtyFields = new BitSet(paramInt2);
			this.oldValues = new Vector(paramInt2);
		}

		int getRowNum() {
			return this.rowNum;
		}

		Variant getOldValue(int paramInt) {
			return (Variant) this.oldValues.elementAt(paramInt);
		}

		void setOldValue(int paramInt, Variant paramVariant) {
			paramVariant = new Variant(paramVariant);
			if (paramInt < this.oldValues.size())
				this.oldValues.setElementAt(paramVariant, paramInt);
			else
				this.oldValues.addElement(paramVariant);
		}

		boolean isNew() {
			return (this.flags & IS_NEW) > 0;
		}

		void setNew(boolean paramBoolean) {
			this.flags = (byte) (this.flags & (IS_NEW ^ 0xFFFFFFFF));
			if (paramBoolean)
				this.flags = (byte) (this.flags | IS_NEW);
		}

		boolean isDeleted() {
			return (this.flags & IS_DELETED) > 0;
		}

		void setDeleted(boolean paramBoolean) {
			this.flags = (byte) (this.flags & (IS_DELETED ^ 0xFFFFFFFF));
			if (paramBoolean)
				this.flags = (byte) (this.flags | IS_DELETED);
		}

		boolean isFieldDirty(int paramInt) {
			return this.dirtyFields.get(paramInt);
		}

		void setFieldDirty(int paramInt, boolean paramBoolean) {
			this.dirtyFields.set(paramInt, paramBoolean);
		}
	}

	private static class Condition {
		boolean combineAsAnd = true;
		String fieldName = null;
		Field field = null;
		int compareType = 2;
		Variant compareTo = new Variant();

		private Condition() {
		}

		Condition(Recordset param1) {
			this();
		}
	}
}