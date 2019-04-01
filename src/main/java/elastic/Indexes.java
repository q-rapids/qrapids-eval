package elastic;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;



public class Indexes {
	
	// Singleton
	private static final Indexes instance = new Indexes();
	
	private String indexPropertiesFile = "index.properties";
	
	// Targets
	private String metricsIndex;
	private String metricsIndexType;
	private String factorsIndex;
	private String factorsIndexType;
	private String indicatorsIndex;
	private String indicatorsIndexType;
	
	// Sources
	private String sonarqubeIssuesIndex;
	private String sonarqubeMeasuresIndex;
	
	private String sonarqubeProjectId;
	private String sonarqubeProjectIdField;

	private String snapshotDate;

	private String jiraIndex;
	private String jiraProjectKey;
	
	private String jenkinsIndex;
	private String jenkinsJobname;
	
	private String redmineIndex;
	private String redmineProject;


	private String elasticsearchServerIp;
	
	private Properties indexProps = new Properties();
	InputStream input = null;
	
	
	private Indexes()  {
		
		indexProps = new Properties();
		try {
			input = new FileInputStream(indexPropertiesFile);
			indexProps.load(input);
			
			elasticsearchServerIp = indexProps.getProperty("elasticsearch.server.ip");
			
			metricsIndex = indexProps.getProperty("metrics.index");
			factorsIndex = indexProps.getProperty("factors.index");
			indicatorsIndex = indexProps.getProperty("indicators.index");
			
			metricsIndexType = indexProps.getProperty("metrics.index.type");
			factorsIndexType = indexProps.getProperty("factors.index.type");
			indicatorsIndexType = indexProps.getProperty("indicators.index.type");
			
			sonarqubeIssuesIndex = indexProps.getProperty("sonarqube.issues.index");
			sonarqubeMeasuresIndex = indexProps.getProperty("sonarqube.measures.index");
			
			snapshotDate = indexProps.getProperty("sonarqube.snapshotDate");
			sonarqubeProjectId = indexProps.getProperty("sonarqube.projectId");
			sonarqubeProjectIdField = indexProps.getProperty("sonarqube.projectId.field");
			
			jiraIndex = indexProps.getProperty("jira.index");
			jiraProjectKey = indexProps.getProperty("jira.projectkey");
			
			jenkinsIndex = indexProps.getProperty("jenkins.index");
			jenkinsJobname = indexProps.getProperty("jenkins.jobname");
			
			redmineIndex = indexProps.getProperty("redmine.index");
			redmineProject = indexProps.getProperty("redmine.project");
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static Indexes getInstance() {
		return instance;
	}
	
	public String getElasticsearchServerIp() {
		return elasticsearchServerIp;
	}

	public String getMetricsIndex() {
		return metricsIndex;
	}

	public void setMetricsIndex(String metricsIndex) {
		this.metricsIndex = metricsIndex;
	}

	public String getFactorsIndex() {
		return factorsIndex;
	}

	public String getIndicatorsIndex() {
		return indicatorsIndex;
	}

	public String getMetricsIndexType() {
		return metricsIndexType;
	}

	public String getFactorsIndexType() {
		return factorsIndexType;
	}

	public String getIndicatorsIndexType() {
		return indicatorsIndexType;
	}

	public String getSonarqubeIssuesIndex() {
		return sonarqubeIssuesIndex;
	}

	public String getSonarqubeMeasuresIndex() {
		return sonarqubeMeasuresIndex;
	}
	
	public String getSnapshotDate() {
		return snapshotDate;
	}
	
	public String getSonarqubeProjectId() {
		return sonarqubeProjectId;
	}
	
	
	public String getSonarqubeProjectIdField() {
		return sonarqubeProjectIdField;
	}

	public void setSonarqubeProjectIdField(String sonarqubeProjectIdField) {
		this.sonarqubeProjectIdField = sonarqubeProjectIdField;
	}

	public String getJiraIndex() {
		return jiraIndex;
	}

	public String getJiraProjectKey() {
		return jiraProjectKey;
	}

	public String getJenkinsIndex() {
		return jenkinsIndex;
	}

	public String getJenkinsJobname() {
		return jenkinsJobname;
	}

	public String getRedmineIndex() {
		return redmineIndex;
	}

	public String getRedmineProject() {
		return redmineProject;
	}

	public static void main(String[] args) throws IOException {
		Indexes i = new Indexes();
		System.out.println(i.getFactorsIndex());
		System.out.println(i.getFactorsIndexType());
		
		System.out.println(i.getIndicatorsIndex());
		System.out.println(i.getIndicatorsIndexType());
		
		System.out.println(i.getMetricsIndex());
		System.out.println(i.getMetricsIndexType());
		
		System.out.println(i.getSonarqubeIssuesIndex());
		System.out.println(i.getSonarqubeMeasuresIndex());
		System.out.println();
		
	}

}
