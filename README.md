# Q-Rapids eval ![](https://img.shields.io/badge/License-Apache2.0-blue.svg)
Qrapids-eval computes metrics, factors, and indicators on raw data stored in Elasticsearch. In the q-rapids context, raw data is produced by a q-rapids kafka connectors (read from Jira, Sonarqube or other sources). Q-Rapids eval aggregates the raw data into metrics, and further on into factors and indicators, according to a defined quality model.

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

The *eval.properties* file defines global configuration options. Currently, only the url for notifying the dashboard about a new evaluation is contained:

```
dashboard.notification.url=http://<address>/QRapids-<version>/api/assessStrategicIndicators
```

### projects/default/project.properties
The project.properties file contains the top-level configuration for a project evaluation. It defines the project.name (which will be appended to the metrics/factors/indicators/relations index names), the addresses to source and target Elasticsearch servers, the name and other properties of the source indexes(e.g. Sonarqube, Jira), and the names and types of the created (or reused) target indexes (metrics, factors, indicators, relations). 

**NEW** in this version of qr-eval is the configurable Error Handling. Error handling takes place when the computation of metrics, factors, or indicators fails. This can happen because of missing data, errors in formulas (e.g. division by 0) and for other reasons. The onError property allows to set a project-wide default (which can be overwritten for metrics, factors etc.) how to handle these errors.
+ The 'drop' option just drops the metrics/factors/indicators item that can't be computed, no record is stored. 
+ The 'set0' option stores a record with value 0.


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

Values of the * project.properties * can be used in * params- *  and * metrics * queries. To refer to a project property in a query's property file, prefix the property-name with '$$'. In the example below, the project properties sonarqube.measures.index and sonarqube.measures.bcKey are used in the * 01_lastSnapshotDate.properties * in the params folder:

```
index=$$sonarqube.measures.index
param.bcKey=$$sonarqube.measures.bcKey
result.lastSnapshotDate=hits.hits[0]._source.snapshotDate
```


### projects/default/params
In the first phase of a project evaluation, qr-eval executes the queries in the params folder (* params queries*). These do not compute metrics or factors, but allow for querying arbitrary other values, which then can be used in subsequent * params * and * metrics * queries as parameters. The results of params queries can be used in subsequent params and metrics queries without declaration in the associated property-files (unlike values of project.properties, where declaration is necessary)

The * params * queries are executed in sequence (alphabetical order). For this reason, it is a good practice to follow the suggested naming scheme for parameter queries and start the name of with a sequence of numbers (e.g. 01_query_name, 02_other_name). Since params queries build on each other, a proper ordering is necessary.


A query consists of a pair of files:
* A .properties file, that declares the index the query should run on, as well as parameters and results of the query
* A .query file that contains the actual query in Elasticsearch syntax (see [Elasticsearch DSL](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html))

__Example (01_lastShnapshotDate)__

01_lastSnapshotDate.properties

```
index=$$sonarqube.measures.index
param.bcKey=$$sonarqube.measures.bcKey
result.lastSnapshotDate=hits.hits[0]._source.snapshotDate
```
+ The index property is read from the project.properties files ($$-notation).
+ The query uses one parameter (bcKey), which is also read from the project properties file. Parameters of a query are declared with prefix 'param.' 
+ The query defines one result (lastSnapshotDate), that is specified as a path within the query result delivered by elasticsearch. Results are declared with prefix 'result.'
All results computed by params queries can be used as parameters (without declaration) in subsequent params- and metrics queries. Make sure that the names of the results of params queries are unique, otherwise they will get overwritten.

__Query Parameters__

Qr-eval internally uses [Elasticsearch search templates](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-template.html) to perform * params, metrics *, and other queries. Search templates can receive parameters (noted with double curly braces: {{parameter}} ). The parameters are replaced by actual values, before the query is executed. The replacement is done verbatim and doesn't care about data types. Thus, if you want a string parameter, you'll have to add quotes around the parameter yourself (as seen below with the evaluationDate parameter).
+ The evaluationDate is available to all * params * and * metrics * queries without declaration. Qr-eval started without command-line options sets the evaluationDate to the date of today (string, format yyyy-mm-dd).
+ Elements of the * project.properties * can be declared as a parameter with the $$-notation, as seen above (param.bcKey)
+ Literals (numbers and strings) can be used after declaration as parameters (e.g by * param.myThreshold=15 *)
+ Results (noted with prefix 'result.') of * params queries * can be used as parameters in succeeding * params * and * metrics * queries without declaration.

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

The query limits the size of the result to one and sorts in descending order.

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
The folder contains the metrics definitions of a project. As * params queries *, * metrics queries * consist of a pair of files, a .properties and a .query file. In addition to params queries, metrics queries compute a metric value defined by a formula. The computed metric value is stored in the metrics index (defined in project.properties) after query execution.

Computed metrics get aggregated into factors. Therefore you have to specify the factors, a metric is going to influence. Metrics can influence one or more factors, that are supplied as a comma-separated list of factor-ids together with the weight describing the strength of the influence. In the example below, the metric 'complexity' influences two factors (codequality and other) with weights 2.0 for codequality and 1.0 for other. The value of a factor is then computed as a weighted sum of all metrics influencing a factor.

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
factors=codequality,other
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

__Note:__ The onError property can be set to 'drop' or 'set0' and overwrites to setting in project.properties.


complextiy.query

