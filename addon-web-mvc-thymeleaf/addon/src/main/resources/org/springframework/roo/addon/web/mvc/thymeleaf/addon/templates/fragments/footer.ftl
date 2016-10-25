<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8" />
    <title>Footer</title>
  </head>
<#if userManagedComponents?has_content && userManagedComponents["body"]??>
  ${userManagedComponents["body"]}
<#else>
  <body id="body">
    <!--
    Only the internal content of the following div is included within
    the template, in footer fragment
    -->
    <div class="container" data-layout-fragment="footer">

       <div class="row">
      	 <div class="col-sm-6 col-sm-offset-3">
           <small class="clearfix">
             Made with <a href="http://projects.spring.io/spring-roo/" target="_blank" data-th-text="| Spring Roo © ${r"${#calendars.format(#dates.createNow(),'yyyy')}"}|">
             Spring Roo &copy; 2016</a> •
             We <span class="glyphicon glyphicon-heart"></span> 
             <a href="https://github.com/spring-projects/spring-roo/" target="_blank">Open source</a> •
             <a data-th-href="@{/accessibility}" href="/accessibility"><span data-th-text="${r"#{"}label_accessibility${r"}"}">Accessibility</span></a>
            </small>
         </div>
       </div>
       <!-- certified logos -->
       <div class="row">
        <div class="col-sm-6 col-sm-offset-6 text-right">

         <a title="Explanation of WCAG 2.0 Level Double-A Conformance"
            data-th-title="${r"#{label_accessibility_title}"}"
            target="_blank"
            href="http://www.w3.org/WAI/WCAG2AA-Conformance">
            <img height="32" width="88"
                 src="http://www.w3.org/WAI/wcag2AA"
                 data-th-alt="${r"#{label_accessibility_alt}"}"
                 alt="Level Double-A conformance, W3C WAI Web Content
                 Accessibility Guidelines 2.0">
         </a>
         &nbsp;
         <a title="Application developed and tested with OWASP -
             Open Web Application Security Project"
             data-th-title="${r"#{label_owasp_title}"}"
            target="_blank"
            href="https://www.owasp.org">
          <img height="32" width="90"
               data-th-src="@{/public/img/owasp_logo.png}"
               src="../static/public/img/owasp_logo.png"
               data-th-alt="${r"#{label_owasp_alt}"}"
               alt="Application developed and tested with OWASP">
         </a>
        </div>
       </div>

    </div>

  </body>
</#if>
</html>
