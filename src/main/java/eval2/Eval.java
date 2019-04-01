package eval2;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import util.FileUtils;

public class Eval {
	
	private static Logger log = Logger.getLogger("eval2.Eval");
	
	private static final String PROJECTS_DIR = "./projects";
	private static final File PROJECTS_FOLDER = new File(PROJECTS_DIR);
	
	private static List<String> evaluationDates = new ArrayList<>();
	
	private static Date fromDate;
	private static Date toDate;
	
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	
	
	public static void main(String[] args) {
		
		if ( args.length != 0 && args.length != 2 && args.length != 4 ) {
			usage();
			return;
		}
		
		if ( args.length == 0 ) {
			evaluationDates.add( dateFormat.format(new Date()) );
		}
			
		if ( args.length == 2 ) {
			if ( args[0].equals("evaluationDate") ) {
				try {
					dateFormat.parse(args[1]);
					evaluationDates.add( args[1] );
					log.info("Using user-defined evaluationDate: " + args[1]);
				} catch (ParseException e) {
					usage();
					return;
				}
			} else {
				usage();
				return;
			}
		}
		
		if ( args.length == 4 ) {
			if ( args[0].equals("from") && args[2].equals("to") ) {
				try {
					fromDate = dateFormat.parse(args[1]);
					toDate = dateFormat.parse(args[3]);
					
					evaluationDates.addAll( enumeratePeriod(fromDate, toDate) );
					
					log.info("Using user-defined evaluation period: " + dateFormat.format( fromDate ) + " - " + dateFormat.format( toDate ) );
				} catch (ParseException e) {
					usage();
					return;
				}
			} else {
				usage();
				return;
			}
		}

		List<File> projectFolders = getProjectFolders();
		
		for ( File projectDir : projectFolders ) {
			for ( String ed : evaluationDates ) {
				EvalProject ep = new EvalProject(projectDir, ed);
				ep.run();
			}
		}
		
		notifyDashboard();
		
	}
	
	private static List<String> enumeratePeriod(Date fromDate, Date toDate) {

		Calendar c = Calendar.getInstance();
		  // number of days to add
		
		List<String> days = new ArrayList<>();
		
		Date i = fromDate;
		while ( i.compareTo(toDate) <= 0 ) {
			days.add( dateFormat.format(i) );
			c.setTime(i);
			c.add(Calendar.DATE, 1);
			i = c.getTime();
		}
		
		return days;
	}
	
	private static void usage() {
		System.out.println("Usage:");
		System.out.println("java -jar qr-eval.jar");
		System.out.println("java -jar qr-eval.jar evaluationDate 2019-01-31");
		System.out.println("java -jar qr-eval.jar from 2019-01-01 to 2019-01-31");
	}


	private static int notifyDashboard() {
		
		File f = new File("./projects/eval.properties");
		Properties props = FileUtils.loadProperties(f);
		String notificationURL = props.getProperty("dashboard.notification.url");
				
		URL obj;
		try {
			
			obj = new URL(notificationURL);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			// optional default is GET
			con.setRequestMethod("GET");

			return  con.getResponseCode();
			
		} catch (Exception e) {
			// e.printStackTrace();
			log.warning( "Could not nofify dashboard, URL=" + notificationURL + ".\n" );
			return 1;
		} 
		
	}


	private static List<File> getProjectFolders() {
		
		List<File> result = new ArrayList<>();
		
		for ( File p : PROJECTS_FOLDER.listFiles() ) {
			if ( p.isDirectory() ) {
				result.add(p);
			}
		}

		return result;
	}

}
