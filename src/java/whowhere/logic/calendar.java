//
// $Id$

package whowhere.logic;

import java.text.SimpleDateFormat;
import java.util.*;

import com.samskivert.util.IntIntMap;
import com.samskivert.webmacro.*;
import com.samskivert.servlet.user.*;
import org.webmacro.servlet.WebContext;

import whowhere.*;
import whowhere.data.*;

public class calendar implements Logic
{
    public void invoke (WebContext context) throws Exception
    {
	// parse the dates we were given, if we were given any
	String begin = context.getForm("begin");
	String end = context.getForm("end");
	java.sql.Date endingAfter = null, startingBefore = null;

	if (begin != null && end != null) {
	    endingAfter = new java.sql.Date(_qfmt.parse(begin).getTime());
	    if (endingAfter == null) {
		context.put("error", "error.invalid_begin_date");
	    }
	    startingBefore = new java.sql.Date(_qfmt.parse(end).getTime());
	    if (startingBefore == null) {
		context.put("error", "error.invalid_end_date");
	    }
	}

	if (endingAfter == null || startingBefore == null) {
	    Calendar cal = Calendar.getInstance();
	    endingAfter = new java.sql.Date(cal.getTime().getTime());
	    cal.add(Calendar.YEAR, 1);
	    startingBefore = new java.sql.Date(cal.getTime().getTime());
	}

	// stick the dates back into the context for use by the form
	Hashtable form = new Hashtable();
	form.put("begin", _qfmt.format(endingAfter));
	form.put("end", _qfmt.format(startingBefore));
	context.put("Form", form);

	// load up the trips
	Trip[] trips =
	    WhoWhere.repository.getTrips(endingAfter, startingBefore);

	// bail out now if we've got no trips
	if (trips == null) {
	    return;
	}

	// load up our user record
	User user = WhoWhere.usermgr.loadUser(context.getRequest());
	// and put our userid into the context for the display to muck
	// with
	if (user != null) {
	    context.put("userid", new Integer(user.userid));
	} else {
	    context.put("userid", new Integer(-1));
	}

	// sort our trips by start date
	Arrays.sort(trips);

	// figure out how many travelers are involved and assign each
	// traveler to a column
	IntIntMap ttable = new IntIntMap();
	ArrayList tnames = new ArrayList();
	int column = 1; // column zero is for the dates
	for (int i = 0; i < trips.length; i++) {
	    Trip t = trips[i];
	    if (!ttable.contains(t.travelerid)) {
		ttable.put(t.travelerid, column++);
		tnames.add(new Integer(t.travelerid));
	    }
	}

	// convert the traveler ids into names
	int[] userids = new int[tnames.size()];
	for (int i = 0; i < tnames.size(); i++) {
	    userids[i] = ((Integer)tnames.get(i)).intValue();
	}
	UserRepository rep = WhoWhere.usermgr.getRepository();
	String[] names = rep.loadUserNames(userids);
	context.put("names", names);

	// phase one of the processing invovles figuring out how all of
	// the trips overlap and how much vertical space each trip will
	// consume in the calendar
	ArrayList markers = new ArrayList();
	for (int i = 0; i < trips.length; i++) {
	    Trip t = trips[i];
	    int tcol = ttable.get(t.travelerid);
	    // add a marker for the start of the trip
	    Marker start = new Marker(t, t.begins, tcol, null);
	    markers.add(start);
	    // and one for the end of the trip (linked to the start)
	    markers.add(new Marker(null, t.ends, tcol, start));
	}

	// now sort the markers
	Collections.sort(markers);

	// now go in and update the rowspan information for each marker
	// based on the number of rows in between the start and end of
	// each trip
	int rownum = -1, maxrow = -1;
	Date rowdate = new Date(0L); // the beginning of time! (almost)
	int msize = markers.size(); // we'll be adding markers as we go
	for (int i = 0; i < msize; i++) {
	    Marker m = (Marker)markers.get(i);
	    // if we moved to a new date, increment the row
	    if (m.date.compareTo(rowdate) > 0) {
		rownum++;
		rowdate = m.date;
		// and add a row date marker
		markers.add(new Marker(rownum, 1, 0, _mdfmt.format(rowdate)));
	    }
	    // fill in this marker's starting row
	    m.rowstart = rownum;
	    // if this marker is not linked to a start marker, then we're
	    // done with it
	    if (m.start == null) {
		continue;
	    }
	    // otherwise, this is the end of a trip and we want to compute
	    // the span and fill it into the trip's starting marker
	    m.start.rowspan = rownum - m.start.rowstart + 1;
	    // and remove this end marker
	    markers.remove(i--);
	    msize--;
	    // lastly, keep track of our highest row number
	    int endrow = m.start.rowstart + m.start.rowspan;
	    if (endrow > maxrow) {
		maxrow = endrow;
	    }
	}

	// insert spacers between trips to make everything layout properly
	int[] rowpos = new int[ttable.size()];
	msize = markers.size(); // we'll be adding markers as we go
	for (int i = 0; i < msize; i++) {
	    Marker m = (Marker)markers.get(i);
	    // skip column zero markers (they just display the date)
	    if (m.column == 0) {
		continue;
	    }
	    int tcol = ttable.get(m.trip.travelerid) - 1;
	    // if this marker is beyond its traveler's current row
	    // position, we need to insert a spacer
	    if (rowpos[tcol] < m.rowstart) {
		markers.add(new Marker(rowpos[tcol], m.rowstart-rowpos[tcol],
				       tcol+1, null));
	    }
	    // now move this traveler down to the end of this trip
	    rowpos[tcol] = m.rowstart+m.rowspan;
	}

	// add spacers for the very end of the table
	for (int i = 0; i < rowpos.length; i++) {
	    if (rowpos[i] < maxrow) {
		markers.add(new Marker(rowpos[i], maxrow-rowpos[i],
				       i+1, null));
	    }
	}

	// sort the whole dang thing again
	Collections.sort(markers);

	// and stuff it on into the context for the display to worry about
	if (markers.size() > 0) {
	    context.put("markers", markers);
	}
    }

