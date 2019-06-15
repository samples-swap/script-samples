/*
For some use-cases you might access a git repository from a Java application. JGit offers a helpful integration with builder pattern APIs. The Git client can authenticate itself using SSH keys.

To open a Git repository call the cloneRepository() command.
*/
File workingDir = Files.createTempDirectory("workspace").toFile();
TransportConfigCallback transportConfigCallback = new SshTransportConfigCallback();
git = Git.cloneRepository()
        .setDirectory(workingDir)
        .setTransportConfigCallback(transportConfigCallback)
        .setURI("ssh://example.com/repo.git")
        .call();
Our own implementation of the transport config callback configures the SSH communication for accessing the repository. We want to tell JGit to use SSH communication and to ignore the host key checking.

private static class SshTransportConfigCallback implements TransportConfigCallback {

    private final SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
        @Override
        protected void configure(OpenSshConfig.Host hc, Session session) {
            session.setConfig("StrictHostKeyChecking", "no");
        }
    };

    @Override
    public void configure(Transport transport) {
        SshTransport sshTransport = (SshTransport) transport;
        sshTransport.setSshSessionFactory(sshSessionFactory);
    }

}

/*
Per default this will take the id_rsa key file, available under the ~/.ssh. If you want to specify another file location or configure a key secured by a passphrase, change the creation of the Java Secure Channel as well.
*/

private static class SshTransportConfigCallback implements TransportConfigCallback {

    private final SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
        @Override
        protected void configure(OpenSshConfig.Host hc, Session session) {
            session.setConfig("StrictHostKeyChecking", "no");
        }

        @Override
        protected JSch createDefaultJSch(FS fs) throws JSchException {
            JSch jSch = super.createDefaultJSch(fs);
            jSch.addIdentity("/path/to/key", "super-secret-passphrase".getBytes());
            return jSch;
        }
    };

    @Override
    public void configure(Transport transport) {
        SshTransport sshTransport = (SshTransport) transport;
        sshTransport.setSshSessionFactory(sshSessionFactory);
    }

}

/*
Now you can issue commands on your Git handle.
*/