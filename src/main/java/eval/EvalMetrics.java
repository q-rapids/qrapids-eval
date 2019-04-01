package eval;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import static org.elasticsearch.index.query.QueryBuilders.*;

import org.apache.lucene.index.IndexNotFoundException;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.cardinality.Cardinality;
import org.elasticsearch.search.sort.SortOrder;

import elastic.Client;
import elastic.Indexes;
import elastic.Queries;
import elastic.model.Metric;
import eval.props.MetricsProperties;

public class EvalMetrics {

	private static String sonarqubeMeasuresIndex = Indexes.getInstance().getSonarqubeMeasuresIndex();
	private static String sonarqubeIssuesIndex = Indexes.getInstance().getSonarqubeIssuesIndex();

	private static String snapshotDate = Indexes.getInstance().getSnapshotDate();
	private static String projectId = Indexes.getInstance().getSonarqubeProjectId();
	private static String projectIdField = Indexes.getInstance().getSonarqubeProjectIdField();
	private static MetricsProperties metricsProperties = MetricsProperties.getInstance();

	private static String jiraIndex = Indexes.getInstance().getJiraIndex();
	private static String jiraProjectKey = Indexes.getInstance().getJiraProjectKey();

	private static String jenkinsIndex = Indexes.getInstance().getJenkinsIndex();
	private static String jenkinsJobname = Indexes.getInstance().getJenkinsJobname();

	private static String redmineIndex = Indexes.getInstance().getRedmineIndex();
	private static String redmineProject = Indexes.getInstance().getRedmineProject();

	private static SimpleDateFormat formatYMD = new SimpleDateFormat("yyyy-MM-dd");

	private EvalMetrics() {
	}

	private static void writeMetric( Metric m ) {
		
		if ( m.getValue() != null && !m.getValue().isNaN() ) {
			IndexWriter.writeMetric(m);
			System.out.println("Metric " + m.getMetric() + " written, value=" + m.getValue());
		} else {
			System.out.println("Metric " + m.getMetric() + " NOT written, value=" + m.getValue());
		}
		
	}
	
	public static void eval(String evaluationDate) {

		MetricsProperties metricProps = MetricsProperties.getInstance();

		String lastSnapshotDateString;
		try {
			lastSnapshotDateString = getSnapshotDateString();
		} catch (RuntimeException rte) {
			rte.printStackTrace();
			lastSnapshotDateString = null;
		}

		if (lastSnapshotDateString != null) {

			Metric complexity = metricProps.prepareMetric("complexity");
			if (complexity.getEnabled()) {
				evalComplexity(complexity, lastSnapshotDateString);
				complexity.setEvaluationDate(evaluationDate);
				writeMetric(complexity);
			}

			Metric comments = metricProps.prepareMetric("comments");
			if (comments.getEnabled()) {
				evalComments(comments, lastSnapshotDateString);
				comments.setEvaluationDate(evaluationDate);
				writeMetric(comments);
			}

			Metric duplication = metricProps.prepareMetric("duplication");
			if (duplication.getEnabled()) {
				evalDuplication(duplication, lastSnapshotDateString);
				duplication.setEvaluationDate(evaluationDate);
				writeMetric(duplication);
			}

			Metric nonblockingfiles = metricProps.prepareMetric("nonblockingfiles");
			if (nonblockingfiles.getEnabled()) {
				evalNonBlockingFiles(nonblockingfiles, lastSnapshotDateString);
				nonblockingfiles.setEvaluationDate(evaluationDate);
				writeMetric(nonblockingfiles);
			}
		} else {
			System.out.println("SonarQube Measure Metrics not computed! No Snapshot available.");
		}

		Metric testsuccess = metricProps.prepareMetric("testsuccess");
		if (testsuccess.getEnabled()) {
			testsuccess.setEvaluationDate(evaluationDate);
			evalTestSuccess(testsuccess);
			writeMetric(testsuccess);
		}

		Metric bugsratiojira = metricProps.prepareMetric("bugsratiojira");
		if (bugsratiojira.getEnabled()) {
			evalBugsRatioJira(bugsratiojira);
			bugsratiojira.setEvaluationDate(evaluationDate);
			writeMetric(bugsratiojira);
		}
		
		Metric bugsratioredmine = metricProps.prepareMetric("bugsratioredmine");
		if (bugsratioredmine.getEnabled()) {
			evalBugsRatioRedmine(bugsratioredmine);
			bugsratioredmine.setEvaluationDate(evaluationDate);
			writeMetric(bugsratioredmine);
		}

		Metric welldefinedissuesjira = metricProps.prepareMetric("welldefinedissuesjira");
		if (welldefinedissuesjira.getEnabled()) {
			evalIssuesWelldefinedJira(welldefinedissuesjira);
			welldefinedissuesjira.setEvaluationDate(evaluationDate);
			writeMetric(welldefinedissuesjira);
		}
		
		Metric welldefinedissuesredmine = metricProps.prepareMetric("welldefinedissuesredmine");
		if (welldefinedissuesredmine.getEnabled()) {
			evalIssuesWelldefinedRedmine(welldefinedissuesredmine);
			welldefinedissuesredmine.setEvaluationDate(evaluationDate);
			writeMetric(welldefinedissuesredmine);
		}

		Metric testperformance = metricProps.prepareMetric("testperformance");
		if (testperformance.getEnabled()) {
			evalTestPerformance(testperformance);
			testperformance.setEvaluationDate(evaluationDate);
			writeMetric(testperformance);
		}
	}

