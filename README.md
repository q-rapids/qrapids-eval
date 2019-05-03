# Q-Rapids eval ![](https://img.shields.io/badge/License-Apache2.0-blue.svg)
The q-rapids eval component queries raw data stored in an Elasticsearch server  to compute metrics, factors, and indicators, which are also get stored in an Elasticsearch server. In the q-rapids context, raw data is produced by a q-rapids kafka connector (read from Jira, Sonarqube or other sources). Q-Rapids eval aggregates the raw data into metrics, and further on into factors and indicators, according to a defined quality model.

##Configuration
Q-Rapids eval is a commandline tool and is configured via a set of text files (query- and property-files) that are stored in a special folder structure. The top-folder is named 'projects'. This folder has to be present in the same directory where the qrapids-eval.jar file is stored. Each subfolder defines a quality model for a project to be evaluated.

The folder structure shown below defines the evaluation of one project 'default'.

```
+---projects
    +---default
    |   +---factors
    |   |     factor.properties
    |   |     factor.query
    |   +---indicators
    |   |     indicator.properties
    |   |     indicator.query
    |   +---metrics
    |   |     comments.properties
    |   |     comments.query
    |   |     complexity.properties
    |   |     complexity.query
    |   |     duplication.properties
    |   |     duplication.query
    |   +---params
    |   |     01_lastSnapshotDate.properties
    |   |     01_lastSnapshotDate.query
    |   |     02_filesInSnapshot.properties
    |   |     02_filesInSnapshot.query
    |   |
    |   |  factors.properties
    |   |  indicators.properties
    |   |  project.properties
    |
    |
    |  eval.properties
```
### projects/eval.properties

The * eval.properties * file defines global configuration options. Currently, only the url for notifying the dashboard about a new evaluation is contained:

```
dashboard.notification.url=http://<address>/QRapids-<version>/api/assessStrategicIndicators
```

### projects/default/project.properties
The project.properties file contains the top-level configuration for a project evaluation. It defines the project.name (which will be appended to the metrics/factors/indicators/relations index names), the addresses to source and target elasticsearch servers, and name and special properties of the source indexes (e.g. sonarqube).

```properties
# project name
# must be lowercase since it becomes part of the metrics/factors/indicators/relations index names, mandatory
project.name=default

# Elasticsearch source data, mandatory
elasticsearch.source.ip=localhost

# Elasticsearch target data (metrics, factors, indicators, relations, ...), mandatory
# Could be same as source
elasticsearch.target.ip=localhost

########################
#### SOURCE INDEXES ####
########################

# sonarqube measures index
sonarqube.measures.index=sonarqube.measures
sonarqube.measures.bcKey=<your-sonarqube-base-component-key>

# sonarqube issues index
sonarqube.issues.index=sonarqube.issues
sonarqube.issues.project=<your-sonarqube-project-key>

########################
#### TARGET INDEXES ####
########################

# rules for index names: lowercase, no special chars (dot "." allowed), no leading numbers, 

# metrics index, mandatory
metrics.index=metrics
metrics.index.type=metrics

# factors index, mandatory
factors.index=factors
factors.index.type=factors

# impacts index, mandatory
relations.index=relations
relations.index.type=relations

# factors index, mandatory
indicators.index=indicators
indicators.index.type=indicators

# global error handling default: 'drop' or 'set0', default is 'drop'.
# Error handling takes place when the computation of a metric/factor/indicator/relation fails.
# Strategy 'drop' doesn't store the item, 'set0' sets the item's value to 0.
# The setting can be overwritten for specific metrics, factors, and indicators
onError=set0
```

Values of the project.properties can be referred to in property files of queries inside the params folder and properties of queries inside the metrics folder. To refer to a project property, the property name is prefixed by '$$'. In the example below, the project properties sonarqube.measures.index and sonarqube.measures.bcKey properties are used in the 01_lastSnapshotDate.properties in the params folder:

```
index=$$sonarqube.measures.index
param.bcKey=$$sonarqube.measures.bcKey
result.lastSnapshotDate=hits.hits[0]._source.snapshotDate
```

