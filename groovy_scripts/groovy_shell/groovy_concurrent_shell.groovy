/**
 * Runs commands using /bin/sh and returns stdout as string
 *
 * <p>
 * If the exit code of the command is non-zero, the stderr of the command is printed to stderr
 * and a RuntimeException will be thrown.
 * </p>
 * <b>Example</b>
 * <pre><code>
 * def files = sh('ls $HOME').split()
 * </code></pre>
 * foobar
 */
def sh(cmd) {
    def proc = ["/bin/sh", "-c", cmd].execute()
    def pool = Executors.newFixedThreadPool(2)
    def stdoutFuture = pool.submit({ -> proc.inputStream.text} as Callable<String>)
    def stderrFuture = pool.submit({ -> proc.errorStream.text} as Callable<String>)
    proc.waitFor()
    def exitValue = proc.exitValue()
    if(exitValue != 0) {
        System.err.println(stderrFuture.get())
        throw new RuntimeException("$cmd returned $exitValue")
    }
    return stdoutFuture.get()
}