	private static String getSnapshotDateString() {
		Long lastSnapshotDateMillis = Queries.getLastestDateValue(sonarqubeMeasuresIndex, snapshotDate, projectIdField,
				projectId);

		if (lastSnapshotDateMillis != null) {
			Date lastSnapshotDate = new Date(lastSnapshotDateMillis);
			String lastSnapshotDateString = formatYMD.format(lastSnapshotDate);
			return lastSnapshotDateString;
		} else {
			return null;
		}
	}

	public static void evalComplexity(Metric cplx, String lastSnapshotDateString) {

		String upperThreshold = metricsProperties.get("complexity.threshold.upper");
		Float ut = upperThreshold.isEmpty() ? 0 : Float.parseFloat(upperThreshold);

		String lowerThreshold = metricsProperties.get("complexity.threshold.upper");
		Float lt = lowerThreshold.isEmpty() ? 0 : Float.parseFloat(lowerThreshold);

		TransportClient tc = Client.getClient();

		try {
			SearchResponse sr = tc.prepareSearch( sonarqubeMeasuresIndex ).setSize(0)
					.setQuery(QueryBuilders.boolQuery().must(termQuery(projectIdField, projectId))
							.must(termQuery("snapshotDate", lastSnapshotDateString))
							.must(termQuery("metric", "function_complexity"))
							.must(termQuery("qualifier", "FIL")))
					.addAggregation(AggregationBuilders.range("good").field("floatvalue").addUnboundedTo(ut))
					.addAggregation(AggregationBuilders.range("bad").field("floatvalue").addUnboundedFrom(lt)).execute()
					.actionGet();

			Range g = sr.getAggregations().get("good");
			Long goodItemCount = g.getBuckets().get(0).getDocCount();

			Range b = sr.getAggregations().get("bad");
			Long badItemCount = b.getBuckets().get(0).getDocCount();

			Float value = (goodItemCount.floatValue() / (goodItemCount + badItemCount));
			cplx.setValue(value);
		} catch (Throwable th) {
			th.printStackTrace();
			cplx.setValue(null);
		}

	}

	public static void evalComments(Metric comments, String lastSnapshotDateString) {

		String upperThreshold = metricsProperties.get("comments.threshold.upper");
		Float ut = upperThreshold.isEmpty() ? 0 : Float.parseFloat(upperThreshold);

		String lowerThreshold = metricsProperties.get("comments.threshold.lower");
		Float lt = lowerThreshold.isEmpty() ? 0 : Float.parseFloat(lowerThreshold);

		TransportClient tc = Client.getClient();

		try {
			SearchResponse sr = tc.prepareSearch(sonarqubeMeasuresIndex).setSize(0)
					.setQuery(QueryBuilders.boolQuery().must(termQuery(projectIdField, projectId))
							.must(termQuery("snapshotDate", lastSnapshotDateString))
							.must(termQuery("metric", "comment_lines_density")).must(termQuery("qualifier", "FIL")))
					.addAggregation(AggregationBuilders.range("all").field("floatvalue").addUnboundedFrom(0))
					.addAggregation(AggregationBuilders.range("good").field("floatvalue").addRange(lt, ut)).execute()
					.actionGet();

			Range all = sr.getAggregations().get("all");
			Long allItemCount = all.getBuckets().get(0).getDocCount();

			Range good = sr.getAggregations().get("good");
			Long goodItemCount = good.getBuckets().get(0).getDocCount();

			Float value = (goodItemCount.floatValue() / allItemCount);
			comments.setValue(value);
		} catch (Throwable th) {
			th.printStackTrace();
			comments.setValue(null);
		}

	}

