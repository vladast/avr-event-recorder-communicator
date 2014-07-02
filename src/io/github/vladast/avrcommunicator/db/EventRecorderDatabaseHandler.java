package io.github.vladast.avrcommunicator.db;

import java.util.ArrayList;
import java.util.Date;

import io.github.vladast.avrcommunicator.AvrRecorderConstants;
import io.github.vladast.avrcommunicator.db.dao.DeviceDAO;
import io.github.vladast.avrcommunicator.db.dao.EventDAO;
import io.github.vladast.avrcommunicator.db.dao.EventRecorderDAO;
import io.github.vladast.avrcommunicator.db.dao.OnDatabaseRequestListener;
import io.github.vladast.avrcommunicator.db.dao.SessionDAO;
import io.github.vladast.avrcommunicator.db.dao.TouchableDAO;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

public class EventRecorderDatabaseHandler extends SQLiteOpenHelper implements OnDatabaseRequestListener {

	private static final String TAG = EventRecorderDatabaseHandler.class.getSimpleName();
	
	private static final String DATABASE_NAME					= "event_recorder.db";
	private static final int DATABASE_VERSION					= 1;
	
	public static final int TABLE_INVALID_ID					= -1;
	
	private static final String DEVICE_TABLE_NAME				= "device";
	private static final String DEVICE_COL_ID					= "id";
	private static final String DEVICE_COL_TYPE					= "type";
	private static final String DEVICE_COL_CODE					= "code"; // v2.0
	private static final String DEVICE_COL_DESCRIPTION			= "description";
	private static final String DEVICE_COL_VID					= "vid";
	private static final String DEVICE_COL_PID					= "pid";
	
	private static final String SESSION_TABLE_NAME				= "session";
	private static final String SESSION_COL_ID					= "id";
	private static final String SESSION_COL_ID_DEVICE			= "idDevice"; // v2.0
	private static final String SESSION_COL_NAME				= "name";
	private static final String SESSION_COL_DESCRIPTION			= "description";
	private static final String SESSION_COL_EVENTS				= "numOfEvents";
	private static final String SESSION_COL_EVENT_TYPES			= "numOfEventTypes";
	private static final String SESSION_COL_INDEX_DEV_SESSION	= "indexDeviceSession";
	private static final String SESSION_COL_TIMESTAMP_UPLOADED	= "timestampUploaded";
	private static final String SESSION_COL_TIMESTAMP_RECORDED	= "timestampRecorded";
	
	private static final String TOUCHABLE_TABLE_NAME			= "touchable";
	private static final String TOUCHABLE_COL_ID				= "id";
	private static final String TOUCHABLE_COL_NAME				= "name";
	
	private static final String EVENT_TABLE_NAME				= "event";
	private static final String EVENT_COL_ID					= "id";
	private static final String EVENT_COL_ID_SESSION			= "idSession";
	private static final String EVENT_COL_ID_TOUCHABLE			= "idTouchable";
	private static final String EVENT_COL_INDEX_DEV_EVENT		= "indexDeviceEvent";
	private static final String EVENT_COL_TIMESTAMP				= "timestamp";
	
