package elastic.model;

import java.util.HashMap;
import java.util.Map;

public class Metric {

	private Boolean enabled;

	private String metric;
	private String name;
	private String description;
	private String[] factors;
	private String evaluationDate;
	private Float value;
	// datasource, to enable drill down in the elasticsearch index
	private String datasource;
	// source, it indicates the source from where the metric is coming, for those
	// cases in which the same metric may come from diverse sources. E.g., issue
	// tracking system: JIRA or Redmine
	private String source;

	public Metric(Boolean enabled, String metric, String name, String description, String[] factors,
			String evaluationDate, Float value, String datasource, String source) {

		this.enabled = enabled;
		this.metric = metric;
		this.name = name;
		this.description = description;
		this.factors = factors;
		this.evaluationDate = evaluationDate;
		this.value = value;
		this.datasource = datasource;
		this.source = source;
	}

	public String getId() {
		return this.metric + "-" + evaluationDate;
	}

	public Map<String, Object> getMap() {
		Map<String, Object> result = new HashMap<String, Object>();

		result.put("metric", metric);
		result.put("name", name);
		result.put("description", description);
		result.put("factors", factors);
		result.put("evaluationDate", evaluationDate);
		result.put("value", value);
		result.put("datasource", datasource);

		return result;
	}

	public Float getValue() {
		return value;
	}

	public void setValue(Float value) {
		this.value = value;
	}

	public void setEvaluationDate(String evaluationDate) {
		this.evaluationDate = evaluationDate;
	}

	public String getSource() {
		return source;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public String getName() {
		return name;
	}

	public String getMetric() {
		return metric;
	}

}
