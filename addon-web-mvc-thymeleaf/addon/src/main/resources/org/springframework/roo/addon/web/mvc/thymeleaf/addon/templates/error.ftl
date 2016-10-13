<!DOCTYPE html>
<html lang="en" data-layout-decorator="layouts/default-layout">
<head>
    <meta charset="UTF-8" data-th-remove="all" />
    <meta http-equiv="X-UA-Compatible" content="IE=edge" data-th-remove="all" />
    <meta name="viewport" content="width=device-width, initial-scale=1"
      data-th-remove="all" />
    <meta name="description"
      content="A next-generation rapid application development tool for Java developers. With Roo you can easily build full Java applications in minutes."
      data-th-remove="all" />
    <meta name="author"
      content="Spring Roo development team"
      data-th-remove="all" />

    <link rel="shortcut icon" href="../../static/public/img/favicon.ico"
       data-th-remove="all" />

    <link rel="apple-touch-icon" href="../../static/public/img/apple-touch-icon.png"
       data-th-remove="all" />

    <title data-th-text="|${r"#{"}label_error${r"}"} - ${projectName}|">Error - ${projectName}</title>

    <!-- Bootstrap -->
    <link rel="stylesheet" type="text/css"
      href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.css"
      data-th-remove="all"></link>

    <!-- IE10 viewport hack for Surface/desktop Windows 8 bug -->
    <link rel="stylesheet" type="text/css"
      href="https://maxcdn.bootstrapcdn.com/css/ie10-viewport-bug-workaround.css"
      data-th-remove="all"></link>

    <!-- Font Awesome -->
    <link rel="stylesheet" type="text/css"
    href="https://maxcdn.bootstrapcdn.com/font-awesome/4.6.2/css/font-awesome.min.css"
    data-th-remove="all" />

    <!-- Bootswatch CSS custom -->
    <link rel="stylesheet" type="text/css"
      href="../static/public/css/theme.css"
      data-th-remove="all" />

    <!-- Roo CSS -->
    <link rel="stylesheet" type="text/css"
      href="../static/public/css/springroo.css"
      data-th-remove="all" />

   <!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->
    <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
    <!--[if lt IE 9]>
        <script src="https://oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js"></script>
      	<script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
    <![endif]-->

</head>
<#if userManagedComponents?has_content && userManagedComponents["body"]??>
  ${userManagedComponents["body"]}
<#else>
  <body id="body">
  <!--
    Only the internal content of the following tag "section" is included within
    the template, in "content" fragment
  -->
  <header>
    <h1 data-th-text="${r"#{"}label_errorpage${r"}"}">Error Page</h1>
  </header>

  <section data-layout-fragment="content">

    <div class="alert alert-danger fade in" role="alert">
      <h4 data-th-text="${r"#{"}label_errorpage_header${r"}"}">¡Error!</h4>
      <p>
        <span data-th-text="${r"#{info_error}"}">An unexpected error has occurred</span>
        (type=<span data-th-text="${r"${error}"}">Bad</span>, status=<span data-th-text="${r"${status}"}">500</span>).
      </p>
      <div data-th-text="${r"${message}"}"></div>
    </div>

  </section>

  <footer class="container">
    <div class="row">
      <div class="col-sm-6 col-sm-offset-3">
        <small class="clearfix">
            Made with <a href="http://projects.spring.io/spring-roo/" target="_blank">
            Spring Roo &copy; 2016</a> •
            We <span class="glyphicon glyphicon-heart"></span> 
            <a href="https://github.com/spring-projects/spring-roo/" target="_blank">Open source</a> •
            <a data-th-href="@{/accessibility}" href="accessibility.html"><span data-th-text="${r"#{"}label_accessibility${r"}"}">Accessibility</span></a>
        </small>
        </div>
      </div>
      <!-- certified logos -->
      <div class="row">
        <div class="col-sm-6 col-sm-offset-6 text-right">

         <a title="Explanation of WCAG 2.0 Level Double-A Conformance"
            target="_blank"
            href="http://www.w3.org/WAI/WCAG2AA-Conformance">
            <img height="32" width="88"
                 src="http://www.w3.org/WAI/wcag2AA"
                 alt="Level Double-A conformance, W3C WAI Web Content
                 Accessibility Guidelines 2.0">
         </a>
         &nbsp;
         <a title="Application developed and tested with OWASP -
             Open Web Application Security Project"
            target="_blank"
            href="https://www.owasp.org">
          <img height="32" width="90"
               src="../static/public/img/owasp_logo.png"
               alt="Application developed and tested with OWASP">
         </a>
        </div>
      </div>
  </footer>

</body>
</#if>
</html>