	/* v1.0
	create table device (
		    id integer primary key autoincrement not null, 
		    type integer not null, 
		    code integer, 
		    description text, 
		    vid integer, 
		    pid integer
		);

		create table session (
		    id integer primary key autoincrement not null, 
		    idDevice integer not null, 
		    name text not null, 
		    description text, 
		    numOfEvents integer not null, 
		    numOfEventTypes integer not null, 
		    indexDeviceSession integer, 
		    timestampUploaded datetime not null, 
		    timestampRecorded datetime not null,
		    foreign key (idDevice) references device(id)
		);

		create table touchable (
		    id integer primary key autoincrement not null, 
		    name text not null
		);

		create table event (
		    id integer primary key autoincrement not null, 
		    idSession integer not null, 
		    idTouchable integer not null, 
		    indexDeviceEvent integer, 
		    foreign key (idSession) references session(id), 
		    foreign key (idTouchable) references touchable(id)
		);
	*/
	// TODO: String.format called for each SQL statement.
	private static final String CREATE_TABLE_DEVICE	= String.format("create table %s (%s integer primary key, %s integer, %s integer, %s text, %s integer, %s integer);",
			DEVICE_TABLE_NAME, DEVICE_COL_ID, DEVICE_COL_TYPE, DEVICE_COL_CODE, DEVICE_COL_DESCRIPTION, DEVICE_COL_VID, DEVICE_COL_PID);
	private static final String CREATE_TABLE_SESSION = 
			"create table session (id integer primary key, idDevice integer, name text, description text, numOfEvents integer, numOfEventTypes integer, indexDeviceSession integer, timestampUploaded datetime, timestampRecorded datetime, foreign key (idDevice) references device(id));";
	private static final String CREATE_TABLE_TOUCHABLE = String.format("create table %s (%s integer primary key, %s text);", 
			TOUCHABLE_TABLE_NAME, TOUCHABLE_COL_ID, TOUCHABLE_COL_NAME);
	private static final String CREATE_TABLE_EVENT = "create table event (id integer primary key, idSession integer, idTouchable integer, indexDeviceEvent integer, timestamp integer, foreign key (idSession) references session(id), foreign key (idTouchable) references touchable(id));";
	
	public EventRecorderDatabaseHandler(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d(TAG, String.format("Creating database '%s' [version %d]...", DATABASE_NAME, DATABASE_VERSION));
		debugSqlStatement(CREATE_TABLE_DEVICE);
		db.execSQL(CREATE_TABLE_DEVICE);
		debugSqlStatement(CREATE_TABLE_SESSION);
		db.execSQL(CREATE_TABLE_SESSION);
		debugSqlStatement(CREATE_TABLE_TOUCHABLE);
		db.execSQL(CREATE_TABLE_TOUCHABLE);
		debugSqlStatement(CREATE_TABLE_EVENT);
		db.execSQL(CREATE_TABLE_EVENT);
		
		insertDefaultValues(db, TouchableDAO.class);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Export current database into memory, drop database, create database with new schema, import exported data
		
	}
	
	private void debugSqlStatement(String sqlStatement) {
		Log.d(TAG, String.format("Executing SQL statement: '%s'", sqlStatement));
	}
	
	/**
	 * Called during database creation to insert default values in created tables.
	 * @param db <code>SQLiteDatabase</code> object to be updated.
	 */
	private void insertDefaultValues(SQLiteDatabase db, Class<?> clazz) {
		if(clazz.getSimpleName().equals(SessionDAO.class.getSimpleName())) {
			
		} else if (clazz.getSimpleName().equals(EventDAO.class.getSimpleName())) {
			
		} else if (clazz.getSimpleName().equals(DeviceDAO.class.getSimpleName())) {
			
		} else if (clazz.getSimpleName().equals(TouchableDAO.class.getSimpleName())) {
			String insertStatement = "insert into " + TOUCHABLE_TABLE_NAME + "(" + TOUCHABLE_COL_NAME + ") values ";
			for(int i = 0; i < AvrRecorderConstants.MAX_EVENT_NUMBER; ++i) {
				insertStatement += "('" + AvrRecorderConstants.DEFAULT_PREF_TOUCHABLE_NAME_PREFIX + String.valueOf(i + 1) + "')" + ((i == (AvrRecorderConstants.MAX_EVENT_NUMBER - 1)) ? ";" : ",");
			}
			debugSqlStatement(insertStatement);
			db.execSQL(insertStatement);	

			insertStatement = String.format("insert into %s (%s,%s) values (%d,'%s')", 
					TOUCHABLE_TABLE_NAME, TOUCHABLE_COL_ID, TOUCHABLE_COL_NAME, TouchableDAO.TOUCHABLE_END_EVENT_ID, TouchableDAO.TOUCHABLE_END_EVENT_NAME);
			debugSqlStatement(insertStatement);
			db.execSQL(insertStatement);
		}
		
	}

	@Override
	public long OnAdd(Object obj) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		String queryTableName = SESSION_TABLE_NAME;
		
