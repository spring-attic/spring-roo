<!DOCTYPE html>
<html data-layout-decorator="layouts/default-layout">
<head>
<meta charset="utf-8" />
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<meta name="author"
    content="Spring Roo" />
<link rel="icon" href="../../static/img/favicon.ico"
    data-th-href="@{/img/favicon.ico}" data-th-remove="all" />

<title>Show ${entityName}</title>

<!-- CSS -->
<link data-th-remove="all" href="../../static/css/bootstrap.min.css"
    rel="stylesheet" />
<link data-th-remove="all" href="../../static/css/standard.css"
    rel="stylesheet" />

<noscript>
    <link data-th-remove="all" rel="stylesheet"
        href="../../static/css/standard.css" />
</noscript>
<!--[if IE 8 ]> <html data-th-remove="all" class="ie8" lang="es"> <![endif]-->
<!--[if lt IE 9]><script data-th-remove="all" src="../../static/js/html5shiv.min.js"></script><![endif]-->

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

            <div class="bg-header">
                <div class="organization-logo">
                    <a
                        title="${projectName}"
                        href="/"><img
                        alt="${projectName}"
                        src="../../static/img/logo_spring_roo.png" /></a>
                </div>
                <div class="application-name">${projectName}</div>
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

        <!--START CONTENT -->
        <section data-layout-fragment="content" data-th-object="${modelAttribute}">
            <div class="container-fluid content">

                <!-- CONTENT -->
                <h1>Show</h1>

                <div class="panel panel-default">
                    <div class="panel-heading">
                        <h3 class="panel-title">Show ${entityName}</h3>
                    </div>
                    <div class="panel-body">
                        <dl class="dl-horizontal">
                            <!-- Entity Fields -->
                            <#list fields as field>
                                <dt data-th-text="${field.fieldName}">${field.fieldName}</dt>
                                <dd data-th-text="*{{${modelAttribute}}}">Value</dd>
                            </#list>
                        </dl>

                    </div>
                </div>

                <div class="clearfix">
                    <div class="pull-left">
                        <button data-th-onclick="'location.href=\'' + @{${controllerPath}} + '\''"
                            type="button" class="btn btn-default" onclick="location.href='list.html'">
                            <span class="glyphicon glyphicon-chevron-left" aria-hidden="true"></span>Back
                        </button>
                    </div>
                    <div class="pull-right">
                        <button
                            data-th-onclick="'location.href=\'' + @{${controllerPath}/{id}/edit-form(id=*{id})} + '\''"
                            type="button" class="btn btn-primary" onclick="location.href='edit.html'">Update</button>
                    </div>
                </div>

            </div>
        </section>
        <!--END CONTENT-->

    </div>
    <!--END CONTAINER-->

    <footer class="container">
        <p class="text-right">© Powered by Spring Roo</p>
    </footer>

    <script data-th-remove="all" src="../../static/js/jquery.min.js"></script>
    <script data-th-remove="all" src="../../static/js/bootstrap.min.js"></script>
    <script data-th-remove="all" src="../../static/js/main.js"></script>
    <!-- IE10 viewport hack for Surface/desktop Windows 8 bug -->
    <script data-th-remove="all"
        src="../../static/js/ie10-viewport-bug-workaround.js"></script>


</body>
</html>