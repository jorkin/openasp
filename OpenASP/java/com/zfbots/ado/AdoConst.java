package com.zfbots.ado;

public abstract interface AdoConst
{
  public static final int adUnsupported = 0;
  public static final int adUnknown = -1;
  public static final int adCmdUnspecified = -1;
  public static final int adCmdUnknown = 8;
  public static final int adCmdText = 1;
  public static final int adCmdTable = 2;
  public static final int adCmdStoredProc = 4;
  public static final int adCmdFile = 256;
  public static final int adCmdTableDirect = 512;
  public static final int adCmdGetTables = 1024;
  public static final int adCmdGetColumns = 2048;
  public static final int adCmdGetPrimaryKeys = 4096;
  public static final int adCmdGetForeignKeys = 8192;
  public static final int adCmdGetImportedKeys = 8192;
  public static final int adCmdGetUniqueIndexes = 16384;
  public static final int adCmdGetNonUniqueIndexes = 32768;
  public static final int adCmdGetExportedKeys = 65536;
  public static final int adCmdGetCrossReference = 131072;
  public static final int adModeUnknown = 0;
  public static final int adModeRead = 1;
  public static final int adModeWrite = 2;
  public static final int adModeReadWrite = 3;
  public static final int adModeShareDenyRead = 4;
  public static final int adModeShareDenyWrite = 8;
  public static final int adModeShareExclusive = 12;
  public static final int adModeShareDenyNone = 16;
  public static final int adHoldRecords = 256;
  public static final int adMovePrevious = 512;
  public static final int adAddNew = 16778240;
  public static final int adDelete = 16779264;
  public static final int adUpdate = 16809984;
  public static final int adBookmark = 8192;
  public static final int adApproxPosition = 16384;
  public static final int adUpdateBatch = 65536;
  public static final int adResync = 131072;
  public static final int adNotify = 262144;
  public static final int adFind = 524288;
  public static final int adOpenUnspecified = -1;
  public static final int adOpenForwardOnly = 0;
  public static final int adOpenKeyset = 1;
  public static final int adOpenDynamic = 2;
  public static final int adOpenStatic = 3;
  public static final int adEmpty = 0;
  public static final int adNull = 1;
  public static final int adSmallInt = 3;
  public static final int adInteger = 4;
  public static final int adSingle = 7;
  public static final int adDouble = 8;
  public static final int adCurrency = 10;
  public static final int adDecimal = 10;
  public static final int adError = 5;
  public static final int adBoolean = 11;
  public static final int adVariant = 12;
  public static final int adIDispatch = 13;
  public static final int adIUnknown = 13;
  public static final int adDate = 14;
  public static final int adBSTR = 15;
  public static final int adTinyInt = 2;
  public static final int adBigInt = 6;
  public static final int adUnsignedTinyInt = 2;
  public static final int adUnsignedSmallInt = 3;
  public static final int adUnsignedInt = 4;
  public static final int adUnsignedBigInt = 6;
  public static final int adNumeric = 131;
  public static final int adUserDefined = 132;
  public static final int adGUID = 72;
  public static final int adDBDate = 133;
  public static final int adDBTime = 134;
  public static final int adDBTimeStamp = 135;
  public static final int adChar = 129;
  public static final int adVarChar = 200;
  public static final int adLongVarChar = 201;
  public static final int adWChar = 130;
  public static final int adVarWChar = 202;
  public static final int adLongVarWChar = 203;
  public static final int adBinary = 128;
  public static final int adVarBinary = 204;
  public static final int adLongVarBinary = 205;
  public static final int adChapter = 136;
  public static final int adFileTime = 64;
  public static final int adDBFileTime = 137;
  public static final int adPropVariant = 138;
  public static final int adVarNumeric = 139;
  public static final int adArray = 8192;
  public static final int adByRef = 16384;
  public static final int adVector = 4096;
  public static final int adFldUnspecified = -1;
  public static final int adFldBookmark = 1;
  public static final int adFldMayDefer = 2;
  public static final int adFldUpdatable = 4;
  public static final int adFldUnknownUpdatable = 8;
  public static final int adFldFixed = 16;
  public static final int adFldIsNullable = 32;
  public static final int adFldMayBeNull = 64;
  public static final int adFldLong = 128;
  public static final int adFldRowID = 256;
  public static final int adFldRowVersion = 512;
  public static final int adFldCacheDeferred = 4096;
  public static final int adFldNegativeScale = 16384;
  public static final int adFldKeyColumn = 32768;
  public static final int adFilterNone = 0;
  public static final int adFilterPendingRecords = 1;
  public static final int adFilterAffectedRecords = 2;
  public static final int adFilterFetchedRecords = 3;
  public static final int adFilterPredicate = 4;
  public static final int adFilterConflictingRecords = 5;
  public static final int adFilterFailedRecords = 2;
  public static final int adGetRowsRest = -1;
  public static final int adXactUnspecified = -1;
  public static final int adXactChaos = 16;
  public static final int adXactReadUncommitted = 256;
  public static final int adXactBrowse = 256;
  public static final int adXactCursorStability = 4096;
  public static final int adXactReadCommitted = 4096;
  public static final int adXactRepeatableRead = 65536;
  public static final int adXactSerializable = 1048576;
  public static final int adXactIsolated = 1048576;
  public static final int adXactCommitRetaining = 131072;
  public static final int adXactAbortRetaining = 262144;
  public static final int adLockReadOnly = 1;
  public static final int adLockPessimistic = 2;
  public static final int adLockOptimistic = 3;
  public static final int adLockBatchOptimistic = 4;
  public static final int adParamSigned = 16;
  public static final int adParamNullable = 64;
  public static final int adParamLong = 128;
  public static final int adParamUnknown = 0;
  public static final int adParamInput = 1;
  public static final int adParamOutput = 2;
  public static final int adParamInputOutput = 3;
  public static final int adParamReturnValue = 4;
  public static final int adPosUnknown = -1;
  public static final int adPosBOF = -2;
  public static final int adPosEOF = -3;
  public static final int adPropNotSupported = 0;
  public static final int adPropRequired = 1;
  public static final int adPropOptional = 2;
  public static final int adPropRead = 512;
  public static final int adPropWrite = 1024;
  public static final int adRecOK = 0;
  public static final int adRecNew = 1;
  public static final int adRecModified = 2;
  public static final int adRecDeleted = 4;
  public static final int adRecUnmodified = 8;
  public static final int adRecInvalid = 16;
  public static final int adRecMultipleChanges = 64;
  public static final int adRecPendingChanges = 128;
  public static final int adRecCanceled = 256;
  public static final int adRecCantRelease = 1024;
  public static final int adRecConcurrencyViolation = 2048;
  public static final int adRecIntegrityViolation = 4096;
  public static final int adRecMaxChangesExceeded = 8192;
  public static final int adRecObjectOpen = 16384;
  public static final int adRecOutOfMemory = 32768;
  public static final int adRecPermissionDenied = 65536;
  public static final int adRecSchemaViolation = 131072;
  public static final int adRecDBDeleted = 262144;
  public static final int adRecError = 32;
  public static final int adAdoError = 32;
  public static final int adRsnAddNew = 1;
  public static final int adRsnDelete = 2;
  public static final int adRsnUpdate = 3;
  public static final int adRsnUndoUpdate = 4;
  public static final int adRsnUndoAddNew = 5;
  public static final int adRsnUndoDelete = 6;
  public static final int adRsnRequery = 7;
  public static final int adRsnResynch = 8;
  public static final int adRsnClose = 9;
  public static final int adRsnMove = 10;
  public static final int adRsnFirstChange = 11;
  public static final int adRsnMoveFirst = 12;
  public static final int adRsnMoveNext = 13;
  public static final int adRsnMovePrevious = 14;
  public static final int adRsnMoveLast = 15;
  public static final int adPropISOUnspecified = -1;
  public static final int adPropISOReadUncommitted = 256;
  public static final int adPropISOReadCommitted = 4096;
  public static final int adPropISORepeatableRead = 65536;
  public static final int adPropISOSerializable = 1048576;
  public static final int adPropCLStart = 1;
  public static final int adPropCLEnd = 2;
  public static final int adPropCUDML = 1;
  public static final int adPropCUTableDef = 2;
  public static final int adPropCUIndexDef = 4;
  public static final int adPropCUPrivilegeDef = 8;
  public static final int adPropCNBNull = 1;
  public static final int adPropCNBNonNull = 2;
  public static final int adPropGBEqualsSelect = 1;
  public static final int adPropGBContainsSelect = 2;
  public static final int adPropGBNoRelation = 3;
  public static final int adPropHTCatalogs = 1;
  public static final int adPropHTProviders = 2;
  public static final int adPropICUpper = 1;
  public static final int adPropICLower = 2;
  public static final int adPropICSensitive = 3;
  public static final int adPropICMixed = 4;
  public static final int adPropLMNone = 1;
  public static final int adPropLMRead = 2;
  public static final int adPropLMIntent = 4;
  public static final int adPropLMWrite = 8;
  public static final int adPropNCEnd = 1;
  public static final int adPropNCHigh = 2;
  public static final int adPropNCLow = 3;
  public static final int adPropNCStart = 4;
  public static final int adPropCBDelete = 1;
  public static final int adPropCBPreserve = 2;
  public static final int adPropSUDML = 1;
  public static final int adPropSUTableDef = 2;
  public static final int adPropSUIndexDef = 4;
  public static final int adPropSUPrivilegeDef = 8;
  public static final int adPropSQCorrelated = 1;
  public static final int adPropSQComparison = 2;
  public static final int adPropSQExists = 4;
  public static final int adPropSQIn = 8;
  public static final int adPropSQQuantified = 16;
  public static final int adWAGetIndexInfo = 1;
  public static final int adWACallSetDouble = 2;
  public static final int adWAIgnoreReadOnly = 4;
  public static final int adWASkipGetCatalog = 8;
  public static final int e_OTHER = 1;
  public static final int e_SELECT = 2;
  public static final int e_WHERE = 4;
  public static final int e_GROUP = 8;
  public static final int e_ORDER = 16;
  public static final int e_STORED_PROC = 32;
  public static final int STAT_TYPE = 7;
  public static final int STAT_INDEX_NAME = 6;
  public static final int STAT_COLUMN_NAME = 9;
  public static final int COL_CATALOG_NAME = 1;
  public static final int COL_SCHEMA_NAME = 2;
  public static final int COL_TABLE_NAME = 3;
  public static final int COL_COLUMN_NAME = 4;
  public static final int COL_COLUMN_TYPE = 5;
  public static final int COL_TYPE_NAME = 6;
  public static final int COL_COLUMN_PREC = 7;
  public static final int COL_BUFFER_LENGTH = 8;
  public static final int COL_COLUMN_SCALE = 9;
  public static final int COL_RADIX = 10;
  public static final int COL_NULLABLE = 11;
  public static final int COL_REMARKS = 12;
  public static final int COL_COLUMN_DEF = 13;
  public static final int COL_SQL_TYPE = 14;
  public static final int COL_DATETIME_SUB = 15;
  public static final int COL_CHAR_OCTET_LEN = 16;
  public static final int COL_ORDINAL_POSITION = 17;
  public static final int COL_ISNULLABLE = 18;
  public static final int PK_CATALOG_NAME = 1;
  public static final int PK_SCHEMA_NAME = 2;
  public static final int PK_TABLE_NAME = 3;
  public static final int PK_COLUMN_NAME = 4;
  public static final int PK_KEY_SEQ = 5;
  public static final int PK_NAME = 6;
  public static final int FK_PKCATALOG_NAME = 1;
  public static final int FK_PKSCHEMA_NAME = 2;
  public static final int FK_PKTABLE_NAME = 3;
  public static final int FK_PKCOLUMN_NAME = 4;
  public static final int FK_FKCATALOG_NAME = 5;
  public static final int FK_FKSCHEMA_NAME = 6;
  public static final int FK_FKTABLE_NAME = 7;
  public static final int FK_FKCOLUMN_NAME = 8;
  public static final int FK_KEY_SEQ = 9;
  public static final int FK_UPDATE_RULE = 10;
  public static final int FK_DELETE_RULE = 11;
  public static final int FK_FK_NAME = 12;
  public static final int FK_PK_NAME = 13;
  public static final int DEFERRABILITY = 14;
  public static final int IX_CATALOG_NAME = 1;
  public static final int IX_SCHEMA_NAME = 2;
  public static final int IX_TABLE_NAME = 3;
  public static final int IX_NON_UNIQUE = 4;
  public static final int IX_QUALIFIER = 5;
  public static final int IX_NAME = 6;
  public static final int IX_TYPE = 7;
  public static final int IX_ORDINAL_POSITION = 8;
  public static final int IX_COLUMN_NAME = 9;
  public static final int IX_ASC_OR_DESC = 10;
  public static final int IX_CARDINALITY = 11;
  public static final int IX_PAGES = 12;
  public static final int IX_FILTER_CONDITION = 13;
  public static final int IX_TYPE_TABLE_STATISTIC = 0;
  public static final int IX_TYPE_TABLE_CLUSTERED = 1;
  public static final int IX_TYPE_TABLE_HASHED = 2;
  public static final int IX_TYPE_TABLE_OTHER = 3;
}