	public static void evalDuplication(Metric duplication, String lastSnapshotDateString) {

		String upperThreshold = metricsProperties.get("duplication.threshold.upper");
		Float ut = upperThreshold.isEmpty() ? 0 : Float.parseFloat(upperThreshold);

		String lowerThreshold = metricsProperties.get("duplication.threshold.lower");
		Float lt = lowerThreshold.isEmpty() ? 0 : Float.parseFloat(lowerThreshold);

		TransportClient tc = Client.getClient();

		try {
			SearchResponse sr = tc.prepareSearch(sonarqubeMeasuresIndex).setSize(0)
					.setQuery(QueryBuilders.boolQuery().must(termQuery(projectIdField, projectId))
							.must(termQuery("snapshotDate", lastSnapshotDateString))
							.must(termQuery("metric", "duplicated_lines_density")).must(termQuery("qualifier", "FIL")))
					.addAggregation(AggregationBuilders.range("all").field("floatvalue").addUnboundedFrom(0))
					.addAggregation(AggregationBuilders.range("good").field("floatvalue").addRange(lt, ut)).execute()
					.actionGet();

			Range all = sr.getAggregations().get("all");
			Long allItemCount = all.getBuckets().get(0).getDocCount();

			Range good = sr.getAggregations().get("good");
			Long goodItemCount = good.getBuckets().get(0).getDocCount();

			Float value = (goodItemCount.floatValue() / allItemCount);
			duplication.setValue(value);
		} catch (Throwable th) {
			th.printStackTrace();
			duplication.setValue(null);
		}

	}

	public static void evalTestSuccess(Metric testsuccess) {

		TransportClient tc = Client.getClient();

		try {
			SearchResponse sr = tc.prepareSearch(jenkinsIndex).setSize(1)
					.setQuery(QueryBuilders.boolQuery().must(termQuery("jobName", jenkinsJobname)))
					.addSort("buildNumber", SortOrder.DESC).execute().actionGet();

			SearchHit[] hits = sr.getHits().getHits();
			if (hits.length > 0) {
				SearchHit h = hits[0];
				Map<String, Object> m = h.getSource();

				Integer failNum = (Integer) m.get("testsFail");
				Integer passNum = (Integer) m.get("testsPass");
				Integer skipNum = (Integer) m.get("testsSkip");

				Float value = (passNum.floatValue() - skipNum - failNum) / (passNum + skipNum + failNum);
				testsuccess.setValue(value);
			}
		} catch (Throwable th) {
			testsuccess.setValue(null);
			th.printStackTrace();
		}

	}

	public static void evalTestPerformance(Metric testperformance) {

		TransportClient tc = Client.getClient();

		try {
			SearchResponse sr = tc.prepareSearch(jenkinsIndex).setSize(1)
					.setQuery(QueryBuilders.boolQuery().must(termQuery("jobName", jenkinsJobname)))
					.addSort("buildNumber", SortOrder.DESC).execute().actionGet();

			SearchHit[] hits = sr.getHits().getHits();
			if (hits.length > 0) {
				SearchHit h = hits[0];
				Map<String, Object> m = h.getSource();

				Double testDuration = (Double) m.get("testDuration");
				Integer limit = Integer.parseInt(metricsProperties.get("testperformance.limit.seconds"));

				if (testDuration > limit) {
					testperformance.setValue(0.0f);
				} else {
					testperformance.setValue(1.0f);
				}

			}
		} catch (Throwable th) {
			testperformance.setValue(null);
			th.printStackTrace();
		}

	}

