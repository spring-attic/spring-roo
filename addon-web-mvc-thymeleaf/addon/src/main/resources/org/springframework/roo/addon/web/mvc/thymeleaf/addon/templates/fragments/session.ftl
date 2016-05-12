<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8" />
    <title>Session</title>
  </head>
  <body>
    <!--
    Only the internal content of the following div is included within
    the template, in session fragment
    -->
    <div data-layout-fragment="session">

      <!-- User menu -->
      <ul class="nav navbar-nav navbar-right upper-nav session">
          <li><a href="#"><span class="glyphicon glyphicon-user" aria-hidden="true"></span>&nbsp;<span class="hidden-sm" data-th-text="${r"#{"}label_user${r"}"}"> User</span></a></li>
      </ul>

    </div>

  </body>
</html>