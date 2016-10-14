<!DOCTYPE html>
<html lang="en" data-layout-decorator="layouts/default-layout">
<head>
<meta charset="UTF-8" data-th-remove="all" />
<meta http-equiv="X-UA-Compatible" content="IE=edge" data-th-remove="all" />
<meta name="viewport" content="width=device-width, initial-scale=1"
  data-th-remove="all" />
<meta name="description"
  content="Spring Roo, a next-generation rapid application development tool for Java developers. With Roo you can easily build full Java applications in minutes."
  data-th-remove="all" />
<meta name="author"
  content="Spring Roo development team"
  data-th-remove="all" />

<link rel="shortcut icon" href="../../static/public/img/favicon.ico"
       data-th-remove="all" />

<link rel="apple-touch-icon" href="../../static/public/img/apple-touch-icon.png"
       data-th-remove="all" />

<title data-th-text="|${r"#{"}label_show_entity(${r"#{"}${entityLabel}${r"}"})${r"}"} - ${projectName}|">Show ${entityName} - ${projectName}</title>

<!-- Bootstrap -->
<link rel="stylesheet" type="text/css"
  href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.css"
  data-th-remove="all"></link>

<!-- IE10 viewport hack for Surface/desktop Windows 8 bug -->
<link rel="stylesheet" type="text/css"
  href="https://maxcdn.bootstrapcdn.com/css/ie10-viewport-bug-workaround.css"
  data-th-remove="all"></link>

<!-- Font Awesome -->
<link rel="stylesheet" type="text/css"
  href="https://maxcdn.bootstrapcdn.com/font-awesome/4.6.2/css/font-awesome.min.css"
  data-th-remove="all" />

<!-- Datatables -->
<link
  data-th-href="@{/webjars/datatables/1.10.11/media/css/jquery.dataTables.css}"
  rel="stylesheet" type="text/css"
  href="https://cdn.datatables.net/1.10.11/css/jquery.dataTables.css"></link>

<link
  data-th-href="@{/webjars/datatables.net-bs/1.10.11/css/dataTables.bootstrap.css}"
  rel="stylesheet" type="text/css"
  href="https://cdn.datatables.net/1.10.11/css/dataTables.bootstrap.css"></link>

<link
  data-th-href="@{/webjars/datatables.net-responsive-bs/2.0.2/css/responsive.bootstrap.css}"
  rel="stylesheet" type="text/css"
  href="https://cdn.datatables.net/responsive/2.0.2/css/responsive.bootstrap.css"></link>

<link
  data-th-href="@{/webjars/datatables.net-buttons-bs/1.1.2/css/buttons.bootstrap.css}"
  rel="stylesheet" type="text/css"
  href="https://cdn.datatables.net/buttons/1.1.2/css/buttons.bootstrap.css"></link>

<link
  data-th-href="@{/webjars/datatables.net-select-bs/1.1.2/css/select.bootstrap.css}"
  rel="stylesheet" type="text/css"
  href="https://cdn.datatables.net/select/1.1.2/css/select.bootstrap.css"></link>

<!-- Bootswatch CSS custom -->
<link rel="stylesheet" type="text/css"
  href="../../static/public/css/theme.css"
  data-th-remove="all" />

<!-- Roo CSS -->
<link rel="stylesheet" type="text/css"
   href="../../static/public/css/springroo.css"
   data-th-remove="all" />

<!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->
<!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
<!--[if lt IE 9]>
      <script src="https://oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js"></script>
      <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
<![endif]-->

</head>

<#if userManagedComponents?has_content && userManagedComponents["body"]??>
  ${userManagedComponents["body"]}
