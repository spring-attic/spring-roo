<!--WARNING: This file is maintained by ROO! IT WILL BE OVERWRITTEN unless you specify null	@RooWebScaffold(automaticallyMaintainView = false) in the governing controller-->
<jsp:directive.include file="/WEB-INF/jsp/includes.jsp"/>
<jsp:directive.include file="/WEB-INF/jsp/header.jsp"/>
<script type="text/javascript">dojo.require("dijit.TitlePane");</script>
<div dojoType="dijit.TitlePane" style="width: 100%" title="Spring WebFlow - View State One">
<h1>View State One</h1>

<p>This is a simple example to get started with Spring Web Flow. The buttons below lead you to another view state (Proceed) or to an end state.</p>

<form method="POST" >
	<div class="submit">
		<input type="submit" id="cancel" name="_eventId_cancel" value="Cancel"/>			
		<input type="submit" id="success" name="_eventId_success" value="Proceed" />
	</div>
</form>
</div>
<jsp:directive.include file="/WEB-INF/jsp/footer.jsp"/>