<!DOCTYPE html>
<html lang="en" data-layout-decorate="layouts/default-layout">
  <head>
    <meta charset="UTF-8" data-th-remove="all"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge" data-th-remove="all"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"
      data-th-remove="all"/>
    <meta name="description"
      content="Spring Roo, a next-generation rapid application development tool for Java developers. With Roo you can easily build full Java applications in minutes."
      data-th-remove="all"/>
    <meta name="author"
      content="Spring Roo development team"
      data-th-remove="all"/>

    <link rel="shortcut icon" href="../../static/public/img/favicon.ico"
      data-th-remove="all"/>
    <link rel="apple-touch-icon" href="../../static/public/img/apple-touch-icon.png"
      data-th-remove="all"/>

    <title data-th-text="|${r"#{"}label_create_entity(${r"#{"}${entityLabel}${r"}"})${r"}"}|">
    Create ${entityName} - ${projectName} - SpringRoo Application</title>

    <!--/* Bootstrap */-->
    <link rel="stylesheet" type="text/css"
      href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.css"
      data-th-remove="all"></link>

    <!--/* IE10 viewport hack for Surface/desktop Windows 8 bug */-->
    <link rel="stylesheet" type="text/css"
      href="https://maxcdn.bootstrapcdn.com/css/ie10-viewport-bug-workaround.css"
      data-th-remove="all"></link>

    <!--/* Font Awesome */-->
    <link rel="stylesheet" type="text/css"
      href="https://maxcdn.bootstrapcdn.com/font-awesome/4.6.2/css/font-awesome.min.css"
      data-th-remove="all"/>

    <!-- Select2 -->
    <link rel="stylesheet" type="text/css"
      href="https://cdnjs.cloudflare.com/ajax/libs/select2/4.0.3/css/select2.css"
      data-th-href="@{/webjars/select2/dist/css/select2.css}"/>
    <link rel="stylesheet" type="text/css"
      href="https://cdnjs.cloudflare.com/ajax/libs/select2-bootstrap-theme/0.1.0-beta.7/select2-bootstrap.css"
      data-th-href="@{/webjars/select2-bootstrap-theme/dist/select2-bootstrap.css}"/>

    <!-- DateTimePicker -->
    <link rel="stylesheet" type="text/css"
      href="https://cdnjs.cloudflare.com/ajax/libs/jquery-datetimepicker/2.5.4/build/jquery.datetimepicker.min.css"
      data-th-href="@{/webjars/datetimepicker/build/jquery.datetimepicker.min.css}"/>

    <!--/* Bootswatch CSS custom */-->
    <link rel="stylesheet" type="text/css"
      href="../../static/public/css/theme.css"
      data-th-remove="all"/>

    <!--/* Roo CSS */-->
    <link rel="stylesheet" type="text/css"
       href="../../static/public/css/springroo.css"
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
              <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#mainnav" aria-expanded="false">
                <span class="sr-only">Menu</span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
              </button>

              <!-- Brand logo -->
              <div class="organization-logo navbar-brand">
                <a title="${projectName}" href="/">
                  <img alt="${projectName}" data-th-src="@{/public/img/logo.png}" src="../../static/public/img/logo.png" />
                </a>
              </div>
              <!-- Name application -->
              <div class="application-name navbar-brand hidden-xs"><a href="/" data-th-href="@{/}">${projectName}</a></div>

            </div>
            <!-- /navbar header -->

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
              <!-- User menu -->
              <ul class="nav navbar-nav navbar-right upper-nav session">
                <li class="dropdown">
                  <a class="dropdown-toggle" data-toggle="dropdown" role="button"
                    aria-haspopup="true" aria-expanded="false">
                    <span class="glyphicon glyphicon-user" aria-hidden="true"></span>&nbsp;
                    <span class="hidden-sm" data-sec-authentication="principal.username">User</span>
                    <span class="caret"></span>
                  </a>
                  <ul class="dropdown-menu">
                   <li><a href="#"><span class="glyphicon glyphicon-wrench" aria-hidden="true"></span>
                   &nbsp;<span data-th-text="${r"#{"}label_profile${r"}"}">Admin Profile</span></a></li>
                   <li><a href="#"><span class="glyphicon glyphicon-lock" aria-hidden="true"></span>
                   &nbsp;<span data-th-text="${r"#{"}label_change_password${r"}"}">Change password</span></a></li>
                   <li>
                     <form data-th-action="@{/logout}" action="/logout" method="post">
                       <button type="submit" class="btn btn-link">
                        <span class="glyphicon glyphicon-log-out" aria-hidden="true"></span>
                        <span data-th-text="${r"#{"}label_logout ${r"}"}">Log out</span>
                       </button>
                     </form>
                   </li>
                 </ul>
                </li>
              </ul>
              <!-- User menu links -->
              <ul class="nav navbar-nav navbar-right upper-nav links">
                <li><a href="#"><span class="glyphicon glyphicon-envelope" aria-hidden="true"></span>
                &nbsp;<span class="hidden-sm" data-th-text="${r"#{"}label_contact${r"}"}">Contact</span></a></li>
                <li><a href="#"><span class="glyphicon glyphicon-question-sign" aria-hidden="true"></span>
                &nbsp;<span class="hidden-sm" data-th-text="${r"#{"}label_help${r"}"}">Help</span></a></li>
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

    <!--CONTAINER-->
    <div class="container bg-container">


      <!-- CONTENT -->
      <section data-layout-fragment="content">
        <div class="container-fluid content">
  	<!--
  	  Only the inner content of the following tag "section" is included
  	  within the template, in the section "content"
  	-->

          <h1 data-th-text="${r"#{"}label_edit_entity(${r"#{"}${entityLabel}})}">Edit ${entityName}</h1>

          <#assign dconfig=detail.configuration>
          <!-- FORM -->
          <form class="form-horizontal validate" method="POST" data-th-object="${modelAttribute}"
            data-th-action="${r"${"}(#mvc.url('${dconfig.mvcUrl_create}')).buildAndExpand(${modelAttributeName}.${identifierField})}">

            <fieldset id="containerFields">
              <legend data-th-text="${r"#{"}label_data_entity(${r"#{"}${detail.configuration.entityLabel}})}">${detail.entityName} data </legend>

              <#if detail.userManaged>
                ${detail.codeManaged}
              <#else>
                <div class="form-group has-error has-feedback" data-z="${detail.z}" id="${detail.entityItemId}"
                    data-th-class="form-group">
                    <div class="col-md-6">
                      <!-- Select2 -->
                      <select id="${detail.fieldName}" name="${detail.fieldName}Ids"
                        class="form-control dropdown-select-ajax"
                        data-allow-clear="true"
                        data-ajax--cache="true"
                        data-ajax--data-type="json"
                        data-ajax--delay="250"
                        multiple="multiple"
                        data-data-ajax--url="${r"${"}(#mvc.url('${dconfig.mvcUrl_select2}')).build()}"
                        data-data-placeholder="${r"#{"}${select2_placeholder}}">
                          <option data-th-each="item: *{${detail.pathStringFieldNames}}"
                             selected="true"
                             data-th-text="${r"${"}item}" data-th-value="${r"${"}item.${dconfig.identifierField}}">Another product to select</option>
                      </select>
                    </div>
                </div>
              </#if>
              </fieldset>

              <!-- buttons form -->
              <div class="form-group">
                <div class="col-md-9 col-md-offset-3">
                    <button type="reset" class="btn btn-default"
                      onclick="location.href='list.html'"
                      data-th-onclick="'location.href=\'' + ${r"${"}(#mvc.url('${mvcUrl_list}')).build()} + '\''"
                      data-th-text="${r"#{"}label_reset}">Cancel</button>
                    <button type="submit" class="btn btn-primary" data-th-text="${r"#{"}label_submit}">Save</button>
                </div>
              </div>

          </form>
          <!-- /FORM -->

        </div>

      </section>
      <!-- /CONTENT-->
  </div>
  <!-- /CONTAINER-->

  <footer class="container">
     <div class="row">
      <div class="col-sm-6 col-sm-offset-3">
        <small class="clearfix">
            Made with <a href="http://projects.spring.io/spring-roo/" target="_blank">
            Spring Roo &copy; 2016</a> •
            We <span class="glyphicon glyphicon-heart"></span>
            <a href="https://github.com/spring-projects/spring-roo/" target="_blank">Open source</a> •
            <a data-th-href="@{/accessibility}" href="../accessibility.html"><span data-th-text="${r"#{"}label_accessibility${r"}"}">Accessibility</span></a>
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
               src="../../static/public/img/owasp_logo.png"
               alt="Application developed and tested with OWASP">
         </a>
        </div>
      </div>
  </footer>

  <!-- JavaScript
  ================================================== -->
  <!-- Placed at the end of the document so that the pages load faster -->
  <!-- JQuery -->


  <div data-layout-fragment="javascript">
       <!-- Datatables fragment -->
     <div data-th-replace="fragments/js/select2 :: select2-js">
      // TODO add js CDN
     </div>
    <script type="text/javascript" data-th-inline="javascript">
      // IIFE - Immediately Invoked Function Expression
      (function(list) {

        // The global jQuery object is passed as a parameter
        list(window.jQuery, window, document);

      }(function($, window, document) {

        // The $ is now locally scoped, it won't collide with other libraries

        // Listen for the jQuery ready event on the document
        // READY EVENT BEGIN
        $(function() {

          // Put this page local javascript here!

        });

        // READY EVENT END
      }));
    </script>

  </div>

    <!-- Application -->
    <script type="text/javascript" charset="utf8"
      src="../../static/public/js/main.js"></script>

</body>
</#if>
</html>
