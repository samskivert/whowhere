//
// $Id$

package whowhere.data;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.samskivert.jdbc.MySQLRepository;
import com.samskivert.jdbc.jora.*;
import com.samskivert.util.*;

/**
 * The repository provides access to the trip database.
 */
public class Repository extends MySQLRepository
{
    /**
     * Creates the repository and opens the trip database. A properties
     * object should be supplied with the following fields:
     *
     * <pre>
     * driver=[jdbc driver class]
     * url=[jdbc driver url]
     * username=[jdbc username]
     * password=[jdbc password]
     * </pre>
     *
     * @param props a properties object containing the configuration
     * parameters for the repository.
     */
    public Repository (Properties props)
	throws SQLException
    {
	super(props);
    }

    protected void createTables ()
	throws SQLException
    {
	// create our table objects
	_ttable = new Table(Trip.class.getName(), "trips", _session,
			    "tripid");
    }

    /**
     * @return the entry with the specified trip id or null if no entry
     * with that id exists.
     */
    public Trip getTrip (int tripid)
	throws SQLException
    {
        // make sure our session is established
        ensureConnection();

	// look up the trip
	Cursor ec = _ttable.select("where tripid = " + tripid);

	// fetch the trip from the cursor
	Trip trip = (Trip)ec.next();

	if (trip != null) {
	    // call next() again to cause the cursor to close itself
	    ec.next();
	}

	return trip;
    }

    /**
     * Fetches trips that intersect the date range provided.
     *
     * @param travelerid The traveler whose circle of trips are to be
     * shown.
     * @param endingAfter The beginning of the date range.
     * @param startingBefore The end of the date range.
     */
    public Trip[] getTrips (int travelerid, Date endingAfter,
                            Date startingBefore)
	throws SQLException
    {
        // make sure our session is established
        ensureConnection();

        // first we have to look up the travelerids of this traveler's
        // circle of friends
        int[] tids = loadCircle(travelerid, true);
        String tidstr = StringUtil.toString(tids);

        // now we can look up the trips
	Cursor tc = _ttable.select("where ends >= '" + endingAfter +
				   "' AND begins <= '" + startingBefore +
                                   "' AND travelerid in " + tidstr);
	List tlist = tc.toArrayList();
	Trip[] trips = new Trip[tlist.size()];
	tlist.toArray(trips);
	return trips;
    }

    /**
     * Loads the userids of the friends in the circle of the specified
     * traveler.
     *
     * @param includeTraveler if true, the traveler will be included in
     * the list; if false only the friends.
     *
     * @return An array containing the userids of the friends of the
     * specified traveler. An array is always returned, never null.
     */
    public int[] loadCircle (int travelerid, boolean includeTraveler)
	throws SQLException
    {
        // make sure our session is established
        ensureConnection();

        ArrayList flist = new ArrayList();

        // add the travler themselves to their circle if requested
        if (includeTraveler) {
            flist.add(new Integer(travelerid));
        }

        // now look up their friends
	Statement stmt = _session.connection.createStatement();
        try {
            String query = "select friendid from friends " +
                "where travelerid = " + travelerid;
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                flist.add(rs.getObject(1));
            }

        } finally {
            stmt.close();
        }

        // and create an integer array
        int[] circle = new int[flist.size()];
        for (int i = 0; i < circle.length; i++) {
            circle[i] = ((Integer)flist.get(i)).intValue();
        }
        return circle;
    }

    /**
     * Expands the friends circle of two travelers by placing each of the
     * travelers into the other's friends circle.
     */
    public void expandCircle (int tid1, int tid2)
        throws SQLException
    {
        // make sure our session is established
        ensureConnection();

        boolean add1 = true, add2 = true;
	Statement stmt = _session.connection.createStatement();

        try {
            // make sure they aren't already in one another's circle
            String query = "select travelerid from friends where " +
                "(travelerid=" + tid1 + " AND friendid=" + tid2 + ") OR " +
                "(travelerid=" + tid2 + " AND friendid=" + tid1 + ")";
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                if (rs.getInt(1) == tid1) {
                    add1 = false;
                }
                if (rs.getInt(1) == tid2) {
                    add2 = false;
                }
            }

            // now add whichever connections need to be added
            if (add1) {
                stmt.executeUpdate("insert into friends values(" + tid1 +
                                   ", " + tid2 + ")");
            }
            if (add2) {
                stmt.executeUpdate("insert into friends values(" + tid2 +
                                   ", " + tid1 + ")");
            }

        } finally {
            stmt.close();
        }
    }

    /**
     * Removes the link between two travelers in the friends table.
     */
    public void excommunicate (int tid1, int tid2)
        throws SQLException
    {
        // make sure our session is established
        ensureConnection();

	Statement stmt = _session.connection.createStatement();
        try {
            // simply remove both rows
            stmt.executeUpdate("delete from friends where " +
                               "travelerid = " + tid1 +
                               " AND friendid = " + tid2);
            stmt.executeUpdate("delete from friends where " +
                               "travelerid = " + tid2 +
                               " AND friendid = " + tid1);
        } finally {
            stmt.close();
        }
    }

    public Trip[] getTrips (int travelerid)
	throws SQLException
    {
        // make sure our session is established
        ensureConnection();

	Cursor tc = _ttable.select("where travelerid = " + travelerid);
	List tlist = tc.toArrayList();
	Trip[] trips = new Trip[tlist.size()];
	tlist.toArray(trips);
	return trips;
    }

    /**
     * Inserts a new entry into the table. All fields except the tripid
     * should contain valid values. The tripid field should be zero and it
     * will be filled in by this function.
     */
    public void insertTrip (final Trip trip)
	throws SQLException
    {
	execute(new Operation () {
	    public void invoke () throws SQLException
	    {
		// insert the trip into the table
		_ttable.insert(trip);

		// update the tripid now that it's known
		trip.tripid = lastInsertedId();
	    }
	});
    }

    /**
     * Updates a trip that was previously fetched from the database.
     */
    public void updateTrip (final Trip trip)
	throws SQLException
    {
	execute(new Operation () {
	    public void invoke () throws SQLException
	    {
		_ttable.update(trip);
	    }
	});
    }

    /**
     * Removes the trip from the database.
     */
    public void deleteTrip (final Trip trip)
	throws SQLException
    {
	execute(new Operation () {
	    public void invoke () throws SQLException
	    {
		_ttable.delete(trip);
	    }
	});
    }

    protected Table _ttable;
    protected Table _ctable;
}
