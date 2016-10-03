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

 <title data-th-text="${r"#{"}label_list_entity(${r"#{"}${entityLabelPlural}${r"}"})${r"}"}">${projectName} - List ${entityName}</title>

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
    
<!-- Datatables -->  
       
  <link rel="stylesheet" type="text/css" 
        href="//cdn.datatables.net/1.10.12/css/dataTables.bootstrap.css"></link>
  
  <link rel="stylesheet" type="text/css" 
        href="//cdn.datatables.net/responsive/2.1.0/css/responsive.bootstrap.css"></link>
  
  <link rel="stylesheet" type="text/css" 
        href="//cdn.datatables.net/buttons/1.2.0/css/buttons.bootstrap.css"></link>
  
  <link rel="stylesheet" type="text/css" 
        href="//cdn.datatables.net/select/1.2.0/css/select.bootstrap.css"></link>

</head>

<body>

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
          <a href="/" data-th-href="@{/}">${projectName}</a>
        </div>
      </div>

      <!-- MAIN MENU -->
      <nav class="navbar navbar-default">
        <div class="container-fluid">

          <!-- collapsed menu button -->
          <div class="navbar-header">
            <button type="button" class="navbar-toggle collapsed"
              data-toggle="collapse"
              data-target="#bs-example-navbar-collapse-1" aria-expanded="false">
              <span class="sr-only">Dropdown</span>
              <span class="icon-bar"></span>
              <span class="icon-bar"></span>
              <span class="icon-bar"></span>
            </button>
            <a class="navbar-brand" href="#">Main Menu</a>
          </div>

          <div id="bs-example-navbar-collapse-1" 
            class="navbar-collapse collapse">
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
        <h1 data-th-text="${r"#{"}${entityLabelPlural}${r"}"}">${entityName}s</h1>

        <!--START TABLE-->
        <div class="table-responsive" id="containerFields">
          <table id="${entityName}Table" 
                 class="table table-striped table-hover table-bordered" 
                 data-row-id="${identifierField}"
                 data-select="single"
                 data-z="${z}"
                 data-order="[[ 0, &quot;asc&quot; ]]"
                 data-th-attr="data-create-url=@{${controllerPath}/create-form/}">
            <caption data-th-text="${r"#{"}label_list_entity(${r"#{"}${entityLabelPlural}${r"}"})${r"}"}">List ${entityName}</caption>
            <thead>
              <tr>
                <#list fields as field>
                <#if field.type != "LIST">
                <th data-th-text="${r"#{"}${field.label}${r"}"}">${field.fieldName}</th>
                </#if>
                </#list>
                <th data-th-text="${r"#{"}label_tools${r"}"}">Tools</th>
              </tr>
            </thead>
            <tbody data-th-remove="all">
              <tr>
                <#list fields as field>
                <#if field.type != "LIST">
                <td>${field.fieldName}</td>
                </#if>
                </#list>
                <td data-th-text="${r"#{"}label_tools${r"}"}">Tools</td>
              </tr>
            </tbody>
          </table>
        </div>
        <!--END TABLE-->
        
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
                            data-z="${field.z}"
                            data-row-id="${field.configuration.identifierField}" data-defer-loading="0"
                            data-order="[[ 0, &quot;asc&quot; ]]"
                            data-create-url-function="create${field.configuration.referencedFieldType}Url">
                            <caption data-th-text="${r"#{"}label_list_of_entity(${r"#{"}${field.configuration.referencedFieldLabel}${r"}"})${r"}"}">List ${field.fieldNameCapitalized}</caption>
                            <thead>
                              <tr>
                                <#list field.configuration.referenceFieldFields as referencedFieldField>
                                <#if referencedFieldField != entityName>
                                    <th data-th-text="${r"#{"}${referencedFieldField.label}${r"}"}">${referencedFieldField.fieldName}</th>
                                </#if>
                                </#list>
                                <th data-th-text="${r"#{"}label_tools${r"}"}">Tools</th>
                              </tr>
                            </thead>
                            <tbody data-th-remove="all">
                              <tr>
                                <#list field.configuration.referenceFieldFields as referencedFieldField>
                                <#if referencedFieldField != entityName>
                                    <td>${referencedFieldField.fieldName}</td>
                                </#if>
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
        
        <div class="clearfix">
          <div class="pull-left">
            <a href="../index.html" class="btn btn-default" data-th-href="@{/}"> 
               <span class="glyphicon glyphicon-chevron-left" aria-hidden="true"></span>
               <span data-th-text="${r"#{label_back}"}">Back</span>
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
     jQuery(document).ready( function () {
        var ${entityName}Table = jQuery('#${entityName}Table').DataTable({
            'ajax': {
                  'url': [[@{${controllerPath}/search/${finderPath}}]],
                  'data': [[${r"${formBean}"}]]
              },
            'columns': [
              <#list fields as field>
              { 'data': '${field.fieldName}' },
              </#list>
              { 
                'data': '${identifierField}',
                'orderable': false,
                'searchable': false,
                'render': function ( data, type, full, meta ) {
                    var baseUrl = [[@{${controllerPath}/}]];
                    return '<a role="button" class="btn-accion ver" href="' + baseUrl + data + '" data-th-text="${r"#{label_show}"}">Show</a>' +
                    '<a role="button" class="btn-accion modificar" href="' + baseUrl + data + '/edit-form" data-th-text="${r"#{label_edit}"}">Edit</a>' +
                    '<a role="button" class="btn-accion eliminar" data-th-text="${r"#{label_delete}"}" onclick="javascript:jQuery.delete${entityName}(' + data + ')"/>'
                }
              }
            ]  
        });
        
        jQuery.extend({
           'delete${entityName}': function(${identifierField}) {
               var baseUrl = [[@{${controllerPath}/}]];
               jQuery.ajax({
                   url: baseUrl + ${identifierField},
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
         
         <#if details?size != 0>
         	<#assign firstDetail = true>
             <#list details as field>
             	<#if firstDetail == false>
             	function initialize${field.fieldNameCapitalized}Table() {
             	</#if>
               	jQuery('#${field.fieldNameCapitalized}Table').DataTable({
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
                      <#if referencedFieldField != entityName>
                        { 'data': '${referencedFieldField.fieldName}' },
                      </#if>
                      </#list>
                      { 
                        'data': '${field.configuration.identifierField}',
                        'orderable': false,
                        'searchable': false,
                        'render': function ( data, type, full, meta ) {
                            return '';
                        }
                      }
                    ]  
                });
                  <#if firstDetail == false>
             }
                  	jQuery('#${field.fieldNameCapitalized}Tab').on('shown.bs.tab', function (e) {
						if (jQuery.fn.DataTable.isDataTable('#${field.fieldNameCapitalized}Table') === false) {
							initialize${field.fieldNameCapitalized}Table();
							var url${field.fieldNameCapitalized} = jQuery.${field.fieldNameCapitalized}BaseUrl();
							if (url${field.fieldNameCapitalized}) {
								jQuery('#${field.fieldNameCapitalized}Table').DataTable().ajax.url(urlPruebaPets).load();
							}
						}
	    			});
                  </#if>
                  jQuery.extend({
                    'current${entityName}Id': undefined,
                    '${field.fieldNameCapitalized}BaseUrl': function() {
                      if(jQuery.current${entityName}Id) {
                        return [[@{${controllerPath}/}]] + jQuery.current${entityName}Id + '${field.configuration.controllerPath}/';
                      }
                      return undefined;
                    },
                    'create${field.fieldNameCapitalized}Url': function() {
                      if(jQuery.current${entityName}Id) {
                        return jQuery.${field.fieldNameCapitalized}BaseUrl() + jQuery.createUri + '/';
                      }
                      return undefined;
                    },
                    'update${field.fieldNameCapitalized}Url': function(${field.fieldNameCapitalized}Id) {
                      if(jQuery.current${entityName}Id) {
                        return jQuery.${field.fieldNameCapitalized}BaseUrl() + ${field.fieldNameCapitalized}Id + '/'+ jQuery.editUri + '/';
                      }
                      return undefined;
                    },
                    'delete${field.fieldNameCapitalized}Url': function(${field.fieldNameCapitalized}Id) {
                      if(jQuery.current${entityName}Id) {
                        return jQuery.${field.fieldNameCapitalized}BaseUrl() + ${field.fieldNameCapitalized}Id + '/'+ jQuery.deleteUri + '/';
                      }
                      return undefined;
                    }
                  });
             	<#assign firstDetail = false>
             </#list>
             
           ${entityName}Table.on( 'select', function ( e, dt, type, indexes ) {
              if ( type === 'row' ) {
                var new${entityName}Id = ${entityName}Table.rows( indexes ).ids()[0];
                if (jQuery.current${entityName}Id != new${entityName}Id) {
                  jQuery.current${entityName}Id = new${entityName}Id;
                  <#list details as field>
                  var url${field.fieldNameCapitalized} = jQuery.${field.fieldNameCapitalized}BaseUrl();
                  if (jQuery.fn.DataTable.isDataTable('#${field.fieldNameCapitalized}Table') === true) {
                 	jQuery('#${field.fieldNameCapitalized}Table').DataTable().ajax.url( url${field.fieldNameCapitalized} ).load();
                  }
                  </#list>
                }
              }
            });
         </#if>
         
         
         
        
    });
    </script>
   
  </div>

  <!-- Application -->
  <script type="text/javascript" src="../../static/public/js/main.js" data-th-remove="all"></script>

</body>

</html>