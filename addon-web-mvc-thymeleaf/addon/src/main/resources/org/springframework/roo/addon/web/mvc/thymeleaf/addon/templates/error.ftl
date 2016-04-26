<!DOCTYPE html>
<html data-layout-decorator="layouts/default-layout">
  <head>
    <meta charset="UTF-8" data-th-remove="all" />
    <title>Error</title>
  </head>
  <body>
    <header>
        <h1>Error Page</h1>
    </header>
    <section data-layout-fragment="content">
        <h2>Error!</h2>
        <div>
            An error occurs (type=<span data-text="${r"${error}"}">Bad</span>, status=<span data-text="${r"${status}"}">500</span>).
        </div>
        <hr/>
        <div data-text="${r"${message}"}"></div>
        <hr/>
        <li><a href="/" data-th-href="@{/}">Home</a></li>
        </ul>
    </section>
    <footer>
        Powered by Spring Roo
    </footer> 
  </body>
</html>