		if(obj.getClass().getSimpleName().equals(SessionDAO.class.getSimpleName())) {
			// NOTE: Not setting ID
			queryTableName = SESSION_TABLE_NAME;
			values.put(SESSION_COL_ID_DEVICE			,((SessionDAO)obj).getIdDevice());
			values.put(SESSION_COL_NAME					,((SessionDAO)obj).getName());
			values.put(SESSION_COL_DESCRIPTION			,((SessionDAO)obj).getDescription());
			values.put(SESSION_COL_EVENTS				,((SessionDAO)obj).getNumberOfEvents());
			values.put(SESSION_COL_EVENT_TYPES			,((SessionDAO)obj).getNumberOfEventTypes());
			values.put(SESSION_COL_INDEX_DEV_SESSION	,((SessionDAO)obj).getIndexDeviceSession());
			values.put(SESSION_COL_TIMESTAMP_UPLOADED	,((SessionDAO)obj).getTimestampUploaded().getTime());
			values.put(SESSION_COL_TIMESTAMP_RECORDED	,((SessionDAO)obj).getTimestampRecorded().getTime());
		} else if (obj.getClass().getSimpleName().equals(EventDAO.class.getSimpleName())) {
			// NOTE: Not setting ID
			queryTableName = EVENT_TABLE_NAME;
			values.put(EVENT_COL_ID_SESSION				,((EventDAO)obj).getIdSession());
			values.put(EVENT_COL_ID_TOUCHABLE			,((EventDAO)obj).getIdTouchable());
			values.put(EVENT_COL_INDEX_DEV_EVENT		,((EventDAO)obj).getIndexDeviceEvent());
			values.put(EVENT_COL_TIMESTAMP				,((EventDAO)obj).getTimestamp());
		} else if (obj.getClass().getSimpleName().equals(DeviceDAO.class.getSimpleName())) {
			// NOTE: Not setting ID
			queryTableName = DEVICE_TABLE_NAME;
			values.put(DEVICE_COL_TYPE					,((DeviceDAO)obj).getType());
			values.put(DEVICE_COL_CODE					,((DeviceDAO)obj).getCode());
			values.put(DEVICE_COL_DESCRIPTION			,((DeviceDAO)obj).getDescription());
			values.put(DEVICE_COL_VID					,((DeviceDAO)obj).getVendorId());
			values.put(DEVICE_COL_PID					,((DeviceDAO)obj).getProductId());
		} else if (obj.getClass().getSimpleName().equals(TouchableDAO.class.getSimpleName())) {
			// NOTE: Not setting ID
			queryTableName = TOUCHABLE_TABLE_NAME;
			values.put(TOUCHABLE_COL_NAME				,((TouchableDAO)obj).getName());
		}		
		
		long result = db.insert(queryTableName, null, values);
		
