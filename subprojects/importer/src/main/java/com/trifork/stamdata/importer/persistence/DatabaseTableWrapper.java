// The contents of this file are subject to the Mozilla Public
// License Version 1.1 (the "License"); you may not use this file
// except in compliance with the License. You may obtain a copy of
// the License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS
// IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
// implied. See the License for the specific language governing
// rights and limitations under the License.
//
// Contributor(s): Contributors are attributed in the source code
// where applicable.
//
// The Original Code is "Stamdata".
//
// The Initial Developer of the Original Code is Trifork Public A/S.
//
// Portions created for the Original Code are Copyright 2011,
// Lægemiddelstyrelsen. All Rights Reserved.
//
// Portions created for the FMKi Project are Copyright 2011,
// National Board of e-Health (NSI). All Rights Reserved.

package com.trifork.stamdata.importer.persistence;

import static com.trifork.stamdata.importer.util.DateUtils.toMySQLdate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Rune Skou Larsen <rsj@trifork.com>
 */
public class DatabaseTableWrapper<T extends StamdataEntity>
{
	private static Logger logger = LoggerFactory.getLogger(DatabaseTableWrapper.class);

	private PreparedStatement insertRecordStmt;
	private PreparedStatement insertAndUpdateRecordStmt;
	private PreparedStatement updateRecordStmt;
	private PreparedStatement selectByIdStmt;
	private PreparedStatement updateValidToStmt;
	private PreparedStatement updateValidFromStmt;
	private PreparedStatement deleteStmt;

	protected Connection connection;

	public ResultSet currentRS;
	private Class<T> type;
	private String tablename;
	private Method idMethod;

	private List<Method> outputMethods;
	private List<String> notUpdatedColumns;
	private int insertedRecords, updatedRecords, deletedRecords = 0;

	public DatabaseTableWrapper(Connection connnection, Class<T> type) throws SQLException
	{
		this(connnection, type, Dataset.getEntityTypeDisplayName(type));
	}

	protected DatabaseTableWrapper(Connection con, Class<T> clazz, String tableName) throws SQLException
	{
		this.tablename = tableName;
		this.type = clazz;
		this.connection = con;
		this.idMethod = AbstractStamdataEntity.getIdMethod(clazz);

		outputMethods = AbstractStamdataEntity.getOutputMethods(clazz);
		notUpdatedColumns = locateNotUpdatedColumns();
		insertRecordStmt = prepareInsertStatement();
		insertAndUpdateRecordStmt = prepareInsertAndUpdateStatement();
		updateRecordStmt = prepareUpdateStatement();
		selectByIdStmt = prepareSelectByIdStatement();
		updateValidToStmt = prepareUpdateValidtoStatement();
		updateValidFromStmt = prepareUpdateValidFromStatement();
		deleteStmt = prepareDeleteStatement();
	}

	private PreparedStatement prepareInsertStatement() throws SQLException
	{

		String sql = "INSERT INTO " + tablename + " (" + "ModifiedDate, CreatedDate, ValidFrom, ValidTo";
		for (Method method : outputMethods)
		{
			sql += ", ";
			String name = AbstractStamdataEntity.getOutputFieldName(method);
			sql += name;
		}
		sql += ") values (";
		sql += "?,"; // modifieddate
		sql += "?,"; // createddate
		sql += "?,"; // validfrom
		sql += "?"; // validto

		for (int i = 0; i < outputMethods.size(); i++)
		{
			sql += ",?";
		}

		sql += ")";

		// logger.debug("Preparing insert statement: " + sql);
		return connection.prepareStatement(sql);
	}

	private PreparedStatement prepareInsertAndUpdateStatement() throws SQLException
	{
		String sql = "insert into " + tablename + " (ModifiedDate, CreatedDate, ValidFrom, ValidTo";
		for (Method method : outputMethods)
		{
			sql += ", ";
			String name = AbstractStamdataEntity.getOutputFieldName(method);
			sql += name;
		}

		for (String notUpdateName : notUpdatedColumns)
		{
			sql += ", " + notUpdateName;
		}

		sql += ") values (";
		sql += "?,"; // modifieddate
		sql += "?,"; // createddate
		sql += "?,"; // validfrom
		sql += "?"; // validto

		for (int i = 0; i < outputMethods.size(); i++)
		{
			sql += ",?";
		}

		for (int i = 0; i < notUpdatedColumns.size(); i++)
		{
			sql += ",?";
		}

		sql += ")";

		return connection.prepareStatement(sql);
	}

