//
// $Id$

package whowhere.logic;

import java.util.Hashtable;
import javax.servlet.http.HttpServletRequest;

import org.webmacro.*;
import org.webmacro.servlet.WebContext;

import com.samskivert.net.MailUtil;
import com.samskivert.servlet.user.*;
import com.samskivert.webmacro.*;
import com.samskivert.util.StringUtil;

import whowhere.Log;
import whowhere.WhoWhere;
import whowhere.data.*;

public class addfriend implements Logic
{
    public void invoke (Application app, WebContext ctx)
        throws Exception
    {
        UserManager usermgr = ((WhoWhere)app).getUserManager();
	User user = usermgr.requireUser(ctx.getRequest());
        String errmsg = null;

        // load up the email template (this exposes more innards of
        // WebMacro than I'd like but we don't have a handle on the
        // WMServlet at this point, so we've little choice)
        Template etmpl = (Template)
            ctx.getBroker().get("template", "/addfriend.tmpl");

	// if they've submitted the form, we send an email to the
	// specified address
	if (FormUtil.equals(ctx, "action", "request")) {
	    // parse our fields
            String address = FormUtil.requireParameter(
                ctx, "address", "addfriend.error.missing_address");
            String subject = FormUtil.requireParameter(
                ctx, "subject", "addfriend.error.missing_subject");

            // make sure the email address is valid
            if (MailUtil.isValidAddress(address)) {
                // generate the mail message
                String sender = user.realname + "<" + user.email + ">";
                String url = constructURL(ctx.getRequest(), user);
                String text = generateMailText(user.realname, sender, address,
                                               subject, url, etmpl, ctx);
                // and send it
                MailUtil.deliverMail(text);
                // let the user know we sent it
                errmsg = "addfriend.message.request_sent";
                // also cause the form not to be displayed
                ctx.put("noform", "true");

            } else {
                errmsg = "addfriend.error.invalid_address";
            }

	} else {
	    // create defaults for the form values
	    Hashtable defaults = new Hashtable();
	    defaults.put("address", "");
            String def =
                app.translate(ctx, "addfriend.create.default_subject");
	    defaults.put("subject", def);
	    ctx.put("Form", defaults);

            // stick a default email in the context
            String url = constructURL(ctx.getRequest(), user);
            String msg = generateMailText("(Your name here)",
                                          "(you &lt;your email&gt;)",
                                          "(them)", "(Your subject here)",
                                          url, etmpl, ctx);
            ctx.put("email", msg);
	}

        // handle any message that was generated
        if (errmsg != null) {
            ctx.put("error", app.translate(ctx, errmsg));
        }
    }

    protected String generateMailText (String name, String sender,
                                       String recipient, String subject,
                                       String url, Template etmpl,
                                       WebContext ctx)
    {
        // stick some values in the context and execute the email
        // template, then stick that in the context
        ctx.put("name", name);
        ctx.put("sender", sender);
        ctx.put("recipient", recipient);
        ctx.put("subject", subject);
        ctx.put("url", url);

        try {
            FastWriter fw = new FastWriter(
                ctx.getBroker(),
                ctx.getResponse().getCharacterEncoding());
            etmpl.write(fw, ctx);
            return fw.toString();

        } catch (Exception e) {
            Log.warning("Error executing email template: " + e);
            return "";
        }
    }

    protected String constructURL (HttpServletRequest req, User user)
    {
        StringBuffer url = new StringBuffer("http://");
        url.append(req.getServerName());
        if (req.getServerPort() != 80) {
            url.append(":").append(req.getServerPort());
        }
        String uri = StringUtil.replace(req.getRequestURI(),
                                        "addfriend.wm", "accept.wm");
        url.append(uri);
        url.append("?who=").append(user.userid);
        return url.toString();
    }
}
