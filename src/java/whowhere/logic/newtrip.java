//
// $Id$

package whowhere.logic;

import java.util.Date;
import java.util.Hashtable;

import org.webmacro.servlet.WebContext;
import com.samskivert.webmacro.*;
import com.samskivert.servlet.user.*;

import whowhere.*;
import whowhere.data.*;

public class newtrip implements Logic
{
    public void invoke (WebContext context) throws Exception
    {
	User user = WhoWhere.usermgr.requireUser(context.getRequest());

	// if they've submitted the form, we create a new trip and stick
	// it into the dataabse
	if (FormUtil.equals(context, "action", "create")) {
	    Trip trip = new Trip();

	    // set the travelerid from the userid of the calling user
	    trip.travelerid = user.userid;

	    // parse our fields
	    trip.destination =
		FormUtil.requireParameter(context, "destination",
					  "error.missing_destination");
	    Date date = 
		FormUtil.requireDateParameter(context, "begins",
					      "error.invalid_begins");
	    trip.begins = new java.sql.Date(date.getTime());
	    date =
		FormUtil.requireDateParameter(context, "ends",
					      "error.invalid_ends");
	    trip.ends = new java.sql.Date(date.getTime());

	    // check those new dates for sense and sensibility
	    if (trip.begins.compareTo(trip.ends) > 0) {
		throw new DataValidationException("error.ends_before_begins");
	    }

	    // insert the trip into the repository
	    WhoWhere.repository.insertTrip(trip);

	    // let the user know we updated
	    context.put("error", "message.trip_created");

	    // also cause the trip creation form not to be displayed
	    context.put("noform", "true");

	} else {
	    // create defaults for the form values
	    Hashtable defaults = new Hashtable();
	    defaults.put("travelerid", "1");
	    defaults.put("destination", "message.default_destination");
	    defaults.put("begins", "message.default_begins");
	    defaults.put("ends", "message.default_ends");
	    context.put("Form", defaults);
	}
    }
}
