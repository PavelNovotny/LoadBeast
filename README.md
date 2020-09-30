Zatěžuje NOE daty s logů, přičemž dodržuje posloupnost procesu a časové intervaly posílání zpráv. Umožňuje řídit zátěž, tím, že tyto intervaly
zkracuje, nebo prodlužuje. Kontroluje výstup NOE zda se neliší od zalogovaného.

build:
mvn clean install

spuštění loadu:
java -classpath \
./:\
/Users/pavelnovotny/projects/NoeLoad/lib/xmlunit-core-2.6.0.jar:\
/Users/pavelnovotny/projects/NoeLoad/lib/xmlunit-legacy-2.6.0.jar:\
/Users/pavelnovotny/projects/NoeLoad/lib/slf4j-simple-1.7.25.jar:\
/Users/pavelnovotny/projects/NoeLoad/lib/slf4j-api-1.7.25.jar:\
/Users/pavelnovotny/projects/NoeLoad/lib/wlfullclient-11g.jar:\
/Users/pavelnovotny/projects/NoeLoad/lib/ojdbc6_g.jar:\
/Users/pavelnovotny/projects/NoeLoad/target/load_beast-1.0.jar \
cz.to2.noe.load.LoadBeast

ukončení loadu: v případě výjimky (nedostupné JMS apod.), je nutné killnout nebo Ctrl+C, výjimka většinou ukončí jenom
thread ve kterém běží, ale ne ostatní thready.
Pokud běží program bez výjimek, ukončí všechny thready sám, na základě detekce inactivity,
jejíž délka je konfigurovatelný parametr.

načtení logu a příprava load dat:
todo
PrepareLoadFile



poslání test data do lokální jms:
java -classpath \
./:\
/Users/pavelnovotny/projects/NoeLoad/lib/xmlunit-core-2.6.0.jar:\
/Users/pavelnovotny/projects/NoeLoad/lib/xmlunit-legacy-2.6.0.jar:\
/Users/pavelnovotny/projects/NoeLoad/lib/slf4j-simple-1.7.25.jar:\
/Users/pavelnovotny/projects/NoeLoad/lib/slf4j-api-1.7.25.jar:\
/Users/pavelnovotny/projects/NoeLoad/lib/wlfullclient-11g.jar:\
/Users/pavelnovotny/projects/NoeLoad/lib/ojdbc6_g.jar:\
/Users/pavelnovotny/projects/NoeLoad/target/load_beast-1.0.jar \
cz.to2.noe.load.testing.SendTestData


