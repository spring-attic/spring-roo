<#import "fields/input-text.ftl" as text>
<#import "fields/input-date.ftl" as date>
<#import "fields/reference.ftl" as reference>
<#import "fields/checkbox.ftl" as checkbox>
<#import "fields/enum.ftl" as enum>
<!DOCTYPE html>
<html data-layout-decorator="layouts/default-layout">
<head>
<meta charset="UTF-8" data-th-remove="all" />
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

<link rel="shortcut icon" href="../../static/public/img/favicon.ico"
  data-th-remove="all" />

<link rel="apple-touch-icon" href="../../static/public/img/apple-touch-icon.png"
  data-th-remove="all" />

<title data-th-text="${r"#{"}label_edit_entity(${r"#{"}${entityLabel}${r"}"})${r"}"}">${projectName}
  - Edit ${entityName}</title>

<!-- Bootstrap -->
    <link rel="stylesheet" type="text/css"
      href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.css"
      data-th-remove="all"></link>

    <!-- Font Awesome -->
    <link rel="stylesheet" type="text/css" 
       href="https://maxcdn.bootstrapcdn.com/font-awesome/4.6.2/css/font-awesome.min.css"/>


    <!-- Bootswatch CSS custom -->
    <link rel="stylesheet" type="text/css"
      href="../static/public/css/theme.css"
      data-th-remove="all" />

    <!-- Roo CSS -->
    <link rel="stylesheet" type="text/css"
      href="../static/public/css/springroo.css"
      data-th-remove="all" />

    <!--[if lt IE 9]>
       <script src="/public/js/html5shiv.min.js"></script>
        <script src="/public/js/respond.min.js"></script>
    <![endif]-->

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

</head>
<body>

  <!--START CONTAINER-->
  <div class="container bg-container">

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
          <a href="/" data-th-href="@{/}">${projectName}</a>
        </div>
      </div>

      <!-- MAIN MENU -->
      <nav class="navbar navbar-default">
        <div class="container-fluid">

          <!-- collapsed menu button -->
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
            <!-- Main menu -->
            <div class="row">
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

            <!-- User Menu -->
            <div class="container upper-nav">
              <ul class="nav navbar-nav navbar-right session">
                <li><span class="glyphicon glyphicon-user" aria-hidden="true"></span>User</li>
                <li><button type="submit" class="exit">
                    <span class="glyphicon glyphicon-off" aria-hidden="true"></span><span>Exit</span>
                  </button></li>
              </ul>
              <ul class="nav navbar-nav navbar-right links">
                <li><a href="#"><span class="glyphicon glyphicon-envelope" aria-hidden="true"></span>
                    <span data-th-text="${r"#{"}label_contact${r"}"}">Contact</span></a></li>
                <li><a href="#"><span class="glyphicon glyphicon-question-sign" aria-hidden="true"></span>
                    <span data-th-text="${r"#{"}label_help${r"}"}">Help</span></a></li>
              </ul>
            </div>
          </div>
        </div>
      </nav>
    </header>
    <!-- END HEADER -->

    <!--START CONTENT-->
    <section data-layout-fragment="content">
      <div class="container-fluid content">

        <h1 data-th-text="${r"#{"}label_search_entity(${r"#{"}${entityLabel}${r"}"})${r"}"}">Search ${entityName}</h1>

        <!--START FORM-->
        <form class="form-horizontal" method="POST" data-th-object="${r"${formBean}"}"
          data-th-action="@{${controllerPath}/search/${action}}">
          <fieldset id="containerFields">
            <#list fields as field>
                <#if field.type == "TEXT">
                    <@text.input label=field.label fieldName=field.fieldName z=field.z />
                <#elseif field.type == "DATE">
                    <@date.input label=field.label 
                    fieldName=field.fieldName
                    z=field.z
                    format=field.configuration.format />
                <#elseif field.type == "REFERENCE">
                    <@reference.input label=field.label 
                        fieldName=field.fieldName 
                        z=field.z
                        referencedEntity=field.configuration.referencedEntity
                        identifierField=field.configuration.identifierField
                        referencedPath=field.configuration.referencedPath
                        fieldOne=field.configuration.fieldOne
                        fieldTwo=field.configuration.fieldTwo />
                <#elseif field.type == "ENUM">
                    <@enum.input label=field.label 
                    fieldName=field.fieldName
                    z=field.z
                    items=field.configuration.items />                
                <#elseif field.type == "BOOLEAN">
                    <@checkbox.input label=field.label fieldName=field.fieldName z=field.z />                
                </#if>
            </#list>

            <!-- FORM BUTTONS -->
            <div class="form-group">
              <div class="col-md-12">
                <div class="pull-left">
                  <!-- TODO IE8 -->
                  <button type="reset" class="btn btn-default"
                    onclick="location.href='list.html'"
                    data-th-onclick="'location.href=\'' + @{${controllerPath}} + '\''" 
                    data-th-text="${r"#{"}label_reset${r"}"}">Cancel</button>
                </div>
                <div class="pull-right">
                  <input type="submit" value="Accept" data-th-value="${r"#{"}label_submit${r"}"}" class="btn btn-primary" />
                </div>
              </div>
            </div>

          </fieldset>
        </form>
        <!--END FORM-->

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
  <!-- JQuery -->
  <script type="text/javascript" charset="utf8"
    src="https://code.jquery.com/jquery-1.12.3.js"></script>

  <!-- Bootstrap -->
  <script type="text/javascript"
    src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.js"></script>
    
  <!-- Application -->
  <script src="../../static/public/js/main.js"></script>

  <div data-layout-fragment="javascript">
      <!-- Moment.js -->
      <script data-th-src="@{/public/js/moment.js}" 
        src="../../static/public/js/moment.js"></script>
      <script data-th-src="@{/public/js/moment-} + ${r"${application_locale}"} + .js" 
        src="../../static/public/js/moment-en.js"></script>
      <!-- Select2 -->
      <script src="../../static/public/js/select2.full.js" data-th-src="@{/public/js/select2.full.js}"></script>
      <script src="../../static/public/js/select2.full-es.js" data-th-src="@{/public/js/select2.full-es.js}"></script>
      <script src="../../static/public/js/select2-defaults.js" data-th-src="@{/public/js/select2-defaults.js}"></script>
      <!-- DateTime Picker -->
      <script src="../../static/public/js/jquery.datetimepicker.full.min.js" data-th-src="@{/public/js/jquery.datetimepicker.full.min.js}"></script>
      <script src="../../static/public/js/datetimepicker-defaults.js" data-th-src="@{/public/js/datetimepicker-defaults.js}"></script>
  </div>

  <!-- IE10 viewport hack for Surface/desktop Windows 8 bug -->
  <script src="../../static/public/js/ie10-viewport-bug-workaround.js"></script>
  
</body>
</html>