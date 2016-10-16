<#import "fields/input-text.ftl" as text>
<#import "fields/input-date.ftl" as date>
<#import "fields/reference.ftl" as reference>
<#import "fields/checkbox.ftl" as checkbox>
<#import "fields/enum.ftl" as enum>
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

<title data-th-text="|${r"#{"}label_search_entity(${r"#{"}${entityLabel}${r"}"})${r"}"} - ${projectName}|">${entityName} Search - ${projectName}</title>

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

<!-- Select2 -->
<link rel="stylesheet" type="text/css"
  href="https://cdnjs.cloudflare.com/ajax/libs/select2/4.0.2/css/select2.css"
  data-th-href="@{/webjars/select2/4.0.2/dist/css/select2.css}" />
<link rel="stylesheet" type="text/css"
  href="https://cdnjs.cloudflare.com/ajax/libs/select2-bootstrap-theme/0.1.0-beta.7/select2-bootstrap.css"
  data-th-href="@{/webjars/select2-bootstrap-theme/0.1.0-beta.7/dist/select2-bootstrap.css}" />

<!-- DateTimePicker -->
<link rel="stylesheet" type="text/css"
  href="https://cdnjs.cloudflare.com/ajax/libs/jquery-datetimepicker/2.5.4/build/jquery.datetimepicker.min.css" 
  data-th-href="@{/webjars/datetimepicker/2.5.4/build/jquery.datetimepicker.min.css}" />

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

    <!--CONTENT-->
    <section data-layout-fragment="content">
      <div class="container-fluid content">
	<!--
	  Only the inner content of the following tag "section" is included
	  within the template, in the section "content"
        -->

        <h1 data-th-text="${r"#{"}label_search_entity(${r"#{"}${entityLabel}${r"}"})${r"}"}">${entityName} Search</h1>

	<div class="panel panel-default">
	  <div class="panel-body">

        <!--FORM-->
        <form id="search-form" class="form-horizontal" method="POST" data-th-object="${r"${formBean}"}"
          data-th-action="@{${controllerPath}/search/${action}}" action="finderList.html">
          <fieldset id="containerFields">
            <#list fields as field>
                <#if field.type == "TEXT">
                    <@text.input label=field.label fieldName=field.fieldName fieldId=field.fieldId z=field.z size=3 />
                <#elseif field.type == "DATE">
                    <@date.input label=field.label 
                    fieldName=field.fieldName
                    fieldId=field.fieldId
                    z=field.z
                    format=field.configuration.format />
                <#elseif field.type == "REFERENCE">
                    <@reference.input label=field.label 
                        fieldName=field.fieldName 
                        fieldId=field.fieldId
                        z=field.z
                        referencedEntity=field.configuration.referencedEntity
                        identifierField=field.configuration.identifierField
                        referencedPath=field.configuration.referencedPath
                        fieldOne=field.configuration.fieldOne
                        fieldTwo=field.configuration.fieldTwo />
                <#elseif field.type == "ENUM">
                    <@enum.input label=field.label 
                    fieldName=field.fieldName
                    fieldId=field.fieldId
                    z=field.z
                    items=field.configuration.items />                
                <#elseif field.type == "BOOLEAN">
                    <@checkbox.input label=field.label fieldName=field.fieldName fieldId=field.fieldId z=field.z />                
                </#if>
            </#list>
          </fieldset>

            <!-- form buttons -->
            <div class="form-group">
              <div class="col-md-9 col-md-offset-3">      
                  <button type="reset" class="btn btn-default"
                    onclick="location.href='list.html'"
                    data-th-onclick="'location.href=\'' + @{${controllerPath}} + '\''" 
                    data-th-text="${r"#{"}label_reset${r"}"}">Cancel</button>            
                  <button type="submit" class="btn btn-primary">
		    <span class="glyphicon glyphicon-search" aria-hidden="true"></span>&nbsp;<span
                      data-th-text="${r"#{"}label_search${r"}"}">Search</span>
                  </button> 
              </div>
            </div>

        </form>
        <!-- /FORM -->

         </div>
       </div>
       <!-- panel -->

       <!-- RESULT -->
       <div id="result"></div>

      </div>
      <!-- /CONTENT-->

    </section>

  </div>
  <!-- /CONTAINER -->

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

  <div data-layout-fragment="javascript">

    <!-- DateTimePicker -->
    <script
      src="https://cdnjs.cloudflare.com/ajax/libs/jquery-datetimepicker/2.5.4/build/jquery.datetimepicker.full.min.js"
      data-th-src="@{/webjars/datetimepicker/2.5.4/build/jquery.datetimepicker.full.min.js}"></script>
    <script src="../../static/public/js/datetimepicker-defaults.js"
      data-th-src="@{/public/js/datetimepicker-defaults.js}"></script>

    <!-- jquery.inputmask -->
    <script
       src="https://cdnjs.cloudflare.com/ajax/libs/jquery.inputmask/3.3.1/jquery.inputmask.bundle.min.js"
       data-th-src="@{/webjars/jquery.inputmask/3.3.1/min/jquery.inputmask.bundle.min.js}"></script>
    <script type="text/javascript" data-th-inline="javascript">
      (function(jQuery) {
        jQuery(document).ready(
	  function() {	 
	    Inputmask.extendAliases({
	      'numeric' : {	  
	         'groupSeparator' : /*[[${r"#{"}label_inputmask_groupSeparator${r"}"}]]*/'.',	      
	         'radixPoint' : /*[[${r"#{"}label_inputmask_radixPoint${r"}"}]]*/','
	       },
	       'currency' : {	        
	          'prefix' : /*[[${r"#{"}label_inputmask_prefix${r"}"}]]*/'',	
	          'suffix' : /*[[${r"#{"}label_inputmask_suffix${r"}"}]]*/' €'
	        }
	    });
	 });
       })(jQuery);
    </script>
    <script src="../../static/public/js/inputmask-defaults.js"
      data-th-src="@{/public/js/inputmask-defaults.js}"></script>

    <!-- JQuery Validation -->
    <script
      src="http://ajax.aspnetcdn.com/ajax/jquery.validate/1.15.0/jquery.validate.min.js"
      data-th-src="@{/webjars/jquery-validation/1.15.0/dist/jquery.validate.min.js}">
    </script>
    <script
      src="http://ajax.aspnetcdn.com/ajax/jquery.validate/1.15.0/additional-methods.min.js"
      data-th-src="@{/webjars/jquery-validation/1.15.0/dist/additional-methods.min.js}">
    </script>
    <script
      src="http://ajax.aspnetcdn.com/ajax/jquery.validate/1.15.0/localization/messages_es.js"
      data-th-src="@{/webjars/jquery-validation/1.15.0/src/localization/messages_}+${r"${#"}locale.language${r"}"}+'.js'"
      data-th-if="${r"${#"}locale.language${r"}"} != 'en'">
    </script>
    <script src="../../static/public/js/validation-defaults.js"
      data-th-src="@{/public/js/validation-defaults.js}">
    </script>
    <script type="text/javascript" data-th-inline="javascript">
      (function(jQuery) {
         jQuery(document).ready(function() {
	   jQuery.extend( jQuery.validator.messages, {
	     'dateformat' : /*[[${r"#{"}error_invalidDate${r"}"}]]*/ 'Please enter a correct date/time',
	     'inputmask': /*[[${r"#{"}lerror_invalidMaskValue${r"}"}]]*/ 'Please enter a valid value',
	   });
	 });
      })(jQuery);
    </script>

    <!-- Select2 -->
    <div data-layout-include="fragments/js/select2 :: select2">

       <!-- Select2 scripts ONLY for HTML templates
	    Content replaced by the select2 template fragment select2.html
       -->
       <script
         src="https://cdnjs.cloudflare.com/ajax/libs/select2/4.0.2/js/select2.full.js"
         data-th-src="@{/webjars/select2/4.0.2/dist/js/select2.full.js}"></script>
       <script
         src="https://cdnjs.cloudflare.com/ajax/libs/select2/4.0.2/js/i18n/es.js"
         data-th-src="@{/webjars/select2/4.0.2/dist/js/i18n/}+${r"${#"}locale.language${r"}"}+'.js'"
         data-th-if="${r"${#"}locale.language${r"}"} != 'en'"></script>

       <script type="text/javascript" data-th-inline="javascript">
         /*<![CDATA[*/   
	 jQuery('.dropdown-select-simple').select2({
	   debug : false,
           theme : 'bootstrap',
           allowClear : true,
         });      
         jQuery('.dropdown-select-ajax').select2(
           {
             debug : false,
	     theme : 'bootstrap',
	     allowClear : true,
	     ajax : {
               data : function(params) {	
		 var query = {
		   'search[value]' : params.term,
		   'page' : params.page - 1,
		 }
		 return query;
	       },	   
	       processResults : function(data, page) {
		 var idField = this.options.get('idField');
		 var txtFields = this.options.get('textFields');
		 var fields = txtFields.split(',');	
		 var results = [];
		 jQuery.each(data.content, function(i, entity) {
		   var id = entity[idField];
		   var text = '';		
		   jQuery.each(fields, function(i, fieldName) {
		     text = text.concat(' ', entity[fieldName]);
		   });	
		   var obj = {
		     'id' : id,
		     'text' : jQuery.trim(text)
		   };	
		   jQuery.each(entity, function(key, val) {
		     var attribute = jQuery.trim(key);
		     var value = jQuery.trim(val);
		     obj[attribute] = value;
		    });		
		    results.push(obj);
		  });	
		  var morePages = !data.last;
		  return {
		    results : results,
		    pagination : {
		      more : morePages
		    }
		  };
		},
	      },
            });
         /*]]>*/
       </script>

     </div>

    </div>

    <!-- Application -->
    <script type="text/javascript" charset="utf8"
      src="../../static/public/js/main.js"></script>
  
</body>
</#if>
</html>