<#else>
  <body id="body">

  <!--CONTAINER-->
  <div class="container bg-container">

      <!-- HEADER -->
      <header role="banner">

        <!-- BANNER -->
        <div class="bg-header">
          <div class="jumbotron bg-banner">
              <div class="container">
                <h1 class="project-name">${projectName}</h1>
                <h2 class="project-tagline">Hello, this is your home page.</h2>
              </div>
          </div>
        </div>

        <!-- Main navbar -->
        <nav class="navbar navbar-inverse navbar-fixed-top">
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
              <ul class="nav navbar-nav navbar-right upper-nav language">
                <li class="dropdown">
                  <a class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">
                    <span class="glyphicon glyphicon-user" aria-hidden="true"></span>&nbsp;<span class="hidden-sm">Language</span><span class="caret"></span>
                  </a>
                  <ul class="dropdown-menu" id="languageFlags">
                   <li id="enFlag"><a href="?lang=en"><img class="flag"
                   data-th-src="@{/static/public/img/en.png}" src="../../static/public/img/en.png"
                   alt="English">&nbsp;<span>English</span></a> </li>
                   <li id="esFlag"><a href="?lang=es"><img class="flag"
                   data-th-src="@{/static/public/img/es.png}" src="../../static/public/img/es.png"
                   alt="Spanish">&nbsp;<span>Spanish</span></a> </li>
                 </ul>
               </li>
              </ul>

              <!-- User menu -->
              <ul class="nav navbar-nav navbar-right upper-nav session">
                <li class="dropdown">
                  <a class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">
                    <span class="glyphicon glyphicon-user" aria-hidden="true"></span>&nbsp;<span class="hidden-sm">User</span><span class="caret"></span>
                  </a>
                  <ul class="dropdown-menu">
                   <li><a href="#"><span class="glyphicon glyphicon-wrench" aria-hidden="true"></span>&nbsp;<span>Admin Profile</span></a></li>
                   <li><a href="#"><span class="glyphicon glyphicon-lock" aria-hidden="true"></span>&nbsp;<span>Change password</span></a></li>
		   <li><form action="/logout" method="post">
		     <button type="button" class="btn btn-link">
		       <span class="glyphicon glyphicon-log-out" aria-hidden="true"></span>
		       <span>Log out</span>
		     </button>
		   </form></li>
                 </ul>
                </li>
              </ul>

              <!-- User menu links -->
              <ul class="nav navbar-nav navbar-right upper-nav links">
                <li><a href="#"><span class="glyphicon glyphicon-envelope" aria-hidden="true"></span>&nbsp;<span class="hidden-sm">Contact</span></a></li>
                <li><a href="#"><span class="glyphicon glyphicon-question-sign" aria-hidden="true"></span>&nbsp;<span class="hidden-sm">Help</span></a></li>
              </ul>

           </div>

          </div>
        </nav>

      </header>
      <!-- /HEADER -->

    <!-- CONTENT -->
    <section data-layout-fragment="content" data-th-object="${modelAttribute}">
      <div class="container-fluid content">

        <!-- CONTENT -->
        <h1 data-th-text="${r"#{"}label_show_entity(${r"#{"}${entityLabel}${r"}"})${r"}"}">Show ${entityName}</h1>

        <ul class="list-unstyled" id="containerFields">
          <#list fields as field>
            <#if field.userManaged>
              ${field.codeManaged}
            <#else>
              <li id="${field.fieldId}" data-z="${field.z}">
          	    <strong data-th-text="${r"#{"}${field.label}${r"}"}">${field.fieldName}</strong>
                <span data-th-text="*{{${field.fieldName}}}">${field.fieldName}Value</span>
       	      </li>
           </#if>
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
                        <!-- TABLE -->
                        <div class="table-responsive">
                          <table id="${field.fieldNameCapitalized}Table"
                                 class="table table-striped table-hover table-bordered"
                                 data-row-id="${field.configuration.identifierField}"
                                 data-select="single"
                                 data-z="${field.z}"
                                 data-order="[[ 0, &quot;asc&quot; ]]">
                            <caption data-th-text="${r"#{"}label_list_of_entity(${r"#{"}${field.configuration.referencedFieldLabel}${r"}"})${r"}"}">${field.fieldNameCapitalized} List</caption>
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
                        <!-- /TABLE -->
                    </div>
                  </#list>
              </div>
            </#if>
        </ul>

        <div class="clearfix">
          <div class="pull-left">
            <a id="${entityName}_list" href="list.html" class="btn btn-default"
	       data-th-href="@{${controllerPath}}"> <span
	       class="glyphicon glyphicon-chevron-left" aria-hidden="true"></span><span
	       data-th-text="${r"#{"}label_back${r"}"}">Back</span>
	    </a>
          </div>
          <div class="pull-right">
	     <a id="${entityName}_edit" href="edit.html" class="btn btn-primary"
	        data-th-href="@{${controllerPath}/${r"{"}${identifierField}${r"}"}/edit-form(${identifierField}=${r"*{id}"})}"
	        data-th-text="${r"#{label_edit}"}">Edit</a>
          </div>
        </div>

      </div>
      <!-- /CONTENT-->

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
  <script type="text/javascript" charset="utf8"
    src="https://code.jquery.com/jquery-1.12.3.js"></script>

  <!-- Bootstrap -->
  <script type="text/javascript"
    src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.js"></script>

  <!-- IE10 viewport hack for Surface/desktop Windows 8 bug -->
  <script type="text/javascript" charset="utf8"
    src="../../static/public/js/ie10-viewport-bug-workaround.js"></script>

  <!-- MomentJS -->
  <script
     src="https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.13.0/moment.js">
  </script>
  <script
     src="https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.13.0/locale/es.js"
     data-th-if="${r"${#"}locale.language${r"}"} != 'en'">
  </script>
  <script src="../../static/public/js/moment-defaults.js">
  </script>

  <!-- Javascript fragment -->
  <div data-layout-fragment="javascript">

    <!-- Progress bar -->
    <script type="text/javascript">
      function loading() {
        jQuery('.progress').removeClass('hide');
      }
     </script>

     <!-- Datatables fragment -->
     <div data-layout-include="fragments/js/datatables :: datatables">

       <!-- Datatables scripts ONLY for HTML templates
            Content replaced by the datatables template fragment datatables.html
       -->
       <script type="text/javascript" charset="utf8"
         src="https://cdn.datatables.net/1.10.11/js/jquery.dataTables.js"
         data-th-src="@{/webjars/datatables/1.10.11/media/js/jquery.dataTables.js}"></script>
       <script type="text/javascript" charset="utf8"
         src="https://cdn.datatables.net/1.10.11/js/dataTables.bootstrap.js"
         data-th-src="@{/webjars/datatables.net-bs/1.10.11/js/dataTables.bootstrap.js}"></script>
        <!-- Datatables responsive plugin -->
        <script type="text/javascript" charset="utf8"
          src="https://cdn.datatables.net/responsive/2.0.2/js/dataTables.responsive.js"
          data-th-src="@{/webjars/datatables.net-responsive/2.0.2/js/dataTables.responsive.js}"></script>
        <script type="text/javascript" charset="utf8"
          src="https://cdn.datatables.net/responsive/2.0.2/js/responsive.bootstrap.js"
          data-th-src="@{/webjars/datatables.net-responsive-bs/2.0.2/js/responsive.bootstrap.js}"></script>
        <!-- Datatables buttons plugins -->
        <script type="text/javascript" charset="utf8"
          src="https://cdn.datatables.net/buttons/1.1.2/js/dataTables.buttons.js"
          data-th-src="@{/webjars/datatables.net-buttons/1.1.2/js/dataTables.buttons.js}"></script>
        <script type="text/javascript" charset="utf8"
          src="https://cdn.datatables.net/buttons/1.1.2/js/buttons.bootstrap.js"
          data-th-src="@{/webjars/datatables.net-buttons-bs/1.1.2/js/buttons.bootstrap.js}"></script>
        <script type="text/javascript" charset="utf8"
          src="https://cdn.datatables.net/buttons/1.1.2/js/buttons.colVis.js"
          data-th-src="@{/webjars/datatables.net-buttons/1.1.2/js/buttons.colVis.js}"></script>
        <script type="text/javascript" charset="utf8"
          src="https://cdn.datatables.net/buttons/1.1.2/js/buttons.flash.js"
          data-th-src="@{/webjars/datatables.net-buttons/1.1.2/js/buttons.flash.js}"></script>
        <script type="text/javascript" charset="utf8"
          src="https://cdn.datatables.net/buttons/1.1.2/js/buttons.html5.js"
          data-th-src="@{/webjars/datatables.net-buttons/1.1.2/js/buttons.html5.js}"></script>
        <!-- Datatables select plugin -->
        <script type="text/javascript" charset="utf8"
          src="https://cdn.datatables.net/select/1.1.2/js/dataTables.select.js"
          data-th-src="@{/webjars/datatables.net-select/1.1.2/js/dataTables.select.js}"></script>

    </div>

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
  <script src="../../static/public/js/main.js"></script>

</body>
</#if>
</html>
