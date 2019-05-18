package newptemp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Scanner;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 *
 * @author andy
 */
public class Main {

    private static final String username = "igotdarighttemperature@gmail.com";
    private static final String password = "";//redacted
    private static ArrayList<TempReading> temperatures = new ArrayList<>();
    private static ArrayList<Person> people = new ArrayList<>();
    private static File data;

    /**
     * The OpenWater application pulls surface water temperature data from an
     * RSS feed published by the National Oceanic and Atmospheric
     * Administration and emails it to a list of willing recipients. The
     * temperatures can be sent in either Fahrenheit or centigrade. Utilizes
     * the javax.mail library for email sending functionality.
     * 
     * @param args takes a .csv file containing name, email, and location info
     * @throws MessagingException
     * @throws FileNotFoundException
     */
    public static void main(String[] args)
    		throws MessagingException,FileNotFoundException {
    	readWeatherRSS();
    	data = new File(args[0]);
    	Scanner scanner = new Scanner(data);
    	scanner.nextLine();
    	while(scanner.hasNext()) {
    		String row = scanner.nextLine();
    		String[] rowValues = row.split(",");
			ArrayList<String> personVals = new ArrayList();
    		for(String s : rowValues) {
    			personVals.add(s);
    		}
    		Person p = new Person(personVals.remove(0), personVals.remove(0),
    				personVals.remove(0));
    		while(!personVals.isEmpty()) {
    			String placeName = personVals.remove(0).trim();
    			placeName = placeName.replace("\"", "");
    			p.addLocation(getTempReading(placeName));
    		}
    		people.add(p);
    	}
        sendTemperature();
        
    }
    
    /**
     * The readWeatherRSS method pulls data from he NOAA's Coastal Water
     * Temperature guide and then breaks it down into a format that the
     * OpenWater application can use. Most of it is hardcoded and only works
     * with this specific xml file.
     * 
     * Wishlist: Functionality to handle any XML file.
     * 
     * Data pulled from here: https://www.nodc.noaa.gov/dsdt/cwtg/rss/all.xml
     */ 
    public static void readWeatherRSS(){
        try{
            URL weatherUrl = new URL(
                    "https://www.nodc.noaa.gov/dsdt/cwtg/rss/all.xml");
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(weatherUrl.openStream()));
            String line = in.readLine();
            line = in.readLine();
            while((line=in.readLine()) != null){
                if(line.contains("<item>")){
                    line = in.readLine();
                    String locationName = line.substring(line.indexOf("<title>")
                            + 7);
                    locationName = locationName.substring(0,
                            locationName.indexOf("</title>"));
                    
                    String pubDate = in.readLine();
                    pubDate = pubDate.substring(pubDate
                    		.indexOf("<pubDate>")+9);
                    pubDate = pubDate.substring(0,pubDate.length()-10);
                    in.readLine();
                    
                    String temp = in.readLine();
                    temp = temp.substring(temp
                    		.indexOf("</strong>") + 9).trim();
                    temp = temp.substring(0,4);
                    double temperature = Double.parseDouble(temp);
                    
                    addTempReading(locationName, pubDate, temperature); 
                }
            }
        } catch (MalformedURLException ue){
            System.out.println("Malformed URL");
        } catch (IOException ieo){
            System.out.println("Something went wrong with reading the"
                + " contents");
        }
    }
    
    /**
     * Creates a new TempReading object and adds it to the temperatures field,
     * which is an ArrayList of TempReading objects
     * @param locName   A String representing the name of the location from
     * which the temperature is being drawn from.
     * @param pubDate   A String representing the date when the water
     * temperature was drawn.
     * @param fTemp     A double representing the Fahrenheit temperature of the
     * water at the given location.
     */
    public static void addTempReading(String locName, String pubDate,
            double fTemp){
        TempReading tr = new TempReading(locName, pubDate, fTemp);
        temperatures.add(tr);
    }
    
    /**
     * Returns a TempReading object from the master temperatures list based on
     * the locname String object passed in.
     * @param locName	The name of a certain location
     * @return	A TempReading object corresponding to that name.
     */
    public static TempReading getTempReading(String locName) {
    	for(TempReading tr : temperatures) {
    		if(tr.getLocationName().contains(locName)) {
    			return tr;
    		}
    	}
    	return null;
    }
    
    /**
     * The sendTemperature method sends an email to each person that contains
     * the water temperatures they would like to see.
     * @throws MessagingException 
     */
    public static void sendTemperature() throws MessagingException{
    	for(Person p : people) {
	        try{
	            /* Prepare the email using a gmail mail server. The mail server
	             * used by the application will eventually be a personally
	             * owned server. */
	            Properties props = new Properties();
	            props.put("mail.smtp.auth", "true");
	            props.put("mail.smtp.starttls.enable", "true");
	            props.put("mail.smtp.host", "smtp.gmail.com");
	            props.put("mail.smtp.port", "587");
	
	            Session session = Session.getInstance(props,
	              new javax.mail.Authenticator() {
	                protected PasswordAuthentication getPasswordAuthentication()
	                {
	                    return new PasswordAuthentication(username, password);
	                }
	              });
	
	            Message message = new MimeMessage(session);
	            message.setFrom(new InternetAddress(
	                    "igotdarighttemperature@gmail.com"));
	            message.setRecipients(Message.RecipientType.TO,
	                    InternetAddress.parse(p.getEmail()));
	            message.setSubject("Today's Water Temperatures");
	
	
	            /*Populate the body of the email with the water temps*/
	            String report = ("Hi " + p.getFirstName() + "!" + "\n" + "\n");
	            report += ("Here are your water temperatures for today!" + "\n");
	            for(TempReading tr: p.getLocations()) {
	            	report += (tr.getLocationName() + ": ");
	            	report += (tr.getFTemp() + "\u00b0" + "F" + "\n");
	            }
	            report += "\n";
	            report += "Have a great swim!";
	            message.setText(report);
	            //Send the email
	            Transport.send(message);
	
	        } catch (AddressException ae){
	            System.out.println("address exception");
	        }
    	}
    }
}

