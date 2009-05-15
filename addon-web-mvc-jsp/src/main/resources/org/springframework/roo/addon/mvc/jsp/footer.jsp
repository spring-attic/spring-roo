
	  <table class="footer">
	    <tr>
	      <td><a href="<c:url value="/" />">Home</a></td>
	      <td align="right">
	      <% if (request.getUserPrincipal() != null) { %>
	      <a href="<c:url value="/j_spring_security_logout" />">Logout</a></td>
	      <% } %>
	      <td align="right"><img src="<c:url value="/static/images/springsource-logo.png" />" alt="Sponsored by SpringSource"/></td>
	    </tr>
	  </table>

	</div>
  </div>
</body>

</html>
