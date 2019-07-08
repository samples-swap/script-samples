def exec(cmd) {
  println cmd
  def process = new ProcessBuilder([ "sh", "-c", cmd])
                                    .directory(new File("/tmp"))
                                    .redirectErrorStream(true) 
                                    .start()
  process.outputStream.close()
  process.inputStream.eachLine {println it}
  process.waitFor();
  return process.exitValue()
}
 
[
  "mkdir /tmp/,x",
  "echo FROM busybox > /tmp/,x/Dockerfile",
  "DOCKER_HOST=tcp://localhost:2375 docker build -t test/test:1.0 /tmp/,x",
  "DOCKER_HOST=tcp://localhost:2375 docker push --force=true test/test:1.0",
  "rm /tmp/,x/Dockerfile",
  "rmdir /tmp/,x",
  "DOCKER_HOST=tcp://localhost:2375 docker rmi test/test:1.0"
].each {
  exec(it)
}
