<!DOCTYPE html>
<html data-layout-decorator="layouts/default-layout">
  <head>
    <meta charset="UTF-8" data-th-remove="all"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge" data-th-remove="all"/>
    <meta name="viewport" content="width=device-width, initial-scale=1" data-th-remove="all"/>
    <meta name="description" content="${projectName}" data-th-remove="all"/>
    <meta name="author" content="Spring Roo" data-th-remove="all" />
    <title>Home</title>

    <!-- Bootstrap core CSS -->
    <link rel="stylesheet" type="text/css"
          href="../static/public/css/bootstrap.min.css" data-th-href="@{/public/css/bootstrap.min.css}" data-th-remove="all" />

    <!-- Spring Roo CSS -->
    <link rel="stylesheet" type="text/css"
          href="../static/public/css/standard.css" data-th-href="@{/public/css/standard.css}" data-th-remove="all"/>
    <noscript><link rel="stylesheet" href="../static/public/css/nojs-standard.css" data-th-href="@{/public/css/nojs-standard.css}" data-th-remove="all"/></noscript>

  </head>
  <body>

  <div class="container upper-nav">
    <div class="session">
      <div><span class="glyphicon glyphicon-user" aria-hidden="true"></span>User</div>
      <div><span class="glyphicon glyphicon-calendar" aria-hidden="true"></span>Last Access: 00-00-0000</div>
      <button type="submit" class="exit"><span class="glyphicon glyphicon-off" aria-hidden="true"></span>Exit</button>
    </div>
  </div>

  <!--START CONTAINER-->
  <div class="container bg-container">

    <!-- HEADER -->
    <header role="banner">
      <div class="bg-header">
        <div class="organization-logo"><a title="${projectName}" href="/"><img alt="${projectName}" src="../static/public/img/logo_spring_roo.png" /></a></div>
        <div class="application-name">${projectName}</div>
      </div>

      <nav class="navbar navbar-default">
        <div class="container-fluid">

          <div class="navbar-header">
            <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1" aria-expanded="false">
              <span class="sr-only">Dropdown</span>
              <span class="icon-bar"></span>
                <span class="icon-bar"></span>
              <span class="icon-bar"></span>
            </button>
            <a class="navbar-brand" href="#">Main Menu</a>
          </div>

          <div id="bs-example-navbar-collapse-1" class="navbar-collapse collapse">
            <ul class="nav navbar-nav">
              <li class="active"><a href="#">Active Menu 1</a></li>
              <li><a href="#">Menu 2</a></li>
              <li class="dropdown">
                <a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">Dropdown Menu 3<span class="caret"></span></a>
                <ul class="dropdown-menu">
                  <li><a href="#">Submenu 1</a></li>
                  <li><a href="#">Submenu 2</a></li>
                  <li><a href="#">Submenu 3</a></li>
                </ul>
              </li>
            </ul>
          </div>
        </div>
      </nav>
    </header>
    <!-- END HEADER -->

    <!--START CONTENT-->
    <section data-layout-fragment="content">
      <div class="container-fluid content">
        <section class="main">
            <div class="jumbotron">
              <h2>Welcome to ${projectName}</h2>
            </div>
        </section>
      </div>
      <!--END CONTENT-->
    </section>
  </div>
  <!--END CONTAINER-->

  <footer class="container">
    <p class="text-right">© Powered By Spring Roo</p>
  </footer>


    <!-- Bootstrap core JavaScript
    ================================================== -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script src="../static/public/js/jquery.min.js" data-th-src="@{/public/js/jquery.min.js}" data-th-remove="all">
    </script>
    <script src="../static/public/js/bootstrap.min.js" data-th-src="@{/public/js/bootstrap.min.js}" data-th-remove="all">
    </script>
    <script src="../static/public/js/main.js" data-th-src="@{/public/js/main.js}" data-th-remove="all">
    </script>
    <!-- IE10 viewport hack for Surface/desktop Windows 8 bug -->
     <script src="../static/public/js/ie10-viewport-bug-workaround.js" data-th-src="@{/public/js/ie10-viewport-bug-workaround.js}" data-th-remove="all">
    </script>

    <div data-layout-fragment="javascript" >
    </div>

  </body>
</html>