```
{ 
  "size" : 0,
  "query": {
    "bool": {
      "must" : [
			{ "term" : { "bcKey" : "{{bcKey}}" } },
			{ "term" : { "snapshotDate" : "{{lastSnapshotDate}}" } },
			{ "term" : { "metric" : "function_complexity"} },
			{ "term" : { "qualifier" : "FIL" } }
      ]
    }
  },
  "aggs": {
    "goodBad" : {
      "range" : {
        "field" : "floatvalue",
        "ranges" : [
          { "to" : {{avgcplx.threshold}} }, 
          { "from" : {{avgcplx.threshold}} }
        ]
      }
    }
  }
}
```

The complexity query is based on a [bool query](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-bool-query.html) and uses a [bucket range aggregation](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-bucket-range-aggregation.html) to derive its results.
The query considers documents/records that fulfill the following conditions:
+ Only documents with a specific {{bcKey}} (only files of this project)
+ Only documents with a specific {{snapshotDate}} (parameter derived in * params query * 01_snapshotDate)
+ Only documents for metric "function_complexity"
+ Only documents with qualifier "FIL" (analyze only files, not folders etc.)

In the bucket range aggregation, the matching documents are divided into two buckets: 
+ Files with function_complexity < avgcplx.threshold
+ Files with function_complexity >= avgcplx.threshold

__Example result__

```
{
  "took" : 22,
  "timed_out" : false,
  "_shards" : { ... },
  "hits" : {
    "total" : 53,
    ...
  },
  "aggregations" : {
    "goodBad" : {
      "buckets" : [
        { "key" : "*-15.0", "to" : 15.0, "doc_count" : 53 },
        { "key" : "15.0-*", "from" : 15.0, "doc_count" : 0 }
      ]
    }
  }
}
```


The metric (percentage of files having tolerable complexity) is then computed as: 

```
metric=complexity.good / ( complexity.good + complexity.bad ) = 53 / ( 53 + 0 ) = 100%
```

### projects/default/factors.properties
The factors.properties file defines factors to compute along with their properties. Factors don't do sophisticated computations, they serve as a point for the aggregation of metric values. Factors are then aggregated into indicators, so they have to specify the indicators they are influencing along with the weights of the influence. The notation used is <factorid>.<propertyname> = <propertyvalue>. 


+ The * enabled * attribute enables/disables a factor (no records written for a factor when disabled)
+ The * name * property supplies a user-friendly name of a factor 
+ The * decription * attribute describes the intention of the factor
+ The * indicators * attribute contains a list of influenced indicators (which are defined in a separate properties file).
+ The * weights * attribute sets the strength of the influence. Obviously, the lists in 'indicators' and 'weights' have to have the same length!
+ The * onError * attribute tells qr-eval what to do in case of factor computation errors (e.g. no metrics influence a factor, which results in a division by zero)

Example factor definition (codequality):

```
codequality.enabled=true
codequality.name=Code Quality
codequality.description=It measures the impact of code changes in source code quality. Specifically, ...
codequality.indicators=productquality
codequality.weights=1.0
codequality.onError=set0
```

__Note:__ The onError property can be set to 'drop' or 'set0' and overwrites to setting in project.properties.

### projects/default/indicators.properties
The indicators.properties file defines the indicators for a project. The parents- and weights-attribute currently have no effect, but could define an additional level of aggregation in future. 

```
productquality.enabled=true
productquality.name=Product Quality
productquality.description=Quality of the Product
productquality.parents=meta
productquality.weights=1.0
```

### projects/default/factors
Defines the query for aggregation of metrics into factors, based on relations index. DON'T TOUCH, unless you know what you are doing.

### projects/default/indicators
Defines the query for aggregation of factors into indicators, based on relations index. DON'T TOUCH, unless you know what you are doing.



## Running qrapids-eval

### Prerequisites
* Elasticsearch source and target servers are running and contain appropriate data
* Java 1.8 is installed
* A projects folder exists in the directory of qrapids-eval<version>.jar and contains a proper quality model configuration

### Run without commandline parameters
The date of the current day (format yyyy-MM-dd) will be available as parameter 'evaluationDate' in params- and metrics-queries

```
java -jar qrapids-eval-<version>-jar-with-dependencies.jar
```

### Specify a single evaluation date
The specified evaluationDate will be available as parameter 'evaluationDate' in params- and metrics-queries.

```
java -jar qrapids-eval-<version>-jar-with-dependencies.jar evaluationDate 2019-03-01
```

### Specify a date range for evaluation
The defined projects will be evaluated for each day in the specified range.

```
java -jarqrapids-eval-<version>-jar-with-dependencies.jar from 2019-03-01 to 2019-03-30
```

### Build the connector
```
mvn package assembly:single
```
After build, you'll find the generated jar in the target folder

## Model validation
Before the evaluation of a project starts, qrapids-eval performs a basic evaluation of the qualtity model. A warning is logged in the following cases:
+ A metrics-query mentions a factor in the factors-property, but the factor isn't defined in the factors.properties file.
+ A factor mentioned in a metric is not enabled
+ A factor is defined in factors.properties, but not mentioned in any metrics-query
+ An indicator is mentioned in the indicators-property of a defined factor, but is not defined in the indicators.properties file
+ An indicator is mentioned in the indicators-property of a defined factor, but is not enabled
+ An indicator is defined in indicators.properties, but it is not mentioned in any indicators-property of the defined factors


## Built With

* [Maven](https://maven.apache.org/) - Dependency Management


## Authors

* **Axel Wickenkamp, Fraunhofer IESE**

