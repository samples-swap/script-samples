import groovy.sql.Sql

/**
 * Created by tim on 8/28/16.
 */

def host = "SERVER_ADDRESS"
def database = "MYSQL_SCHEMA"
def username = "SQL_LOGIN"
def password = "SQL_PASSWORD"

def sql = Sql.newInstance(conn, username, password)

def output = "login,name\n"

sql.eachRow("select login,name from customers limit 100") { row ->
    output += "$row.login,$row.name\n"
}

def file = new File("test.csv")
file.text = output