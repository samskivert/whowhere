//
// $Id$

package whowhere.logic;

import java.text.SimpleDateFormat;
import java.util.*;

import com.samskivert.util.IntMap;
import com.samskivert.webmacro.*;
import com.samskivert.servlet.user.*;
import org.webmacro.servlet.WebContext;
import org.webmacro.util.Bag;

import whowhere.*;
import whowhere.data.*;

public class calendar implements Logic
{
    public void invoke (Application app, WebContext ctx)
        throws Exception
    {
        UserManager usermgr = ((WhoWhere)app).getUserManager();
        String errmsg = null;

        // generate some default dates
        Calendar cal = Calendar.getInstance();
        java.sql.Date endingAfter =
            new java.sql.Date(cal.getTime().getTime());
        cal.add(Calendar.YEAR, 1);
        java.sql.Date startingBefore =
            new java.sql.Date(cal.getTime().getTime());

	// put the default values into "begin" and "end" in case parsing
	// goes awry before we get a chance to put something reasonable in
	// there
	ctx.put("begin", _qfmt.format(endingAfter));
	ctx.put("end", _qfmt.format(startingBefore));

	// parse the dates we were given, if we were given any
        Date ea = FormUtil.getDateParameter(
            ctx, "begin", "calendar.error.invalid_begin_date");
        Date sb = FormUtil.getDateParameter(
            ctx, "end", "calendar.error.invalid_end_date");

        if (ea != null && sb != null) {
            endingAfter = new java.sql.Date(ea.getTime());
            startingBefore = new java.sql.Date(sb.getTime());
        }

	// stick the new dates into the context for use by the form
	ctx.put("begin", _qfmt.format(endingAfter));
	ctx.put("end", _qfmt.format(startingBefore));

	// load up the trips
        Repository rep = ((WhoWhere)app).getRepository();
	Trip[] trips = rep.getTrips(endingAfter, startingBefore);

	// bail out now if we've got no trips
	if (trips == null) {
	    return;
	}

	// load up our user record
	User user = usermgr.loadUser(ctx.getRequest());
	// and put our userid into the context for the display to muck
	// with
	if (user != null) {
	    ctx.put("userid", new Integer(user.userid));
	} else {
	    ctx.put("userid", new Integer(-1));
	}

	// sort our trips by start date
	Arrays.sort(trips);

	// figure out which travelers are involved and assign colors
        IntMap tmap = new IntMap();
        int cidx = 0;
	for (int i = 0; i < trips.length; i++) {
	    Trip t = trips[i];
            if (!tmap.contains(t.travelerid)) {
                tmap.put(t.travelerid, COLORS[cidx]);
                // hopefully we won't wrap around, but we don't want to
                // AOOBE if we do
                cidx = (cidx+1) % COLORS.length;
            }
	}

	// convert the traveler ids into names
	int[] userids = new int[tmap.size()];
        Enumeration tids = tmap.keys();
        for (int i = 0; tids.hasMoreElements(); i++) {
	    userids[i] = ((Integer)tids.nextElement()).intValue();
	}
        UserRepository urep = usermgr.getRepository();
	String[] names = urep.loadUserNames(userids);
	ctx.put("names", names);

        // put each traveler's colors into an array so the UI can display
        // a key
	String[] colors = new String[names.length];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = (String)tmap.get(userids[i]);
        }
        ctx.put("colors", java.util.Arrays.asList(colors));