    /** Used to sort out trip layout on the calendar display. */
    public static class Marker implements Comparable
    {
	public Trip trip;
	public Date date;
	public int column;
	public Marker start;

	public int rowstart = -1;
	public int rowspan = -1;

	public Marker (Trip trip, Date date, int column, Marker start)
	{
	    this.trip = trip;
	    this.date = date;
	    this.column = column;
	    this.start = start;
	}

	public Marker (int rowstart, int rowspan, int column, String text)
	{
	    this.rowstart = rowstart;
	    this.rowspan = rowspan;
	    this.column = column;
	    _text = text;
	}

	public int compareTo (Object other)
	{
	    Marker om = (Marker)other;
	    int rv;
	    // if we have a row number, use that as our primary key,
	    // otherwise use our date
	    if (rowstart >= 0 && om.rowstart >= 0) {
		rv = rowstart - om.rowstart;
	    } else {
		rv = date.compareTo(om.date);
	    }
	    // use the column as our secondary key
	    return (rv == 0) ? (column - om.column) : rv;
	}

	public String text ()
	{
	    if (trip != null) {
		StringBuffer buf = new StringBuffer();
		buf.append("<b>");
		buf.append(trip.destination).append("</b><br>");
		buf.append("<font size=\"-2\">");
		buf.append(_mdfmt.format(trip.begins)).append(" to ");
		buf.append(_mdfmt.format(trip.ends));
		buf.append("</font>");
		return buf.toString();

	    } else if (_text != null) {
		return _text;

	    } else {
		return "&nbsp;";
	    }
	}

	public String bgcolor ()
	{
	    if (trip != null) {
		return "#99CC66";
	    } else if (_text != null) {
		return "#CCFF99";
	    } else {
		return "#CCCCCC";
	    }
	}

	public Integer owner ()
	{
	    return new Integer((trip != null) ? trip.travelerid : -1);
	}

	public Integer tripid ()
	{
	    return new Integer((trip != null) ? trip.tripid : -1);
	}

	protected String _text;
    }

    protected static SimpleDateFormat _mdfmt =
	new SimpleDateFormat("MMM d");

    protected static SimpleDateFormat _qfmt =
	new SimpleDateFormat("yyyy-MM-dd");

    // unfortunately java's i18n stuff doesn't seem to provide services
    // for obtaining the ordinal string for a given number. i'm not in the
    // mood to write a general purpose routine for this, so we hack...
    protected static String[] ORDINALS =
    { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };
}
