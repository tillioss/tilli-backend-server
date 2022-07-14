tilli-backend-server
************************

##Required 

 1.Please Install Redis in Your System

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


To run locally - using SBT.
**********************

sbt "runMain com.teqbahn.bootstrap.StarterMain local 2553 8093  <ServerIP>  <RedisIP>:<RedisPort> <MailId> <MailPassword> <filepath>"


local - server running environment 

2553 - Akka Port 

8093 - Attp Port 

httpHostName : 192.0.0.1


<ServerIP>  - Replace your system IP Address 
 ex : 192.0.0.1

<RedisIP> - Replace your Redis IP Address
  ex : 127.0.0.1

<RedisPort> -  Replace your Redis Port
  ex : 6379

If You Required Send Mail,Please Configure Mail Setup OtherWise Using This Dummy Values 

<MailId>  -  Replace your mail id :
 ex : xxxx@xyz.com

<MailPassword>  -  Replace your password :
 ex : password123

<filepath> - Replace your system filepath 
 ex : /html/tilli


ex:
sbt "runMain com.teqbahn.bootstrap.StarterMain local 2553 8093  192.1.0.1  127.0.0.1:6379 xxxx@xyz.com password123 /html/tilli"



 




 
 