	private PreparedStatement prepareUpdateStatement() throws SQLException
	{
		String sql = "UPDATE " + tablename + " SET ModifiedDate = ?, ValidFrom = ?, ValidTo = ?";

		for (Method method : outputMethods)
		{
			sql += ", " + AbstractStamdataEntity.getOutputFieldName(method) + " = ?";
		}

		sql += " WHERE " + Dataset.getIdOutputName(type) + " = ? and ValidFrom = ? and ValidTo = ?";

		return connection.prepareStatement(sql);
	}

	private PreparedStatement prepareSelectByIdStatement() throws SQLException
	{
		// Select where ids match and validity intervals overlap.

		String pstmtString = "SELECT * FROM " + tablename + " WHERE " + Dataset.getIdOutputName(type) + " = ? AND NOT (ValidTo < ? or ValidFrom > ?) ORDER BY ValidTo";
		return connection.prepareStatement(pstmtString);
	}

	public void insertRow(StamdataEntity sde, Date transactionTime)
	{
		applyParamsToInsertStatement(insertRecordStmt, sde, transactionTime, transactionTime);

		try
		{
			insertRecordStmt.execute();
			if (++insertedRecords % 1000 == 0)
			{
				logger.debug("Inserted " + tablename + ": " + insertedRecords);
			}
		}
		catch (SQLException sqle)
		{
			String message = "An error occured while inserting new entity of type: " + Dataset.getEntityTypeDisplayName(type);
			try
			{
				message += "entityid: " + sde.getKey();
			}
			catch (Exception e)
			{}

			throw new RuntimeException(message, sqle);
		}
	}

	public void insertAndUpdateRow(StamdataEntity sde, Date transactionTime)
	{
		applyParamsToInsertAndUpdateStatement(insertAndUpdateRecordStmt, sde, transactionTime, transactionTime);

		try
		{
			insertAndUpdateRecordStmt.execute();
			if (++insertedRecords % 1000 == 0)
			{
				logger.debug("Inserted " + tablename + ": " + insertedRecords);
			}
		}
		catch (SQLException sqle)
		{
			String message = "An error occured while inserting new entity of type: " + Dataset.getEntityTypeDisplayName(type);
			try
			{
				message += "entityid: " + sde.getKey() + " SQLError: " + sqle.getMessage();
			}
			catch (Exception e)
			{
				message += " Error: " + e.getMessage();
			}
			throw new RuntimeException(message, sqle);
		}
	}

	public void updateRow(T sde, Date transactionTime, Date existingValidFrom, Date existingValidTo)
	{
		applyParamsToUpdateStatement(updateRecordStmt, sde, transactionTime, transactionTime, existingValidFrom, existingValidTo);

		try
		{
			updateRecordStmt.execute();

			if (++updatedRecords % 1000 == 0)
			{
				logger.debug("Updated " + tablename + ": " + updatedRecords);
			}
		}
		catch (SQLException sqle)
		{
			String message = "An error occured while inserting new entity of type: " + Dataset.getEntityTypeDisplayName(type);

			try
			{
				message += "entityid: " + sde.getKey();
			}
			catch (Exception e)
			{

			}

			throw new RuntimeException(message, sqle);
		}
	}

	public int getInsertedRows()
	{
		return insertedRecords;
	}

	private PreparedStatement prepareUpdateValidtoStatement() throws SQLException
	{
		String pstmtString = "update " + tablename + " set ValidTo = ?, " + "ModifiedDate = ? WHERE " + Dataset.getIdOutputName(type) + " = ? and ValidFrom = ?";

		return connection.prepareStatement(pstmtString);
	}

