//
// $Id$

package whowhere.data;

import java.sql.Date;

public class Trip implements Comparable
{
    public int tripid;

    public int travelerid;

    public String destination;

    public Date begins;

    public Date ends;

    public String description;

    public int getTripid ()
    {
        return tripid;
    }

    public String getDestination ()
    {
        return destination;
    }

    public String getDescription ()
    {
        return description;
    }

    public Date getBegins ()
    {
        return begins;
    }

    public Date getEnds ()
    {
        return ends;
    }

    public int compareTo (Object other)
    {
	return begins.compareTo(((Trip)other).begins);
    }

    public String toString ()
    {
	return "[id=" + tripid + ", travelerid=" + travelerid +
	    ", destination=" + destination + ", begins=" + begins +
	    ", ends=" + ends + ", description=" + description + "]";
    }
}
