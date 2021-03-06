//
// $Id$

package whowhere.logic;

import com.samskivert.servlet.user.*;

import com.samskivert.servlet.util.FriendlyException;
import com.samskivert.servlet.util.ParameterUtil;
import com.samskivert.servlet.util.RequestUtils;

import com.samskivert.util.Crypt;
import com.samskivert.velocity.*;

import whowhere.WhoWhere;
import whowhere.data.*;

public class accept implements Logic
{
    public void invoke (Application app, InvocationContext ctx)
        throws Exception
    {
        UserManager usermgr = ((WhoWhere)app).getUserManager();
	User user = usermgr.loadUser(ctx.getRequest());
        String errmsg = null;

        // get our parameters
	int friendid = ParameterUtil.requireIntParameter(
            ctx.getRequest(), "who", "accept.error.missing_param");
	int random = ParameterUtil.requireIntParameter(
            ctx.getRequest(), "when", "accept.error.missing_param");
	String hash = ParameterUtil.requireParameter(
            ctx.getRequest(), "how", "accept.error.missing_param");

        // load up the inviting user
        UserRepository urep = usermgr.getRepository();
        User inviter = urep.loadUser(friendid);
        if (inviter == null) {
            throw new FriendlyException("accept.error.no_such_user");
        }
        ctx.put("inviter", inviter.realname);

        // make sure the authorization stuff matches
        String thash = Crypt.crypt(Integer.toString(random), inviter.password);
        if (!thash.equals(hash)) {
            throw new FriendlyException("accept.error.missing_param");
        }

        // if they aren't logged in, configure the page to let them know
	// that they need to
        if (user == null) {
            ctx.put("need_account", new Boolean(true));
            // stick our from URL into the context as well
            String eurl = RequestUtils.getLocationEncoded(ctx.getRequest());
            ctx.put("from", eurl);

        } else {
            // make sure things are sane
            if (user.userId == friendid) {
                throw new FriendlyException("accept.error.cant_invite_self");
            }

            // they've accepted. update the friends table
            if (ParameterUtil.parameterEquals(
                ctx.getRequest(), "action", "accept")) {
                TripRepository rep = ((WhoWhere)app).getRepository();
                rep.expandCircle(user.userId, friendid);
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
