<!DOCTYPE html>
<html lang="en" data-layout-decorate="~{layouts/default-layout}">
  <#if userManagedComponents?has_content && userManagedComponents["head"]??>
  ${userManagedComponents["head"]}
  <#else>
  <head id="head">

      <title data-th-text="${r"#{"}label_error}">Error - Spring Roo application</title>

  </head>
  </#if>

  <#if userManagedComponents?has_content && userManagedComponents["body"]??>
    ${userManagedComponents["body"]}
  <#else>
  <body id="body">

    <header role="banner">
      <h1 data-th-text="${r"#{"}label_errorpage}">Error Page</h1>
      <!-- Content replaced by layout of the page displayed -->
    </header>

    <!-- CONTAINER -->
    <div class="container bg-container">
    <!-- CONTENT -->
      <!--
        Only the inner content of the following tag "section" is included
        within the template, in the section "content"
      -->
      <section data-layout-fragment="content">
        <div class="alert alert-danger fade in" role="alert">
          <h4 data-th-text="${r"#{"}label_errorpage_header}">¡Error!</h4>
          <p>
            <span data-th-text="${r"#"}{info_error}">An unexpected error has occurred</span>
            (type=<span data-th-text="${r"$"}{error}">Bad</span>, status=<span data-th-text="${r"${status}"}">500</span>).
          </p>
          <div data-th-text="${r"$"}{message}"></div>
        </div>
      </section>

    <footer class="container">
      <!-- Content replaced by layout of the page displayed -->
    </footer>

  </body>
  </#if>

</html>