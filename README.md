###Noe load beast

Zatěžuje NOE daty s logů, přičemž dodržuje posloupnost procesu a časové intervaly posílání zpráv. Umožňuje řídit zátěž, tím, že tyto intervaly
umí konfiguračně zkrátit, nebo prodlužit. Kontroluje výstup NOE zda se neliší od zalogovaného.

######build:    
`mvn clean install`

######runtime:  
jsou potřeba jdbc knihovny pro oracle, a klientské jms knihovny. Zbytek knihoven se dá použít z mvn repository.


#####příprava loadu:
```
java -classpath ./:/Users/pavelnovotny/projects/NoeLoad/lib/ojdbc6_g.jar:/Users/pavelnovotny/projects/NoeLoad/lib/slf4j-simple-1.7.25.jar:/Users/pavelnovotny/projects/NoeLoad/lib/slf4j-api-1.7.25.jar:/Users/pavelnovotny/projects/NoeLoad/target/load_beast-1.0.jar cz.to2.noe.load.PrepareLoadFile
```

#####spuštění loadu:
```
java -classpath ./:/Users/pavelnovotny/projects/NoeLoad/lib/xmlunit-core-2.6.0.jar:/Users/pavelnovotny/projects/NoeLoad/lib/xmlunit-legacy-2.6.0.jar:/Users/pavelnovotny/projects/NoeLoad/lib/slf4j-simple-1.7.25.jar:/Users/pavelnovotny/projects/NoeLoad/lib/slf4j-api-1.7.25.jar:/Users/pavelnovotny/projects/NoeLoad/lib/wlfullclient-11g.jar:/Users/pavelnovotny/projects/NoeLoad/target/load_beast-1.0.jar cz.to2.noe.load.LoadBeast
```
#####ukončení loadu:  
v případě výjimky (nedostupné JMS apod.), je nutné killnout nebo Ctrl+C, výjimka většinou ukončí jenom thread ve kterém běží, ale ne ostatní thready. Pokud běží program bez výjimek, ukončí všechny thready sám, na základě detekce nečinnosti, jejíž délka je konfigurovatelný parametr.


######konfigurace  
v příkladech níž jsou *property* soubory na classpath v aktuálním adresáři. Property jsou samopopisné.  
 
*beast-iteration.properties*
zde se čte a zapisuje spuštěná iterace loadu, která se pak použije jako suffix klíčů pro NOE, aby byly unikátní
```
#Wed Sep 30 11:48:15 CEST 2020
current.load.iteration=11
``` 

*beast-noe.properties*  
hlavní konfigurační soubor
```
###########################
# Create load data file
###########################
load.file=/Users/pavelnovotny/projects/NOE-sandbox/data/load/load.txt
date.from=9.9.2020
date.to=9.9.2020

#produkce NOE DB
#db.connect.string=jdbc:oracle:thin:@ocsxgfdb01p-scan.ux.to2cz.cz:1521/CONOEP
#db.user=noe
#db.passwd=A7FV_V2ad
#predprod NOE DB

db.connect.string=jdbc:oracle:thin:@ocsxpptdb01r-scan.ux.to2cz.cz:1521/CONOER
db.user=noe
db.passwd=KIfpcOkE


###########################
# Do load using created file
###########################

#faster or slower load or the same speed
load.speed.factor=1000
#load.speed.factor=0.0001
#load.speed.factor=1

#Milliseconds for factor 1 to wait for send or receive before ending program
#recomputed automatically for different factors
work.monitor.inactivity.threshold=72000000

#number of delay threads to buffer for earlier order time read from file
threads.pipe.delay.count=10
#number of threads for internal pipe
threads.pipe.receive.count=3

jms.send.jndi.initial.factory=weblogic.jndi.WLInitialContextFactory
#number of servers in url = number of threads for jms
jms.send.jndi.provider.url=t3://localhost:7777,t3://localhost:7777,t3://localhost:7777
jms.send.queue.name=cipesb.cip2aq.queue.request
jms.send.connection.factory.jndi.name=weblogic.jms.ConnectionFactory

jms.receive.jndi.initial.factory=weblogic.jndi.WLInitialContextFactory
#number of servers in url = number of threads for jms
jms.receive.jndi.provider.url=t3://localhost:7777,t3://localhost:7777,t3://localhost:7777
jms.receive.queue.name=cipesb.bscs.queue.error
jms.receive.connection.factory.jndi.name=weblogic.jms.ConnectionFactory

```

*simplelogger.properties*  
konfigurace logování.


######pro účely testování loadu na weblogicu:
pro ty co nemají NOE.  
poslání testovacích zpráv do lokální jms:  
```
java -classpath ./:/Users/pavelnovotny/projects/NoeLoad/lib/xmlunit-core-2.6.0.jar:/Users/pavelnovotny/projects/NoeLoad/lib/xmlunit-legacy-2.6.0.jar:/Users/pavelnovotny/projects/NoeLoad/lib/slf4j-simple-1.7.25.jar:/Users/pavelnovotny/projects/NoeLoad/lib/slf4j-api-1.7.25.jar:/Users/pavelnovotny/projects/NoeLoad/lib/wlfullclient-11g.jar:/Users/pavelnovotny/projects/NoeLoad/target/load_beast-1.0.jar cz.to2.noe.load.testing.SendTestData
```
load:
```
java -classpath ./:/Users/pavelnovotny/projects/NoeLoad/lib/xmlunit-core-2.6.0.jar:/Users/pavelnovotny/projects/NoeLoad/lib/xmlunit-legacy-2.6.0.jar:/Users/pavelnovotny/projects/NoeLoad/lib/slf4j-simple-1.7.25.jar:/Users/pavelnovotny/projects/NoeLoad/lib/slf4j-api-1.7.25.jar:/Users/pavelnovotny/projects/NoeLoad/lib/wlfullclient-11g.jar:/Users/pavelnovotny/projects/NoeLoad/target/load_beast-1.0.jar cz.to2.noe.load.LoadBeast
```


