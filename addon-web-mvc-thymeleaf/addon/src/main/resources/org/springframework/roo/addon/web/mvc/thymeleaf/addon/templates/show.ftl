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

<title data-th-text="${r"#{"}label_show_entity(${r"#{"}${entityLabel}${r"}"})${r"}"}">${projectName} - Show ${entityName}</title>

<!-- Bootstrap core CSS -->
<link rel="stylesheet" type="text/css"
  href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.css"
  data-th-remove="all"></link>

<!-- IE10 viewport hack for Surface/desktop Windows 8 bug -->
<link rel="stylesheet" type="text/css"
  href="https://maxcdn.bootstrapcdn.com/css/ie10-viewport-bug-workaround.css"
  data-th-remove="all"></link>

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

            <!-- main menu -->
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

            <!-- user menu -->
            <div class="container upper-nav">
                <ul class="nav navbar-nav navbar-right session">
                  <li><span class="glyphicon glyphicon-user" aria-hidden="true"></span>
                      <span data-th-text="${r"#{"}label_user${r"}"}">User</span></li>
                  <li data-th-text="${r"#{"}label_last_access(00-00-0000)${r"}"}">
                       <span class="glyphicon glyphicon-calendar" aria-hidden="true"></span>Last Access: 00-00-0000</li>
                  <li><button type="submit" class="exit"><span class="glyphicon glyphicon-off" aria-hidden="true"></span>
                       <span data-th-text="${r"#{"}label_exit${r"}"}">Exit</span></button></li>
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
    <section data-layout-fragment="content" data-th-object="${modelAttribute}">
      <div class="container-fluid content">

        <!-- CONTENT -->
        <h1 data-th-text="${r"#{"}label_show_entity(${r"#{"}${entityLabel}${r"}"})${r"}"}">Show ${entityName}</h1>
        
        <dl class="dl-horizontal" id="containerFields">
          <#list fields as field>
        	<dt id="${field.fieldName}Label" data-th-text="${r"#{"}${field.label}${r"}"}" data-z="${field.z}">${field.fieldName}</dt>
   			<dd id="${field.fieldName}" data-th-text="*{{${field.fieldName}}}" data-z="${field.z}">${field.fieldName}Value</dd>
          </#list>
          
          <#if details?size != 0>
              <hr>
              <ul class="nav nav-tabs">
              <#assign firstDetail=true>
              <#list details as field>
                <#if firstDetail == true>
                  <li class="active"><a id="${field.fieldNameCapitalized}Tab" data-toggle="tab" href="#detail-${field.fieldNameCapitalized}">${field.fieldNameCapitalized}</a></li>
                  <#assign firstDetail=false>
                <#else>
                    <li><a id="${field.fieldNameCapitalized}Tab" data-toggle="tab" href="#detail-${field.fieldNameCapitalized}">${field.fieldNameCapitalized}</a></li>
                </#if>
              </#list>
              </ul>
              
              <div class="tab-content">
                <#assign firstDetail=true>
                <#list details as field>
                    <#if firstDetail == true>
                        <div id="detail-${field.fieldNameCapitalized}" class="tab-pane active">
                        <#assign firstDetail=false>
                    <#else>
                        <div id="detail-${field.fieldNameCapitalized}" class="tab-pane">
                    </#if>
                        <!--START TABLE-->
                        <div class="table-responsive">
                          <table id="${field.fieldNameCapitalized}Table" 
                                 class="table table-striped table-hover table-bordered" 
                                 data-row-id="${field.configuration.identifierField}"
                                 data-select="single"
                                 data-z="${field.z}"
                                 data-order="[[ 0, &quot;asc&quot; ]]">
                            <caption data-th-text="${r"#{"}label_list_of_entity(${r"#{"}${field.configuration.referencedFieldLabel}${r"}"})${r"}"}">List ${field.fieldNameCapitalized}</caption>
                            <thead>
                              <tr>
                                <#list field.configuration.referenceFieldFields as referencedFieldField>
                                <th data-th-text="${r"#{"}${referencedFieldField.label}${r"}"}">${referencedFieldField.fieldName}</th>
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
                    </div>
                  </#list>
              </div>
            </#if>
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
            <button
              data-th-onclick="'location.href=\'' + @{${controllerPath}/${r"{"}${identifierField}${r"}"}/edit-form(${identifierField}=${r"*{id}"})} + '\''"
              data-sec-authorize="hasAnyRole('ROLE_ADMIN','ROLE_EMPLEADO')"
              type="button" class="btn btn-primary" onclick="location.href='edit'"
              data-th-text="${r"#{label_edit}"}">Edit</button>
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

  <!-- JavaScript
    ================================================== -->
  <!-- JQuery -->
  <script type="text/javascript" charset="utf8" src="https://code.jquery.com/jquery-1.12.3.js" data-th-remove="all"></script>
  <!-- Bootstrap -->
  <script type="text/javascript" src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.js" data-th-remove="all"></script>

  <!-- IE10 viewport hack for Surface/desktop Windows 8 bug -->
  <script src="https://maxcdn.bootstrapcdn.com/js/ie10-viewport-bug-workaround.js"></script>

  <div data-layout-fragment="javascript">
    
    <!-- Datatables -->
    <script type="text/javascript" charset="utf8" src="//cdn.datatables.net/1.10.12/js/jquery.dataTables.js"></script>
    <script type="text/javascript" charset="utf8" src="//cdn.datatables.net/1.10.12/js/dataTables.bootstrap.js"></script>
    <!-- Datatables responsive plugin -->
    <script type="text/javascript" charset="utf8" src="//cdn.datatables.net/responsive/2.1.0/js/dataTables.responsive.js"></script>
    <script type="text/javascript" charset="utf8" src="//cdn.datatables.net/responsive/2.1.0/js/responsive.bootstrap.js"></script>
    <!-- Datatables buttons plugins -->
    <script type="text/javascript" charset="utf8" src="//cdn.datatables.net/buttons/1.2.0/js/dataTables.buttons.js"></script>
    <script type="text/javascript" charset="utf8" src="//cdn.datatables.net/buttons/1.2.0/js/buttons.bootstrap.js"></script>
    <script type="text/javascript" charset="utf8" src="//cdn.datatables.net/buttons/1.2.0/js/buttons.colVis.js"></script>
    <script type="text/javascript" charset="utf8" src="//cdn.datatables.net/buttons/1.2.0/js/buttons.flash.js"></script>
    <script type="text/javascript" charset="utf8" src="//cdn.datatables.net/buttons/1.2.0/js/buttons.html5.js"></script>
    <!-- Datatables select plugin -->
    <script type="text/javascript" charset="utf8" src="//cdn.datatables.net/select/1.2.0/js/dataTables.select.js"></script>
    <!-- Datatables application defaults -->
    <script type="text/javascript" charset="utf8" src="../../static/public/js/datatables-defaults.js" data-th-src="@{/public/js/datatables-defaults.js}"></script>
    <script type="text/javascript" charset="utf8" src="../../static/public/js/datatables-defaults-en.js" data-th-src="@{/public/js/datatables-defaults-en.js}"></script>
    
    
    <!-- Datatables page configs -->
    <script type="text/javascript" data-th-inline="javascript">
    $(document).ready( function () {
    	<#assign firstDetail = true>
        <#list details as field>
            var ${field.fieldNameCapitalized}BaseUrl = ${r"[["}@{${controllerPath}/${r"{"}${identifierField}${r"}"}${field.configuration.controllerPath}/(${identifierField}=${r"${"}${modelAttributeName}${r"."}${identifierField}${r"})}]]"} +  '';
            <#if firstDetail == false>
            function initialize${field.fieldNameCapitalized}Table() {
            </#if>
            
            jQuery('#${field.fieldNameCapitalized}Table').DataTable({
                'ajax': {
                      'url': ${field.fieldNameCapitalized}BaseUrl
                 },
                'buttons' : [
                    {
                        'extend' : 'colvis',
                        'className' : 'btn-accion'
                    }, 
                    {
                        'extend' : 'pageLength',
                        'className' : 'btn-accion'
                    } 
                ],
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
            <#if firstDetail == false>
            }
            jQuery('#${field.fieldNameCapitalized}Tab').on('shown.bs.tab', function (e) {
				if (jQuery.fn.DataTable.isDataTable('#${field.fieldNameCapitalized}Table') === false) {
					initialize${field.fieldNameCapitalized}Table();
				}
	    	});
            </#if>
            <#assign firstDetail = false>
        </#list>
    });
    </script>
   
  </div>

  <!-- Application -->
  <script type="text/javascript" src="../../static/public/js/main.js" data-th-remove="all"></script>
  
</body>

</html>