__Error Handling__

Error handling takes place when the computation of metrics, factors, or indicators fails. This can happen because of missing data, errors in formulas (e.g. division by 0) and for other reasons. The onError.default property allows to set a project-wide default (which can be overwritten for metrics, factors etc.) how to handle these errors.
The 'drop' option just drops the metrics/factors/indicators item that can't be computed, no record is stored. The 'set0' option stores a record with value 0.

### projects/default/params
In the first phase of a project evaluation, qr-eval executes the queries in the params folder (hereafter referred to as params queries). These do not compute metrics or factors, but allow for querying arbitrary other values, which then can be used on the metric query level as parameters. The params queries are executed in sequence (alphabetical order). For this reason, it is a good practice to follow the suggested naming scheme for parameter queries and start the name of a parameter query with a sequence of numbers (e.g. 01_query_name, 02_other_name). The results derived by a previous params query can be used as parameters in a succeeding params query.

A query (params & metrics) consists of a pair of files:
* A .properties file, that declares the index the query should run on, as well as parameters and results of the query
* A .query file that contains the actual query in Elasticsearch syntax (see [Elasticsearch DSL](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html))

__Example (01_lastShnapshotDate)__

01_lastShnapshotDate.properties

```
index=$$sonarqube.measures.index
param.bcKey=$$sonarqube.measures.bcKey
result.lastSnapshotDate=hits.hits[0]._source.snapshotDate
```
+ The index property is read from the project.properties files ($$-notation).
+ The query uses one parameter (bcKey), which is also read from the project properties file. Parameters of a query are declared with prefix 'param.' 
+ The query defines one result (lastSnapshotDate), that is specified as a path within the query result delivered by elasticsearch. Results are declared with prefix 'result.'
All results computed by params queries can be used as parameters (without declaration) in metrics queries. Make shure that the names of the results of params queries are unique, otherwise they will get overwritten.

01_lastSnapshotDate.query

```
{
	"query": {
		"bool": {
			"must" : [
				{ "term" : { "bcKey" : "{{bcKey}}" } },
				{ "range" : { "snapshotDate" : { "lte" : "{{evaluationDate}}", "format": "yyyy-MM-dd" } } }
      		]
		}
	},
	"size": 1,
	"sort": [
    	{ "snapshotDate": { "order": "desc" 	} }
	]
}
```

The lastSnapshotDate query is a [bool query](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-bool-query.html). It defines two conditions that have to evaluate to TRUE for matching documents:
+ The documents must have the supplied parameter {{bcKey}} as value of the field bcKey (match only records of the specified project)
+ The value of the field snapshotDate must be lower or equal to the evaluationDate. The {{evaluationDate}} parameter is available to all queries without declaration and typically contains the date of today in format yyyy-MM-dd. The evaluationDate can be supplied via command-line (see command-line-options).

The query limits the size of the result to one (size : 1) and sorts in descending order.

Example query result:

```json
{
  "took" : 31,
  "timed_out" : false,
  "_shards" : {...} ,
  "hits" : {
    "total" : 144491,
    "max_score" : null,
    "hits" : [
      {
        "_index" : "sonarqube.measures",
        "_type" : "sonarqube",
        "_id" : "sonarqube.measures+0+149155",
        "_score" : null,
        "_source" : {
          ...
          "snapshotDate" : "2018-12-04",
          "bcKey" : "ptsw_official",
          ...
        },
        "sort" : [
          1543881600000
        ]
      }
    ]
  }
}
```

The result of the query is specified as path in the returned json: __"hits" -> "hits" [0] -> "_source" -> "snapshotDate" = "2018-12-04"__

### projects/default/metrics
The folder contains the metrics definitions of a project. As params queries, metrics queries consist of a pair of files, a .properties file and a .query file. In addition to params queries, metrics queries compute a metric value defined by a formula. The computed metric value is stored in the metrics index (defined in project.properties) after query execution.

__Example: complexity query__

complexity.properties