	// phase one of the processing invovles figuring out how all of
	// the trips overlap and how much vertical space each trip will
	// consume in the calendar
	ArrayList markers = new ArrayList();
	for (int i = 0; i < trips.length; i++) {
	    Trip t = trips[i];
	    // add a marker for the start of the trip
	    Marker start = new Marker(t, t.begins, -1, null,
                                      (String)tmap.get(t.travelerid));
	    markers.add(start);
	    // and one for the end of the trip (linked to the start)
	    markers.add(new Marker(null, t.ends, -1, start, null));
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

	// assign columns and insert spacers between trips to make
	// everything layout properly
	int[] rowpos = new int[tmap.size()];
        int maxcolumn = 0;

	msize = markers.size(); // we'll be adding markers as we go
	for (int i = 0; i < msize; i++) {
	    Marker m = (Marker)markers.get(i);
	    // skip column zero markers (they just display the date)
	    if (m.column == 0) {
		continue;
	    }
            // figure out what column to use for this trip
	    int tcol = 0;
            while (rowpos[tcol] > m.rowstart) {
                tcol++;
                // the pathological case is where every single traveler
                // has a trip on a particular date and at least one of the
                // travelers has gone and scheduled overlapping trips;
                // in this case our array needs to be expanded because we
                // only alotted enough columns for everyone to have an
                // overlapping trip at the same time
                if (tcol >= rowpos.length) {
                    int[] nrpos = new int[rowpos.length*2];
                    System.arraycopy(rowpos, 0, nrpos, 0, rowpos.length);
                    rowpos = nrpos;
                }
            }
            // update this marker with the proper position
            m.column = tcol+1;
            // make a note if we've forged into the land of a whole new
            // column
            if (tcol > maxcolumn) {
                maxcolumn = tcol;
            }
	    // if this marker is more than one slot beyond the current row
	    // position, we need to insert a spacer
	    if (rowpos[tcol] < m.rowstart) {
                Marker sp = new Marker(rowpos[tcol], m.rowstart-rowpos[tcol],
				       tcol+1, null);
		markers.add(sp);
	    }
	    // now move this traveler down to the end of this trip
	    rowpos[tcol] = m.rowstart+m.rowspan;
	}

	// add spacers for the very end of the table
	for (int i = 0; i <= maxcolumn; i++) {
	    if (rowpos[i] < maxrow) {
		markers.add(new Marker(rowpos[i], maxrow-rowpos[i],
                                       i+1, null));
	    }
	}

	// sort the whole dang thing again
	Collections.sort(markers);

	// and stuff it on into the context for the display to worry about
	if (markers.size() > 0) {
	    ctx.put("markers", markers);
	}

        // handle any message that was generated
        if (errmsg != null) {
            ctx.put("error", app.translate(ctx, errmsg));
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

        public String bgcolor;

	public Marker (Trip trip, Date date, int column, Marker start,
                       String bgcolor)
	{
	    this.trip = trip;
	    this.date = date;
	    this.column = column;
	    this.start = start;
            this.bgcolor = bgcolor;
	}

	public Marker (int rowstart, int rowspan, int column, String text)
	{
	    this.rowstart = rowstart;
	    this.rowspan = rowspan;
	    this.column = column;
	    _text = text;
            if (_text == null) {
                this.bgcolor = "#CCCCCC";
            } else {
                this.bgcolor = "#CCFF99";
            }
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
	    return _text == null ? "&nbsp;" : _text;
        }

	public String begins ()
	{
	    return _mdfmt.format(trip.begins);
        }

	public String ends ()
	{
	    return _mdfmt.format(trip.ends);
        }

	public Integer owner ()
	{
	    return new Integer((trip != null) ? trip.travelerid : -1);
	}

	public Integer tripid ()
	{
	    return new Integer((trip != null) ? trip.tripid : -1);
	}

        public String toString ()
        {
            return "[date=" + date + ", column=" + column +
                ", rowstart=" + rowstart + ", rowspan=" + rowspan + "]";
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

    // these colors are used to identify travelers in the display. ah the
    // go2palette lives on!
    protected static String[] COLORS = {
        "#6699CC", "#CC6600", "#FFCC33", "#CC6666", "#996699", "#99CC66",
        "#99CCFF", "#FFCC66", "#FFFF99", "#FFCCCC", "#CCCCFF", "#CCFF99",
        "#0066FF", "#FF9900", "#FFFF33", "#FF0033", "#9933FF", "#00CC00",
    };
}
