/*
 * Very simple HTTP server using http://sparkjava.com/
 *
 * Start the server with "groovy server.groovy".
 */

@Grab( group = 'com.sparkjava', module = 'spark-core', version = '2.1' )
import static spark.Spark.*

staticFileLocation '.'

get '/hello', { req, res -> log req; '{"hi": true, "ho": 3}' }
post '/data', { req, res -> log req; "Thank you for sending me data: ${req.body()}" }

def log( req ) {
    println "Handling ${req.requestMethod()} ${req.pathInfo()}"
    println "Headers: ${req.headers().collect { it + ': ' + req.headers( it ) }}"
    def b
    if ( ( b = req.body() ) ) {
        println "Body: $b"
    } else {
        println "No body"
    }
}

println 'Enter to exit'
System.in.newReader().readLine()
println "Done"
stop()

