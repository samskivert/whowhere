//
// $Id$

package whowhere.logic;

import java.util.Date;
import java.util.Hashtable;

import com.samskivert.servlet.user.*;
import com.samskivert.servlet.util.DataValidationException;
import com.samskivert.servlet.util.ParameterUtil;
import com.samskivert.velocity.*;

import whowhere.WhoWhere;
import whowhere.data.*;

public class newtrip implements Logic
{
    public void invoke (Application app, InvocationContext ctx)
        throws Exception
    {
        UserManager usermgr = ((WhoWhere)app).getUserManager();
	User user = usermgr.requireUser(ctx.getRequest());
        String errmsg = null;

	// if they've submitted the form, we create a new trip and stick
	// it into the dataabse
	if (ParameterUtil.parameterEquals(
            ctx.getRequest(), "action", "create")) {
	    Trip trip = new Trip();

	    // set the travelerid from the userid of the calling user
	    trip.travelerid = user.userId;

	    // parse our fields
	    trip.destination = ParameterUtil.requireParameter(
                ctx.getRequest(), "destination",
                "newtrip.error.missing_destination");
	    Date date = ParameterUtil.requireDateParameter(
                ctx.getRequest(), "begins", "newtrip.error.invalid_begins");
	    trip.begins = new java.sql.Date(date.getTime());
	    date = ParameterUtil.requireDateParameter(
                ctx.getRequest(), "ends", "newtrip.error.invalid_ends");
	    trip.ends = new java.sql.Date(date.getTime());
	    trip.description = ParameterUtil.getParameter(
                ctx.getRequest(), "description", false);

	    // check those new dates for sense and sensibility
	    if (trip.begins.compareTo(trip.ends) > 0) {
		throw new DataValidationException(
                    "newtrip.error.ends_before_begins");
	    }

	    // insert the trip into the repository
            TripRepository rep = ((WhoWhere)app).getRepository();
	    rep.insertTrip(trip);

	    // let the user know we updated
	    errmsg = "newtrip.message.trip_created";

	    // also cause the trip creation form not to be displayed
	    ctx.put("noform", "true");
	}

        // handle any message that was generated
        if (errmsg != null) {
            ctx.put("error", app.translate(ctx, errmsg));
        }
    }
}
