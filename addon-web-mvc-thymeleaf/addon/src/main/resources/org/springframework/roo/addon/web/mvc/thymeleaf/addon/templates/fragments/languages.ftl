<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8" />
    <title>Languages</title>
  </head>
<#if userManagedComponents?has_content && userManagedComponents["body"]??>
  ${userManagedComponents["body"]}
<#else>
  <body id="body">
    <!--
    Only the internal content of the following div is included within
    the template, in session fragment
    -->
    <ul data-th-fragment="languages" class="nav navbar-nav navbar-right upper-nav languages">
      <!-- Language -->
        <li class="dropdown">
          <a class="dropdown-toggle" data-toggle="dropdown" role="button"
            aria-haspopup="true" aria-expanded="false">
  			    <span class="glyphicon glyphicon-globe" aria-hidden="true"></span>&nbsp
  			    <span class="hidden-sm" data-th-text="${r"#{"}language_label}">Language
  			    </span><span class="caret"></span>
  			   </a>
  			   <ul class="dropdown-menu" id="languageFlags">
  			   <#if languages??>
      			<#list languages as language>
      			<li id="${language.localePrefix}Flag">
      			 <a href="?lang=${language.localePrefix}"
      			    data-th-title="${r"#{"}label_gotoLanguage}+' ${language.language}'">
      			   <img class="flag"
      			   data-th-src="@{/public/img/${language.localePrefix}.png}"
      			   src="/public/img/${language.localePrefix}.png"
      			   alt="${language.language}" />&nbsp;
      			   <span data-th-text="${r"#"}{language_label_${language.localePrefix}}">
      			   ${language.language}</span>
      			 </a>
      			</li>
      		  </#list>
      		 </#if>
    	     </ul>
        </li>
    </ul>

  </body>
  </#if>
</html>