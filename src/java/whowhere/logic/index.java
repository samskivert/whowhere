//
// $Id$

package whowhere.logic;

import org.webmacro.*;
import org.webmacro.servlet.WebContext;

import com.samskivert.servlet.user.*;
import com.samskivert.servlet.RedirectException;
import com.samskivert.webmacro.*;
import com.samskivert.util.StringUtil;

import whowhere.Log;
import whowhere.WhoWhere;
import whowhere.data.*;

public class index implements Logic
{
    public void invoke (Application app, WebContext ctx)
        throws Exception
    {
        UserManager usermgr = ((WhoWhere)app).getUserManager();
	User user = usermgr.loadUser(ctx.getRequest());

        // if the user is already logged in, redirect them to the calendar
        // page; otherwise display the welcome page as is
        if (user != null) {
            String uri = ctx.getRequest().getRequestURI();
            if (uri.endsWith("/")) {
                uri = uri + "calendar.wm";
            } else {
                uri = StringUtil.replace(uri, "index.wm", "calendar.wm");
            }
            System.out.println("URI: " + uri);
            throw new RedirectException(uri);
        }
    }
}
