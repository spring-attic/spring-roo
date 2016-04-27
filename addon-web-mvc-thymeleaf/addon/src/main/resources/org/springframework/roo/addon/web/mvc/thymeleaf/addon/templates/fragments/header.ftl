<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8" />
    <title>Header</title>
  </head>
  <body>
    <div data-layout-fragment="header">
      <div class="bg-header">
        <div class="organization-logo"><a title="${projectName}" href="/"><img alt="${projectName}" data-th-src="@{/img/logo_spring_roo.png}" src="../../static/img/logo_spring_roo.png" /></a></div>
        <div class="application-name">${projectName}</div>
      </div>
    </div>
  </body>
</html>