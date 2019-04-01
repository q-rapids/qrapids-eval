package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class FileUtils {

	public static String readFile(File f)  {
		InputStream is;
		try {
			is = new FileInputStream(f);
			java.util.Scanner s = new java.util.Scanner(is);
			s.useDelimiter("\\A");
			String template = s.next();
			s.close();
			
			return template;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static Properties loadProperties( File f ) {
		Properties props = new Properties();

		try {
			props.load( new FileInputStream(f) );
			return props;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return props;

	}
	
	
}
