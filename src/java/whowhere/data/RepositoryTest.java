//
// $Id$

package whowhere.data;

import java.sql.Date;
import java.util.Calendar;
import java.util.Properties;

public class RepositoryTest
{
    public static void main (String[] args)
    {
	try {
	    Properties props = new Properties();
	    props.put("driver", "org.gjt.mm.mysql.Driver");
	    props.put("url", "jdbc:mysql://localhost:3306/whowhere");
	    props.put("username", "www");
	    props.put("password", "Il0ve2PL@Y");

	    Repository rep = new Repository(props);

	    // insert a trip into the database
//  	    Trip itrip = new Trip();
//  	    itrip.travelerid = 1;
//  	    itrip.destination = "Morocco";
//  	    Calendar cal = Calendar.getInstance();
//  	    cal.set(2000, 11, 27);
//  	    // fucking Calendar. why is getTimeInMillis protected?
//  	    itrip.begins = new Date(cal.getTime().getTime());
//  	    cal.set(2001, 0, 22);
//  	    itrip.ends = new Date(cal.getTime().getTime());
//  	    System.out.println("--> " + itrip);
//    	    rep.insertTrip(itrip);

	    // retrieve and display it
//  	    Trip trip = rep.getTrip(itrip.tripid);
//  	    System.out.println("<-- " + trip);

	    // get all trips by traveler with id 1
//  	    Trip[] trips = rep.getTrips(1);

	    // get all trips happening in 2000
	    Trip[] trips = rep.getTrips(new Date(100, 0, 1),
					new Date(100, 11, 31));
	    for (int i = 0; i < trips.length; i++) {
		System.out.println(trips[i]);
	    }

	    rep.shutdown();

	} catch (Exception e) {
	    e.printStackTrace(System.err);
	}
    }
}
