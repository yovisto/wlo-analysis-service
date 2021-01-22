# WLO Analysis Webservice

# Requirements

- Docker
- Java 8 or higher
- Dependencies:
	- [kea-wiki-extraction](https://github.com/yovisto/kea-wiki-extraction)
	- [kea-el](https://github.com/yovisto/kea-el)


# Installation/Setup

## Retrieve and prepare data

Follow the installation instruction for the [kea-wiki-extraction](https://github.com/yovisto/kea-wiki-extraction) and [kea-el](https://github.com/yovisto/kea-el) tools.


Copy the content of the [kea-el](https://github.com/yovisto/kea-el)  ```data```  directory (lucene indexes) to the ```docker/tomcat/data``` directory of this project.

Put the ```categories.txt``` of the [kea-wiki-extraction](https://github.com/yovisto/kea-wiki-extraction) into this project's ```data``` directory and convert it to ```.nt``` format with the following commands:


	cat data/categories.txt | awk '{print "<https://de.wikipedia.org/wiki/" $1 "> <http://www.w3.org/2004/02/skos/core#broader> <https://de.wikipedia.org/wiki/" $2 "> . "}' > data/categories.nt


Convert and verify the ```categories.nt``` file to ```.ttl```. You might use [rapper](http://librdf.org/raptor/rapper.html) or a similar RDF conversion utility:


	rapper -e -i ntriples data/categories.nt > data/categories.ttl

(The *.nt file might contain some invalid triples, which we are ignoring for sake of simplicity (parameter ```-e```).)

## Building the Java App

(Make sure you have built and installed the dependency: [kea-el](https://github.com/yovisto/kea-el).)

Run the Maven installation:

	mvn install 


## Prepare Docker Components

### Apache Tomcat

Copy the created ```*.war``` file to the Tomcat data directory and rename it to ```ROOT.war```:

```
cp target/wlo-as-1.0.0-SNAPSHOT.war docker/tomcat/webapps/ROOT.war
```

### Virtuoso 
##### Retrieve RDF files from the subjects, discipline mappings, and OEH vocabs


To collect the necessary data to deploy in the Virtuoso RDF triplestore, the following script is provided:

```
sh collectRdfData.sh
```

The script performs the following downloads:

* Get the subject area mappings from [wlo-metadata-mappings](https://github.com/yovisto/wlo-metadata-mappings).
<!-- 
```
wget https://raw.githubusercontent.com/yovisto/wlo-metadata-mappings/main/subjectAreas/subjectAreasMapping.ttl
```
-->

* Get the keyword mappings from [wlo-metadata-mappings](https://github.com/yovisto/wlo-metadata-mappings).

<!-- 
```
wget https://github.com/yovisto/wlo-metadata-mappings/raw/main/keywords/keywordMapping.ttl
```
-->

* Get and unzip the normdata file from [wlo-metadata-mappings](https://github.com/yovisto/wlo-metadata-mappings)

<!-- 
```
wget https://github.com/yovisto/wlo-metadata-mappings/raw/main/normdata/normdata.ttl.zip
unzip normdata.ttl.zip
```
-->

* Retrieve the discipline description from [oeh-metadata-vocabs](https://github.com/openeduhub/oeh-metadata-vocabs).

<!-- 
```
wget https://raw.githubusercontent.com/openeduhub/oeh-metadata-vocabs/master/discipline.ttl
```
-->

* Retrieve the Schlagwortverzeichnis (keywords) from [oeh-metadata-eaf-schlagwortverzeichnis](https://github.com/openeduhub/oeh-metadata-eaf-schlagwortverzeichnis)

<!-- 
```
wget https://raw.githubusercontent.com/openeduhub/oeh-metadata-eaf-schlagwortverzeichnis/main/data/eaf-graph-by-subject-all.ttl
```
-->

* Retrieve the Sachgebietssystematik from [oeh-metadata-eaf-sachgebietssystematiken](https://github.com/openeduhub/oeh-metadata-eaf-sachgebietssystematiken)

<!-- 
```
wget https://raw.githubusercontent.com/openeduhub/oeh-metadata-eaf-sachgebietssystematiken/master/eaf-sachgebietssystematik-all.ttl
```
-->

* Copy all downloaded files to the Virtuoso data loading directory ```docker/virtuoso/data/toLoad```. 

<!-- 
```
cp subjectAreasMapping.ttl docker/virtuoso/data/toLoad
cp keywordMapping.ttl docker/virtuoso/data/toLoad
cp normdata.ttl docker/virtuoso/data/toLoad
cp discipline.ttl docker/virtuoso/data/toLoad
cp eaf-graph-by-subject-all.ttl docker/virtuoso/data/toLoad
cp eaf-sachgebietssystematik-all.ttl docker/virtuoso/data/toLoad
```
-->

## Startup

### Setup Docker Network

```
docker network create wlo-net
```

### Start the Vituoso

```
cd docker/virtuoso
docker run --name wlo-virtuoso --network wlo-net -p 8890:8890 -p 1111:1111 -e DBA_PASSWORD=dba -e SPARQL_UPDATE=true -e DEFAULT_GRAPH=https://wirlernenonline.de -v `pwd`/data:/data  -d tenforce/virtuoso:virtuoso7.2.5
cd ../../
```

Lets try to make a sparql query. 

```
open http://0.0.0.0:8890/sparql
```

Query:
```
select distinct * where {?s <http://www.w3.org/2004/02/skos/core#prefLabel> ?o} LIMIT 100
```

### Start the Tomcat


```
cd docker/tomcat
docker run --name wlo-tomcat --network wlo-net -p 8080:8080 -d -v "$(pwd)"/webapps:/usr/local/tomcat/webapps -v "$(pwd)"/cache:/usr/local/tomcat/keacache -v "$(pwd)"/data:/var/indices tomcat:7
cd ../../
```

(You might consider to delete the cache dir content ```docker/tomact/cache```, if you restart Tomcat.)

Lets test the extration service:

```
open "http://0.0.0.0:8080/services/extract/Armstrong landet auf dem Mond"
```

Should result in a JSON output contiang the annotated text as well as additional   information.


## Usage

Send an example document extracted from the WLO dump ```exampleDoc.json``` to the analysis service:

```
curl -X POST -H "Content-Type: application/json" -d @exampleDoc.json http://0.0.0.0:8080/services/analyze
```
