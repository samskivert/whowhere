//
// $Id$

package whowhere;

/**
 * A placeholder class that contains a reference to the log object used by
 * the whowhere package.
 */
public class Log
{
    /**
     * This is the log instance that will be used to log all messages for
     * the whowhere package.
     */
    public static com.samskivert.util.Log log =
	new com.samskivert.util.Log("whowhere");

    /** Convenience function. */
    public static void debug (String message)
    {
	log.debug(message);
    }

    /** Convenience function. */
    public static void info (String message)
    {
	log.info(message);
    }

    /** Convenience function. */
    public static void warning (String message)
    {
	log.warning(message);
    }

    /** Convenience function. */
    public static void logStackTrace (Throwable t)
    {
	log.logStackTrace(com.samskivert.util.Log.WARNING, t);
    }
}
