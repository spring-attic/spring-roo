<!DOCTYPE html>
<html lang="en" data-layout-decorate="layouts/default-layout-no-menu">
  <head>
    <meta charset="UTF-8" data-th-remove="all"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge" data-th-remove="all"/>
    <meta name="viewport" content="width=device-width, initial-scale=1" data-th-remove="all"/>
    <meta name="description" content="Spring Roo, a next-generation rapid application development tool for Java developers.
      With Roo you can easily build full Java applications in minutes." data-th-remove="all"/>
    <meta name="author"
      content="Spring Roo development team" data-th-remove="all"/>

    <link rel="shortcut icon" href="../public/img/favicon.ico"
       data-th-remove="all"/>
    <link rel="apple-touch-icon" href="../public/img/apple-touch-icon.png"
       data-th-remove="all"/>
    <title data-th-text="${r"#{"}label_login}">Login- Spring Roo application</title>

    <!--/* Bootstrap */-->
    <link rel="stylesheet" type="text/css"
      href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.css"
      data-th-remove="all"/>
    <!--/* IE10 viewport hack for Surface/desktop Windows 8 bug */-->
    <link rel="stylesheet" type="text/css"
      href="https://maxcdn.bootstrapcdn.com/css/ie10-viewport-bug-workaround.css"
      data-th-remove="all"/>
    <!--/* Font Awesome */-->
    <link rel="stylesheet" type="text/css"
      href="https://maxcdn.bootstrapcdn.com/font-awesome/4.6.2/css/font-awesome.min.css"
      data-th-remove="all"/>
    <!--/* Bootswatch CSS custom */-->
    <link rel="stylesheet" type="text/css"
      href="../static/public/css/theme.css"
      data-th-remove="all"/>
    <!--/* Roo CSS */-->
    <link rel="stylesheet" type="text/css"
      href="../static/public/css/springroo.css"
      data-th-remove="all"/>
    <!--/* HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries */-->
    <!--/* WARNING: Respond.js doesn't work if you view the page via file:// */-->
    <!--/*[if lt IE 9]>
      <script src="https://oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js" data-th-remove="all"></script>
      <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js" data-th-remove="all"></script>
    <![endif]*/-->

  </head>
<#if userManagedComponents?has_content && userManagedComponents["body"]??>
  ${userManagedComponents["body"]}
