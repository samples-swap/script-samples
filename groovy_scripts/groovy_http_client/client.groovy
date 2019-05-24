/*
 * This is a runnable groovy script.
 * Run with "groovy client.groovy".
 *
 * Don't forget to start the server.groovy script first (shown in this gist).
 */

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.ToString
import groovy.transform.TypeChecked

@Grab( group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7.2' )
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.Method
import org.apache.http.util.EntityUtils

/**
 * Simple trait that adds HTTP client capabilities to any class.
 */
trait CanTalkHttp {
    SimpleHttpResponse withClient( HTTPBuilder httpClient,
                                   HttpOperation operation ) {
        def handler = { HttpResponseDecorator resp ->
            Map<String, Collection<String>> headersMap = [ : ]
            resp.headers.each { headersMap.get( it.name, [ ] ) << it.value }
            new SimpleHttpResponse( resp.status, EntityUtils.toString( resp.entity ), headersMap )
        }

        httpClient.handler.success = handler
        httpClient.handler.failure = handler

        println "Running ${operation.method} to ${operation.path}"

        httpClient.request( operation.method, operation.accept ) {
            HTTPBuilder.RequestConfigDelegate d = getDelegate()
            d.uri.path = operation.path
            d.setHeaders( operation.headers )
            if ( operation instanceof Post ) {
                d.send operation.contentType, operation.body
            }
        } as SimpleHttpResponse
    }
}

/**
 * Representation of a HTTP response that is easy to check type-safely.
 */
@Immutable
@CompileStatic
@ToString( includeFields = true, includeNames = true, includePackage = false )
class SimpleHttpResponse {
    int statusCode
    String body
    Map<String, ? extends Collection<String>> headers
}

/**
 * Parent class of all HTTP method types.
 */
abstract class HttpOperation {
    abstract Method method
    String path = '/'
    String accept = 'application/json'
    Map<String, ?> headers = [ : ]
}

/*
 * GET HTTP Request configuration
 */
class Get extends HttpOperation {
    final Method method = Method.GET
}

/*
 * POST HTTP Request configuration
 */
class Post extends HttpOperation {
    final Method method = Method.POST
    String contentType = 'application/json'
    String body = ''
}

/*
 * Simple example class showing how to make use of the CanTalkHttp trait.
 */
@TypeChecked
class Example implements CanTalkHttp, Runnable {
    final HTTPBuilder client = new HTTPBuilder( 'http://localhost:4567' )

    void show( SimpleHttpResponse resp ) {
        println "Server responded with ${resp.statusCode}"
        println "Headers: ${resp.headers}"
        println "Body: ${resp.body}"
    }

    void run() {
        // run a GET Request with our client
        def resp = withClient( client, new Get( path: '/hello' ) )

        // Show the response
        show resp

        // run a POST Request
        resp = withClient( client, new Post(
                path: 'data',
                body: '{"hello": true, "name": "Renato"}' ) )

        show resp
    }
}

new Example().run()
