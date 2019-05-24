import javax.management.*
import javax.management.remote.*

def creds = [ 'username', 'password' ] as String[]
def env = [ (JMXConnector.CREDENTIALS) : creds ]
def serverUrl = 'service:jmx:rmi:///jndi/rmi://hostname:port/jmxrmi'
def server = JMXConnectorFactory.connect(new JMXServiceURL(serverUrl), env).MBeanServerConnection
new GroovyMBean(server, 'java.lang:type=Threading').dumpAllThreads(true, true).each { thread ->
    println "THREAD: ${thread.threadName}"
}

true