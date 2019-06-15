String output = new ProcessExecutor()
  .dir("D:/myfolder")
  .command("git", "clone", "--depth", "1", url)
  .redirectErrorStream(true)
  .readOutput(true)
  .execute()
  .outputUTF8(); 