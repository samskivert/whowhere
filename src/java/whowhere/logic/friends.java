//
// $Id$

package whowhere.logic;

import org.webmacro.*;
import org.webmacro.servlet.WebContext;

import com.samskivert.servlet.user.*;
import com.samskivert.servlet.util.RequestUtils;
import com.samskivert.webmacro.*;

import whowhere.Log;
import whowhere.WhoWhere;
import whowhere.data.*;

public class friends implements Logic
{
    public void invoke (Application app, WebContext ctx)
        throws Exception
    {
        UserManager usermgr = ((WhoWhere)app).getUserManager();
        UserRepository urep = usermgr.getRepository();
        Repository rep = ((WhoWhere)app).getRepository();

	User user = usermgr.requireUser(ctx.getRequest());
        String errmsg = null;

	// if they've submitted the form, they want to delete someone from
	// their friends list
	if (FormUtil.equals(ctx, "action", "delete")) {
            int friendid = FormUtil.requireIntParameter(
                ctx, "who", "friends.error.missing_friendid");
            rep.excommunicate(user.userid, friendid);

            // let them know that we've done the deed
            User friend = urep.loadUser(friendid);
            String fname = (friend != null) ? friend.realname : "<?>";
            ctx.put("error", app.translate(
                ctx, "friends.message.excommunicated", fname));
	}

        // now load up their circle of friends
        int[] fids = rep.loadCircle(user.userid, false);
        String[] names = urep.loadRealNames(fids);

        // make the names and ids available to the page
        ctx.put("fids", fids);
        ctx.put("names", java.util.Arrays.asList(names));

        // load up everyone's name and stuff that into the context
        ctx.put("allnames", urep.loadAllRealNames());

        // handle any message that was generated
        if (errmsg != null) {
            ctx.put("error", app.translate(ctx, errmsg));
        }
    }
}
