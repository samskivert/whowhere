//
// $Id$

package whowhere;

import java.util.Properties;
import javax.servlet.ServletContext;

import com.samskivert.servlet.user.UserManager;
import com.samskivert.webmacro.Application;
import whowhere.data.Repository;

/**
 * The whowhere class contains references to application-wide resources
 * (like the database repository) and handles initialization and cleanup
 * for those resources.
 */
public class WhoWhere extends Application
{
    /** Returns the database repository in use by the application. */
    public Repository getRepository ()
    {
        return _repository;
    }

    /** Returns the user manager in use by the application. */
    public UserManager getUserManager ()
    {
        return _usermgr;
    }

    public void init (ServletContext context)
    {
	try {
	    // initialize the user manager
	    Properties props = new Properties();
	    props.put("driver", "org.gjt.mm.mysql.Driver");
	    props.put("url", "jdbc:mysql://localhost:3306/samskivert");
	    props.put("username", "www");
	    props.put("password", "Il0ve2PL@Y");
	    props.put("login_url", "/usermgmt/login.wm?from=%R");
	    _usermgr = new UserManager(props);

	    // initialize the trip repository
	    props = new Properties();
	    props.put("driver", "org.gjt.mm.mysql.Driver");
	    props.put("url", "jdbc:mysql://localhost:3306/whowhere");
	    props.put("username", "www");
	    props.put("password", "Il0ve2PL@Y");
	    _repository = new Repository(props);

	    Log.info("WhoWhere application initialized.");

	} catch (Throwable t) {
	    Log.warning("Error initializing application: " + t);
	}
    }

    public void shutdown ()
    {
	try {
	    _repository.shutdown();
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

    protected Repository _repository;
    protected UserManager _usermgr;

    /** The name of our translation messages file. */
    protected static final String MESSAGE_BUNDLE_PATH = "messages";
}
