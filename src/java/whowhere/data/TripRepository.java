//
// $Id$

package whowhere.data;

import java.sql.*;
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
	// create our table object
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
     * @param endingAfter The beginning of the date range.
     * @param startingBefore The end of the date range.
     */
    public Trip[] getTrips (Date endingAfter, Date startingBefore)
	throws SQLException
    {
        // make sure our session is established
        ensureConnection();

	Cursor tc = _ttable.select("where ends >= '" + endingAfter +
				   "' AND begins <= '" + startingBefore + "'");
	List tlist = tc.toArrayList();
	Trip[] trips = new Trip[tlist.size()];
	tlist.toArray(trips);
	return trips;
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
}
