<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8" />
    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <meta name="description"
      content="A next-generation rapid application development tool for Java developers. With Roo you can easily build full Java applications in minutes." />
    <meta name="author"
      content="Spring Roo team" />
    <link data-th-href="@{/public/img/favicon.ico}" data-th-remove="all" rel="icon"
       href="../../static/public/img/favicon.ico" />

    <link rel="shortcut icon" href="../../static/public/img/favicon.ico"
      data-th-href="@{/public/img/favicon.ico}" />

    <link rel="apple-touch-icon" href="../../static/public/img/apple-touch-icon.png"
      data-th-href="@{/public/img/apple-touch-icon.png}" />

    <title data-layout-title-pattern="$DECORATOR_TITLE - $CONTENT_TITLE" data-th-title="${r"${projectName}"}">Spring Roo application</title>

    <!-- Bootstrap -->
    <link rel="stylesheet" type="text/css"
        href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.css"></link>
        
    <link rel="stylesheet" type="text/css"
        href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap-theme.css"></link>      
        
    <!-- Select2 -->
    <link rel="stylesheet" type="text/css"
      data-th-href="@{/public/css/select2.css}"
      href="../../static/public/css/select2.css" />
    <link rel="stylesheet" type="text/css"
      data-th-href="@{/public/css/select2-bootstrap.css}"
      href="../../static/public/css/select2-bootstrap.css" />
      
      
    <!-- DateTimePicker -->
    <link rel="stylesheet" type="text/css"
        data-th-href="@{/public/css/jquery.datetimepicker.css}"
        href="../../static/public/css/jquery.datetimepicker.css" />
    <script data-th-src="@{/public/js/jquery.min.js}"
        src="../../static/public/js/jquery.min.js"></script>

    <!-- Font Awesome -->
    <link rel="stylesheet" type="text/css" 
      href="https://maxcdn.bootstrapcdn.com/font-awesome/4.6.2/css/font-awesome.min.css"/>

    <!-- Bootswatch CSS custom -->
    <link rel="stylesheet" type="text/css"
      href="../../static/public/css/theme.css"
      data-th-href="@{/public/css/theme.css}" />

    <!-- Roo CSS -->
    <link rel="stylesheet" type="text/css"
      href="../../static/public/css/springroo.css"
      data-th-href="@{/public/css/springroo.css}" />

    <!-- HTML5 shim y Respond.js para soporte de elementos HTML5 en IE8 y media queries -->
    <!--[if lt IE 9]>
          <script src="/public/js/html5shiv.min.js"></script>
          <script src="/public/js/respond.min.js"></script>
        <![endif]-->

  </head>
  <body>

     <header role="banner">
        <div data-layout-include="fragments/header :: header">
          <!-- Content replaced by the header template fragment header.html -->
          <h1>Sample page header for direct display of the template</h1>
        </div>

        <div data-layout-include="fragments/menu :: menu">
          <!-- Content replaced by the menu template fragment menu.html -->
          <span>Application menu</span>
        </div>

    </header>

    <div class="container bg-container">


      <section data-layout-fragment="content">
        <!-- Content replaced by the content fragment of the page displayed -->
        <h2>Sample static body for direct display of the template</h2>
        <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent
          scelerisque neque neque, ac elementum quam dignissim interdum. Phasellus et
          placerat elit. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
          Praesent scelerisque neque neque, ac elementum quam dignissim interdum.
          Phasellus et placerat elit.</p>
      </section>
    </div>

    <footer class="container" data-layout-include="fragments/footer :: footer">
      <!-- Content replaced by the footer template fragment footer.html -->
      &copy; 2016 Spring Roo (footer for example for direct display of the template)
    </footer>

      <!-- Bootstrap core JavaScript
    ================================================== -->
  <script data-th-src="@{/public/js/jquery.min.js}"
    src="../../static/public/js/jquery.min.js"></script>
  <script data-th-src="@{/public/js/bootstrap.min.js}"
    src="../../static/public/js/bootstrap.min.js"></script>

    <!-- IE10 viewport hack for Surface/desktop Windows 8 bug -->
    <script src="../../static/public/js/ie10-viewport-bug-workaround.js"
      data-th-src="@{/public/js/ie10-viewport-bug-workaround.js}">
    </script>

      <div data-layout-fragment="javascript">
          <!-- Moment.js --> 
          <script data-th-src="@{/public/js/moment.js}" src="../../static/public/js/moment.js"></script> 
          <script data-th-src="@{/public/js/moment-} + ${r"${application_locale}"} + .js" src="../../static/public/js/moment-en.js"></script> 
          <!-- Select2 -->
          <script src="../../static/public/js/select2.full.js" data-th-src="@{/public/js/select2.full.js}"></script>
          <script src="../../static/public/js/select2.full-es.js" data-th-src="@{/public/js/select2.full-es.js}"></script>
          <script src="../../static/public/js/select2-defaults.js" data-th-src="@{/public/js/select2-defaults.js}"></script>
          <!-- DateTime Picker -->
          <script src="../../static/public/js/jquery.datetimepicker.full.min.js" data-th-src="@{/public/js/jquery.datetimepicker.full.min.js}"></script>
          <script src="../../static/public/js/datetimepicker-defaults.js" data-th-src="@{/public/js/datetimepicker-defaults.js}"></script>
      </div>

    <!-- Application -->
    <script src="../../static/public/js/main.js"
      data-th-src="@{/public/js/main.js}">
      </script>
  </body>
</html>