<!DOCTYPE html>
<html lang="es" data-layout-decorator="layouts/default-layout">
<head>
<meta charset="utf-8" />
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta name="viewport" content="width=device-width, initial-scale=1" />

<title>List ${entityName}</title>

<!-- CSS -->
<link data-th-href="@{/css/bootstrap.min.css}" data-th-remove="all"
  href="../../static/css/bootstrap.min.css" rel="stylesheet" />
<link data-th-href="@{/css/standard.css}" data-th-remove="all"
  href="../../static/css/standard.css" rel="stylesheet" />

<!--[if IE 8 ]> <html class="ie8" lang="es"/> <![endif]-->
<!--[if lt IE 9]><script data-th-remove="all" data-th-src="@{/js/html5shiv.min.js}" src="../../static/js/html5shiv.min.js"></script><![endif]-->

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
        <!-- START TABLE -->
        <div class="table-responsive">
          <script type="text/javascript">
            function actionButtons(data, type, full) {
              return '<a role="button" class="btn-accion ver" href="${controllerPath}/' + data + '">Show</a>' +
                     '<a role="button" class="btn-accion modificar" href="${controllerPath}/' + data + '/edit-form">Edit</a>' +
                     '<a role="button" class="btn-accion eliminar" href="${controllerPath}/' + data + '/delete-form">Delete</a>'
            }
          </script>
          <script data-th-inline="javascript" data-th-object="${modelAttribute}">
            function ajaxParams(){
              return {
                'headers': {'Accept':'application/vnd.datatables+json'},
              };
            }
          </script>
          <a class="btn-accion agregar" href="create.html" data-th-href="@{${controllerPath}/create}">Add</a>

          <table id="${entityName}-table"
                 class="table table-striped table-hover table-bordered"
                 dt:table="true"
                 data-dt-table="true"
                 data-dt-url="@{${controllerPath}/}"
                 data-dt-ajaxParams="ajaxParams">
            <caption>List ${entityName}</caption>
            <thead>
              <tr>
              <#list fields as field>
                <th data-dt-property="${field.fieldName}">${field.fieldName}</th>
              </#list>
              </tr>
            </thead>
          </table>
        </div>
        <!--END TABLE-->

        <div class="clearfix">
          <div class="pull-left">
            <a href="../index.html" class="btn btn-default"
               data-th-href="@{/}">
               <span class="glyphicon glyphicon-chevron-left" aria-hidden="true"></span>Back
            </a>
          </div>
        </div>

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