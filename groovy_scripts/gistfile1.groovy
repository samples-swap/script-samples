#!/usr/bin/env groovy

import groovy.sql.Sql

def sql = Sql.newInstance("jdbc:mysql://localhost/statistics", "user",
           "password", "com.mysql.jdbc.Driver")

File f = new File(".");
f.eachDir { dir ->
    dir.eachFile { file ->
        sql.executeInsert("INSERT INTO db (week, db, size) VALUES (${dir.getName()}, ${file.getName()}, ${file.size()})")
        println dir.getName() + " " + file.getName()  + " "  + file.size()
    }
}
