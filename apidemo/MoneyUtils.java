package apidemo;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import com.ib.controller.NewContract;



public class MoneyUtils {
	private MoneyUtils()  { }
	private final static String mainPropertiesFileName = "main.properties";
	
	private static class SingletonHolder {
		private static final MoneyUtils shared = new MoneyUtils();
	}

	public static MoneyUtils shared() {
		return SingletonHolder.shared;
	}	
	
	public static Map<String,String> readMainProperties() {
		Map<String, String> dictionary = new HashMap<String, String>();
		
		Properties prop = new Properties();
		InputStream input = null;
	 
		try {
	 
			input = new FileInputStream(mainPropertiesFileName);
	 
			// load a properties file
			prop.load(input);
			
			Enumeration<?> e = prop.propertyNames();
			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				String value = prop.getProperty(key);
				System.out.println("Reading Properties:Key : " + key + ", Value : " + value);
				
				dictionary.put(key, value);
			}
	 
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}	
		return dictionary;	
	}

	public static void writeMainProperties(Map<String,String> map)
	{
		Properties prop = new Properties();
		OutputStream output = null;
	 
		try {
	 
			output = new FileOutputStream(mainPropertiesFileName);
			for (Map.Entry<String, String> entry : map.entrySet()) {
			    String key = entry.getKey();
			    Object value = entry.getValue();
			    
			    prop.setProperty(key, (String)value);
			    System.out.println("Writing Properties:Key : " + key + ", Value : " + value);
			}
			/*
			Enumeration<?> e = prop.propertyNames();
			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				String value = prop.getProperty(key);
				System.out.println("Key : " + key + ", Value : " + value);
			}
	 		*/ 
			prop.store(output, null);  //Save Properties
	 
		} catch (IOException io) {
			io.printStackTrace();
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