	private PreparedStatement prepareUpdateValidFromStatement() throws SQLException
	{
		String pstmtString = "update " + tablename + " set ValidFrom = ?, " + "ModifiedDate = ? WHERE " + Dataset.getIdOutputName(type) + " = ? AND ValidFrom = ?";

		return connection.prepareStatement(pstmtString);
	}

	public PreparedStatement prepareDeleteStatement() throws SQLException
	{
		String sql = "delete from " + tablename + " where " + Dataset.getIdOutputName(type) + " = " + "? and ValidFrom = ?";
		return connection.prepareStatement(sql);
	}

	public int applyParamsToInsertStatement(PreparedStatement pstmt, StamdataEntity sde, Date transactionTime, Date createdTime)
	{
		int idx = 1;

		try
		{
			pstmt.setTimestamp(idx++, new Timestamp(transactionTime.getTime()));
			pstmt.setTimestamp(idx++, new Timestamp(createdTime.getTime()));
			pstmt.setTimestamp(idx++, new Timestamp(sde.getValidFrom().getTime()));
			pstmt.setTimestamp(idx++, new Timestamp(sde.getValidTo().getTime()));
		}
		catch (SQLException sqle)
		{
			throw new RuntimeException("An error occured during application of parameters to a prepared statement. ", sqle);
		}

		for (Method method : outputMethods)
		{
			Object o;

			try
			{
				o = method.invoke(sde);
			}
			catch (InvocationTargetException ite)
			{
				throw new RuntimeException("An error occured during application of parameters to a prepared statement. Entity type: [" + sde.getClass() + "]. The entity id was: [" + sde.getKey() + "]. Could not invoke target method: [" + method.getName() + "]", ite);
			}
			catch (IllegalAccessException iae)
			{
				throw new RuntimeException("An error occured during application of parameters to a prepared statement. Entity type: [" + sde.getClass() + "]. The entity id was: [" + sde.getKey() + "]. Could not access method: [" + method.getName() + "]", iae);
			}

			try
			{
				if (!setObjectOnPreparedStatement(pstmt, idx++, o))
				{
					logger.warn("method " + Dataset.getEntityTypeDisplayName(sde.getClass()) + "." + method.getName() + " has unsupported returntype: " + o + ". DB mapping unknown");
					throw new RuntimeException("An error occured during application of parameters to a prepared statement. Entity type: [" + sde.getClass() + "]. The entity id was: [" + sde.getKey() + "]. There was an error setting value for method: [" + method.getName() + "]. The type is not supported");
				}
			}
			catch (SQLException sqle)
			{
				throw new RuntimeException("An error occured during application of parameters to a prepared statement. Entity type:[" + sde.getClass() + "]. The entity id was: [" + sde.getKey() + "]. Could not set field for method name: [" + method.getName() + "].", sqle);
			}
		}

		return idx;
	}

	public int applyParamsToInsertAndUpdateStatement(PreparedStatement pstmt, StamdataEntity sde, Date transactionTime, Date createdTime)
	{
		int idx = applyParamsToInsertStatement(pstmt, sde, transactionTime, createdTime);

		try
		{
			currentRS.last();
		}
		catch (SQLException e)
		{
			throw new RuntimeException("An error occured during application of parameters to a prepared statement. " + "The database contained no records for entity id [" + sde.getKey() + "]");
		}
		for (String notUpdateName : notUpdatedColumns)
		{
			try
			{
				Object o = currentRS.getObject(notUpdateName);
				if (!setObjectOnPreparedStatement(pstmt, idx++, o))
				{
					logger.warn("Column " + notUpdateName + " has unsupported returntype: " + o + ". DB mapping unknown");
					throw new RuntimeException("An error occured during application of parameters to a prepared statement. Entity type: [" + sde.getClass() + "]. The entity id was: [" + sde.getKey() + "]. There was an error setting value for record: [" + notUpdateName + "]. The type is not supported");
				}
			}
			catch (SQLException sqle)
			{
				throw new RuntimeException("An error occured during application of parameters to a prepared statement. Entity type:[" + sde.getClass() + "]. The entity id was: [" + sde.getKey() + "]. Could not set field for record name: [" + notUpdateName + "].", sqle);
			}
		}
		return idx;
	}

