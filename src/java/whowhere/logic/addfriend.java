//
// $Id$

package whowhere.logic;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Hashtable;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.Template;

import com.samskivert.net.MailUtil;
import com.samskivert.servlet.user.*;
import com.samskivert.servlet.util.ParameterUtil;
import com.samskivert.util.Crypt;
import com.samskivert.util.StringUtil;
import com.samskivert.velocity.*;

import whowhere.Log;
import whowhere.WhoWhere;
import whowhere.data.*;

public class addfriend implements Logic
{
    public void invoke (Application app, InvocationContext ctx)
        throws Exception
    {
        UserManager usermgr = ((WhoWhere)app).getUserManager();
	User user = usermgr.requireUser(ctx.getRequest());
        String errmsg = null;

        // load up the email template
        Template etmpl = ctx.getTemplate("/addfriend.tmpl");

	// if they've submitted the form, we send an email to the
	// specified address
	if (ParameterUtil.parameterEquals(
            ctx.getRequest(), "action", "request")) {
	    // parse our fields
            String address = ParameterUtil.requireParameter(
                ctx.getRequest(), "address",
                "addfriend.error.missing_address");
            String subject = ParameterUtil.requireParameter(
                ctx.getRequest(), "subject",
                "addfriend.error.missing_subject");

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
                                       InvocationContext ctx)
    {
        // stick some values in the context and execute the email
        // template, then stick that in the context
        ctx.put("name", name);
        ctx.put("sender", sender);
        ctx.put("recipient", recipient);
        ctx.put("subject", subject);
        ctx.put("url", url);

        try {
            StringWriter sw = new StringWriter();
            etmpl.merge(ctx, sw);
            return sw.toString();

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
        url.append("?who=").append(user.userId);
        // generate an encryption of a random number and the requesting
        // user's password so that people can't spoof invitations from
        // someone that didn't invite them
        int random = new Random().nextInt(100);
        url.append("&when=").append(random);
        String hash = Crypt.crypt(Integer.toString(random), user.password);
        try {
            url.append("&how=").append(URLEncoder.encode(hash, "UTF-8"));
        } catch (UnsupportedEncodingException uee) {
            Log.warning("Unsupported encoding " + uee);
        }
        return url.toString();
    }
}
