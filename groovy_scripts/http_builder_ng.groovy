 
 
 /*
 https://rdmueller.github.io/httpbuilder-ng-redirect/
 */
 @Grab('io.github.http-builder-ng:http-builder-ng-core:1.0.3')

    import static groovyx.net.http.HttpBuilder.configure

    def (statusCode, headers, body)
    
    (statusCode, headers, body) = configure {
        request.uri = "http://example.com"
        request.headers = headers
    }.get(List) { 
        response.success { FromServer resp, Object body -> 
            [
                resp.statusCode,
                resp.headers,
                body.toString()
            ]
        }
    }   
    