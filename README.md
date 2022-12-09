# **tilli-backend-server**

Tilli is a web-based application with scala as backend and uses truffle as a tool to compile. It uses MongoDB as a database.

************************

## Joining the Tilli Community 
We are a community of learning designers, game designers, developers and educators with an interest in designing game-based, social emotional learning experience for kids. We create an environment where our community can effectively explore, create innovative and open aid distribution technology services that are inclusive, accountable, and accessible for everyone.

### Contribution Guidelines

We would love your input! We want to make contributing to this project as easy and transparent as possible so kindly go through our contribution guidelines here: [Contribution Guidelines](https://tillioss.github.io/docs/Contribution-Guidelines)

### Code of Conduct
<br>Please note that Tilli’s open-source projects are governed by our [Code of conduct](https://tillioss.github.io/docs/code-of-conduct). 

## Getting started
To get started, please have a look at our [User and Developer Documentation](https://tillioss.github.io/docs/getting-started-developer).

*********************

# Installing/ Setting Up
## Required 

 Please Install Redis in Your System


## Redis Setup 
'''/etc/init.d/redis-server stop'''

on Mac:
redis-cli shutdown


If you Need backup your existing data
'''cd /var/lib/redis/
cp dump.rdb dump1.rdb'''

**Replace redis data**

'''cp /var/www/html/teqbahn/tilli/tilli-backend-server/data/dump.rdb /var/lib/redis/'''

**Restart your redis**

'''/etc/init.d/redis-server restart'''


To run locally - using SBT.

sbt "runMain com.teqbahn.bootstrap.StarterMain local 2553 8093  \<ServerIP\>  \<RedisIP\>:\<RedisPort\> \<MailId\> \<MailPassword\> \<filepath\>"



**local** - server running environment 
  
  2553 - Akka Port 
  
  8093 - Attp Port 

**httpHostName** : 192.0.0.1



\<**ServerIP**\>  - Replace your system IP Address 
 
 example : 192.0.0.1


\<**RedisIP**\> - Replace your Redis IP Address

example : 127.0.0.1


\<**RedisPort**\> -  Replace your Redis Port

example : 6379


If You Required Send Mail,Please Configure Mail Setup OtherWise Using This Dummy Values 

\<**MailId**\>  -  Replace your mail id :

example : xxxx@xyz.com


\<**MailPassword**\>  -  Replace your password :

example : password123


\<**filepath**\> - Replace your system filepath 
 
 example : /html/tilli



**example:**
**sbt "runMain com.teqbahn.bootstrap.StarterMain local 2553 8093  192.1.0.1  127.0.0.1:6379 xxxx@xyz.com password123 /html/tilli"**



 




 
 
