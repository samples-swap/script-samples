@Grab(group='com.jcraft', module='jsch', version='0.1.46')

import com.jcraft.jsch.*

java.util.Properties config = new java.util.Properties()
config.StrictHostKeyChecking = "no"

JSch ssh = new JSch()
Session session = ssh.getSession(user, host, port)
session.config = config
session.password = password
session.connect()
def sftp = session.openChannel("sftp")
sftp.connect()
println "${sftp.ls(".")}"
sftp.disconnect()
session.disconnect()