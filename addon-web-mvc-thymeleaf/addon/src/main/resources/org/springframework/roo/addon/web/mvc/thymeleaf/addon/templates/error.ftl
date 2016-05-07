<!DOCTYPE html>
<html data-layout-decorator="layouts/default-layout">
<head>
    <meta charset="UTF-8" data-th-remove="all" />
    <meta http-equiv="X-UA-Compatible" content="IE=edge" data-th-remove="all" />
    <meta name="viewport" content="width=device-width, initial-scale=1"
      data-th-remove="all" />
    <meta name="description"
      content="Spring Roo"
      data-th-remove="all" />
    <meta name="author"
      content="Spring Roo"
      data-th-remove="all" />
     <link data-th-href="@{/public/img/favicon.ico}" data-th-remove="all" rel="icon"
       href="../../static/public/img/favicon.ico" />

    <link rel="shortcut icon" href="../../static/public/img/favicon.ico"
       data-th-remove="all" />

    <link rel="apple-touch-icon" href="../../static/public/img/apple-touch-icon.png"
       data-th-remove="all" />

    <title data-th-text="${r"#{"}label_error${r"}"}">Error</title>

    <!-- Bootstrap -->
    <link rel="stylesheet" type="text/css"
      href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.css"
      data-th-remove="all"></link>

    <!-- Roo CSS -->
    <link rel="stylesheet" type="text/css"
      href="../static/public/css/springroo.css"
      data-th-remove="all" />

    <!-- HTML5 shim y Respond.js para soporte de elementos HTML5 en IE8 y media queries -->
    <!--[if lt IE 9]>
       <script src="/public/js/html5shiv.min.js"></script>
        <script src="/public/js/respond.min.js"></script>
    <![endif]-->

</head>
<body>
  <header>
    <h1 data-th-text="${r"#{"}label_errorpage${r"}"}">Error Page</h1>
  </header>

  <section data-layout-fragment="content">

    <div class="alert alert-danger fade in" role="alert">
      <h4 data-th-text="${r"#{"}label_errorpage_header${r"}"}">¡Error!</h4>
      <p>
        <span data-th-text="${r"#{info_error}"}">An error occurred</span>
        (type=<span data-th-text="${r"${error}"}">Bad</span>, status=<span data-th-text="${r"${status}"}">500</span>).
      </p>
      <div data-text="${r"${message}"}"></div>
    </div>

  </section>
  <footer> &copy; Powered By Spring Roo </footer>
</body>
</html>