//
// $Id$

package whowhere.logic;

import java.util.Date;

import org.webmacro.servlet.WebContext;
import com.samskivert.webmacro.*;
import com.samskivert.servlet.user.*;

import whowhere.*;
import whowhere.data.*;

public class edittrip implements Logic
{
    public void invoke (WebContext context) throws Exception
    {
	User user = WhoWhere.usermgr.requireUser(context.getRequest());
	int tripid = FormUtil.requireIntParameter(context, "tripid",
						  "err.missing_trip_id");

	Trip trip = WhoWhere.repository.getTrip(tripid);
	if (trip == null) {
	    context.put("error", "err.no_such_trip");
	    return;
	}

	// make sure this user owns this trip
	if (user.userid != trip.travelerid) {
	    context.put("error", "err.not_owner_of_trip");
	    return;
	}

	// if we're submitting edits, modify the trip object and store it
	// back into the database
	if (FormUtil.equals(context, "action", "update")) {
	    // insert the trip into the context so that it will be
	    // displayed to the user
	    context.put("trip", trip);

	    // parse our updated fields
	    trip.destination =
		FormUtil.requireParameter(context, "destination",
					  "err.missing_destination");
	    Date date = 
		FormUtil.requireDateParameter(context, "begins",
					      "err.invalid_begins");
	    trip.begins = new java.sql.Date(date.getTime());
	    date =
		FormUtil.requireDateParameter(context, "ends",
					      "err.invalid_ends");
	    trip.ends = new java.sql.Date(date.getTime());

	    // check those new dates for sense and sensibility
	    if (trip.begins.compareTo(trip.ends) > 0) {
		throw new DataValidationException("err.ends_before_begins");
	    }

	    // update the trip in the repository
	    WhoWhere.repository.updateTrip(trip);

	    // let the user know we updated
	    context.put("error", "msg.trip_updated");

	} else if (FormUtil.equals(context, "action", "delete")) {
	    // remove the trip from the repository
	    WhoWhere.repository.deleteTrip(trip);

	    // let the user know what we did
	    context.put("error", "msg.trip_deleted");

	} else {
	    // insert the trip into the context so that it will be
	    // displayed to the user
	    context.put("trip", trip);
	}
    }
}
