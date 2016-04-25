<!DOCTYPE html>
<html data-layout-decorator="layouts/default-layout">
  <head>
    <meta charset="UTF-8" data-th-remove="all"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge" data-th-remove="all"/>
    <meta name="viewport" content="width=device-width, initial-scale=1" data-th-remove="all"/>
    <meta name="description" content="Ministerio de Sanidad, Servicios Sociales e Igualdad" data-th-remove="all"/>
    <meta name="author" content="Ministerio de Sanidad, Servicios Sociales e Igualdad" data-th-remove="all" />
    <title>Inicio</title>

    <!-- Bootstrap core CSS -->
    <link rel="stylesheet" type="text/css"
          href="../static/css/bootstrap.min.css" data-th-href="@{/css/bootstrap.min.css}" data-th-remove="all" />

    <!-- MSSSI CSS -->
    <link rel="stylesheet" type="text/css"
          href="../static/css/sanidad-internet.css" data-th-href="@{/css/sanidad-internet.css}" data-th-remove="all"/>
    <noscript><link rel="stylesheet" href="../static/css/nojs-sanidad-internet.css" data-th-href="@{/css/nojs-sanidad-internet.css}" data-th-remove="all"/></noscript>

  </head>
  <body>

  <div class="container upper-nav">
    <div class="session">
      <div><span class="glyphicon glyphicon-user" aria-hidden="true"></span>Nombre Apellido Apellido</div>
      <div><span class="glyphicon glyphicon-calendar" aria-hidden="true"></span>Último acceso: 00-00-0000</div>
      <button type="submit" class="exit"><span class="glyphicon glyphicon-off" aria-hidden="true"></span>Salir</button>
    </div>
  </div>

  <!--INICIO CONTENEDOR-->
  <div class="container bg-container">

    <!-- CABECERA -->
    <header role="banner">
      <div class="bg-header">
        <div class="organization-logo"><a title="Ministerio de Sanidad, Servicios Sociales e Igualdad - Gobierno de España" href="http://www.msssi.gob.es/"><img alt="Ministerio de Sanidad, Servicios Sociales e Igualdad - Gobierno de España" src="../static/img/logo_ministerio.jpg" /></a></div>
        <div class="application-name">Título de la aplicación <small>Subtítulo de la aplicación</small></div>
      </div>

      <nav class="navbar navbar-default">
        <div class="container-fluid">

          <div class="navbar-header">
            <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1" aria-expanded="false">
              <span class="sr-only">Desplegable</span>
              <span class="icon-bar"></span>
                <span class="icon-bar"></span>
              <span class="icon-bar"></span>
            </button>
            <a class="navbar-brand" href="#">Menú principal</a>
          </div>

          <div id="bs-example-navbar-collapse-1" class="navbar-collapse collapse">
            <ul class="nav navbar-nav">
              <li class="active"><a href="#">Menú 1 activo</a></li>
              <li><a href="#">Menú 2</a></li>
              <li class="dropdown">
                <a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">Menú 3 desplegable<span class="caret"></span></a>
                <ul class="dropdown-menu">
                  <li><a href="#">Submenú 1</a></li>
                  <li><a href="#">Submenú 2</a></li>
                  <li><a href="#">Submenú 3</a></li>
                </ul>
              </li>
            </ul>
          </div>
        </div>
      </nav>
    </header>
    <!-- FIN CABECERA -->

    <!--INICIO CONTENIDO-->
    <section data-layout-fragment="content">
      <div class="container-fluid content">
        <!-- inicio contenido principal -->
        <section class="main">
          <h2>Bienvenido a Northwind</h2>
            <p>
              Desde esta aplicación podrá realizar sus compras cómodamente.
            </p>
            <a href="customerorders/show.html" data-th-href="@{/customerorders/1}">Ver un pedido</a><br/>
            <a href="shippers/show.html" data-th-href="@{/shippers/1}">Ver un transportista</a><br/>
            <a href="categories/show.html" data-th-href="@{/categories/1}">Ver una categoría</a><br/>
            <a href="stores/show.html" data-th-href="@{/stores/1}">Ver un almacén</a><br/>
            <a href="employees/show.html" data-th-href="@{/employees/2}">Ver un empleado</a><br/>
            <a href="customers/show.html" data-th-href="@{/customers/1}">Ver un cliente</a><br/>
            <hr/>
            <a href="suppliers/create.html" data-th-href="@{/suppliers/create-form}">Crear un Proveedor</a><br/>
            <hr/>
            <a href="customers/list.html" data-th-href="@{/customers}">Listado de Clientes</a><br/>
            <!-- TODO IE8 -->
            <a href="customerorders/list.html" data-th-href="@{/customerorders}">Listado de Pedidos</a><br/>
        </section>
      </div>
      <!--FIN CONTENIDO-->
    </section>
  </div>
  <!--FIN CONTENEDOR-->

  <footer class="container">
    <p class="text-right">© Ministerio de Sanidad, Servicios Sociales e Igualdad</p>
  </footer>


    <!-- Bootstrap core JavaScript
    ================================================== -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script src="../static/js/jquery.min.js" data-th-src="@{/js/jquery.min.js}" data-th-remove="all">
    </script>
    <script src="../static/js/bootstrap.min.js" data-th-src="@{/js/bootstrap.min.js}" data-th-remove="all">
    </script>
    <script src="../static/js/main.js" data-th-src="@{/js/main.js}" data-th-remove="all">
    </script>
    <!-- IE10 viewport hack for Surface/desktop Windows 8 bug -->
     <script src="../static/js/ie10-viewport-bug-workaround.js" data-th-src="@{/js/ie10-viewport-bug-workaround.js}" data-th-remove="all">
    </script>

  </body>
</html>