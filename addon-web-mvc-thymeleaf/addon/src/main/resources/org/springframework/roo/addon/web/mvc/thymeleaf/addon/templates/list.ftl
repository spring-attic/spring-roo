<!DOCTYPE html>
<html data-layout-decorator="layouts/default-layout">

<head>
  <meta charset="utf-8" />
  <meta http-equiv="X-UA-Compatible" content="IE=edge" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <meta name="description" content="${projectName}" data-th-remove="all" />
  <meta name="author" content="Spring Roo" data-th-remove="all" />
  
  <title data-th-text="${r"#{"}label_list_entity(${r"#{"}${entityLabelPlural}${r"}"})${r"}"}">List ${entityName}</title>
  
  <!-- Bootstrap -->
  
  <link rel="stylesheet" type="text/css" data-th-remove="all"
        href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.css"></link>
        
  <link rel="stylesheet" type="text/css" data-th-remove="all"
        href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap-theme.css"></link>      
         
  <!-- Datatables -->  
       
  <link rel="stylesheet" type="text/css" 
        href="//cdn.datatables.net/1.10.11/css/jquery.dataTables.css"></link>
  
  <link rel="stylesheet" type="text/css" 
        href="https://cdn.datatables.net/1.10.11/css/dataTables.bootstrap.css"></link>
  
  <link rel="stylesheet" type="text/css" 
        href="https://cdn.datatables.net/responsive/2.0.2/css/responsive.bootstrap.css"></link>
  
  <link rel="stylesheet" type="text/css" 
        href="https://cdn.datatables.net/buttons/1.1.2/css/buttons.bootstrap.css"></link>
  
  <link rel="stylesheet" type="text/css" 
        href="https://cdn.datatables.net/select/1.1.2/css/select.bootstrap.css"></link>
  
  <!-- Spring Roo Styles -->
  <link rel="stylesheet" type="text/css" data-th-href="@{/public/css/standard.css}" data-th-remove="all"
        href="../../static/public/css/standard.css"></link>

  <!-- HTML5 shim and Respond.js to support HTML5 elements and media queries -->
  <!--[if lt IE 9]>
    <script src="js/html5shiv.min.js"></script>
    <script src="js/respond.min.js"></script>
  <![endif]-->
</head>

