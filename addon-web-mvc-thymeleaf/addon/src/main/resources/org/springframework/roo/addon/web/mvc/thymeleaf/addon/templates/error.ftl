<!DOCTYPE html>
<html data-layout-decorator="layouts/default-layout">
  <head>
    <meta charset="UTF-8" data-th-remove="all" />
    <title data-th-text="${r"#{"}label_error${r"}"}">Error</title>
  </head>
  <body>
    <header>
        <h1 data-th-text="${r"#{"}label_error_page${r"}"}">Error Page</h1>
    </header>
    <section data-layout-fragment="content">
        <h2>Error!</h2>
        <div>
        	<span data-th-text="${r"#{info_error}"}">An error occurred</span>
            (type=<span data-th-text="${r"${error}"}">Bad</span>, status=<span data-th-text="${r"${status}"}">500</span>).
        </div>
        <hr/>
        <div data-text="${r"${message}"}"></div>
        <hr/>
    </section>
    <footer>
        &copy; Powered By Spring Roo
    </footer> 
  </body>
</html>