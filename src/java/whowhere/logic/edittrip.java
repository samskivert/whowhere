//
// $Id$

package whowhere.logic;

import java.util.Date;

import org.webmacro.servlet.WebContext;
import com.samskivert.webmacro.*;
import com.samskivert.servlet.user.*;

import whowhere.WhoWhere;
import whowhere.data.*;

public class edittrip implements Logic
{
    public void invoke (Application app, WebContext ctx)
        throws Exception
    {
        UserManager usermgr = ((WhoWhere)app).getUserManager();
	User user = usermgr.requireUser(ctx.getRequest());
        String errmsg = null;

        // look up the specified trip
	int tripid = FormUtil.requireIntParameter(
            ctx, "tripid", "edittrip.error.missing_trip_id");
        TripRepository rep = ((WhoWhere)app).getRepository();
	Trip trip = rep.getTrip(tripid);
	if (trip == null) {
            throw new DataValidationException(
                "edittrip.error.no_such_trip");
	}

	// make sure this user owns this trip
	if (user.userid != trip.travelerid) {
            throw new DataValidationException(
                "edittrip.error.not_owner_of_trip");
	}

	// if we're submitting edits, modify the trip object and store it
	// back into the database
	if (FormUtil.equals(ctx, "action", "update")) {
	    // insert the trip into the context so that it will be
	    // displayed to the user
	    ctx.put("trip", trip);

	    // parse our updated fields
	    trip.destination = FormUtil.requireParameter(
                ctx, "destination", "edittrip.error.missing_destination");
	    Date date = FormUtil.requireDateParameter(
                ctx, "begins", "edittrip.error.invalid_begins");
	    trip.begins = new java.sql.Date(date.getTime());
	    date = FormUtil.requireDateParameter(
                ctx, "ends", "edittrip.error.invalid_ends");
	    trip.ends = new java.sql.Date(date.getTime());
            trip.description = FormUtil.getParameter(
                ctx, "description", false);

	    // check those new dates for sense and sensibility
	    if (trip.begins.compareTo(trip.ends) > 0) {
		throw new DataValidationException(
                    "edittrip.error.ends_before_begins");
	    }

	    // update the trip in the repository
	    rep.updateTrip(trip);

	    // let the user know we updated
	    errmsg = "edittrip.message.trip_updated";

	} else if (FormUtil.equals(ctx, "action", "delete")) {
	    // remove the trip from the repository
	    rep.deleteTrip(trip);

	    // let the user know what we did
	    errmsg = "edittrip.message.trip_deleted";

	} else {
	    // insert the trip into the context so that it will be
	    // displayed to the user
	    ctx.put("trip", trip);
	}

        // handle any message that was generated
        if (errmsg != null) {
            ctx.put("error", app.translate(ctx, errmsg));
        }
    }
}
