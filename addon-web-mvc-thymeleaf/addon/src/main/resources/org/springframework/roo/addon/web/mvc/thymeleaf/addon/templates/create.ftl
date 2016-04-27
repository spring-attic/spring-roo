<!DOCTYPE html>
<html lang="es" data-layout-decorator="layouts/default-layout">
<head>
<meta charset="utf-8" />
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta name="viewport" content="width=device-width, initial-scale=1" />

<title>Create ${entityName}</title>

<!-- CSS -->
<link data-th-href="@{/css/bootstrap.min.css}" data-th-remove="all"
    href="../../static/css/bootstrap.min.css" rel="stylesheet" />
<link data-th-href="@{/css/standard.css}" data-th-remove="all"
    href="../../static/css/standard.css" rel="stylesheet" />
<!--[if IE 8 ]> <html class="ie8" lang="es"/> <![endif]-->
<!--[if lt IE 9]><script data-th-remove="all" data-th-src="@{/js/html5shiv.min.js}" src="../../static/js/html5shiv.min.js"></script><![endif]-->

<!-- DateTimePicker -->
<link rel="stylesheet" type="text/css"
    href="../../static/css/jquery.datetimepicker.css" />
<script src="../../static/js/jquery.min.js"></script>
<script src="../../static/js/jquery.datetimepicker.full.min.js"></script>
<script type="text/javascript">
  jQuery(function() {
    jQuery(".datetimepicker").datetimepicker({
      format : "d/m/Y"
    });
  });
</script>
</head>
<body>

  <div class="container upper-nav">
    <div class="session">
      <div>
        <span class="glyphicon glyphicon-user" aria-hidden="true"></span>User
      </div>
      <div>
        <span class="glyphicon glyphicon-calendar" aria-hidden="true"></span>Last Access: 00-00-0000
      </div>
      <button type="submit" class="exit">
        <span class="glyphicon glyphicon-off" aria-hidden="true"></span>Exit
      </button>
    </div>
  </div>

  <!--START CONTAINER-->
  <div class="container bg-container">

    <!-- HEADER -->
    <header role="banner">
      <!-- header -->
      <div class="bg-header">
        <div class="organization-logo">
          <a
            title="${projectName}"
            href="/"><img
            alt="${projectName}"
            src="../../static/img/logo_spring_roo.png" /></a>
        </div>
        <div class="application-name">
          ${projectName}
        </div>
      </div>

        <!-- NAVIGATION MENU -->
        <nav class="navbar navbar-default">
            <div class="container-fluid">
    
                <div class="navbar-header">
                    <button type="button" class="navbar-toggle collapsed"
                        data-toggle="collapse" data-target="#bs-example-navbar-collapse-1"
                        aria-expanded="false">
                        <span class="sr-only">Dropdown</span> <span class="icon-bar"></span> <span
                            class="icon-bar"></span> <span class="icon-bar"></span>
                    </button>
                    <a class="navbar-brand" href="#">Main menu</a>
                </div>
    
                <div id="bs-example-navbar-collapse-1" class="navbar-collapse collapse">
                    <ul class="nav navbar-nav">
                        <li class="active"><a href="#">Menu 1</a></li>
                        <li><a href="#">Menu 2</a></li>
                        <li class="dropdown"><a href="#" class="dropdown-toggle"
                            data-toggle="dropdown" role="button" aria-haspopup="true"
                            aria-expanded="false">Dorpdown Menu 3<span class="caret"></span></a>
                            <ul class="dropdown-menu">
                                <li><a href="#">Submenu 1</a></li>
                                <li><a href="#">Submenu 2</a></li>
                                <li><a href="#">Submenu 3</a></li>
                            </ul></li>
                    </ul>
                </div>
    
            </div>
        </nav>
    </header>
    <!-- END HEADER -->

    <!--START CONTENT-->
        <section data-layout-fragment="content">
            <div class="container-fluid content">
                <h1>Create ${entityName}</h1>

                <!-- START FORM -->
                <form class="form-horizontal">
                    <fieldset>
                        <#list fields as field>
                          <div class="form-group">
                            <label for="${field.fieldName}" class="col-md-3 control-label">
                                ${field.fieldName} <abbr title="Mandatory">(*)</abbr>
                            </label>
                            <div class="col-md-6">
                                <input type="text" class="form-control"
                                    id="${field.fieldName}" placeholder="${field.fieldName}"
                                    data-toggle="tooltip" title="${field.fieldName}" />
                            </div>
                        </div>
                        </#list>
                        
                        <div class="row">
                            <div class="col-md-9 col-md-offset-3">
                                <button type="reset" class="btn btn-default"
                                    onclick="location.href='list.html'"
                                    data-th-onclick="'location.href=\'' + @{${controllerPath}} + '\''">Cancel</button>
                                <button type="submit" class="btn btn-primary"
                                    onclick="location.href='list.html'"
                                    data-th-onclick="'location.href=\'' + @{${controllerPath}} + '\''">Accept</button>
                            </div>
                        </div>
                        
                    </fieldset>
                </form>
                <!--END FORM -->

            </div>
           <!--END CONTENT-->

    </section>

  </div>
  <!--END CONTAINER-->

    <footer class="container">
        <p class="text-right">© Powered by Spring Roo</p>
    </footer>

  <script data-th-remove="all" data-th-src="@{../js/bootstrap.min.js}"
    src="../../static/js/bootstrap.min.js"></script>
  <script data-th-remove="all" data-th-src="@{../js/main.js}"
    src="../../static/js/main.js"></script>

</body>

</html>