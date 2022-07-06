tilli-backend-server
************************

setup
*********************


##Redis Setup 
'''/etc/init.d/redis-server stop'''

on Mac:
redis-cli shutdown


If you Need backup your existing data
'''cd /var/lib/redis/
cp dump.rdb dump1.rdb'''

Replace redis data

'''cp /var/www/html/teqbahn/tilli/tilli-backend-server/data/dump.rdb /var/lib/redis/'''

Restart your redis
'''/etc/init.d/redis-server restart'''

##File Setup

Setup/Replace "fileSystemPath -> <your_data_path>" in StarterMain.scala
From /data/files.zip move and extract to <your_data_path>



To run locally - using SBT.
**********************

sbt "runMain com.teqbahn.bootstrap.StarterMain local 2553 8093  192.0.0.1  127.0.0.1:6379 xxxx@xyz.com password123 saltkey"

server running environment : local

akkaPort : 2553

httpPort : 8093

httpHostName : 192.0.0.1

1. Replace your system IP Address :
   192.0.0.1

2. Replace your mail id :
   xxxx@xyz.com

3. Replace your password :
   password123

4. Replace your host and port number(host:port)
   127.0.0.1:6379









 
 