```properties
# index the query runs on, mandatory
# values starting with $$ are looked up in project.properties
index=$$sonarqube.measures.index

# metric props, mandatory
enabled=true
name=Complexity
description=Percentage of files that do not exceed a defined average complexity per function
factors=codequality,other_factor
weights=2.0,1.0

# query parameter
param.bcKey=$$sonarqube.measures.bcKey
param.avgcplx.threshold=15

# query results (can be used in metric calculation)
result.complexity.good=aggregations.goodBad.buckets[0].doc_count
result.complexity.bad=aggregations.goodBad.buckets[1].doc_count

# metric defines a formula based on execution results of params- and metrics-queries
metric=complexity.good / ( complexity.good + complexity.bad )
onError=set0
```



### factors.properties
The factors.properties file defines factors to compute along with their properties.

Example factor definition (codequality):

```
codequality.enabled=true
codequality.name=Code Quality
codequality.description=It measures the impact of code changes in source code quality. Specifically, ...
codequality.indicators=productquality
codequality.weights=1.0
codequality.onError
```









## Running the Connector

### Prerequisites

* Kafka has to be setup and running (see [Kafka Connect](https://docs.confluent.io/current/connect/index.html))
* If you want your data to be transfered to Elasticsearch, Elasticsearch has to be setup and running. (see [Set up Elasticsearch](https://www.elastic.co/guide/en/elasticsearch/reference/current/setup.html))

### Build the connector
```
mvn package assembly:single
```
After build, you'll find the generated jar in the target folder

### Configuration files

Example Configuration for kafka standalone connector (standalone.properties)

```properties 
bootstrap.servers=<kafka-ip>:9092

key.converter=org.apache.kafka.connect.storage.StringConverter
value.converter=org.apache.kafka.connect.json.JsonConverter

key.converter.schemas.enable=true
value.converter.schemas.enable=true

internal.key.converter=org.apache.kafka.connect.json.JsonConverter
internal.value.converter=org.apache.kafka.connect.json.JsonConverter
internal.key.converter.schemas.enable=false
internal.value.converter.schemas.enable=false

offset.storage.file.filename=/tmp/connect-sonarqube.offsets

offset.flush.interval.ms=1000
rest.port=8088
```

Configuration for Sonarqube Source Connector Worker (sonarqube.properties)

```properties
name=kafka-sonar-source-connector
connector.class=connect.sonarqube.SonarqubeSourceConnector
tasks.max=1

# sonarqube server url
sonar.url=http://<your-sonarqube-address>:9000

#authenticate, user need right to Execute Analysis
sonar.user=<sonaruser>
sonar.pass=<sonarpass>

# key for measure collection
sonar.basecomponent.key=<key of application under analysis>

#projectKeys for issue collection
sonar.project.key=<key of application under analysis>

# kafka topic names
sonar.measure.topic=sonar.metrics
sonar.issue.topic=sonar.issues

# measures to collect, since sonarqube6 max 15 metrics
# see https://docs.sonarqube.org/latest/user-guide/metric-definitions/
sonar.metric.keys=ncloc,lines,comment_lines,complexity,violations,open_issues,code_smells,new_code_smells,sqale_index,new_technical_debt,bugs,new_bugs,reliability_rating,classes,functions

#poll interval (86400 secs = 24 h)
sonar.interval.seconds=86400

#set snapshotDate manually, format: YYYY-MM-DD
sonar.snapshotDate=
```

Configuration for Elasticsearch Sink Connector Worker (elasticsearch.properties)

```properties
name=kafka-sonarqube-elasticsearch
connector.class=io.confluent.connect.elasticsearch.ElasticsearchSinkConnector
tasks.max=1
topics=sonarqube.measures,sonarqube.issues
key.ignore=true
connection.url=http://<elasticsearch>:9200
type.name=sonarqube

```

End with an example of getting some data out of the system or using it for a little demo


## Running the Connector

```
<path-to-kafka>/bin/connect-standalone standalone.properties sonarqube.properties elasticsearch.properties
```

## Built With

* [Maven](https://maven.apache.org/) - Dependency Management


## Authors

* **Axel Wickenkamp, Fraunhofer IESE**