		db.close();
		((EventRecorderDAO)obj).setId(result);
		return result;
	}

	@Override
	public void OnUpdate(Object obj) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		String queryTableName = SESSION_TABLE_NAME;
		long queryId = -1;
		
		if(obj.getClass().getSimpleName().equals(SessionDAO.class.getSimpleName())) {
			queryTableName = SESSION_TABLE_NAME;
			queryId = ((SessionDAO)obj).getId();
			values.put(SESSION_COL_ID_DEVICE			,((SessionDAO)obj).getIdDevice());
			values.put(SESSION_COL_NAME					,((SessionDAO)obj).getName());
			values.put(SESSION_COL_DESCRIPTION			,((SessionDAO)obj).getDescription());
			values.put(SESSION_COL_EVENTS				,((SessionDAO)obj).getNumberOfEvents());
			values.put(SESSION_COL_EVENT_TYPES			,((SessionDAO)obj).getNumberOfEventTypes());
			values.put(SESSION_COL_INDEX_DEV_SESSION	,((SessionDAO)obj).getIndexDeviceSession());
			values.put(SESSION_COL_TIMESTAMP_UPLOADED	,((SessionDAO)obj).getTimestampUploaded().getTime());
			values.put(SESSION_COL_TIMESTAMP_RECORDED	,((SessionDAO)obj).getTimestampRecorded().getTime());
		} else if (obj.getClass().getSimpleName().equals(EventDAO.class.getSimpleName())) {
			queryTableName = EVENT_TABLE_NAME;
			queryId = ((EventDAO)obj).getId();
			values.put(EVENT_COL_ID_SESSION				,((EventDAO)obj).getIdSession());
			values.put(EVENT_COL_ID_TOUCHABLE			,((EventDAO)obj).getIdTouchable());
			values.put(EVENT_COL_INDEX_DEV_EVENT		,((EventDAO)obj).getIndexDeviceEvent());
			values.put(EVENT_COL_TIMESTAMP				,((EventDAO)obj).getTimestamp());
		} else if (obj.getClass().getSimpleName().equals(DeviceDAO.class.getSimpleName())) {
			queryTableName = DEVICE_TABLE_NAME;
			queryId = ((DeviceDAO)obj).getId();
			values.put(DEVICE_COL_TYPE					,((DeviceDAO)obj).getType());
			values.put(DEVICE_COL_CODE					,((DeviceDAO)obj).getCode());
			values.put(DEVICE_COL_DESCRIPTION			,((DeviceDAO)obj).getDescription());
			values.put(DEVICE_COL_VID					,((DeviceDAO)obj).getVendorId());
			values.put(DEVICE_COL_PID					,((DeviceDAO)obj).getProductId());
		} else if (obj.getClass().getSimpleName().equals(TouchableDAO.class.getSimpleName())) {
			queryTableName = TOUCHABLE_TABLE_NAME;
			queryId = ((TouchableDAO)obj).getId();
			values.put(TOUCHABLE_COL_NAME				,((TouchableDAO)obj).getName());
		}
		
        int updateResult = db.update(queryTableName, values, "id=?", new String[] { String.valueOf(queryId) });
        Log.d(TAG, String.format("Table '%s' update finished with result %d", queryTableName, updateResult));
		
		db.close();
	}

	@Override
	public void OnDelete(Object obj) {		
		SQLiteDatabase db = getWritableDatabase();
		String queryTableName = SESSION_TABLE_NAME;
		long queryId = -1;        
        
		if(obj.getClass().getSimpleName().equals(SessionDAO.class.getSimpleName())) {
			queryTableName = SESSION_TABLE_NAME;
			queryId = ((SessionDAO)obj).getId();
		} else if (obj.getClass().getSimpleName().equals(EventDAO.class.getSimpleName())) {
			queryTableName = EVENT_TABLE_NAME;
			queryId = ((EventDAO)obj).getId();
		} else if (obj.getClass().getSimpleName().equals(DeviceDAO.class.getSimpleName())) {
			queryTableName = DEVICE_TABLE_NAME;
			queryId = ((DeviceDAO)obj).getId();
		} else if (obj.getClass().getSimpleName().equals(TouchableDAO.class.getSimpleName())) {
			queryTableName = TOUCHABLE_TABLE_NAME;
			queryId = ((TouchableDAO)obj).getId();
		}
		
		int deleteResult = db.delete(queryTableName, "id=?", new String[] { String.valueOf(queryId) });
        Log.d(TAG, String.format("Table '%s' update finished with result %d", queryTableName, deleteResult));
		
		db.close();
	}
	
	/**
	 * Retrieves database object based on its primary key
	 * @param clazz Database object's class.
	 * @param id Database object's identifier.
	 * @return Database object.
	 */
	public EventRecorderDAO getDatabaseObjectById(Class<?> clazz, long id) {
		SQLiteDatabase db = getReadableDatabase();
		EventRecorderDAO resultDAO = null;
		String sqlQuery = String.format("select * from %s where id=%d", getTableNameFromClassDao(clazz), id);
		
		Cursor cursor = db.rawQuery(sqlQuery, null);
		
		if(cursor.moveToFirst()){ // Only one object is expected
			if(clazz.getSimpleName().equals(SessionDAO.class.getSimpleName())) {
				resultDAO = new SessionDAO(this);
				((SessionDAO)resultDAO).setId(cursor.getInt(0));
				((SessionDAO)resultDAO).setIdDevice(cursor.getInt(1));
				((SessionDAO)resultDAO).setName(cursor.getString(2));
				((SessionDAO)resultDAO).setDescription(cursor.getString(3));
				((SessionDAO)resultDAO).setNumberOfEvents(cursor.getInt(4));
				((SessionDAO)resultDAO).setNumberOfEventTypes(cursor.getInt(5));
				((SessionDAO)resultDAO).setIndexDeviceSession(cursor.getInt(6));
				((SessionDAO)resultDAO).setTimestampUploaded(new Date(cursor.getLong(7)));
				((SessionDAO)resultDAO).setTimestampRecorded(new Date(cursor.getLong(8)));
			} else if (clazz.getSimpleName().equals(EventDAO.class.getSimpleName())) {
				resultDAO = new EventDAO(this);
				((EventDAO)resultDAO).setId(cursor.getInt(0));
				((EventDAO)resultDAO).setIdSession(cursor.getInt(1));
				((EventDAO)resultDAO).setIdTouchable(cursor.getInt(2));
				((EventDAO)resultDAO).setIndexDeviceEvent(cursor.getInt(3));
				((EventDAO)resultDAO).setTimestamp(cursor.getInt(4));
			} else if (clazz.getSimpleName().equals(DeviceDAO.class.getSimpleName())) {
				resultDAO = new DeviceDAO(this);
				((DeviceDAO)resultDAO).setId(cursor.getInt(0));
				((DeviceDAO)resultDAO).setType(cursor.getInt(1));
				((DeviceDAO)resultDAO).setCode(cursor.getInt(2));
				((DeviceDAO)resultDAO).setDescription(cursor.getString(3));
				((DeviceDAO)resultDAO).setVendorId(cursor.getInt(4));
				((DeviceDAO)resultDAO).setProductId(cursor.getInt(5));
			} else if (clazz.getSimpleName().equals(TouchableDAO.class.getSimpleName())) {
				resultDAO = new TouchableDAO(this);
				((TouchableDAO)resultDAO).setId(cursor.getInt(0));
				((TouchableDAO)resultDAO).setName(cursor.getString(1));
			}				
		}
		
		db.close();
		return resultDAO;	
	}
	
	/**
	 * Retrieves database objects of the same type.
	 * @param clazz Database object's class.
	 * @return Database objects of the given type.
	 */
	public ArrayList<EventRecorderDAO> getDatabaseObjects(Class<?> clazz) {
		
		ArrayList<EventRecorderDAO> resultDAO = new ArrayList<EventRecorderDAO>();
		
		String sqlQuery = "select * from " + getTableNameFromClassDao(clazz);
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.rawQuery(sqlQuery, null);
		
		if(cursor.moveToFirst()) {
			do {
				if(clazz.getSimpleName().equals(SessionDAO.class.getSimpleName())) {
					SessionDAO sessionDAO = new SessionDAO(this);
					sessionDAO.setId(cursor.getInt(0));
					sessionDAO.setIdDevice(cursor.getInt(1));
					sessionDAO.setName(cursor.getString(2));
					sessionDAO.setDescription(cursor.getString(3));
					sessionDAO.setNumberOfEvents(cursor.getInt(4));
					sessionDAO.setNumberOfEventTypes(cursor.getInt(5));
					sessionDAO.setIndexDeviceSession(cursor.getInt(6));
					sessionDAO.setTimestampUploaded(new Date(cursor.getLong(7)));
					sessionDAO.setTimestampRecorded(new Date(cursor.getLong(8)));					
					resultDAO.add(sessionDAO);
				} else if (clazz.getSimpleName().equals(EventDAO.class.getSimpleName())) {
					EventDAO eventDAO = new EventDAO(this);
					eventDAO.setId(cursor.getInt(0));
					eventDAO.setIdSession(cursor.getInt(1));
					eventDAO.setIdTouchable(cursor.getInt(2));
					eventDAO.setIndexDeviceEvent(cursor.getInt(3));
					eventDAO.setTimestamp(cursor.getInt(4));
					resultDAO.add(eventDAO);
				} else if (clazz.getSimpleName().equals(DeviceDAO.class.getSimpleName())) {
					DeviceDAO deviceDAO = new DeviceDAO(this);
					deviceDAO.setId(cursor.getInt(0));
					deviceDAO.setType(cursor.getInt(1));
					deviceDAO.setCode(cursor.getInt(2));
					deviceDAO.setDescription(cursor.getString(3));
					deviceDAO.setVendorId(cursor.getInt(4));
					deviceDAO.setProductId(cursor.getInt(5));
					resultDAO.add(deviceDAO);
				} else if (clazz.getSimpleName().equals(TouchableDAO.class.getSimpleName())) {
					TouchableDAO touchableDAO = new TouchableDAO(this);
					touchableDAO.setId(cursor.getInt(0));
					touchableDAO.setName(cursor.getString(1));
					resultDAO.add(touchableDAO);
				}				
			} while(cursor.moveToNext());
		}
		
		db.close();
		return resultDAO;		
	}
	
	public ArrayList<EventRecorderDAO> getDatabaseObjectsByForeign(Class<?> clazz, EventRecorderDAO foreign) {
		ArrayList<EventRecorderDAO> resultDAO = new ArrayList<EventRecorderDAO>();
		
		String sqlCondition = "";
		
		if(clazz.getSimpleName().equals(SessionDAO.class.getSimpleName())) {
			
		} else if (clazz.getSimpleName().equals(EventDAO.class.getSimpleName())) {
			sqlCondition = String.format("idSession=%d", foreign.getId());
		} else if (clazz.getSimpleName().equals(DeviceDAO.class.getSimpleName())) {
			
		} else if (clazz.getSimpleName().equals(TouchableDAO.class.getSimpleName())) {
			
		}
		
		String sqlQuery = String.format("select * from %s where %s", getTableNameFromClassDao(clazz), sqlCondition);
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.rawQuery(sqlQuery, null);
		
		if(cursor.moveToFirst()) {
			do {
				if(clazz.getSimpleName().equals(SessionDAO.class.getSimpleName())) {
					SessionDAO sessionDAO = new SessionDAO(this);
					sessionDAO.setId(cursor.getInt(0));
					sessionDAO.setIdDevice(cursor.getInt(1));
					sessionDAO.setName(cursor.getString(2));
					sessionDAO.setDescription(cursor.getString(3));
					sessionDAO.setNumberOfEvents(cursor.getInt(4));
					sessionDAO.setNumberOfEventTypes(cursor.getInt(5));
					sessionDAO.setIndexDeviceSession(cursor.getInt(6));
					sessionDAO.setTimestampUploaded(new Date(cursor.getLong(7)));
					sessionDAO.setTimestampRecorded(new Date(cursor.getLong(8)));
					resultDAO.add(sessionDAO);
				} else if (clazz.getSimpleName().equals(EventDAO.class.getSimpleName())) {
					EventDAO eventDAO = new EventDAO(this);
					eventDAO.setId(cursor.getInt(0));
					eventDAO.setIdSession(cursor.getInt(1));
					eventDAO.setIdTouchable(cursor.getInt(2));
					eventDAO.setIndexDeviceEvent(cursor.getInt(3));
					eventDAO.setTimestamp(cursor.getInt(4));
					resultDAO.add(eventDAO);
				} else if (clazz.getSimpleName().equals(DeviceDAO.class.getSimpleName())) {
					DeviceDAO deviceDAO = new DeviceDAO(this);
					deviceDAO.setId(cursor.getInt(0));
					deviceDAO.setType(cursor.getInt(1));
					deviceDAO.setCode(cursor.getInt(2));
					deviceDAO.setDescription(cursor.getString(3));
					deviceDAO.setVendorId(cursor.getInt(4));
					deviceDAO.setProductId(cursor.getInt(5));
					resultDAO.add(deviceDAO);
				} else if (clazz.getSimpleName().equals(TouchableDAO.class.getSimpleName())) {
					TouchableDAO touchableDAO = new TouchableDAO(this);
					touchableDAO.setId(cursor.getInt(0));
					touchableDAO.setName(cursor.getString(1));
					resultDAO.add(touchableDAO);
				}				
			} while(cursor.moveToNext());
		}
		
		db.close();
		return resultDAO;
	}
	
	/**
	 * Counts number of row within given table
	 * @param clazz Database object's class.
	 * @return Number of rows within class' table.
	 */
	public long getDatabaseObjectCount(Class<?> clazz) {
		SQLiteDatabase db = getReadableDatabase();
		long result = 0;
		
		SQLiteStatement sqliteStatement = db.compileStatement("select count(*) from " + getTableNameFromClassDao(clazz));
		result = sqliteStatement.simpleQueryForLong();
		
		db.close();
		
		return result;
	}
	
	/**
	 * Counts number of row within particular table per foreign object's ID value
	 * @param clazz Database object's class to be queried.
	 * @param dao Database object from which ID value is to be extracted.
	 * @return Number of rows within class' table.
	 */
	public long getDatabaseObjectCountByForeignId(Class<?> clazz, EventRecorderDAO dao) {
		SQLiteDatabase db = getReadableDatabase();
		long result = 0;
		
		String sqlCondition = "";
		
		if(clazz.getSimpleName().equals(SessionDAO.class.getSimpleName())) {
			
		} else if (clazz.getSimpleName().equals(EventDAO.class.getSimpleName())) {
			sqlCondition = String.format("idSession=%d", dao.getId());
		} else if (clazz.getSimpleName().equals(DeviceDAO.class.getSimpleName())) {
			
		} else if (clazz.getSimpleName().equals(TouchableDAO.class.getSimpleName())) {
			
		}	
		
		String sqlQuery = String.format("select count(*) from %s where %s", getTableNameFromClassDao(clazz), sqlCondition);
		
		SQLiteStatement sqliteStatement = db.compileStatement(sqlQuery);
		result = sqliteStatement.simpleQueryForLong();
		
		db.close();
		
		return result;
	}
	
	/**
	 * Counts values within the specified column of give table
	 * @param clazz Database object's class.
	 * @param columnName Name of the column containing numbers.
	 * <b>NOTE:</b> It is expected that column is of type INTEGER
	 * @return Sum of all values within given column.
	 */
	public long getDatabaseObjectValueCount(Class<?> clazz, String columnName) {
		// TODO Perform addition of all values within same column of the same table + check column type by calling getType on the cursor
		
		SQLiteDatabase db = getReadableDatabase();
		long result = 0;
		
		SQLiteStatement sqliteStatement = db.compileStatement(String.format("select sum(%s) from %s", columnName, getTableNameFromClassDao(clazz)));
		result = sqliteStatement.simpleQueryForLong();
		
		db.close();
		
		return result;
	}
	
	/**
	 * Counts values within the specified column of given table, per foreign object's ID
	 * @param clazz Database object's class.
	 * @param columnName Name of the column containing numbers.
	 * <b>NOTE:</b> It is expected that column is of type INTEGER
	 * @param dao Database object from which ID value is to be extracted.
	 * @return Sum of all values within given column.
	 */
	public long getDatabaseObjectValueCountByForeignId(Class<?> clazz, String columnName, EventRecorderDAO dao) {
		// TODO Perform addition of all values within same column of the same table + check column type by calling getType on the cursor
		
		SQLiteDatabase db = getReadableDatabase();
		long result = 0;
		
		String sqlCondition = "";
		
		if(clazz.getSimpleName().equals(SessionDAO.class.getSimpleName())) {
			
		} else if (clazz.getSimpleName().equals(EventDAO.class.getSimpleName())) {
			sqlCondition = String.format("idSession=%d", dao.getId());
		} else if (clazz.getSimpleName().equals(DeviceDAO.class.getSimpleName())) {
			
		} else if (clazz.getSimpleName().equals(TouchableDAO.class.getSimpleName())) {
			
		}	
		
		String sqlQuery = String.format("select sum(%s) from %s where %s", columnName, getTableNameFromClassDao(clazz), sqlCondition);		
		
		
		SQLiteStatement sqliteStatement = db.compileStatement(sqlQuery);
		result = sqliteStatement.simpleQueryForLong();
		
		db.close();
		
		return result;
	}
	
	/**
	 * Retrieves last record added into particular table.
	 * @param clazz Database object's class.
	 * @return Last recorded added.
	 */
	public EventRecorderDAO getLastDatabaseObject(Class<?> clazz) {
		SQLiteDatabase db = getReadableDatabase();
		EventRecorderDAO resultDAO = null;
		String sqlQuery = String.format("select * from %s order by id desc limit 1", getTableNameFromClassDao(clazz));
		
		Cursor cursor = db.rawQuery(sqlQuery, null);
		
		Log.d(TAG, cursor.toString());
		
		if(cursor.moveToFirst()) {
			if(clazz.getSimpleName().equals(SessionDAO.class.getSimpleName())) {
				resultDAO = new SessionDAO(this);
				((SessionDAO)resultDAO).setId(cursor.getInt(0));
				((SessionDAO)resultDAO).setIdDevice(cursor.getInt(1));
				((SessionDAO)resultDAO).setName(cursor.getString(2));
				((SessionDAO)resultDAO).setDescription(cursor.getString(3));
				((SessionDAO)resultDAO).setNumberOfEvents(cursor.getInt(4));
				((SessionDAO)resultDAO).setNumberOfEventTypes(cursor.getInt(5));
				((SessionDAO)resultDAO).setIndexDeviceSession(cursor.getInt(6));
				((SessionDAO)resultDAO).setTimestampUploaded(new Date(cursor.getLong(7)));
				((SessionDAO)resultDAO).setTimestampRecorded(new Date(cursor.getLong(8)));
			} else if (clazz.getSimpleName().equals(EventDAO.class.getSimpleName())) {
				resultDAO = new EventDAO(this);
				((EventDAO)resultDAO).setId(cursor.getInt(0));
				((EventDAO)resultDAO).setIdSession(cursor.getInt(1));
				((EventDAO)resultDAO).setIdTouchable(cursor.getInt(2));
				((EventDAO)resultDAO).setIndexDeviceEvent(cursor.getInt(3));
				((EventDAO)resultDAO).setTimestamp(cursor.getInt(4));
			} else if (clazz.getSimpleName().equals(DeviceDAO.class.getSimpleName())) {
				resultDAO = new DeviceDAO(this);
				((DeviceDAO)resultDAO).setId(cursor.getInt(0));
				((DeviceDAO)resultDAO).setType(cursor.getInt(1));
				((DeviceDAO)resultDAO).setCode(cursor.getInt(2));
				((DeviceDAO)resultDAO).setDescription(cursor.getString(3));
				((DeviceDAO)resultDAO).setVendorId(cursor.getInt(4));
				((DeviceDAO)resultDAO).setProductId(cursor.getInt(5));
			} else if (clazz.getSimpleName().equals(TouchableDAO.class.getSimpleName())) {
				resultDAO = new TouchableDAO(this);
				((TouchableDAO)resultDAO).setId(cursor.getInt(0));
				((TouchableDAO)resultDAO).setName(cursor.getString(1));
			}				
		}

		db.close();
		return resultDAO;		
	}
	
	/**
	 * Determines SQL table name based on the give database object's class
	 * @param clazz Database object's class.
	 * @return Table name of the given database object's class
	 */
	private String getTableNameFromClassDao(Class<?> clazz) {
		String tableName = SESSION_TABLE_NAME;
		if(clazz.getSimpleName().equals(SessionDAO.class.getSimpleName())) {
			tableName = SESSION_TABLE_NAME;
		} else if (clazz.getSimpleName().equals(EventDAO.class.getSimpleName())) {
			tableName = EVENT_TABLE_NAME;
		} else if (clazz.getSimpleName().equals(DeviceDAO.class.getSimpleName())) {
			tableName = DEVICE_TABLE_NAME;
		} else if (clazz.getSimpleName().equals(TouchableDAO.class.getSimpleName())) {
			tableName = TOUCHABLE_TABLE_NAME;
		}
		return tableName;
	}
}
