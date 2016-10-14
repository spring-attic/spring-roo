<!DOCTYPE html>
<html lang="es" data-layout-decorator="layouts/default-layout">
<head>
<meta charset="UTF-8" data-th-remove="all" />
<meta http-equiv="X-UA-Compatible" content="IE=edge" data-th-remove="all" />
<meta name="viewport" content="width=device-width, initial-scale=1"
  data-th-remove="all" />
<meta name="description"
  content="Spring Roo, a next-generation rapid application development tool for Java developers. With Roo you can easily build full Java applications in minutes." data-th-remove="all"
  data-th-remove="all" />
<meta name="author"
  content="Spring Roo development team"
  data-th-remove="all" />

<link rel="shortcut icon" href="../static/public/img/favicon.ico"
  data-th-remove="all" />

<link rel="apple-touch-icon" href="../static/public/img/apple-touch-icon.png"
   data-th-remove="all" />

<title data-th-text="|${r"#{"}label_accessibility${r"}"}- ${projectName}|">Accessibility - ${projectName}</title>

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

    <!--Main container-->
    <div class="bg-container">

      <!-- Header -->
      <header role="banner">

        <!--Banner -->
        <div class="bg-header">
          <div class="jumbotron bg-banner">
              <div class="container">
                <h1 class="project-name">${projectName}</h1>
                <h2 class="project-tagline">Hello, this is your home page.</h2>
              </div>
          </div>
        </div>

        <!-- Main navbar -->
        <nav class="navbar navbar-inverse navbar-fixed-top">
         <div class="container">

            <!-- navbar-header -->
            <div class="navbar-header">
              <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#mainnav" aria-expanded="false">
                <span class="sr-only">Menu</span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
              </button>

              <!-- Brand logo -->
              <div class="organization-logo navbar-brand">
                <a title="${projectName}" href="/">
                  <img alt="${projectName}" data-th-src="@{/public/img/logo.png}" src="../static/public/img/logo.png" />
                </a>
              </div>
              <!-- Name application -->
              <div class="application-name navbar-brand hidden-xs"><a href="/" data-th-href="@{/}">${projectName}</a></div>

            </div>
            <!-- navbar header -->

            <!-- menu -->
            <div id="mainnav" class="navbar-collapse collapse">

              <ul class="nav navbar-nav">
                <li class="active"><a href="#">Menu 1 active</a></li>
                <li><a href="#">Menu 2</a></li>
                <li class="dropdown">
                  <a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">Menu 3 dropdown<span class="caret"></span></a>
                  <ul class="dropdown-menu">
                    <li><a href="#">Submenu 1</a></li>
                    <li><a href="#">Submenu 2</a></li>
                    <li><a href="#">Submenu 3</a></li>
                  </ul>
                </li>
              </ul>

              <!-- Language -->
              <ul class="nav navbar-nav navbar-right upper-nav language">
                <li class="dropdown">
                  <a class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">
                    <span class="glyphicon glyphicon-user" aria-hidden="true"></span>&nbsp;<span class="hidden-sm">Language</span><span class="caret"></span>
                  </a>
                  <ul class="dropdown-menu" id="languageFlags">
                   <li id="enFlag"><a href="?lang=en"><img class="flag"
                   data-th-src="@{/static/public/img/en.png}" src="../static/public/img/en.png"
                   alt="English">&nbsp;<span>English</span></a> </li>
                   <li id="esFlag"><a href="?lang=es"><img class="flag"
                   data-th-src="@{/static/public/img/es.png}" src="../static/public/img/es.png"
                   alt="Spanish">&nbsp;<span>Spanish</span></a> </li>
                 </ul>
               </li>
              </ul>

              <!-- User menu -->
              <ul class="nav navbar-nav navbar-right upper-nav session">
                <li class="dropdown">
                  <a class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">
                    <span class="glyphicon glyphicon-user" aria-hidden="true"></span>&nbsp;<span class="hidden-sm">User</span><span class="caret"></span>
                  </a>
                  <ul class="dropdown-menu">
                   <li><a href="#"><span class="glyphicon glyphicon-wrench" aria-hidden="true"></span>&nbsp;<span>Admin Profile</span></a></li>
                   <li><a href="#"><span class="glyphicon glyphicon-lock" aria-hidden="true"></span>&nbsp;<span>Change password</span></a></li>
		   <li><form action="/logout" method="post">
		     <button type="button" class="btn btn-link">
		       <span class="glyphicon glyphicon-log-out" aria-hidden="true"></span>
		       <span>Log out</span>
		     </button>
		   </form></li>
                 </ul>
                </li>
              </ul>

              <!-- User menu links -->
              <ul class="nav navbar-nav navbar-right upper-nav links">
                <li><a href="#"><span class="glyphicon glyphicon-envelope" aria-hidden="true"></span>&nbsp;<span class="hidden-sm">Contact</span></a></li>
                <li><a href="#"><span class="glyphicon glyphicon-question-sign" aria-hidden="true"></span>&nbsp;<span class="hidden-sm">Help</span></a></li>
              </ul>

           </div>

          </div>
        </nav>

      </header>

      <!--Content-->
      <section data-layout-fragment="content">

        <div class="container content">

          <div class="box-center">

          <h2 data-th-text="${r"#{"}label_accessibility${r"}"}">Accessibility</h2>

          <p class="lead" data-th-text="${r"#{"}label_accessibility_lead${r"}"}">Accessibility policy application</p>
          <p data-th-utext="${r"#{"}label_accessibility_text${r"}"}">
           Spring Roo Application is committed to ensuring the accessibility of
           its web content to people with disabilities. All of the content on
           our website will meet <a href="https://www.w3.org/TR/WCAG/" target="_blank"
           title="Will open in a new window to the page https://www.w3.org/TR/WCAG/">
           W3C WAI's Web Content Accessibility Guidelines</a> 2.0, Level AA
           conformance. Any issues should be reported to
           <a href="mailto:springroo@disid.com?Subject=Spring%20Roo%20Accessibility"
           target="_top">springroo@disid.com</a>.
           The technologies that is depended to access the accessible content are HTML, CSS and Javascript.
          </p>
          </div><!-- box-center -->


        </div>
        <!--/content-->

      </section>
    </div>
    <!--container-->

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

    <!-- JavaScript
    ================================================== -->
    <!-- JQuery -->
    <script type="text/javascript" charset="utf8"
      src="https://code.jquery.com/jquery-1.12.3.js"></script>

    <!-- Bootstrap -->
    <script type="text/javascript"
      src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.js"></script>

    <!-- IE10 viewport hack for Surface/desktop Windows 8 bug -->
    <script src="https://maxcdn.bootstrapcdn.com/js/ie10-viewport-bug-workaround.js"></script>

    <!-- Application -->
    <script src="../static/public/js/main.js"></script>
</body>
</#if>
</html>