//
// $Id$

package whowhere;

import java.sql.Date;

public class Trip
{
    public int tripid;

    public int travelerid;

    public String destination;

    public Date begins;

    public Date ends;

    public String toString ()
    {
	return "[id=" + tripid + ", travelerid=" + travelerid +
	    ", destination=" + destination + ", begins=" + begins +
	    ", ends=" + ends + "]";
    }
}
