private def evaluateCaptcha(def remoteIp, def challenge, def response) {
    def config = recaptchaService.getRecaptchaConfig()

    def urlString = "http://api-verify.recaptcha.net/verify"
    def queryString = "privatekey=${config.recaptcha.privateKey}&remoteip=${remoteIp}&challenge=${challenge}&response=${URLEncoder.encode(response)}"

    def url = new URL(urlString)
    def connection = url.openConnection()
    connection.setRequestMethod("POST")
    connection.doOutput = true

    def writer = new OutputStreamWriter(connection.outputStream)
    writer.write(queryString)
    writer.flush()
    writer.close()
    connection.connect()

    def recaptchaResponse = connection.content.text
    log.debug(recaptchaResponse)

    recaptchaResponse.startsWith("true")
}