<#else>
  <body id="body">

      <!-- HEADER -->
      <header role="banner">

        <!-- Main navbar -->
        <nav class="navbar navbar-default navbar-static-top">
         <div class="container">
            <!-- navbar-header -->
            <div class="navbar-header">
              <!-- Brand logo -->
              <div class="organization-logo navbar-brand">
                <a title="${projectName}" href="/">
                  <img alt="${projectName}" data-th-src="@{/public/img/logo.png}" src="../static/public/img/logo.png" />
                </a>
              </div>
              <!-- Name application -->
              <div class="application-name navbar-brand hidden-xs">
              	<a href="/" data-th-href="@{/}">${projectName}</a>
              </div>

            </div>
            <!-- /navbar-header -->

            <!-- menu -->
            <div id="mainnav" class="navbar-collapse collapse">
              <!--/* Language */-->
              <ul class="nav navbar-nav navbar-right upper-nav languages">
                <li class="dropdown">
                  <a class="dropdown-toggle" data-toggle="dropdown" role="button"
                    aria-haspopup="true" aria-expanded="false">
                    <span class="glyphicon glyphicon-globe" aria-hidden="true">
                    </span>&nbsp;<span class="hidden-sm"
                    data-th-text="${r"#{"}language_label${r"}"}">Language</span><span class="caret"></span>
                  </a>
                  <ul class="dropdown-menu" id="languageFlags">
                   <li id="enFlag"><a href="?lang=en"><img class="flag"
                    data-th-src="@{/public/img/en.png}" src="../static/public/img/en.png"
                    alt="English">&nbsp;<span data-th-text="${r"#{"}language_label_en${r"}"}">English</span></a> </li>
                   <li id="esFlag"><a href="?lang=es"><img class="flag"
                    data-th-src="@{/public/img/es.png}" src="../static/public/img/es.png"
                    alt="Spanish">&nbsp;<span data-th-text="${r"#{"}language_label_es${r"}"}">Spanish</span></a> </li>
                  </ul>
                </li>
              </ul>
           </div>

          </div>
        </nav>

        <!-- BANNER -->
        <div class="bg-header">
          <div class="jumbotron bg-banner">
              <div class="container">
                <h1 class="project-name">${projectName}</h1>
                <p class="project-tagline" data-th-text="${r"#{"}info_homepage_project${r"}"}">Hello, this is your home page.</p>
              </div>
          </div>
        </div>

      </header>
      <!-- /HEADER -->

      <!-- Main container -->
      <div class="container bg-container">

        <!-- Content -->
        <section data-layout-fragment="content">

          <div class="container-fluid content">

            <div class="panel panel-default">
              <div class="panel-heading">
                <h1 class="panel-title" data-th-text="${r"#{"}label_login}">Login</h1>
              </div>
              <div class="panel-body">
                <form class="form-horizontal" data-th-action="@{/login}" method="post">
                 <fieldset>
                  <legend class="sr-only" data-th-text="${r"#{help_login}"}">Enter your login and password</legend>
                  <!-- Alerts messages -->
                  <div class="alert alert-info" role="alert"
                    data-th-if="${r"$"}{@environment.getProperty('springlets.security.auth.in-memory.enabled')}"
                    data-th-with="username=${r"$"}{@environment.getProperty('springlets.security.auth.in-memory.user.name')},
                    userpasw=${r"$"}{@environment.getProperty('springlets.security.auth.in-memory.user.password')},
                    adminname=${r"$"}{@environment.getProperty('springlets.security.auth.in-memory.admin.name')},
                    adminpasw=${r"$"}{@environment.getProperty('springlets.security.auth.in-memory.admin.password')}"
                    >
                    <span class="glyphicon glyphicon-info-sign" aria-hidden="true"></span>
                    <span data-th-text="${r"#"}{info_security_login}">You tried to access a
                      restricted area of our application. By default, you can log in with:</span>
                    <blockquote>
                      <span data-th-if="${r"$"}{username}" data-th-text="|${r"$"}{username} / ${r"$"}{userpasw}|">"user/password"</span>
                      <span data-th-if="${r"$"}{adminname}" data-th-text="|${r"$"}{adminname} / ${r"$"}{adminpasw}|">"admin/password"</span>
                    </blockquote>
                  </div>

                  <div data-th-if="${r"${param.error}"}" class="alert alert-danger" role="alert">
                   <span class="glyphicon glyphicon-exclamation-sign" aria-hidden="true"></span>
                   <span class="sr-only" data-th-text="|${r"#{label_error}"}:|">Error:</span>
                   <span data-th-text="${r"#{error_login}"}">Invalid user and password</span>
                  </div>
                  <div data-th-if="${r"${param.logout}"}" class="alert alert-success" role="alert">
                   <span class="glyphicon glyphicon-ok" aria-hidden="true"></span>
                   <span data-th-text="${r"#{info_closed_session}"}">Log out correctly</span>
                  </div>
                  <div data-th-if="${r"${param.expired}"}" class="alert alert-danger" role="alert">
                   <span class="glyphicon glyphicon-exclamation-sign" aria-hidden="true"></span>
                   <span class="sr-only" data-th-text="|${r"#{label_error}"}:|">Error:</span>
                   <span data-th-text="${r"#{error_expired_session}"}">Your session has been expired</span>
                  </div>
                  <div class="form-group has-error has-feedback" data-z="z" id="username"
                      data-th-classappend="${r"${param.error}"}? 'has-error has-feedback'"
                      data-th-class="form-group">
                   <label for="username" class="col-md-3 control-label" data-th-text="${r"#{label_login_username}"}">Username</label>
                   <div class="col-md-6">
                    <input id="username" name="username" type="text"
                        class="form-control" placeholder="Username"
                        data-th-placeholder="${r"#{label_login_username}"}" data-toggle="tooltip"
                        aria-describedby="usernameStatus" />
                    <span data-th-classappend="${r"${param.error}"}? 'glyphicon glyphicon-remove form-control-feedback'"
                        class="glyphicon glyphicon-remove form-control-feedback"
                        data-th-if="${r"${param.error}"}" aria-hidden="true"></span>
                   </div>
                  </div>
                  <div class="form-group has-error has-feedback" data-z="z" id="password"
                      data-th-classappend="${r"${param.error}"}? 'has-error has-feedback'" data-th-class="form-group">
                   <label for="password" class="col-md-3 control-label" data-th-text="${r"#{label_login_password}"}">Password</label>
                   <div class="col-md-6">
                    <input id="password" name="password" type="password"
                        class="form-control" placeholder="Password" data-th-placeholder="${r"#{label_login_password}"}"
                        data-toggle="tooltip" aria-describedby="passwordStatus" />
                    <span data-th-classappend="${r"${param.error}"}? 'glyphicon glyphicon-remove form-control-feedback'"
                        class="glyphicon glyphicon-remove form-control-feedback" data-th-if="${r"${param.error}"}"
                        aria-hidden="true"></span>
                   </div>
                  </div>
                  <div class="form-group">
                   <div class="col-md-9 col-md-offset-3">
                    <button type="reset" class="btn btn-default" data-th-text="${r"#{label_reset}"}">Cancel</button>
                    <button type="submit" class="btn btn-primary" data-th-text="${r"#{label_submit}"}">Accept</button>
                   </div>
                  </div>
                 </fieldset>
                </form>
               </div>
            </div>
          </div>
        </section>
       <!-- content -->
    </div>
    <!-- container -->

   <footer class="container">
      <div class="row">
        <div class="col-sm-6 col-sm-offset-3">
          <small class="clearfix">
            Made with <a href="http://projects.spring.io/spring-roo/" target="_blank" data-th-text="| Spring Roo © ${r"${#calendars.format(#dates.createNow(),'yyyy')}"}|">
            Spring Roo &copy; 2016</a> •
            We <span class="glyphicon glyphicon-heart"></span>
            <a href="https://github.com/spring-projects/spring-roo/" target="_blank">Open source</a> •
            <a data-th-href="@{/accessibility}" href="/accessibility"><span data-th-text="${r"#{"}label_accessibility${r"}"}">Accessibility</span></a>
          </small>
        </div>
      </div>
      <!-- certified logos -->
      <div class="row">
        <div class="col-sm-6 col-sm-offset-6 text-right">
          <a title="Explanation of WCAG 2.0 Level Double-A Conformance"
            data-th-title="${r"#{label_accessibility_title}"}"
            target="_blank"
            href="http://www.w3.org/WAI/WCAG2AA-Conformance">
            <img height="32" width="88"
                 src="http://www.w3.org/WAI/wcag2AA"
                 data-th-alt="${r"#{label_accessibility_alt}"}"
                 alt="Level Double-A conformance, W3C WAI Web Content
                 Accessibility Guidelines 2.0">
          </a>
          &nbsp;
          <a title="Application developed and tested with OWASP -
             Open Web Application Security Project"
             data-th-title="${r"#{label_owasp_title}"}"
            target="_blank"
            href="https://www.owasp.org">
           <img height="32" width="90"
               data-th-src="@{/public/img/owasp_logo.png}"
               src="../static/public/img/owasp_logo.png"
               data-th-alt="${r"#{label_owasp_alt}"}"
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
