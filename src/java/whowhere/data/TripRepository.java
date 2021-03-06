//
// $Id$

package whowhere.data;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.JORARepository;
import com.samskivert.jdbc.jora.*;

import com.samskivert.util.*;

/**
 * The repository provides access to the trip database.
 */
public class TripRepository extends JORARepository
{
    /**
     * The database identifier that the repository will use when fetching
     * a connection from the connection provider. The value is
     * <code>whowhere</code> which you'll probably need to know to
     * properly configure your connection provider.
     */
    public static final String DATABASE_IDENT = "whowhere";

    /**
     * Creates the repository and opens the trip database.
     *
     * @param conprov the database connection provider from which to
     * obtain our database connection.
     */
    public TripRepository (ConnectionProvider conprov)
	throws PersistenceException
    {
	super(conprov, DATABASE_IDENT);
    }

    protected void createTables (Session session)
    {
	// create our table objects
	_ttable = new Table(Trip.class.getName(), "trips", session,
			    "tripid");
    }

    /**
     * @return the entry with the specified trip id or null if no entry
     * with that id exists.
     */
    public Trip getTrip (final int tripid)
	throws PersistenceException
    {
        return (Trip)execute(new Operation () {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws PersistenceException, SQLException
            {
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
        });
    }

    /**
     * Fetches trips that intersect the date range provided.
     *
     * @param travelerid The traveler whose circle of trips are to be
     * shown.
     * @param endingAfter The beginning of the date range.
     * @param startingBefore The end of the date range.
     */
    public Trip[] getTrips (int travelerid, final Date endingAfter,
                            final Date startingBefore)
	throws PersistenceException
    {
        // first we have to look up the travelerids of this traveler's
        // circle of friends
        int[] tids = loadCircle(travelerid, true);
        final String tidstr = StringUtil.toString(tids);

        return (Trip[])execute(new Operation () {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws PersistenceException, SQLException
            {
                // now we can look up the trips
                String query = "where ends >= '" + endingAfter +
                    "' AND begins <= '" + startingBefore +
                    "' AND travelerid in " + tidstr;
                Cursor tc = _ttable.select(query);
                List tlist = tc.toArrayList();
                Trip[] trips = new Trip[tlist.size()];
                tlist.toArray(trips);
                return trips;
            }
        });
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
    public int[] loadCircle (final int travelerid, boolean includeTraveler)
	throws PersistenceException
    {
        final ArrayList flist = new ArrayList();

        // add the travler themselves to their circle if requested
        if (includeTraveler) {
            flist.add(new Integer(travelerid));
        }

        // now look up their friends
        execute(new Operation () {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws PersistenceException, SQLException
            {
                Statement stmt = conn.createStatement();
                try {
                    String query = "select friendid from friends " +
                        "where travelerid = " + travelerid;
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        flist.add(rs.getObject(1));
                    }
                    // nothing to return
                    return null;

                } finally {
                    stmt.close();
                }
            }
        });

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
    public void expandCircle (final int tid1, final int tid2)
        throws PersistenceException
    {
        execute(new Operation () {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws PersistenceException, SQLException
            {
                boolean add1 = true, add2 = true;
                Statement stmt = conn.createStatement();

                try {
                    // make sure they aren't already in one another's circle
                    String query = "select travelerid from friends where " +
                        "(travelerid=" + tid1 +
                        " AND friendid=" + tid2 + ") OR " +
                        "(travelerid=" + tid2 +
                        " AND friendid=" + tid1 + ")";
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
                        query = "insert into friends values(" +
                            tid1 + ", " + tid2 + ")";
                        stmt.executeUpdate(query);
                    }
                    if (add2) {
                        query = "insert into friends values(" +
                            tid2 + ", " + tid1 + ")";
                        stmt.executeUpdate(query);
                    }

                    // nothing to return
                    return null;

                } finally {
                    stmt.close();
                }
            }
        });
    }

    /**
     * Removes the link between two travelers in the friends table.
     */
    public void excommunicate (final int tid1, final int tid2)
        throws PersistenceException
    {
        execute(new Operation () {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws PersistenceException, SQLException
            {
                Statement stmt = conn.createStatement();
                try {
                    // simply remove both rows
                    stmt.executeUpdate("delete from friends where " +
                                       "travelerid = " + tid1 +
                                       " AND friendid = " + tid2);
                    stmt.executeUpdate("delete from friends where " +
                                       "travelerid = " + tid2 +
                                       " AND friendid = " + tid1);
                    // nothing to return
                    return null;

                } finally {
                    stmt.close();
                }
            }
        });
    }

    public Trip[] getTrips (final int travelerid)
	throws PersistenceException
    {
        return (Trip[])execute(new Operation () {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws PersistenceException, SQLException
            {
                Cursor tc = _ttable.select("where travelerid = " + travelerid);
                List tlist = tc.toArrayList();
                Trip[] trips = new Trip[tlist.size()];
                tlist.toArray(trips);
                return trips;
            }
        });
    }

    /**
     * Inserts a new entry into the table. All fields except the tripid
     * should contain valid values. The tripid field should be zero and it
     * will be filled in by this function.
     */
    public void insertTrip (final Trip trip)
	throws PersistenceException
    {
	execute(new Operation () {
	    public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws PersistenceException, SQLException
	    {
		// insert the trip into the table
		_ttable.insert(trip);

		// update the tripid now that it's known
		trip.tripid = liaison.lastInsertedId(conn);

                // nothing to return
                return null;
	    }
	});
    }

    /**
     * Updates a trip that was previously fetched from the database.
     */
    public void updateTrip (final Trip trip)
	throws PersistenceException
    {
	execute(new Operation () {
	    public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws PersistenceException, SQLException
	    {
		_ttable.update(trip);
                // nothing to return
                return null;
	    }
	});
    }

    /**
     * Removes the trip from the database.
     */
    public void deleteTrip (final Trip trip)
	throws PersistenceException
    {
	execute(new Operation () {
	    public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws PersistenceException, SQLException
	    {
		_ttable.delete(trip);
                // nothing to return
                return null;
	    }
	});
    }

    protected Table _ttable;
    protected Table _ctable;
}
