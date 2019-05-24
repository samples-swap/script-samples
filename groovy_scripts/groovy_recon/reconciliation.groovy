#!/usr/bin/env groovy

@Grapes([
    @Grab('com.h2database:h2:1.3.176'),
    @GrabConfig(systemClassLoader=true)
])
 
import java.sql.*
import groovy.sql.Sql
import org.h2.jdbcx.JdbcConnectionPool

def sql = Sql.newInstance("jdbc:h2:reconciliation;AUTO_SERVER=TRUE", "sa", "sa", "org.h2.Driver")

sql.eachRow("SELECT p.id FROM producer_record p WHERE NOT EXISTS(SELECT * FROM consumer_record WHERE id = p.id)") {
    println "Consumer did not receive message: ${it.id}."
}

sql.eachRow("SELECT c.id FROM consumer_record c WHERE NOT EXISTS(SELECT * FROM producer_record WHERE id = c.id)") {
    println "Consumer read message ${it.id} before Producer acknowlegement."
}

sql.eachRow("SELECT COUNT(1) AS total FROM producer_record") {
    println "Total unique produced messages: ${it.getInt('total')}."
}
sql.eachRow("SELECT COUNT(1) AS total FROM consumer_record") {
    println "Total unique consumed messages: ${it.getInt('total')}."
}
