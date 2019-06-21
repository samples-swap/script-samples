import grails.plugin.spock.IntegrationSpec
import java.sql.Connection
import javax.sql.DataSource
import spock.lang.*

class DatabaseSchemaSpec extends IntegrationSpec {

    @Shared def dataSource
    @Shared List<String> tableNames

    def setupSpec() {
        DataSource.mixin(DataSourceCategory)

        tableNames = []
        dataSource.withConnection {connection ->
            def rs = connection.metaData.getTables(null, null, '%', ['TABLE'] as String[])
            while (rs.next()) {
                tableNames << rs.getString(3)
            }
        }
    }

    @Unroll("the #table table has a primary key")
    void "all tables have a primary key"() {
        expect:
        dataSource.withConnection { Connection connection ->
            assert connection.metaData.getPrimaryKeys(null, null, table).next()
        }

        where:
        table << tableNames
    }

}

@Category(DataSource)
class DataSourceCategory {
    static void withConnection(dataSource, Closure closure) {
        Connection connection = dataSource.connection
        try {
            closure(connection)
        } finally {
            connection?.close()
        }
    }
}