<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8" />
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <meta name="author" content="Spring Roo" />
    <link rel="icon" href="../../static/img/favicon.ico" data-th-href="@{/img/favicon.ico}" />

    <!--/*  El título final será la unión del título en el layout más el título de la página. */-->
    <title data-layout-title-pattern="$DECORATOR_TITLE - $CONTENT_TITLE">${projectName} - ${version}</title>

    <!-- Bootstrap core CSS -->
    <link rel="stylesheet" type="text/css"
          href="../../static/css/bootstrap.min.css" data-th-href="@{/css/bootstrap.min.css}" />

    <!-- Standard CSS -->
    <link rel="stylesheet" type="text/css"
          href="../../static/css/standatd.css" data-th-href="@{/css/standard.css}" />
    <noscript><link rel="stylesheet" href="../../static/css/nojs-standard.css" data-th-href="@{/css/nojs-standard.css}"/></noscript>

    <!-- HTML5 shim y Respond.js to be able to support HTML5 elements on IE8 and media queries -->
    <!--[if lt IE 9]>
      <script src="js/html5shiv.min.js"></script>
      <script src="js/respond.min.js"></script>
    <![endif]-->

   </head>
  <body>
    <!-- This content will be replaced with the fragment content of the page session.html -->
    <div data-layout-include="fragments/session :: session">
      <span>Session data</span>
    </div>
    <div class="container bg-container">
      <header role="banner">
        <div data-layout-include="fragments/header :: header">
          <!-- This content will be replaced with the fragment content of the page header.html -->
          <h1>Sample Header to open this template directly on web browser as .html file</h1>
        </div>
        <div data-layout-include="fragments/menu :: menu">
          <!-- This content will be replaced with fragment content menu.html -->
          <span>Application Menu</span>
        </div>
      </header>
      <section data-layout-fragment="content">
        <!-- This content will be replaced with the fragment content of the page body-->
        <h2>Static body to open this template directly on web browser as .html file</h2>
        <p>
          Lorem ipsum dolor sit amet, consectetur adipiscing elit.
          Praesent scelerisque neque neque, ac elementum quam dignissim interdum.
          Phasellus et placerat elit. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
          Praesent scelerisque neque neque, ac elementum quam dignissim interdum.
          Phasellus et placerat elit.
        </p>
      </section>
    </div>

    <footer class="container" data-layout-include="fragments/footer :: footer">
        &copy; Spring Roo 
    </footer>

    <!-- Bootstrap core JavaScript
    ================================================== -->
    <script src="../../static/js/jquery.min.js" data-th-src="@{/js/jquery.min.js}">
    </script>
    <script src="../../static/js/bootstrap.min.js" data-th-src="@{/js/bootstrap.min.js}">
    </script>
    <script src="../../static/js/main.js" data-th-src="@{/js/main.js}">
    </script>
    <!-- IE10 viewport hack for Surface/desktop Windows 8 bug -->
     <script src="../../static/js/ie10-viewport-bug-workaround.js" data-th-src="@{/js/ie10-viewport-bug-workaround.js}">
    </script>

  </body>
</html>