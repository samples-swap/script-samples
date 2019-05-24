ow to create a MultiNode - MultiBroker Cluster for Kafka on AWS
PreRequisites :
Kafka Binary files : http://kafka.apache.org/downloads.html

Atleast 2 AWS machines : AWS EMR or EC2 will be preferable

A Kafka Manager Utility to watch up the Cluster : https://cwiki.apache.org/confluence/display/KAFKA/Ecosystem

Installation
Now first download the kafka Tarball or binaries on your AWS instances and extract them

tar -xzvf kafka_2.10-0.8.2.1.tgz mv kafka_2.10-0.8.2.1 kafka

On Both the Instances, you only need two properties to be changed i.e. zookeeper.properties & server.properties

a) Going with the first one edit "zookeeper.properties" on both the instances to

vi ~/kafka/config/zookeeper.properties
clientPort=2080 #Changing the Port from default "2181" to "2080"
server.1=ec2-<IP1>.amazonaws.com:2888:3888
server.2=ec2-<IP2>.amazonaws.com:2888:3888
#add here more servers if you want
initLimit=5
syncLimit=2
b) Now edit both instances "server.properties" and update the following this

vi ~/kafka/config/server.properties
broker.id=1 
port=9092
host.name=ec2-<IP1>.amazonaws.com #for 2nd EC2 instance it'll be "ec2-<IP2>.amazonaws.com"
num.partitions=4
zookeeper.connect=ec2-<IP1>.amazonaws.com:2080,ec2-<IP2>.amazonaws.com:2080 #Add all the Instances here for each Instance
After this go to the /tmp of every instance and create following things :

cd /tmp/ mkdir zookeeper #Zookeeper temp dir cd zookeeper touch myid #Zookeeper temp file echo '1' >> myid #Add Server ID for Respective Instances i.e. "server.1 and server.2 etc"

Now all is done, Need to start zookeeper and kafka-server on both instances

##Start on both Instances

~/kafka/bin/zookeeper-server-start.sh ~/kafka/config/zookeeper.properties

~/kafka/bin/kafka-server-start.sh ~/kafka/config/server.properties

Now go to KafkaTool and add both of your AWS instances, You'll see multi brokers with multple partitions in it.

Post your questions here for more help