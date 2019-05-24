import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.UserInfo
import com.jcraft.jsch.Channel
import com.jcraft.jsch.ChannelExec

def sshHost = '10.1.2.132'
def sshUser = 'root'
def sshPass = '******'
def sshPort = 22

def localHost = '127.0.0.1'

def targetHost = '172.18.128.33'
def targetUser = 'root'
def targetPass = '******'
def targetPort = 22

println "Opening connection to ${sshUser}@${sshHost}:${sshPort}"
Properties config = new Properties()
config.put("StrictHostKeyChecking", "no")
JSch jsch = new JSch()
//jsch.addIdentity( privateKey ) // privateKey : String, Openssh private key file
// ref: http://xliangwu.iteye.com/blog/1499764

Session sshSession = jsch.getSession(sshUser, sshHost, sshPort)
sshSession.setPassword(sshPass)
sshSession.setConfig(config)
sshSession.connect()
println "Connected"


println "Forwarding connection to ${targetHost}:${targetPort}"
def assignedPort = sshSession.setPortForwardingL(0, targetHost, targetPort)
println "Got port $assignedPort"

/*
def result = ''
def ant = new AntBuilder()
// Ok. Do it...
ant.sshexec(
  	host: localHost,
		port: assignedPort,
		trust: true,
		username: targetUser,
		password: targetPass,
		command: 'top -n 1 -b > top.log   ',
		outputproperty: 'result',
		verbose: false
		)
// And show the result
println ant.project.properties.'result'
*/

Session targetSession = jsch.getSession(targetUser, localHost, assignedPort)
targetSession.setPassword(targetPass)
targetSession.setConfig(config)
targetSession.connect()
println "Connected"

ByteArrayOutputStream out = new ByteArrayOutputStream();

// Could use "shell"
Channel channel = targetSession.openChannel("exec")
// Let's just get the hostname
((ChannelExec)channel).setCommand("top -bn 1")
// Spew errors to console
((ChannelExec)channel).setErrStream(System.err)
// We're not sending anything
channel.setInputStream(null)
// Get the input stream
InputStream is = channel.getInputStream()

channel.setOutputStream(out);
channel.setExtOutputStream(out);

// Connect
channel.connect()
// This could be written better and groovier...
byte[] tmp = new byte[1024]
// Uh oh. We really need a better way out of the loop... :-(
while (true) {

	if (channel.isClosed()) {
		// All done.
		System.out.println("exit-status: " + channel.getExitStatus())
		break
	}
	// Ugly: You might want to change this
	try{Thread.sleep(1000);}catch(Exception ee){}
}

println "=========================================="
println out.toString()
println "=========================================="
println "done.."

// Close channel and session
channel.disconnect()


targetSession.disconnect()
sshSession.disconnect()