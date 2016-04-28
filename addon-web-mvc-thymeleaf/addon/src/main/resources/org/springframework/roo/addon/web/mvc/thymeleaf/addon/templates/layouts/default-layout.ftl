<!DOCTYPE html>
<html>
  <head>

    <meta charset="UTF-8" />
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <meta name="description" content="${projectName}"/>
    <meta name="author" content="Spring Roo" />
    <link rel="icon" href="../../static/public/img/favicon.ico" data-th-href="@{/public/img/favicon.ico}" />

    <title data-layout-title-pattern="$DECORATOR_TITLE - $CONTENT_TITLE">${projectName}</title>

    <!-- Bootstrap -->
    <link rel="stylesheet" type="text/css" 
          href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.css"></link>        
    <link rel="stylesheet" type="text/css" 
          href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap-theme.css"></link>      

    <!-- Spring Roo CSS -->
    <link rel="stylesheet" type="text/css"
          href="../../static/public/css/standard.css" data-th-href="@{/public/css/standard.css}" />
    <noscript><link rel="stylesheet" href="../../static/public/css/nojs-standard.css" data-th-href="@{/public/css/nojs-standard.css}"/></noscript>

    <!-- HTML5 shim y Respond.js to support HTML5 elements and media queries -->
    <!--[if lt IE 9]>
      <script src="js/html5shiv.min.js"></script>
      <script src="js/respond.min.js"></script>
    <![endif]-->

   </head>
  <body>
    <div data-layout-include="fragments/session :: session">
      <span>Session</span>
    </div>
    <div class="container bg-container">
      <header role="banner">
        <div data-layout-include="fragments/header :: header">
          <h1>Sample Header</h1>
        </div>
        <div data-layout-include="fragments/menu :: menu">
          <span>Application menu</span>
        </div>
      </header>
      <section data-layout-fragment="content">
        <h2>Sample Body</h2>
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
        &copy; 2016 Powered By Spring Roo
    </footer>

    <!-- Bootstrap core JavaScript
    ================================================== -->
    <!-- JQuery -->
    <script type="text/javascript" charset="utf8" src="https://code.jquery.com/jquery-1.12.3.js"></script>
    <!-- Bootstrap -->
    <script type="text/javascript" src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/js/bootstrap.js"></script>
  
    <!-- IE10 viewport hack for Surface/desktop Windows 8 bug -->
     <script src="../../static/public/js/ie10-viewport-bug-workaround.js" data-th-src="@{/public/js/ie10-viewport-bug-workaround.js}">
    </script>

    <div data-layout-fragment="javascript">
      <!-- Will include javascript code -->
    </div>

    <script src="../../static/public/js/main.js" data-th-src="@{/public/js/main.js}">
    </script>
  </body>
</html>