	public void applyParamsToUpdateStatement(PreparedStatement pstmt, StamdataEntity sde, Date transactionTime, Date createdTime, Date existingValidFrom, Date existingValidTo)
	{
		int idx = 1;
		try
		{
			pstmt.setTimestamp(idx++, new Timestamp(transactionTime.getTime()));
			pstmt.setTimestamp(idx++, new Timestamp(sde.getValidFrom().getTime()));
			pstmt.setTimestamp(idx++, new Timestamp(sde.getValidTo().getTime()));
		}
		catch (SQLException sqle)
		{
			throw new RuntimeException("An error occured during application of parameters to a prepared statement. ", sqle);
		}

		for (Method method : outputMethods)
		{
			Object o;

			try
			{
				o = method.invoke(sde);
			}
			catch (InvocationTargetException ite)
			{
				throw new RuntimeException("An error occured during application of parameters to a prepared statement. Entity type: [" + sde.getClass() + "]. The entity id was: [" + sde.getKey() + "]. Could not invoke target method: [" + method.getName() + "]", ite);
			}
			catch (IllegalAccessException iae)
			{
				throw new RuntimeException("An error occured during application of parameters to a prepared statement. Entity type: [" + sde.getClass() + "]. The entity id was: [" + sde.getKey() + "]. Could not access method: [" + method.getName() + "]", iae);
			}

			try
			{
				if (!setObjectOnPreparedStatement(pstmt, idx++, o))
				{
					logger.warn("method " + Dataset.getEntityTypeDisplayName(sde.getClass()) + "." + method.getName() + " has unsupported returntype: " + o + ". DB mapping unknown");
					throw new RuntimeException("An error occured during application of parameters to a prepared statement. Entity type: [" + sde.getClass() + "]. The entity id was: [" + sde.getKey() + "]. There was an error setting value for method: [" + method.getName() + "]. The type is not supported");
				}
			}
			catch (SQLException sqle)
			{
				throw new RuntimeException("An error occured during application of parameters to a prepared statement. Entity type:[" + sde.getClass() + "]. The entity id was: [" + sde.getKey() + "]. Could not set field for method name: [" + method.getName() + "].", sqle);
			}
		}

		try
		{
			updateValidToStmt.setObject(3, currentRS.getObject(Dataset.getIdOutputName(type)));
			setObjectOnPreparedStatement(pstmt, idx++, sde.getKey());
			pstmt.setTimestamp(idx++, new Timestamp(existingValidFrom.getTime()));
			pstmt.setTimestamp(idx++, new Timestamp(existingValidTo.getTime()));
		}
		catch (SQLException sqle)
		{
			throw new RuntimeException("An error occured during application of parameters to a prepared statement. ", sqle);
		}
	}

	private boolean setObjectOnPreparedStatement(PreparedStatement pstmt, int idx, Object o) throws SQLException
	{
		if (o instanceof String)
		{
			pstmt.setString(idx++, (String) o);
		}
		else if (o instanceof Integer)
		{
			pstmt.setInt(idx++, (Integer) o);
		}
		else if (o instanceof Long)
		{
			pstmt.setLong(idx++, (Long) o);
		}
		else if (o instanceof Double)
		{
			pstmt.setDouble(idx++, (Double) o);
		}
		else if (o instanceof Boolean)
		{
			int b = (Boolean) o ? 1 : 0;
			pstmt.setInt(idx++, b);
		}
		else if (o instanceof Date)
		{
			pstmt.setTimestamp(idx++, new Timestamp(((Date) o).getTime()));
		}
		else if (o == null)
		{
			pstmt.setNull(idx++, java.sql.Types.NULL);
		}
		else
		{
			return false;
		}

		return true;
	}

	/**
	 * @param id
	 * @param validFrom
	 * @param validTo
	 * @return If at least one version of the entity was found with the
	 *         specified id in the specified validfrom-validto range
	 */
	public boolean fetchEntityVersions(Object id, Date validFrom, Date validTo)
	{
		try
		{
			this.selectByIdStmt.setObject(1, id);
			this.selectByIdStmt.setTimestamp(2, new Timestamp(validFrom.getTime()));
			this.selectByIdStmt.setTimestamp(3, new Timestamp(validTo.getTime()));
			currentRS = selectByIdStmt.executeQuery();
			return currentRS.next();
		}
		catch (SQLException e)
		{
			logger.error("", e);
		}

		return false;
	}

