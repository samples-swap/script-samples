def callGet(String url) {
    new groovy.json.JsonSlurper().parseText(url.toURL().getText())
}

def callPost(String urlString, String queryString) {
    def url = new URL(urlString)
    def connection = url.openConnection()
    connection.setRequestMethod("POST")
    connection.doOutput = true

    def writer = new OutputStreamWriter(connection.outputStream)
    writer.write(getQueryParams())
    writer.flush()
    writer.close()
    connection.connect()

    new groovy.json.JsonSlurper().parseText(connection.content.text)
}

println callGet("https://some/url/for/get")
println callPost("https://some/url/post", "a=1&b=2&c=3")
