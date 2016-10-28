The project is to implement a oai harvester of https://arxiv.org

Usage
-----
Jar file is targeted Java 8

To build jar. Clone the source code, and install maven
http://maven.apache.org/install.html

Then run package
```
mvn package
```


Execute the jar file with options -f for from date (the date that the harvester will get records until now)
or/and -u for until date.

```
java -jar arxiv.jar -f "2016-10-15" -u "2016-10-20"
```

To read about arxiv oai
https://arxiv.org/help/oa/index

Harvester protocol:
https://www.openarchives.org/OAI/openarchivesprotocol.html

There is a harvester manager.
http://oaiharvestmangr.sourceforge.net/

I hope to find time to integrate arxiv harvester into the manaager