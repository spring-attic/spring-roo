<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8" />
    <title>Page menu</title>
  </head>
<#if userManagedComponents?has_content && userManagedComponents["body"]??>
  ${userManagedComponents["body"]}
<#else>
  <body id="body">
    <!--
    Only the internal content of the following div is included within
    the template, in menu fragment
    -->
    <div data-layout-fragment="menu">

        <!-- Main navbar -->
        <nav class="navbar navbar-inverse navbar-fixed-top">
         <div class="container">

            <div class="navbar-header">
              <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#mainnav" aria-expanded="false">
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

            <div id="mainnav" class="navbar-collapse collapse">

              <ul class="nav navbar-nav" id="entitiesMenuEntries">
               <#list menuEntries as entry>
               <#if entry.userManaged>
                 ${entry.codeManaged}
               <#else>
                <li class="dropdown" id="${entry.id}-entry" data-z="${entry.z}">
                 <a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button"
                   aria-haspopup="true" aria-expanded="false"
                   data-th-utext="${r"#{"}label_menu_entry(${r"#{"}${entry.entityLabel}${r"}"})${r"}"}">${entry.entityName}<span class="caret"></span></a>
                 <ul class="dropdown-menu">
                  <li><a href="${entry.path}/create-form" data-th-href="@{${entry.path}/create-form}" data-th-text="${r"#{"}label_create_entity(${r"#{"}${entry.entityLabel}${r"}"})${r"}"}">Create ${entry.entityName}</a></li>
                  <li><a href="${entry.path}" data-th-href="@{${entry.path}}" data-th-text="${r"#{"}label_list_entity(${r"#{"}${entry.entityPluralLabel}${r"}"})${r"}"}">List ${entry.entityName}</a></li>
                  <#if entry.finderNamesAndPaths?has_content>
                    <#list entry.finderNamesAndPaths?keys as finderKey>
                      <li><a href="${entry.path}/${entry.finderNamesAndPaths[finderKey]}" data-th-href="@{${entry.path}/${entry.finderNamesAndPaths[finderKey]}}" data-th-text="${finderKey}" id="${entry.entityName}${finderKey}" >Search ${finderKey}</a></li>
                    </#list>
                  </#if>
                 </ul>
                </li>
               </#if>
               </#list>
              </ul>

              <!-- Language select -->
              <div data-layout-include="fragments/languages :: languages">
                <!-- Content replaced by the session template fragment language.html
                -->
                <span>Language data</span>
              </div>

              <!-- User menu -->
              <div data-layout-include="fragments/session-links :: session">
                <!-- Content replaced by the session template fragment session-links.html
                     It's possible include fragments/session :: session
                     if you want menu user application without links
                -->
                <span>User session data</span>
              </div>

           </div>

          </div>
        </nav>

    </div>

    <!--
    Only the internal content of the following div is included within
    the template, in menu-languages fragment
    -->
    <div data-layout-fragment="menu-languages">
        <!-- Main navbar -->
        <nav class="navbar navbar-inverse navbar-fixed-top">
         <div class="container">

            <div class="navbar-header"> 

              <!-- Brand logo -->
              <div class="organization-logo navbar-brand">
                <a title="${projectName}" href="/" data-th-href="@{/}">
                  <img alt="${projectName}" data-th-src="@{/public/img/logo.png}" src="../../static/public/img/logo.png" />
                </a>
              </div>
              <!-- Name application -->
              <div class="application-name navbar-brand hidden-xs"><a href="/" data-th-href="@{/}">${projectName}</a></div>

            </div><!-- /navbar-header -->

            <div id="mainnav" class="navbar-collapse collapse">

              <!-- Language select -->
              <div data-layout-include="fragments/languages :: languages">
                <!-- Content replaced by the session template fragment language.html
                -->
                <span>Language data</span>
              </div>

           </div>

          </div>
        </nav>

    </div>

  </body>
  </#if>
</html>
