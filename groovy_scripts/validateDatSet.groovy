import groovy.sql.*
import static cucumber.api.groovy.EN.*

def db
def sql
def createTbl
def dropTable
def countRows
def setOfSimpleOrder
def order

try {
    db = [
            url:'jdbc:jtds:sqlserver://ICEHOFMAN-PC:1433/groovy',
            user:'groovy',
            password:'groovy',
            driver:'net.sourceforge.jtds.jdbc.Driver'
    ]

    sql = Sql.newInstance(db.url, db.user, db.password, db.driver)
}
catch (all){
    db = [
            url:'jdbc:h2:groovy',
            user:'sa',
            password:'sa',
            driver:'org.h2.Driver'
        ]

    sql = Sql.newInstance(db.url, db.user, db.password, db.driver)
}
finally {
    setOfSimpleOrder = new DataSet(sql, 'SimpleOrders')
}

if(db.url.contains('sqlserver')) {
    createTbl =
        '''
        CREATE TABLE SimpleOrders (
          id uniqueidentifier PRIMARY KEY NOT NULL
          DEFAULT newid(),
          OrderNumber NVARCHAR(50),
          OrderDesc NVARCHAR(100)
        )
        '''

    dropTable =
        '''
        IF  EXISTS (
        SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'SimpleOrders') AND type in (N'U')
        )
        DROP TABLE SimpleOrders
        '''

    countRows =
        '''
        SELECT * FROM sys.objects
        WHERE object_id = OBJECT_ID(N'SimpleOrders') AND type in (N'U')
        '''

    order = [
            orderNumber: '000abcd',
            orderDesc: 'Test001'
            ]
} else {
    createTbl =
        '''
         CREATE TABLE SimpleOrders (
           id UUID PRIMARY KEY,
           OrderNumber NVARCHAR(50),
           OrderDesc NVARCHAR(100)
         )
         '''

    dropTable = 'DROP TABLE IF EXISTS SimpleOrders'

    countRows = "select count(*) from information_schema.columns where table_name = 'SimpleOrders'"

    order = [
                id: java.util.UUID.randomUUID(),
                orderNumber: '000abcd',
                orderDesc: 'Test001'
            ]
}

Given(~/^An existing database 'Groovy'$/) { ->
    assert true == db.url.contains('groovy')
}
Given(~/^its respective table 'SimpleOrders'$/) { ->
    sql.execute(dropTable)
    sql.execute(createTbl)
    assert 1 == sql.rows(countRows).size()
}
When(~/^creating a record with dataset$/) { ->
    setOfSimpleOrder.add(order)
}
Then(~/^validate the new record with dataset$/) { ->
    def rows = setOfSimpleOrder.rows()
    assert 1 == rows.size()
}