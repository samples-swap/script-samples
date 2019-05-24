import com.sun.net.httpserver.*

Object.metaClass.webapp = {
	{ path ->
		try {
			def attrs = path.split('/')[1..-1]
			[200, owner.delegate.invokeMethod(attrs.head(), attrs.tail() as Object[]) as String]
		} catch (Exception e) {
			[500, e as String]
		}
	}
}

def runWebApp(app, port = 9292) {
	HttpServer server = HttpServer.create(new InetSocketAddress(port),0)
	server.createContext('/', { HttpExchange exchange ->
		def (code, body) = app.call(exchange.requestURI.path)
		exchange.sendResponseHeaders(code,0);
		exchange.responseBody.write(body.bytes)
		exchange.responseBody.close();
	}  as HttpHandler)
	server.start();
}

runWebApp([].webapp())
//                        ^^^^^^^^^^^
//                             |          (x)
//                        ROFLSCALE DB ---/
//

// http://localhost:9292/push/1 -> true
// http://localhost:9292/push/2 -> true
// http://localhost:9292/push/3 -> true

// http://localhost:9292/toString -> [1, 2, 3]

// http://localhost:9292/pop -> 3
// http://localhost:9292/shift ... no shift in groovy :(