<body>

  <!-- Session -->
  <div class="container upper-nav">
    <div class="session">
      <div data-th-text="${r"#{"}label_user${r"}"}">
        <span class="glyphicon glyphicon-user" aria-hidden="true"></span>User
      </div>
      <div data-th-text="${r"#{"}label_last_access(00-00-0000)${r"}"}">
        <span class="glyphicon glyphicon-calendar" aria-hidden="true"></span>Last Access: 00-00-0000
      </div>
      <button type="submit" class="exit" data-th-text="${r"#{"}label_exit${r"}"}">
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
            src="../../static/public/img/logo_spring_roo.png" /></a>
        </div>
        <div class="application-name">
          ${projectName}
        </div>
      </div>

      <!-- menu -->
      <nav class="navbar navbar-default">
        <div class="container-fluid">

          <div class="navbar-header">
            <button type="button" class="navbar-toggle collapsed"
              data-toggle="collapse"
              data-target="#bs-example-navbar-collapse-1" aria-expanded="false">
              <span class="sr-only">Desplegable</span>
              <span class="icon-bar"></span>
              <span class="icon-bar"></span>
              <span class="icon-bar"></span>
            </button>
            <a class="navbar-brand" href="#">Main Menu</a>
          </div>

          <div id="bs-example-navbar-collapse-1"
            class="navbar-collapse collapse">
            <ul class="nav navbar-nav">
              <li class="active"><a href="#">Active Menu 1</a></li>
              <li><a href="#">Menu 2</a></li>
              <li class="dropdown"><a href="#" class="dropdown-toggle"
                data-toggle="dropdown" role="button" aria-haspopup="true"
                aria-expanded="false">Dropdown 3 Menu<span class="caret"></span></a>
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
        <!--START TABLE-->
        <div class="table-responsive">
          <table id="${entityName}Table" 
                 class="table table-striped table-hover table-bordered" 
                 data-row-id="${identifierField}"
                 data-select="single"
                 data-order="[[ 0, &quot;asc&quot; ]]"
                 data-th-attr="data-ajax-url=@{${controllerPath}/},data-create-url=@{${controllerPath}/create-form/}">
            <caption data-th-text="${r"#{"}label_list_of_entity(${r"#{"}${entityLabelPlural}${r"}"})${r"}"}">List ${entityName}</caption>
            <thead>
              <tr>
                <#list fields as field>
                <th data-th-text="${r"#{"}${field.label}${r"}"}">${field.fieldName}</th>
                </#list>
                <th data-th-text="${r"#{"}label_tools${r"}"}">Tools</th>
              </tr>
            </thead>
            <tbody data-th-remove="all">
              <tr>
                <#list fields as field>
                <td>${field.fieldName}</td>
                </#list>
                <td data-th-text="${r"#{"}label_tools${r"}"}">Tools</td>
              </tr>
            </tbody>
          </table>
        </div>
        <!--END TABLE-->
        
        <div class="clearfix">
          <div class="pull-left">
            <a href="../index.html" class="btn btn-default"
               data-th-href="@{/}" data-th-text="${r"#{label_back}"}">
               <span class="glyphicon glyphicon-chevron-left" aria-hidden="true"></span>Back
            </a>
          </div>
        </div>

      </div>
      <!--END CONTENT-->
      
       <!-- MODAL -->
      <div
        data-layout-include="fragments/modal :: modal(id='delete${entityName}', title=${r"#{"}label_delete${r"}"})">

        <script type="text/javascript">
            function openDeleteModal(){
              jQuery('#staticModal').modal();
            }
          </script>

        <div class="modal fade" id="staticModal" tabindex="-1" role="dialog"
          aria-labelledby="staticModalLabel">
          <div class="modal-dialog" role="document">
            <div class="modal-content">
              <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal"
                  aria-label="${r"#{"}label_close${r"}"}">
                  <span aria-hidden="true">&times;</span>
                </button>
                <h2 class="modal-title" id="staticModalLabel" data-th-text="${r"#{"}label_delete${r"}"}">Delete</h2>
              </div>
              <div class="modal-body" id="staticModalBody">
                <p data-th-text="${r"#{"}label_message${r"}"}">Message</p>
              </div>
            </div>
          </div>
        </div>
      </div>
      
    </section>
    
  </div>
  <!--END CONTAINER-->

  <footer class="container">
    <p class="text-right">© Powered by Spring Roo</p>
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
        var ${entityName}Table = jQuery('#${entityName}Table').DataTable({
            'columns': [
              <#list fields as field>
              { 'data': '${field.fieldName}' },
              </#list>
              { 
                'data': '${identifierField}',
                'orderable': false,
                'searchable': false,
                'render': function ( data, type, full, meta ) {
                    return '<a role="button" class="btn-accion ver" href="${controllerPath}/' + data + '" data-th-text="${r"#{label_show}"}">Show</a>' +
                    '<a role="button" class="btn-accion modificar" href="${controllerPath}/' + data + '/edit-form" data-th-text="${r"#{label_edit}"}">Edit</a>' +
                    '<a role="button" class="btn-accion eliminar" data-th-text="${r"#{label_delete}"}" onclick="javascript:jQuery.delete${entityName}(' + data + ')"/>'
                }
              }
            ]  
        });
        
        jQuery.extend({
           'delete${entityName}': function(${identifierField}) {
               jQuery.ajax({
                   url: '${controllerPath}/'+${identifierField},
                   type: 'DELETE',
                   success: function(result) {
                     jQuery('#delete${entityName}ModalBody').empty();
                     jQuery('#delete${entityName}ModalBody').append('<p data-th-text="|${r"#{info_deleted_items_number(1)}"}|" >1 removed item</p>');
                     jQuery('#delete${entityName}Modal').modal();
                     /** Refresh Datatables */
                     ${entityName}Table.ajax.reload();
                   },
                   error: function(jqXHR) {
                     /** Getting error code */
                     var message = '';
                     /** CONFLICT */
                     if (jqXHR.status == 409) {
                         message = '<p data-th-text="|${r"#{info_no_deleted_item}"}|">0 items has been deleted.</p>';
                     }
                     /** NOT_FOUND */
                     if (jqXHR.status == 404 ) {
                         message = '<p data-th-text="|${r"#{info_deleted_item_problem} #{info_no_exist_item}"}|">Error deleting selected item.</p>';
                     }
                     jQuery('#delete${entityName}ModalBody').empty();
                     jQuery('#delete${entityName}ModalBody').append(message);
                     jQuery('#delete${entityName}Modal').modal();
                   }
                });
           }
         });
        
    });
    </script>
   
  </div>

  <!-- Application -->
  <script type="text/javascript" src="../../static/public/js/main.js" data-th-remove="all"></script>

</body>

</html>