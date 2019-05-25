#!/usr/bin/env groovy

@Grab('mysql:mysql-connector-java:5.1.18')
@GrabConfig(systemClassLoader=true)
import groovy.sql.Sql

def sql=Sql.newInstance("jdbc:mysql://mysql-server:3306/database?characterEncoding=UTF-8",
        "mysql-user", "mysql-pwd", "com.mysql.jdbc.Driver")

def today = new Date()

sql.withBatch(1024,'INSERT ignore INTO hotsearch VALUES(?,?,?)'){ps->
    new File('words.sort').splitEachLine("\\s+",'utf8'){
        int count = it[1] as int
        if(it[2]){
            def word = URLDecoder.decode(it[2],'utf8')
            ps.addBatch([word, count, today])
        }
    }
}