	public boolean fetchEntityVersions(Date validFrom, Date validTo)
	{
		try
		{
			String sql = "select * from " + tablename + " where not (ValidTo < '" + toMySQLdate(validFrom) + "' or ValidFrom > '" + toMySQLdate(validTo) + "')";
			currentRS = connection.createStatement().executeQuery(sql);
			return currentRS.next();
		}
		catch (SQLException e)
		{
			logger.error("fetchEntityVersions", e);
			return false;
		}
	}

	public Date getCurrentRowValidFrom()
	{
		try
		{
			// We do the conversion from timestamp to date because
			// the two types cannot be compared easily otherwise.

			return new Date(currentRS.getTimestamp("ValidFrom").getTime());
		}
		catch (SQLException e)
		{
			logger.error("getCurrentRowValidFrom()", e);
		}

		return null;
	}

	public Date getCurrentRowValidTo()
	{
		try
		{
			return new Date(currentRS.getTimestamp("ValidTo").getTime());
		}
		catch (SQLException e)
		{
			logger.error("getCurrentRowValidTo()", e);
		}

		return null;
	}

	public boolean nextRow()
	{
		try
		{
			return currentRS.next();
		}
		catch (SQLException e)
		{
			logger.error("nextRow()", e);
		}
		return false;
	}

	public void copyCurrentRowButWithChangedValidFrom(Date validFrom, Date transactionTime)
	{
		try
		{
			String sql = "insert into " + tablename + " (ModifiedDate, CreatedDate, ValidFrom, ValidTo";
			for (Method method : outputMethods)
			{
				sql += ", ";
				String name = AbstractStamdataEntity.getOutputFieldName(method);
				sql += name;
			}
			for (String notUpdateName : notUpdatedColumns)
			{
				sql += ", " + notUpdateName;
			}

			sql += ") values (";
			sql += "'" + toMySQLdate(transactionTime) + "',"; // modifieddate
			sql += "'" + toMySQLdate(transactionTime) + "',"; // createddate
			sql += "'" + toMySQLdate(validFrom) + "',"; // validfrom
			sql += "'" + toMySQLdate(currentRS.getTimestamp("ValidTo")) + "'"; // validTo

			for (int i = 0; i < outputMethods.size(); i++)
			{
				sql += ",";
				sql += "?";
			}

			for (int i = 0; i < notUpdatedColumns.size(); i++)
			{
				sql += ",?";
			}

			sql += ")";

			PreparedStatement stmt = connection.prepareStatement(sql);
			int idx = 1;

			for (Method method : outputMethods)
			{
				stmt.setObject(idx++, currentRS.getObject(AbstractStamdataEntity.getOutputFieldName(method)));
			}

			for (String notUpdateName : notUpdatedColumns)
			{
				stmt.setObject(idx++, currentRS.getObject(notUpdateName));
			}

			stmt.executeUpdate();
			insertedRecords++;
		}
		catch (SQLException e)
		{
			logger.error("copyCurrentRowButWithChangedValidFrom", e);
		}
	}

	public void updateValidToOnCurrentRow(Date validTo, Date transactionTime)
	{
		logger.debug("Updating validto on current record");

		try
		{
			updateValidToStmt.setTimestamp(1, new Timestamp(validTo.getTime()));
			updateValidToStmt.setTimestamp(2, new Timestamp(transactionTime.getTime()));
			updateValidToStmt.setObject(3, currentRS.getObject(Dataset.getIdOutputName(type)));
			updateValidToStmt.setTimestamp(4, currentRS.getTimestamp("ValidFrom"));

			int rowsAffected = updateValidToStmt.executeUpdate();

			if (rowsAffected != 1) logger.error("updateValidToStmt - expected to update 1 row. updated: " + rowsAffected);

			updatedRecords += rowsAffected;
		}
		catch (SQLException e)
		{
			logger.error("updateValidToOnCurrentRow", e);
		}
	}

