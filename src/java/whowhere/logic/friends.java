//
// $Id$

package whowhere.logic;

import com.samskivert.servlet.user.*;
import com.samskivert.servlet.util.ParameterUtil;
import com.samskivert.servlet.util.RequestUtils;
import com.samskivert.velocity.*;

import whowhere.WhoWhere;
import whowhere.data.*;

public class friends implements Logic
{
    public void invoke (Application app, InvocationContext ctx)
        throws Exception
    {
        UserManager usermgr = ((WhoWhere)app).getUserManager();
        UserRepository urep = usermgr.getRepository();
        TripRepository rep = ((WhoWhere)app).getRepository();

	User user = usermgr.requireUser(ctx.getRequest());
        String errmsg = null;

	// if they've submitted the form, they want to delete someone from
	// their friends list
	if (ParameterUtil.parameterEquals(
            ctx.getRequest(), "action", "delete")) {
            int friendid = ParameterUtil.requireIntParameter(
                ctx.getRequest(), "who", "friends.error.missing_friendid");
            rep.excommunicate(user.userId, friendid);

            // let them know that we've done the deed
            User friend = urep.loadUser(friendid);
            String fname = (friend != null) ? friend.realname : "<?>";
            ctx.put("error", app.translate(
                ctx, "friends.message.excommunicated", fname));
	}

        // now load up their circle of friends
        int[] fids = rep.loadCircle(user.userId, false);
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
