import groovy.json.JsonOutput
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.URL
import java.util.Scanner

def url = new URL("https://${github}/api/v3/repos/${org}/${repo}/git/refs")
def urlConnection = url.openConnection()

urlConnection.setDoOutput(true)
urlConnection.setRequestMethod("POST")
urlConnection.setRequestProperty("Authorization", "Basic ${authString}")
urlConnection.setRequestProperty("Content-Type", "application/json")

def httpRequestBodyWriter = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream()))
httpRequestBodyWriter.write(JsonOutput.toJson([ref: "refs/tags/${originalVersion}", sha: completeSha]))
httpRequestBodyWriter.close()

def httpResponseScanner = new Scanner(urlConnection.getInputStream())
while(httpResponseScanner.hasNextLine()) {
    println(httpResponseScanner.nextLine())
}
httpResponseScanner.close()