	public static void evalBugsRatioJira(Metric bugsratiojira) {

		MetricsProperties mp = MetricsProperties.getInstance();

		String[] statuses = mp.getArray("bugsratiojira.status.unresolved");
		String[] bugIssueTypes = mp.getArray("bugsratiojira.bug.issuetypes");

		TransportClient tc = Client.getClient();

		try {
			SearchResponse sr = null;
				sr = tc.prepareSearch(jiraIndex).setSize(0)
						.setQuery(QueryBuilders
								.boolQuery()
								.must(termQuery("projectKey", jiraProjectKey))
								.must(termsQuery("status", statuses)))
						.addAggregation( AggregationBuilders.filter("openbugs", QueryBuilders.termsQuery("issuetype", bugIssueTypes)))
						.execute().actionGet();


			Long totalHits = sr.getHits().getTotalHits();
			Filter flt = sr.getAggregations().get("openbugs");
			Long openbugs = flt.getDocCount();

			Float value = (totalHits.floatValue() - openbugs) / totalHits;

			bugsratiojira.setValue(value);

		} catch (Throwable th) {
			th.printStackTrace();
			bugsratiojira.setValue(null);
		}
	}

	public static void evalBugsRatioRedmine(Metric bugsratioredmine) {

		MetricsProperties mp = MetricsProperties.getInstance();

		String[] statuses = mp.getArray("bugsratioredmine.status.unresolved");
		String[] bugTrackerTypes = mp.getArray("bugsratioredmine.bug.trackers");

		TransportClient tc = Client.getClient();

		try {
			SearchResponse sr = null;

				sr = tc.prepareSearch(redmineIndex).setSize(0)
						.setQuery(QueryBuilders.boolQuery().must(termQuery("project", redmineProject)).must(termsQuery("status", statuses)))
						.addAggregation( AggregationBuilders.filter("openbugs", QueryBuilders.termsQuery("tracker", bugTrackerTypes)))

						.execute().actionGet();


			Long totalHits = sr.getHits().getTotalHits();
			Filter flt = sr.getAggregations().get("openbugs");
			Long openbugs = flt.getDocCount();

			Float value = (totalHits.floatValue() - openbugs) / totalHits;

			bugsratioredmine.setValue(value);

		} catch (Throwable th) {
			th.printStackTrace();
			bugsratioredmine.setValue(null);
		}
	}

	
	
	public static void evalNonBlockingFiles(Metric nonblockingfiles, String lastSnapshotDateString) {

		TransportClient tc = Client.getClient();

		try {
			SearchResponse sr = tc.prepareSearch(sonarqubeIssuesIndex).setSize(0)
					.setQuery(QueryBuilders.boolQuery()
							.must(termQuery("project", projectId))
							.must(termQuery("snapshotDate", lastSnapshotDateString))
							.must(termsQuery("severity", "BLOCKER", "CRITICAL")))
					.addAggregation(AggregationBuilders.cardinality("distinct_files").field("component")).execute()
					.actionGet();

			Cardinality filesWithIssues = sr.getAggregations().get("distinct_files");
			Long fwi = filesWithIssues.getValue();

			SearchResponse sr2 = tc.prepareSearch(sonarqubeMeasuresIndex).setSize(0)
					.setQuery(QueryBuilders.boolQuery().must(termQuery(projectIdField, projectId))
							.must(termQuery("snapshotDate", lastSnapshotDateString)).must(termQuery("qualifier", "FIL"))
							.must(termQuery("metric", "complexity")))
					.execute().actionGet();

			Long numFiles = sr2.getHits().getTotalHits();

			Float value = (numFiles.floatValue() - fwi.floatValue()) / numFiles;

			nonblockingfiles.setValue(value);
		} catch (Throwable th) {
			th.printStackTrace();
			nonblockingfiles.setValue(null);
		}

	}

	public static void evalBlockerIssues(Metric blockerissues, String lastSnapshotDateString) {

		TransportClient tc = Client.getClient();

		try {
			SearchResponse sr = tc.prepareSearch(sonarqubeIssuesIndex).setSize(0)
					.setQuery(QueryBuilders.boolQuery().must(termQuery("project", projectId))
							.must(termQuery("snapshotDate", lastSnapshotDateString)))
					.addAggregation(AggregationBuilders.terms("severity").field("severity")).execute().actionGet();

			Terms terms = sr.getAggregations().get("severity");
			Long numBlockerIssues = terms.getBucketByKey("BLOCKER").getDocCount();
			Long numIssues = sr.getHits().getTotalHits();

			Float value = 1 - (numBlockerIssues.floatValue() / numIssues);
			blockerissues.setValue(value);
		} catch (Throwable th) {
			th.printStackTrace();
			blockerissues.setValue(null);
		}

	}

