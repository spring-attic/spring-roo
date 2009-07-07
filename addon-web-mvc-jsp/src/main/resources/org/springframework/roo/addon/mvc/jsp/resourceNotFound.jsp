<%@ include file="/WEB-INF/jsp/includes.jsp" %>
<%@ include file="/WEB-INF/jsp/header.jsp" %>

<h2>Requested Resource Not Found</h2>
<p/>
Sorry, we did not find the resource you were looking for. 
<p/>
<c:if test="${not empty exception}">
<h4>Details</h4>
<script type="text/javascript">dojo.require("dijit.Dialog");dojo.require("dijit.form.Button");</script>
<button dojoType="dijit.form.Button" onclick="dijit.byId('dialog1').show()">Show Message</button>

<div dojoType="dijit.Dialog" id="dialog1" title="Exception Message" style="width:800px">
	<c:out value="${exception.localizedMessage}" />
</div>

<button dojoType="dijit.form.Button" onclick="dijit.byId('dialog2').show()">Show Stack Trace</button>

<div dojoType="dijit.Dialog" id="dialog2" title="Exception Stack Trace" style="width:800px">
	<c:forEach items="${exception.stackTrace}" var="trace">
		<c:out value="${trace}" />
	</c:forEach>
</div>

</c:if>
<%@ include file="/WEB-INF/jsp/footer.jsp" %>
