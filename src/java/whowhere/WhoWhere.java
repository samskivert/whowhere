//
// $Id$

package whowhere;

import java.util.Properties;
import javax.servlet.ServletContext;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.StaticConnectionProvider;

import com.samskivert.servlet.JDBCTableSiteIdentifier;
import com.samskivert.servlet.SiteIdentifier;
import com.samskivert.servlet.user.UserManager;
import com.samskivert.util.ServiceUnavailableException;
import com.samskivert.velocity.Application;

import whowhere.data.TripRepository;

/**
 * The whowhere class contains references to application-wide resources
 * (like the database repository) and handles initialization and cleanup
 * for those resources.
 */
public class WhoWhere extends Application
{
    /** Returns the connection provider in use by this application. */
    public final ConnectionProvider getConnectionProvider ()
    {
        return _conprov;
    }

    /** Returns the trip repository in use by the application. */
    public TripRepository getRepository ()
    {
        return _triprep;
    }

    /** Returns the user manager in use by the application. */
    public UserManager getUserManager ()
    {
        return _usermgr;
    }

    protected void willInit ()
    {
        super.willInit();

	try {
            // create a static connection provider
            _conprov = new StaticConnectionProvider(CONN_CONFIG);

	    // initialize the user manager
	    Properties props = new Properties();
	    props.put("login_url", "/usermgmt/login.wm?from=%R");
	    _usermgr = new UserManager(props, _conprov);

	    // initialize the trip repository
	    _triprep = new TripRepository(_conprov);

	    Log.info("WhoWhere application initialized.");

	} catch (Throwable t) {
	    Log.warning("Error initializing application: " + t);
	}
    }

    public void shutdown ()
    {
	try {
	    _usermgr.shutdown();
	    Log.info("WhoWhere application shutdown.");

	} catch (Throwable t) {
	    Log.warning("Error shutting down repository: " + t);
	}
    }

    /** Yes! We want a message manager. */
    public String getMessageBundlePath ()
    {
        return MESSAGE_BUNDLE_PATH;
    }

    /** We want a special site identifier. */
    protected SiteIdentifier createSiteIdentifier (ServletContext ctx)
    {
        try {
            return new JDBCTableSiteIdentifier(_conprov);
        } catch (PersistenceException pe) {
            throw new ServiceUnavailableException(
                "Can't access site database.", pe);
        }
    }

    /** A reference to our connection provider. */
    protected ConnectionProvider _conprov;

    /** A reference to our user manager. */
    protected UserManager _usermgr;

    /** A reference to our trip repository. */
    protected TripRepository _triprep;

    /** The name of our translation messages file. */
    protected static final String MESSAGE_BUNDLE_PATH = "messages";

    /** The path to our database configuration file. */
    protected static final String CONN_CONFIG = "repository.properties";
}
