<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ page import="org.springframework.security.ui.AbstractProcessingFilter" %>
<%@ page import="org.springframework.security.ui.webapp.AuthenticationProcessingFilter" %>
<%@ page import="org.springframework.security.AuthenticationException" %>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">


<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>

  <style type="text/css" media="screen">   
  	@import url("<c:url value="/resources/dojo/resources/dojo.css"/>");
  	@import url("<c:url value="/resources/dijit/themes/tundra/tundra.css"/>");
    @import url("<c:url value="/static/styles/roo.css"/>");
  </style>     
  
  <script djconfig="parseOnLoad: true" src="<c:url value="/resources/dojo/dojo.js"/>" type="text/javascript"></script>
  <script type="text/javascript" src="<c:url value="/resources/spring/Spring.js" />"> </script>
  <script type="text/javascript" src="<c:url value="/resources/spring/Spring-Dojo.js" />"> </script>	 
	<script type="text/javascript">
	dojo.require("dojo.parser");
  </script>
  <title>Spring ROO</title>	
 
</head>

<body class="tundra spring">	

  <div id="wrap">
  
  	<div id="menu">
    <p>You have tried to access a protected area of this application.</p>
    <p>By default you can login as "admin", with a password of "admin".</p>
	</div>

  <div id="main">
<script type="text/javascript">dojo.require("dijit.TitlePane");</script>
<div dojoType="dijit.TitlePane" style="width: 100%" title="Spring Security Login">
    <%-- this form-login-page form is also used as the
         form-error-page to ask for a login again.
         --%>
    <c:if test="${not empty param.login_error}">
      <font color="red">Your login attempt was not successful, try again.<br/>
        Reason: <c:out value="${SPRING_SECURITY_LAST_EXCEPTION.message}"/>.
      </font>
    </c:if>
	<form name="f" action="<c:url value='/static/j_spring_security_check'/>" method="POST"> 
    	<div>
            <label for="j_username">Name:</label>
            <input id="j_username" type='text' name='j_username' style="width:150px"/>
            <script type="text/javascript">Spring.addDecoration(new Spring.ElementDecoration({elementId : "j_username", widgetType : "dijit.form.ValidationTextBox", widgetAttrs : {promptMessage: "Enter Your Username", required : true}})); </script>
        </div>
        <br/>
        <div>
            <label for="j_password">Password:</label>
            <input id="j_password" type='password' name='j_password' style="width:150px" />
            <script type="text/javascript">Spring.addDecoration(new Spring.ElementDecoration({elementId : "j_password", widgetType : "dijit.form.ValidationTextBox", widgetAttrs : {promptMessage: "Enter Your Password", required : true}})); </script>
        </div>
        <br/>
        <div class="submit">
            <script type="text/javascript">Spring.addDecoration(new Spring.ValidateAllDecoration({elementId:'proceed', event:'onclick'}));</script>
            <input id="proceed" type="submit" value="Submit"/>
            <input id="reset" type="reset" value="Reset"/>
        </div>
    </form>
</div>
</div>
</div>
</body>

</html>
