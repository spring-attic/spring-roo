<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8" />
    <title>Page menu</title>
  </head>
  <body>
    <!--
    Only the internal content of the following div is included within
    the template, in menu fragment
    -->
    <div data-layout-fragment="menu">

        <!-- Main navbar -->
        <nav class="navbar navbar-inverse navbar-fixed-top">
         <div class="container">

            <div class="navbar-header">
              <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1" aria-expanded="false">
                <span class="sr-only">Menu</span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
              </button>

              <!-- Brand logo -->
              <div class="organization-logo navbar-brand">
                <a title="${projectName}" href="/" data-th-href="@{/}">
                  <img alt="${projectName}" data-th-src="@{/public/img/logo.png}" src="../../static/public/img/logo.png" />
                </a>
              </div>
              <!-- Name application -->
              <div class="application-name navbar-brand hidden-xs"><a href="/" data-th-href="@{/}">${projectName}</a></div>

            </div><!-- /navbar-header -->

            <div id="bs-example-navbar-collapse-1" class="navbar-collapse collapse">

              <ul class="nav navbar-nav" id="entitiesMenuEntries">
               <#list menuEntries as entry>
                <li class="dropdown" id="${entry.entityName}Entry">
                 <a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" 
                   aria-haspopup="true" aria-expanded="false"
                   data-th-utext="${r"#{"}label_menu_entry(${r"#{"}${entry.entityLabel}${r"}"})${r"}"}">${entry.entityName}<span class="caret"></span></a>
                 <ul class="dropdown-menu">
                  <li><a href="${entry.path}/create-form" data-th-href="@{${entry.path}/create-form}" data-th-text="${r"#{"}label_create_entity(${r"#{"}${entry.entityLabel}${r"}"})${r"}"}">Create ${entry.entityName}</a></li>
                  <li><a href="${entry.path}" data-th-href="@{${entry.path}}" data-th-text="${r"#{"}label_list_entity(${r"#{"}${entry.entityPluralLabel}${r"}"})${r"}"}">List ${entry.entityName}</a></li>
                  <#list entry.finders as finder>
                    <li><a href="${entry.path}/${finder}Form" data-th-href="@{${entry.path}/${finder}Form}" data-th-text="${finder}" id="${entry.entityName}${finder}" >Search ${finder}</a></li>
                  </#list>
                 </ul>
                </li>        
               </#list>
              </ul>
              
              <!-- Languages -->
              <div data-layout-include="fragments/languages :: languages">
              	<span>Languages</span>
              </div>

              <!-- Menu -->
              <div data-layout-include="fragments/session-links :: session">
                <!-- Content replaced by languages and session template fragment session-links.html -->
                <span>User session data</span>
              </div> 
           
           </div>
    

          </div>
        </nav>

    </div>
  </body>
</html>