	public static void evalIssuesWelldefinedJira(Metric welldefinedissuesjira) {

		MetricsProperties mp = MetricsProperties.getInstance();

		// define filter required fields
		BoolQueryBuilder filter = boolQuery();
		String[] requiredFields = mp.getArray("welldefinedissuesjira.required");
		for (String required : requiredFields) {
			filter.must(existsQuery(required));
		}

		String dayrange = mp.get("welldefinedissuesjira.dayrange");
		dayrange = (dayrange.isEmpty()) ? "now-30d" : "now-" + dayrange + "d";

		String[] issuetypes = mp.getArray("welldefinedissuesjira.issuetypes");

		TransportClient tc = Client.getClient();

		try {
			SearchResponse sr = null;

			sr = tc.prepareSearch(jiraIndex).setSize(0)
				.setQuery(QueryBuilders.boolQuery().must(termQuery("projectKey", jiraProjectKey)).must(rangeQuery("created").gte(dayrange)).must(termsQuery("issuetype", issuetypes)))
				.addAggregation(AggregationBuilders.filter("wellDefined", filter))
				.execute().actionGet();

			Long totalHits = sr.getHits().getTotalHits();
			Filter flt = sr.getAggregations().get("wellDefined");
			Long wellDefined = flt.getDocCount();

			if (totalHits == 0) {
				welldefinedissuesjira.setValue(0f);
			} else {
				Float value = wellDefined.floatValue() / totalHits;
				welldefinedissuesjira.setValue(value);
			}

		} catch (Throwable th) {
			th.printStackTrace();
			welldefinedissuesjira.setValue(null);
		}

	}
	
	public static void evalIssuesWelldefinedRedmine(Metric welldefinedissuesredmine) {

		MetricsProperties mp = MetricsProperties.getInstance();

		// define filter required fields
		BoolQueryBuilder filter = boolQuery();
		String[] requiredFields = mp.getArray("welldefinedissuesredmine.required");
		for (String required : requiredFields) {
			filter.must(existsQuery(required));
		}

		String dayrange = mp.get("welldefinedissuesredmine.dayrange");
		dayrange = (dayrange.isEmpty()) ? "now-30d" : "now-" + dayrange + "d";

		String[] trackertypes = mp.getArray("welldefinedissuesredmine.trackertypes");

		TransportClient tc = Client.getClient();

		try {
			SearchResponse sr = null;

			sr = tc.prepareSearch(redmineIndex).setSize(0)
				.setQuery(QueryBuilders.boolQuery().must(termQuery("project", redmineProject)).must(rangeQuery("created_on").gte(dayrange)).must(termsQuery("tracker", trackertypes)))
				.addAggregation(AggregationBuilders.filter("wellDefined", filter))
				.execute().actionGet();

			Long totalHits = sr.getHits().getTotalHits();
			Filter flt = sr.getAggregations().get("wellDefined");
			Long wellDefined = flt.getDocCount();

			if (totalHits == 0) {
				welldefinedissuesredmine.setValue(0f);
			} else {
				Float value = wellDefined.floatValue() / totalHits;
				welldefinedissuesredmine.setValue(value);
			}

		} catch (Throwable th) {
			th.printStackTrace();
			welldefinedissuesredmine.setValue(null);
		}

	}

	public static void evalNonBugDensityJira(Metric nonbugdensity) {

		MetricsProperties mp = MetricsProperties.getInstance();

		String[] statuses = mp.getArray("bugsratiojira.status.unresolved");
		String bugIssueType = mp.get("bugsratiojira.bug.issuetype");

		TransportClient tc = Client.getClient();

		try {
			SearchResponse sr = tc.prepareSearch(jiraIndex).setSize(0)
					.setQuery(QueryBuilders.boolQuery().must(termQuery("projectKey", jiraProjectKey)).must(termsQuery("status", statuses)))
					.addAggregation(AggregationBuilders.filter("openbugs", QueryBuilders.termQuery("issuetype", bugIssueType)))
					.execute().actionGet();

			Long totalHits = sr.getHits().getTotalHits();
			Filter flt = sr.getAggregations().get("openbugs");
			Long openbugs = flt.getDocCount();

			Float value = (totalHits.floatValue() - openbugs) / totalHits;

			nonbugdensity.setValue(value);
		} catch (Throwable th) {
			th.printStackTrace();
			nonbugdensity.setValue(null);
		}

	}
	

	public static void main(String[] args) {

		String evaluationDate = formatYMD.format(new Date());
		eval(evaluationDate);

	}

}