	public void deleteCurrentRow()
	{
		try
		{
			deleteStmt.setObject(1, currentRS.getObject(Dataset.getIdOutputName(type)));
			deleteStmt.setObject(2, currentRS.getObject("ValidFrom"));
			int rowsAffected = deleteStmt.executeUpdate();
			if (rowsAffected != 1) logger.error("deleteCurrentRow - expected to delete 1 row. Deleted: " + rowsAffected);
			deletedRecords += rowsAffected;
		}
		catch (SQLException e)
		{
			logger.error("deleteCurrentRow", e);
		}

	}

	public void updateValidFromOnCurrentRow(Date validFrom, Date transactionTime)
	{
		try
		{
			updateValidFromStmt.setTimestamp(1, new Timestamp(validFrom.getTime()));
			updateValidFromStmt.setTimestamp(2, new Timestamp(transactionTime.getTime()));
			updateValidFromStmt.setObject(3, currentRS.getObject(Dataset.getIdOutputName(type)));
			updateValidFromStmt.setTimestamp(4, currentRS.getTimestamp("ValidFrom"));
			int rowsAffected = updateValidFromStmt.executeUpdate();
			if (rowsAffected != 1) logger.error("updateValidFromStmt - expected to update 1 row. updated: " + rowsAffected);
			updatedRecords += rowsAffected;
		}
		catch (SQLException e)
		{
			logger.error("updateValidFromOnCurrentRow", e);
		}
	}

	public boolean dataInCurrentRowEquals(StamdataEntity sde)
	{
		for (Method method : outputMethods)
		{
			try
			{
				if (!fieldEqualsCurrentRow(method, sde)) return false;
			}
			catch (Exception e)
			{
				logger.error("dataInCurrentRowEquals failed on method " + method.getName(), e);
				return false;
			}
		}

		return true;
	}

	private boolean fieldEqualsCurrentRow(Method method, StamdataEntity sde) throws Exception
	{
		long t0 = System.nanoTime();

		String fieldname = AbstractStamdataEntity.getOutputFieldName(method);
		tgetOutputFieldName += System.nanoTime() - t0;

		Object o = method.invoke(sde);

		if (o instanceof String)
		{
			String value = currentRS.getString(fieldname);
			// Null strings and empty strings are the same
			if (value == null && ((String) o).trim().isEmpty()) return true;
			if (!o.equals(value)) return false;
		}
		else if (o instanceof Integer)
		{
			Integer value = currentRS.getInt(fieldname);
			if (!o.equals(value)) return false;
		}
		else if (o instanceof Long)
		{
			Long value = currentRS.getLong(fieldname);
			if (!o.equals(value)) return false;
		}
		else if (o instanceof Double)
		{
			Double value = currentRS.getDouble(fieldname);
			if (!o.equals(value)) return false;
		}
		else if (o instanceof Boolean)
		{
			Boolean value = (currentRS.getInt(fieldname) == 0 ? Boolean.FALSE : Boolean.TRUE);
			if (!o.equals(value)) return false;
		}
		else if (o instanceof Date)
		{
			Timestamp ts = currentRS.getTimestamp(fieldname);
			if (ts == null) return false;
			long millis = ts.getTime();
			if (millis != ((Date) o).getTime()) return false;
		}
		else if (o == null)
		{
			Object value = currentRS.getObject(fieldname);
			if (value != null) return false;
		}
		else
		{
			String message = "method " + Dataset.getEntityTypeDisplayName(type) + "." + method.getName() + " has unsupported returntype: " + o + ". DB mapping unknown";
			throw new Exception(message);
		}

		return true;
	}

	public int getUpdatedRecords()
	{
		return updatedRecords;
	}

	public int getDeletedRecords()
	{
		return deletedRecords;
	}

