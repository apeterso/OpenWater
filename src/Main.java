package openwater;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;


/**
 * The OpenWater application pulls surface water temperature data from an RSS 
 * feed published by the National Oceanic and Atmospheric Administration and 
 * emails it to a list of willing recipients. The temperatures can be sent in 
 * either Fahrenheit or centigrade. Utilizes the javax.mail library for email 
 * sending functionality.
 * 
 * @author Anders Peterson
 * 6/2018
 */
public class Main {
    
    private static Scanner scanner = new Scanner(System.in);
    private static ArrayList<TempReading> temperatures = new ArrayList<>();
    private static ArrayList<Person> recipients = new ArrayList<>();
    // Username and password for the gmail account that will send out the emails
    private static final String username = ""; //redacted
    private static final String password = ""; //redacted

    public static void main(String[] args) throws AddressException,
            MessagingException {
        readWeatherRSS();
        
        //Begin the text-based user interface
        System.out.println("Welcome to the water temp tool");
        System.out.println("Add recipients in the following format:");
        System.out.println("<first name> <email address>");
        String enter = scanner.nextLine();
        
        /* Take inputs until the user is done, signified by the user simply
         * pressing 'enter' without inputing any text. */
        while(!enter.equals("")){
            if(enter.contains(" ")){
                String name = enter.substring(0,enter.indexOf(" "));
                String email = enter.substring(enter.indexOf(" "),
                        enter.length());
                /* Briefly and uncomprehensively check the email address. The
                 * sendTemperature method later on will handle an invalid mail
                 * server or nonexistant email address. */
                while(!(email.contains("@") && email.contains("."))
                        || email.contains(" ")){
                    System.out.println("Invalid email address, please enter a"
                            + " valid email address");
                    email = scanner.nextLine();
                }
                Person p = new Person(name,email);
                System.out.println("Which states would you like to see? List as"
                        + " two letter abreviation (e.g. CA, IL, ME, etc). Just"
                        + " press enter when done");
                String state = scanner.nextLine();
                /* Add states to the Person object's locations field until the
                 * user is done, signified by simply pressing 'enter' without
                 * inputing any text. */
                while(!state.equals("")){
                    if(state.length()==2){p.addWholeState(temperatures, state);}
                    else{System.out.println("Please enter a valid state");}
                    state = scanner.nextLine();
                }
                recipients.add(p);
                System.out.println("Press enter if done or add another person"
                        + " (<name> <emailaddress>)");
            } else{
                System.out.println("Please enter the name and valid email"
                        + " address in the following format <first name> <email"
                        + " address>");
            }
            enter = scanner.nextLine();
        }
        // Send everyone their water temperature data!
        //sendTemperature();
    }
    
    /*
     * The readWeatherRSS method pulls data from he National Oceanic and
     * Atmospheric Administration (NOAA)'s Coastal WAter Temperature guide and
     * then breaks it down into a format that the OpenWater application can use.
     * Most of it is hardcoded and only works with this specific xml file.
     * 
     * Wishlist: Functionality to handle any xml file.
     * 
     * Data pulled from here: https://www.nodc.noaa.gov/dsdt/cwtg/rss/all.xml
     */ 
    public static void readWeatherRSS(){
        try{
            URL weatherUrl = new URL(
                    "https://www.nodc.noaa.gov/dsdt/cwtg/rss/all.xml");
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(weatherUrl.openStream()));
            String sourceCode = "";
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
                    pubDate = pubDate.substring(pubDate.indexOf("<pubDate>")+9);
                    pubDate = pubDate.substring(0,pubDate.length()-10);
                    in.readLine();
                    
                    String temp = in.readLine();
                    temp = temp.substring(temp.indexOf("</strong>") + 9).trim();
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
     * The sendTemperature method sends an email to each person that contains\
     * the water temperatures they would like to see.
     * @throws MessagingException 
     */
    public static void sendTemperature() throws MessagingException{
        for(Person p: recipients){    
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
                        "anderspeterson11@gmail.com"));
                message.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(p.getEmail()));
                message.setSubject("Today's Temperatures");
                
                
                /*Populate the body of the email with the water temperatures and
                publish dates.*/
                String report = "";
                for(TempReading tr: p.getLocations()){
                    report += (tr.getLocationName() + "\n");
                    report += ("Water temp " + tr.getFTemp() + " degrees"
                            + " Fahrenheit" + "\n");
                    report += (tr.pubDate() + "\n");
                    report += ("\n" + "\n");
                }
                message.setText("Hey " + p.getName() + ","+ "\n" + "\n" +
                        "Here are your water temperatures: " + "\n" + report);
                //Send the email
                Transport.send(message);
                
            } catch (AddressException ae){
                System.out.println("address exception");
            }
        }
    }
    
    /**
     * A method used for testing. The populateRecipients method adds two
     * Person objects with hardcoded fields to the recipients field.
     */
    public static void populateRecipients(){
        Person ders = new Person(""/*redacted*/, ""/*redacted*/);
        ders.addWholeState(temperatures, "CA");
        ders.addWholeState(temperatures, "NY");
        recipients.add(ders);
        Person will = new Person(""/*redacted*/, ""/*redacted*/);
        will.addWholeState(temperatures, "CA");
        will.addWholeState(temperatures, "MD");
        recipients.add(will);
        
        for(Person p: recipients){
            System.out.println(p.getName() + "\n" + p.getEmail());
            System.out.println("Weather temps for " + p.getName() + "\n");
            for(TempReading tr: p.getLocations()){
                System.out.println(tr.getLocationName());
                System.out.println(tr.getCTemp());
                System.out.println("--------");
            }
        }
    }
    
    /**
     * A method used for testing. The listRecipients method prints the name,
     * email address, and locations for each person in the recipients field.
     */
    public static void listRecipients(){
        for(Person p: recipients){
            System.out.println("name: " + p.getName());
            System.out.println("email: " + p.getEmail());
            for(TempReading tr: p.getLocations()){
                System.out.println(tr.getLocationName());
            }
            System.out.println("------------");
        }
    }
}
