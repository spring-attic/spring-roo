<%@ include file="/WEB-INF/jsp/includes.jsp" %>
<%@ include file="/WEB-INF/jsp/header.jsp" %>

<h2>Internal error</h2>
<p/>
Sorry, we encountered an internal error. 
<p/>

<% 
StringBuilder message = new StringBuilder();
StringBuilder stackTrace = new StringBuilder();
StringBuilder cookiesMessage = new StringBuilder();
try {
	// The Servlet spec guarantees this attribute will be available
	Throwable exception = (Throwable) request.getAttribute("javax.servlet.error.exception"); 
	if (exception != null) {
		if (exception instanceof ServletException) {
			// It's a ServletException: we should extract the root cause
			ServletException sex = (ServletException) exception;
			Throwable rootCause = sex.getRootCause();
			if (rootCause == null)
				rootCause = sex;
			message.append(rootCause.getLocalizedMessage());

			for(StackTraceElement element : rootCause.getStackTrace()) {
				stackTrace.append(element).append("\n\n");
			}
		}
		else {
			// It's not a ServletException, so we'll just show it
			for(StackTraceElement element : exception.getStackTrace()) {
				stackTrace.append(element).append("\n\n");
			}
		}
	} 
	else  {
    	stackTrace.append("No error information available");
	} 

	// Display cookies
	Cookie[] cookies = request.getCookies();
	if (cookies != null) {
    	for (int i = 0; i < cookies.length; i++) {
      		cookiesMessage.append(cookies[i].getName()).append("=[").append(cookies[i].getValue()).append("]");
		}
	}
	    
} catch (Exception ex) { 
	ex.printStackTrace(new java.io.PrintWriter(out));
}
request.setAttribute("message", message.toString());
request.setAttribute("stackTrace", stackTrace.toString());
request.setAttribute("cookies", cookiesMessage.toString());
%>

<h4>Details</h4>

<script type="text/javascript">dojo.require("dijit.Dialog");dojo.require("dijit.form.Button");</script>

<button dojoType="dijit.form.Button" onclick="dijit.byId('dialog1').show()">Show Message</button>

<div dojoType="dijit.Dialog" id="dialog1" title="Exception Message" style="width:800px">
	<c:out value="${message}" />
</div>

<button dojoType="dijit.form.Button" onclick="dijit.byId('dialog2').show()">Show Stack Trace</button>

<div dojoType="dijit.Dialog" id="dialog2" title="Exception Stack Trace" style="width:800px">
		<c:out value="${stackTrace}" />
</div>

<button dojoType="dijit.form.Button" onclick="dijit.byId('dialog3').show()">Show Cookies</button>

<div dojoType="dijit.Dialog" id="dialog3" title="Cookies" style="width:800px">
		<c:out value="${cookies}" />
</div>

<%@ include file="/WEB-INF/jsp/footer.jsp" %>
