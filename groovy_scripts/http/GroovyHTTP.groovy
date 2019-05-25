class GroovyHTTP {
  public method='POST'
  public uri
  public host
  public path
  public port
  public params=null
  public socket=null
  public writer=null
  public reader=null
  public writedata
  public headers = []
  public content

  // set the url and create new URI object
  def GroovyHTTP(url) {
    uri = new URI(url)
    host = uri.getHost()
    path = uri.getRawPath() 
    port = uri.getPort() 
    def tpar = uri.getQuery()
    if(tpar != null && tpar != '') {
      tpar.tokenize('&').each{
        def pp = it.tokenize('=');
        this.setParam(pp[0],pp[1]);
      }
    } 
    if(port == null || port < 0) port = 80
    if(path == null || path.length() == 0) path = "/"     
  }

  // sets the method (GET or POST)
  def setMethod(setmethod) {
    method = setmethod
  }

  // push params into this request
  def setParam(var,value) {
    if(params != null) 
      params += '&'
    else
      params=''
    params += var +'='+URLEncoder.encode(value) 
  }

  // clear params
  def clearParams() {
    params = null
  }

  // open a new socket
  def open() { 
    socket = new Socket(host, port)
  }

  // write data to the socket
  def write() { 
    def contentLen = 0
    if(params!=null) contentLen = params.length() 
    def writedata = '';

    if(this.method == 'GET')   
      writedata += "GET " + path +'?'+ params + " HTTP/1.0\r\n"
    else
      writedata += "POST " + path + " HTTP/1.0\r\n"

    writedata +=
      "Host: " + host + "\r\n" + 
      "Content-Type: application/x-www-form-urlencoded\r\n" + 
      "Content-Length: " + contentLen + "\r\n\r\n" + 
      params + "\r\n"
      "Connection: close\r\n\r\n"  

    writer = new PrintWriter(socket.getOutputStream(), true)
    writer.write(writedata)
    writer.flush() 
  }

  // read response from the server
  def read() { 
    reader = new DataInputStream(socket.getInputStream())
    def c = null
    while (null != ((c = reader.readLine()))) {   
      if(c=='') break 
      headers.add(c)   
    } 
  } 

  // get header value by name
  def getHeader(name) {
    def pattern = name + ': ' 
    def result
    headers.each{ 
      if(it ==~ /${pattern}.*/) { 
        result = it.replace(pattern,'').trim()
        return 2
      }   
    }
    return result
  }

  // get the response content
  def getContent() { 
    def row 
    content = ''
    while (null != ((row = reader.readLine()))) content += row + "\r\n"  
    return content = content.trim();
  }

  // close the socket
  def close() {
    reader.close() 
    writer.close()
    socket.close()
  }
}

/*
https://support.tropo.com/hc/en-us/articles/206241213-REST-POST-and-GET-in-each-Scripting-language
h = new GroovyHTTP('http://requestb.in/qwkm8sqw')     
h.setMethod('POST') 
h.setParam('q', 'groovy')
h.setParam('test1', 'This is a test')
h.setParam('test2', 'This is actually working')
h.setParam('test3', 'Kevin Bond!')
h.open()
h.write()
h.read() 
println h.getHeader('Server')  
println h.getContent() 
h.close()

*/