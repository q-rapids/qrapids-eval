package type;

import java.util.Map;

public abstract class IndexItem {

	protected boolean enabled;
	
	protected String project;
	protected String id;
	protected String evaluationDate;
	
	protected String [] parents;
	protected Double [] weights;
	protected String name;
	protected String description;
	protected String datasource;

	protected Double value;
	protected String info;
	
	public static final String ON_ERROR_DROP = "drop";
	public static final String ON_ERROR_SET0 = "set0";
	protected String onError = ON_ERROR_DROP;
	
	public String getElasticId() {
		return this.project + "-" + this.id + "-" + this.evaluationDate;
	};
	
	public abstract String getType();
	
	public abstract Map<String, Object> getMap() ;
	
	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	public String getProject() {
		return project;
	}
	
	public void setProject(String project) {
		this.project = project;
	}
	
	public String getEvaluationDate() {
		return evaluationDate;
	}
	
	public void setEvaluationDate(String evaluationDate) {
		this.evaluationDate = evaluationDate;
	}

	public String[] getParents() {
		return parents;
	}

	public void setParents(String[] parents) {
		this.parents = parents;
	}

	public Double[] getWeights() {
		return weights;
	}

	public void setWeights(Double[] weights) {
		this.weights = weights;
	}

	public String getDatasource() {
		return datasource;
	}

	public void setDatasource(String datasource) {
		this.datasource = datasource;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getInfo() {
		return info;
	}
	
	public void setInfo(String info) {
		this.info = info;
	}
	
	public Double getValue() {
		return value;
	}
	
	public void setValue(Double value) {
		this.value = value;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String onError() {
		return onError;
	}

	public Boolean onErrorDrop() {
		return onError.equals(ON_ERROR_DROP);
	}
	
	public Boolean onErrorSet0() {
		return onError.equals(ON_ERROR_SET0);
	}
	
}
