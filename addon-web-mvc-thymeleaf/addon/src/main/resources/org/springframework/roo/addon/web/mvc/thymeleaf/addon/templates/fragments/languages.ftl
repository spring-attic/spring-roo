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
    <div data-layout-fragment="languages">

      <!-- Language -->
      <ul class="nav navbar-nav navbar-right upper-nav languages">
          <li class="dropdown"> 
		<a class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">
			<span class="glyphicon glyphicon-user" aria-hidden="true"></span>&nbsp<span class="hidden-sm" data-th-text="${r"#{language_label}"}">Language</span><span class="caret"></span>
		</a>
    	        <ul class="dropdown-menu" id="languageFlags">
      			<#list languages as language>
        			<li id="${language.localePrefix}Flag">
					<a href="?lang=${language.localePrefix}">
						<img class="flag" src="/public/img/${language.localePrefix}.png" alt="${language.language}" />&nbsp;
						<span data-th-text="${r"#{language_label_"}${language.localePrefix}${r"}"}">${language.language}</span>
					</a>
				</li>
      		        </#list>
    	        </ul>
          </li>
      </ul>

    </div>

  </body>
  </#if>
</html>