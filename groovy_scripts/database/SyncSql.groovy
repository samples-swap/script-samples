/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.sql.DataSource;

import groovy.sql.InParameter;
import groovy.sql.Sql;

class SyncSql extends Sql {
	public SyncSql(Connection connection) {
		super(connection);
	}
	public SyncSql(DataSource dataSource) {
		super(dataSource);
	}
	public SyncSql(Sql parent) {
		super(parent);
	}

	public int updateToTable(String table, Map<String, Object> map, Map<String,Object> idMap) throws SQLException {
		StringBuilder buffer = new StringBuilder("UPDATE ");
		buffer.append(table);
		buffer.append(" SET ");
		
		List<Object> valuelist = new ArrayList<Object>()

		def buildBuffer = { joinchars, ignoreKeys, key, val, index ->
			if(!ignoreKeys.contains(key)) {
				if(index > 0) {
					buffer.append(joinchars)
				}
				buffer.append(key).append('=')
				if(val != null) {
					buffer.append('?')
					valuelist.add(val)
				} else {
					buffer.append('NULL')
				}
			}
		}

		map.eachWithIndex buildBuffer.curry(',', idMap.keySet()) 
		
		buffer.append ' WHERE '
		
		idMap.eachWithIndex buildBuffer.curry(' AND ', [])

		def sqlstring=buffer.toString()

		//println "Updating: ${sqlstring} \n values: ${valuelist}\n"
		return executeUpdate(sqlstring, valuelist);
	}
	
	public List<List<Object>> insertToTable(String table, Map<String, Object> map) throws SQLException {
		StringBuilder buffer = new StringBuilder("INSERT INTO ");
		buffer.append(table);
		buffer.append(" (");
		buffer.append(map.keySet().join(','))
		buffer.append(") VALUES (")

		List<Object> valuelist = new ArrayList<Object>()
		map.values().eachWithIndex { it, index ->
			if(index > 0) {
				buffer.append(',')
			}
			if(it != null) {
				valuelist.add it
				buffer.append('?')
			} else {
				buffer.append('NULL')
			}
		}
		buffer.append(")")
		return executeInsert(buffer.toString(), valuelist)
	}
}