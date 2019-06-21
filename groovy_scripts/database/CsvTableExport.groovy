import groovy.sql.GroovyRowResult
import org.apache.commons.dbcp.BasicDataSource
import org.apache.commons.lang.StringEscapeUtils

import javax.sql.DataSource
import java.sql.ResultSet
import java.sql.Statement

/**
 * Helper class to get data from database into a simple CSV without any extra formatting.
 */
class CsvTableExport {

    private String url
    private String username
    private String password

    CsvTableExport(String url, String username, String password) {
        this.url = url
        this.username = username
        this.password = password
    }

    public void getData(String query, String outputCsvFile) {
        DataSource source = new BasicDataSource()
        source.url = url
        source.username = username
        source.password = password

        File file = new File(outputCsvFile)
        if (file.exists()) {
            file.delete()
        }

        Statement statement = source.connection.createStatement()
        try {
            ResultSet rs = statement.executeQuery(query)
            while (rs.next()) {
                GroovyRowResult result = rs.toRowResult()
                String line = result.entrySet().collect {
                    StringEscapeUtils.escapeCsv(it.value.toString())
                }.join(',') + "\n"

                file.append(line)
            }
        } finally {
            statement.close()
        }
    }
}
