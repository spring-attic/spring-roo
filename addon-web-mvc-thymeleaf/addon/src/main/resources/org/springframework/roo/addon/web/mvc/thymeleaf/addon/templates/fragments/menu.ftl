<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8" />
    <title>Menu</title>
  </head>
  <body>
    <div data-layout-fragment="menu">
      <nav class="navbar navbar-default">
        <div class="container-fluid">

          <div class="navbar-header">
            <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1" aria-expanded="false">
              <span class="sr-only">Dropdown</span>
              <span class="icon-bar"></span>
              <span class="icon-bar"></span>
              <span class="icon-bar"></span>
            </button>
          <a class="navbar-brand" href="#">Principal Menu</a>
          </div>

          <div id="bs-example-navbar-collapse-1" class="navbar-collapse collapse">
            <ul class="nav navbar-nav">
              <#list menuEntries as entry>
                <li class="dropdown">
                 <a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">${entry.entityName}<span class="caret"></span></a>
                 <ul class="dropdown-menu">
                  <li><a href="${entry.path}/create-form">Create ${entry.entityName}</a></li>
                  <li><a href="${entry.path }">List ${entry.entityName}</a></li>
                  </ul>
                </li>              
              </#list>
            </ul>
         </div>
       </div>
      </nav>
    </div>
  </body>
</html>