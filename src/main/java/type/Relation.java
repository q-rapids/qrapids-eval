package type;

import java.util.HashMap;
import java.util.Map;

public class Relation extends IndexItem {

	private IndexItem source;
	private IndexItem target;
	private Double weight;

	public Relation(
			String project, 
			IndexItem source, 
			IndexItem target, 
			String evaluationDate,
			Double value,
			Double weight
		) {
		this.id = source.id + "->" + target.id;
		this.project = project;
		this.source = source;
		this.target = target;
		this.evaluationDate = evaluationDate;
		this.value = value;
		this.weight = weight;
	}
	
	public Map<String, Object> getMap() {
		Map<String, Object> result = new HashMap<String, Object>();

		result.put("relation", getElasticId() );
		
		result.put("project", project);
		
		result.put("sourceType", source.getType());
		result.put("sourceId", source.getElasticId() );
		
		result.put("targetType", target.getType());
		result.put("targetId", target.getElasticId() );
		
		result.put("value", value);
		result.put("weight", weight);

		result.put("evaluationDate", evaluationDate);

		return result;
	}
	
	@Override
	public String getType() {
		return "relations";
	}
	
	public String getElasticId() {
		return project + "-" + source.id + "->" + target.id + "-" + evaluationDate;
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

	public Double getWeight() {
		return weight;
	}

	public void setWeight(Double weight) {
		this.weight = weight;
	}

	public Double getValue() {
		return value;
	}

	public void setValue(Double value) {
		this.value = value;
	}


}
