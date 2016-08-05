<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8" />
    <title>Languages</title>
  </head>
  <body>
    <!--
    Only the internal content of the following div is included within
    the template, in session fragment
    -->
    <div data-layout-fragment="languages">

      <!-- User menu -->
      <ul class="nav navbar-nav navbar-right upper-nav languages">
          <li class="dropdown"> <a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false" data-th-utext="${r"#{label_menu_entry(#{language_label})}"}"><span class="caret">&nbsp</span></a>
    			<ul class="dropdown-menu" id="languageFlags">
      				<#list languages as language>
        				<li id="${language.localePrefix}Flag"><a href="?lang=${language.localePrefix}"><img class="flag" src="/img/${language.localePrefix}.png" alt="${language.language}" />&nbsp;<span data-th-text="${r"#{language_label_"}${language.localePrefix}${r"}"}">${language.language}</span></a>
        				</li>
      				</#list>
    			</ul>
		  </li>
      </ul>

    </div>

  </body>
</html>