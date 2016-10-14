<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8" />
    <title>Header</title>
  </head>
<#if userManagedComponents?has_content && userManagedComponents["body"]??>
  ${userManagedComponents["body"]}
<#else>
  <body id="body">
    <!--
    Only the internal content of the following div is included within
    the template, in header fragment
    -->
    <div data-layout-fragment="header">

      <!-- BANNER -->
      <div class="bg-header">
        <div class="jumbotron bg-banner">
            <div class="container">
              <h1 class="project-name">${projectName}</h1>
              <h2 class="project-tagline" data-th-text="${r"#{"}info_homepage_project${r"}"}">Hello, this is your home page.</h2>
            </div>
        </div>
      </div>

    </div>
  </body>
</#if>
</html>