//
// $Id$

package whowhere;

import java.util.Properties;
import javax.servlet.ServletContext;

import com.samskivert.servlet.user.UserManager;
import whowhere.data.Repository;

/**
 * The whowhere class contains references to application-wide resources
 * (like the database repository) and handles initialization and cleanup
 * for those resources.
 */
public class WhoWhere
{
    /** The database repository in use by the application. */
    public static Repository repository;

    /** The user manager in use by the application. */
    public static UserManager usermgr;

    public static void init (ServletContext context)
    {
	try {
	    // initialize the user manager
	    Properties props = new Properties();
	    props.put("driver", "org.gjt.mm.mysql.Driver");
	    props.put("url", "jdbc:mysql://localhost:3306/samskivert");
	    props.put("username", "www");
	    props.put("password", "Il0ve2PL@Y");
	    props.put("login_url", "/usermgmt/login.wm?from=%R");
	    usermgr = new UserManager(props);

	    // initialize the trip repository
	    props = new Properties();
	    props.put("driver", "org.gjt.mm.mysql.Driver");
	    props.put("url", "jdbc:mysql://localhost:3306/whowhere");
	    props.put("username", "www");
	    props.put("password", "Il0ve2PL@Y");
	    repository = new Repository(props);

	    Log.info("WhoWhere application initialized.");

	} catch (Throwable t) {
	    Log.warning("Error initializing application: " + t);
	}
    }

    public static void shutdown ()
    {
	try {
	    repository.shutdown();
	    usermgr.shutdown();
	    Log.info("WhoWhere application shutdown.");

	} catch (Throwable t) {
	    Log.warning("Error shutting down repository: " + t);
	}
    }
}
