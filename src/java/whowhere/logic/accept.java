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

public class accept implements Logic
{
    public void invoke (Application app, WebContext ctx)
        throws Exception
    {
        UserManager usermgr = ((WhoWhere)app).getUserManager();
	User user = usermgr.loadUser(ctx.getRequest());
        String errmsg = null;

        // load up the inviting user
	int friendid = FormUtil.requireIntParameter(
            ctx, "who", "accept.error.missing_who");
        UserRepository urep = usermgr.getRepository();
        User inviter = urep.loadUser(friendid);
        if (inviter == null) {
            throw new FriendlyException("accept.error.no_such_user");
        }
        ctx.put("inviter", inviter.realname);

        // if they aren't logged in, configure the page to let them know
	// that they need to
        if (user == null) {
            ctx.put("need_account", new Boolean(true));
            // stick our from URL into the context as well
            String eurl = RequestUtils.getLocationEncoded(ctx.getRequest());
            ctx.put("from", eurl);

        } else {
            // make sure things are sane
            if (user.userid == friendid) {
                throw new FriendlyException("accept.error.cant_invite_self");
            }

            // they've accepted. update the friends table
            if (FormUtil.equals(ctx, "action", "accept")) {
                Repository rep = ((WhoWhere)app).getRepository();
                rep.expandCircle(user.userid, friendid);
                // let the user know that all systems are go
                errmsg = "accept.message.accepted";
            }
        }

        // handle any message that was generated
        if (errmsg != null) {
            ctx.put("error", app.translate(ctx, errmsg));
        }
    }
}