	public List<StamdataEntityVersion> getEntityVersions(Date validFrom, Date validTo) throws SQLException
	{
		String sql = "select " + AbstractStamdataEntity.getOutputFieldName(idMethod) + ", validFrom from " + tablename + " where not (ValidTo < '" + toMySQLdate(validFrom) + "' or ValidFrom > '" + toMySQLdate(validTo) + "')";
		currentRS = connection.createStatement().executeQuery(sql);

		List<StamdataEntityVersion> evs = new ArrayList<StamdataEntityVersion>();

		while (currentRS.next())
		{
			StamdataEntityVersion ev = new StamdataEntityVersion();
			ev.id = currentRS.getObject(1);
			ev.validFrom = currentRS.getTimestamp(2);
			evs.add(ev);
		}

		if (logger.isDebugEnabled()) logger.debug("Returning " + evs.size() + " entity versions");

		return evs;
	}


	public static class StamdataEntityVersion
	{
		public Object id;
		public Date validFrom;
	}

	private static long tIdOfCurrentRowEquals = 0;
	private static long tgetOutputFieldName = 0;

	public boolean getIdOfCurrentRow(StamdataEntity sde)
	{
		long t0 = System.nanoTime();
		try
		{
			return fieldEqualsCurrentRow(idMethod, sde);
		}
		catch (Exception e)
		{
			logger.error("IdOfCurrentRowEquals");
			return false;
		}
		finally
		{
			tIdOfCurrentRowEquals += System.nanoTime() - t0;
		}
	}

	public void truncate()
	{
		try
		{
			connection.createStatement().execute("truncate " + tablename + ";");
		}
		catch (Exception e)
		{
			logger.error("truncate");
		}
	}

	public void drop()
	{
		try
		{
			connection.createStatement().execute("drop table " + tablename + ";");
		}
		catch (Exception e)
		{
			logger.error("truncate");
		}
	}

	public void updateValidToOnEntityVersion(Date validTo, StamdataEntityVersion evs, Date transactionTime)
	{
		try
		{
			updateValidToStmt.setTimestamp(1, new Timestamp(validTo.getTime()));
			updateValidToStmt.setTimestamp(2, new Timestamp(transactionTime.getTime()));
			updateValidToStmt.setObject(3, evs.id);
			updateValidToStmt.setTimestamp(4, new Timestamp(evs.validFrom.getTime()));
			int rowsAffected = updateValidToStmt.executeUpdate();
			if (rowsAffected != 1) logger.error("updateValidToStmt - expected to update 1 row. updated: " + rowsAffected);
			updatedRecords += rowsAffected;
			if (updatedRecords % 100 == 0) logger.debug("Updated validto on " + updatedRecords + " records");
		}
		catch (SQLException e)
		{
			logger.error("updateValidToOnCurrentRow", e);
		}
	}

	/*
	 * Get a list with all columns that will not be updated by the Entity
	 * Entities doesn't have to be complete. The can update only parts of a
	 * table and then the rest have to be copied as not changed.
	 */
	public List<String> locateNotUpdatedColumns()
	{
		ArrayList<String> res = new ArrayList<String>();
		Statement stm = null;

		try
		{
			stm = connection.createStatement();
			stm.execute("desc " + tablename);
			ResultSet rs = stm.getResultSet();

			while (rs.next())
			{
				String colName = rs.getString(1);

				// Ignore all system columns

				if (colName.toUpperCase().indexOf("PID") > 0) continue;
				if (colName.equalsIgnoreCase("ModifiedDate")) continue;
				if (colName.equalsIgnoreCase("CreatedDate")) continue;
				if (colName.equalsIgnoreCase("ValidFrom")) continue;
				if (colName.equalsIgnoreCase("ValidTo")) continue;

				boolean found = false;
				for (Method method : outputMethods)
				{
					// Ignore the columns that are updated by the entity

					String name = AbstractStamdataEntity.getOutputFieldName(method);
					if (colName.equalsIgnoreCase(name))
					{
						found = true;
						continue;
					}
				}

				if (!found) res.add(colName);
			}
		}
		catch (SQLException e)
		{
			logger.error("Error locateNotUpdatedColumns. Error is: " + e.getMessage());
		}
		finally
		{
			try
			{
				if (stm != null) stm.close();
			}
			catch (SQLException e)
			{

			}
		}

		return res;
	}
}