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
    public void invoke (Application app, WebContext ctx)
        throws Exception
    {
        UserManager usermgr = ((WhoWhere)app).getUserManager();
	User user = usermgr.requireUser(ctx.getRequest());
        String errmsg = null;

	// if they've submitted the form, we create a new trip and stick
	// it into the dataabse
	if (FormUtil.equals(ctx, "action", "create")) {
	    Trip trip = new Trip();

	    // set the travelerid from the userid of the calling user
	    trip.travelerid = user.userid;

	    // parse our fields
	    trip.destination = FormUtil.requireParameter(
                ctx, "destination", "newtrip.error.missing_destination");
	    Date date = FormUtil.requireDateParameter(
                ctx, "begins", "newtrip.error.invalid_begins");
	    trip.begins = new java.sql.Date(date.getTime());
	    date = FormUtil.requireDateParameter(
                ctx, "ends", "newtrip.error.invalid_ends");
	    trip.ends = new java.sql.Date(date.getTime());
	    trip.description = FormUtil.getParameter(
                ctx, "description", false);

	    // check those new dates for sense and sensibility
	    if (trip.begins.compareTo(trip.ends) > 0) {
		throw new DataValidationException(
                    "newtrip.error.ends_before_begins");
	    }

	    // insert the trip into the repository
            Repository rep = ((WhoWhere)app).getRepository();
	    rep.insertTrip(trip);

	    // let the user know we updated
	    errmsg = "newtrip.message.trip_created";

	    // also cause the trip creation form not to be displayed
	    ctx.put("noform", "true");

	} else {
	    // create defaults for the form values
	    Hashtable defaults = new Hashtable();
	    defaults.put("travelerid", "1");
            String msg =
                app.translate(ctx, "newtrip.message.default_destination");
	    defaults.put("destination", msg);
            msg = app.translate(ctx, "newtrip.message.default_begins");
	    defaults.put("begins", msg);
            msg = app.translate(ctx, "newtrip.message.default_ends");
	    defaults.put("ends", msg);
	    defaults.put("description", "");
	    ctx.put("Form", defaults);
	}

        // handle any message that was generated
        if (errmsg != null) {
            ctx.put("error", app.translate(ctx, errmsg));
        }
    }
}
