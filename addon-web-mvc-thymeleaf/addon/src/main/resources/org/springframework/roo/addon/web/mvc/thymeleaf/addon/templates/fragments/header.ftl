<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8" />
    <title>Header</title>
  </head>
  <body>
    <!--
    Only the internal content of the following div is included within
    the template, in header fragment
    -->
    <div data-layout-fragment="header">

      <div class="bg-header">
        <div class="jumbotron bg-banner">
            <div class="container">
              <h1 class="project-name" data-th-text="${r"#{"}label_welcome${r"}"}">Hello, we are Spring Roo!</h1>
              <h2 class="project-tagline" data-th-text="${r"#{"}info_startproject${r"}"}">Get start your next awesome project</h2>
            </div>
        </div>
      </div>

    </div>
  </body>
</html>