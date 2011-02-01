package com.trifork.sdm.replication.admin.models;

import static com.trifork.sdm.replication.db.properties.Database.*;
import static java.lang.String.*;
import static org.slf4j.LoggerFactory.*;

import java.sql.*;
import java.util.*;

import org.slf4j.Logger;

import com.google.inject.*;
import com.trifork.sdm.replication.db.properties.Transactional;


public class AuditLog
{
	private static Logger LOG = getLogger(AuditLog.class);

	@Inject
	@Transactional(ADMINISTRATION)
	private Provider<Connection> connectionProvider;


	@Transactional(ADMINISTRATION)
	public List<LogEntry> all() throws SQLException
	{
		Statement statement = null;
		
		try {
			final String SQL = "SELECT * FROM auditlog ORDER BY created_at DESC";
	
			List<LogEntry> logEntries = new ArrayList<LogEntry>();
	
			Connection connection = connectionProvider.get();
			statement = connection.createStatement();
	
			ResultSet row = statement.executeQuery(SQL);
	
			while (row.next())
			{
				LogEntry entry = serialize(row);
				logEntries.add(entry);
			}
	
			return logEntries;
		} finally {
			if (statement != null && !statement.isClosed()) {
				statement.close();
			}
		}
	}


	public boolean create(String message, Object... args) throws SQLException
	{
		return create(format(message, args));
	}


	@Transactional(ADMINISTRATION)
	public boolean create(String message) throws SQLException
	{
		PreparedStatement statement = null;
		try {
			final String CREATE_SQL = "INSERT INTO auditlog SET message = ?, created_at = NOW()";
	
			boolean success = false;
	
			if (message == null || message.isEmpty())
			{
				LOG.warn("Trying to log an empty audit message.");
			}
			else
			{
				Connection connection = connectionProvider.get();
	
				statement = connection.prepareStatement(CREATE_SQL);
				statement.setString(1, message);
	
				int created = statement.executeUpdate();
	
				success = created != -1;
			}
	
			return success;
		} finally {
			if (statement != null && !statement.isClosed()) {
				statement.close();
			}
		} 
	}

	protected LogEntry serialize(ResultSet resultSet) throws SQLException
	{
		String id = resultSet.getString("id");
		String message = resultSet.getString("message");
		Timestamp created_at = resultSet.getTimestamp("created_at");

		LogEntry entry = new LogEntry(id, message, created_at);

		return entry;
	}
}