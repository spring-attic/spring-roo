<!DOCTYPE html>
<html data-layout-decorator="layouts/default-layout">
<head>
<meta charset="utf-8" data-th-remove="all" />
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

<title data-th-text="${r"#{"}label_show(${r"#{"}${entityLabel}${r"}"})${r"}"}">Show ${entityName}</title>

<!-- Bootstrap core CSS -->
<link data-th-href="@{/public/css/bootstrap.min.css}" data-th-remove="all"
  href="../../static/public/css/bootstrap.min.css" rel="stylesheet" />

<!-- Spring Roo CSS -->
<link data-th-href="@{/public/css/standard.css}" data-th-remove="all"
  href="../../static/public/css/standard.css" rel="stylesheet" />
<noscript data-th-remove="all">
  <link data-th-href="@{/public/css/nojs-standard.css}" data-th-remove="all"
    rel="stylesheet" href="../../static/public/css/nojs-standard.css" />
</noscript>

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
          ${projectName}
        </div>
      </div>

      <nav class="navbar navbar-default">
        <div class="container-fluid">

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
        </div>
      </nav>
    </header>
    <!-- END HEADER -->

    <!--START CONTENT-->
    <section data-layout-fragment="content" data-th-object="${modelAttribute}">
      <div class="container-fluid content">

        <!-- CONTENT -->
        <h1 data-th-text="${r"#{"}label_show(${r"#{"}${entityLabel}${r"}"})${r"}"}">${entityName}</h1>
        
        <dl class="dl-horizontal">
          <#list fields as field>
            <#if field.type != "LIST">
                <dt data-th-text="${r"#{"}${field.label}${r"}"}">${field.fieldName}</dt>
                <dd data-th-text="*{{${field.fieldName}}}">${field.fieldName}Value</dd>
            <#else>
                <h3 class="panel-title" data-th-text="${r"#{"}${field.label}${r"}"}">${field.fieldName}</h3>
                <!--START TABLE-->
                <div class="table-responsive">
                  <table id="${field.configuration.referencedFieldType}Table" 
                         class="table table-striped table-hover table-bordered" 
                         data-row-id="${field.configuration.identifierField}"
                         data-select="single"
                         data-order="[[ 0, &quot;asc&quot; ]]">
                    <caption data-th-text="${r"#{"}label_list_of_entity(${r"#{"}${field.configuration.referencedFieldLabelPlural}${r"}"})${r"}"}">List ${field.configuration.referencedFieldType}</caption>
                    <thead>
                      <tr>
                        <#list field.configuration.referenceFieldFields as referencedFieldField>
                        <th>${referencedFieldField.fieldName}</th>
                        </#list>
                        <th data-th-text="${r"#{"}label_tools${r"}"}">Tools</th>
                      </tr>
                    </thead>
                    <tbody data-th-remove="all">
                      <tr>
                        <#list field.configuration.referenceFieldFields as referencedFieldField>
                        <td>${referencedFieldField.fieldName}</td>
                        </#list>
                        <td data-th-text="${r"#{"}label_tools${r"}"}">Tools</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <!--END TABLE-->
            </#if>
          </#list>
        </dl>

        <div class="clearfix">
          <div class="pull-left">
            <button onclick="location.href='list'"
              data-th-onclick="'location.href=\'' + @{${controllerPath}} + '\''" type="button"
              class="btn btn-default">
              <span class="glyphicon glyphicon-chevron-left" aria-hidden="true"></span><span data-th-text="${r"#{"}label_back${r"}"}">Back</span>
            </button>
          </div>
          <div class="pull-right">
            <a href="edit" class="btn btn-primary"
              data-th-href="@{${controllerPath}/${r"{"}${identifierField}${r"}"}/edit-form(${identifierField}=${r"*{id}"})}"
              data-th-text="${r"#{label_edit}"}">Edit</a>
          </div>
        </div>

      </div>
    </section>
    <!--END CONTENT-->

  </div>
  <!--END CONTAINER-->

  <footer class="container">
    <p class="text-right">© Powered By Spring Roo </p>
  </footer>

  <!-- JQuery -->
  <script type="text/javascript" charset="utf8" src="https://code.jquery.com/jquery-1.12.3.js" data-th-remove="all"></script>
  <!-- Bootstrap -->
  <script type="text/javascript" src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.js" data-th-remove="all"></script>

  <div data-layout-fragment="javascript">
    
    <!-- Datatables -->
    <script type="text/javascript" charset="utf8" src="//cdn.datatables.net/1.10.11/js/jquery.dataTables.js"></script>
    <script type="text/javascript" charset="utf8" src="https://cdn.datatables.net/1.10.11/js/dataTables.bootstrap.js"></script>
    <!-- Datatables responsive plugin -->
    <script type="text/javascript" charset="utf8" src="https://cdn.datatables.net/responsive/2.0.2/js/dataTables.responsive.js"></script>
    <script type="text/javascript" charset="utf8" src="https://cdn.datatables.net/responsive/2.0.2/js/responsive.bootstrap.js"></script>
    <!-- Datatables buttons plugins -->
    <script type="text/javascript" charset="utf8" src="https://cdn.datatables.net/buttons/1.1.2/js/dataTables.buttons.js"></script>
    <script type="text/javascript" charset="utf8" src="https://cdn.datatables.net/buttons/1.1.2/js/buttons.bootstrap.js"></script>
    <script type="text/javascript" charset="utf8" src="https://cdn.datatables.net/buttons/1.1.2/js/buttons.colVis.js"></script>
    <script type="text/javascript" charset="utf8" src="https://cdn.datatables.net/buttons/1.1.2/js/buttons.flash.js"></script>
    <script type="text/javascript" charset="utf8" src="https://cdn.datatables.net/buttons/1.1.2/js/buttons.html5.js"></script>
    <!-- Datatables select plugin -->
    <script type="text/javascript" charset="utf8" src="https://cdn.datatables.net/select/1.1.2/js/dataTables.select.js"></script>
    <!-- Datatables application defaults -->
    <script type="text/javascript" charset="utf8" src="../../static/public/js/datatables-defaults.js" data-th-src="@{/public/js/datatables-defaults.js}"></script>
    <script type="text/javascript" charset="utf8" src="../../static/public/js/datatables-defaults-es.js" data-th-src="@{/public/js/datatables-defaults-es.js}"></script>
    
    
    <!-- Datatables page configs -->
    <script type="text/javascript" data-th-inline="javascript">
    $(document).ready( function () {
        var currentId = window.location.href.split("/")[window.location.href.split("/").length - 1];
        <#list fields as field>
            <#if field.type == "LIST">
                var ${field.configuration.referencedFieldType}BaseUrl = '${controllerPath}/' + currentId +  '${field.configuration.controllerPath}/';
                var ${field.configuration.referencedFieldType}Table = jQuery('#${field.configuration.referencedFieldType}Table').DataTable({
                    'ajax': {
                          'url': ${field.configuration.referencedFieldType}BaseUrl
                     },
                    'columns': [
                      <#list field.configuration.referenceFieldFields as referencedFieldField>
                      { 'data': '${referencedFieldField.fieldName}' },
                      </#list>
                      { 
                        'data': '${field.configuration.identifierField}',
                        'orderable': false,
                        'searchable': false,
                        'render': function ( data, type, full, meta ) {
                            return '<a role="button" class="btn-accion show" href="${field.configuration.controllerPath}/' + data + '" data-th-text="${r"#{label_show}"}">Show</a>'
                        }
                      }
                    ]  
                });
            </#if>
        </#list>
    });
    </script>
   
  </div>

  <!-- Application -->
  <script type="text/javascript" src="../../static/public/js/main.js" data-th-remove="all"></script>
  
</body>

</html>