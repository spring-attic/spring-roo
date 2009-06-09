<!--WARNING: This file is maintained by ROO! IT WILL BE OVERWRITTEN unless you specify null	@RooWebScaffold(automaticallyMaintainView = false) in the governing controller-->
<jsp:directive.include file="/WEB-INF/jsp/includes.jsp"/>
<jsp:directive.include file="/WEB-INF/jsp/header.jsp"/>
<script type="text/javascript">dojo.require("dijit.TitlePane");</script>
<div dojoType="dijit.TitlePane" style="width: 100%" title="Spring WebFlow - View State Two">
<h1>View State Two</h1>

<p>This is a new view state in your flow. The button below leads you to end state of this flow.</p>

<form method="POST" >
	<div class="submit">
		<input type="submit" id="cancel" name="_eventId_cancel" value="End Flow"/>	
	</div>		
</form>
</div>
<jsp:directive.include file="/WEB-INF/jsp/footer.jsp"/>