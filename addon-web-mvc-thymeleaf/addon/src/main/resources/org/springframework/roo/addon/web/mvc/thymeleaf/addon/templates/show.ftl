<!DOCTYPE html>
<html lang="es" data-layout-decorator="layouts/default-layout">
<head>
<meta charset="utf-8" data-th-remove="all" />
<meta http-equiv="X-UA-Compatible" content="IE=edge" data-th-remove="all" />
<meta name="viewport" content="width=device-width, initial-scale=1"
  data-th-remove="all" />
<meta name="description"
  content="${projectName}"
  data-th-remove="all" />
<meta name="author"
  content="Spring Roo"
  data-th-remove="all" />
<link data-th-href="@{/public/img/favicon.ico}" data-th-remove="all" rel="icon"
  href="../../static/public/img/favicon.ico" />

<title>Show ${entityName}</title>

<!-- Bootstrap core CSS -->
<link data-th-href="@{/public/css/bootstrap.min.css}" data-th-remove="all"
  href="../../static/public/css/bootstrap.min.css" rel="stylesheet" />

<!-- Spring Roo CSS -->
<link data-th-href="@{/public/css/standard.css}" data-th-remove="all"
  href="../../static/public/css/standard.css" rel="stylesheet" />
<noscript data-th-remove="all">
  <link data-th-href="@{/public/css/nojs-standard.css}" data-th-remove="all"
    rel="stylesheet" href="../../static/public/css/nojs-standard.css" />
</noscript>

</head>

<body>

  <!--START CONTAINER-->
  <div class="container bg-container">

    <!-- session -->
    <div class="container upper-nav">
      <div class="session">
        <div>
          <span class="glyphicon glyphicon-user" aria-hidden="true"></span>User
        </div>
        <div>
          <span class="glyphicon glyphicon-calendar" aria-hidden="true"></span>Last Access: 00-00-0000
        </div>
        <button type="submit" class="exit">
          <span class="glyphicon glyphicon-off" aria-hidden="true"></span>Exit
        </button>
      </div>
    </div>

    <!-- HEADER -->
    <header role="banner">

      <div class="bg-header">
        <div class="organization-logo">
          <a
            title="${projectName}"
            href="/"><img
            alt="${projectName}"
            src="../../static/public/img/logo_spring_roo.png" /></a>
        </div>
        <div class="application-name">
          ${projectName}
        </div>
      </div>

      <!-- NAVIGATION MENU -->
      <nav class="navbar navbar-default">
        <div class="container-fluid">

          <div class="navbar-header">
            <button type="button" class="navbar-toggle collapsed"
              data-toggle="collapse" data-target="#bs-example-navbar-collapse-1"
              aria-expanded="false">
              <span class="sr-only">Dropdown</span> <span class="icon-bar"></span> <span
                class="icon-bar"></span> <span class="icon-bar"></span>
            </button>
            <a class="navbar-brand" href="#">Main Menu</a>
          </div>

          <div id="bs-example-navbar-collapse-1" class="navbar-collapse collapse">
            <ul class="nav navbar-nav">
              <li class="active"><a href="#">Active Menu 1</a></li>
              <li><a href="#">Menu 2</a></li>
              <li class="dropdown"><a href="#" class="dropdown-toggle"
                data-toggle="dropdown" role="button" aria-haspopup="true"
                aria-expanded="false">Dropdown Menu 3<span class="caret"></span></a>
                <ul class="dropdown-menu">
                  <li><a href="#">Submenu 1</a></li>
                  <li><a href="#">Submenu 2</a></li>
                  <li><a href="#">Submenu 3</a></li>
                </ul></li>
            </ul>
          </div>

        </div>
      </nav>

    </header>
    <!-- END HEADER -->

    <!--START CONTENT-->
    <section data-layout-fragment="content" data-th-object="${modelAttribute}">

        <!-- CONTENT -->
        <h1>${entityName}</h1>

        <div class="panel panel-default">
          <div class="panel-heading">
            <h3 class="panel-title">Show ${entityName}</h3>
          </div>
          <div class="panel-body">
            <dl class="dl-horizontal">
              <#list fields as field>
              <dt>${field.fieldName}</dt>
              <dd data-th-text="*{${field.fieldName}}">${field.fieldName}Value</dd>
              </#list>
            </dl>

          </div>
        </div>

        <div class="clearfix">
          <div class="pull-left">
            <button onclick="location.href='list.html'"
              data-th-onclick="'location.href=\'' + @{${controllerPath}} + '\''" type="button"
              class="btn btn-default">
              <span class="glyphicon glyphicon-chevron-left" aria-hidden="true"></span><span>Back</span>
            </button>
          </div>
        </div>

      </div>
    </section>
    <!--END CONTENT-->

  </div>
  <!--END CONTAINER-->

  <footer class="container">
    <p class="text-right">© Powered By Spring Roo </p>
  </footer>

  <!-- Bootstrap core JavaScript
  ================================================== -->
  <script data-th-remove="all" data-th-src="@{/public/js/jquery-1.12.0.min.js}"
    src="../../static/public/js/jquery-1.12.0.min.js">

  </script>
  <script data-th-remove="all" data-th-src="@{/public/js/bootstrap.min.js}"
    src="../../static/public/js/bootstrap.min.js">

  </script>
  <script data-th-remove="all" data-th-src="@{/public/js/main.js}"
    src="../../static/public/js/main.js">

  </script>
  <!-- IE10 viewport hack for Surface/desktop Windows 8 bug -->
  <script data-th-remove="all"
    data-th-src="@{/public/js/ie10-viewport-bug-workaround.js}"
    src="../../static/public/js/ie10-viewport-bug-workaround.js">

  </script>
</body>

</html>