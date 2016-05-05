<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8" />
    <title>Footer</title>
  </head>
  <body>
    <!--
    Only the internal content of the following div is included within
    the template, in footer fragment
    -->
    <div data-layout-fragment="footer">

        <div class="container">
          <small class="clearfix">
          Made with <a href="http://projects.spring.io/spring-roo/" target="_blank" data-th-text="| Spring Roo © ${r"${#calendars.format(#dates.createNow(),'yyyy')}"}|">
          &copy; Spring Roo</a> •
          We <span class="glyphicon glyphicon-heart"></span> <a href="https://github.com/spring-projects/spring-roo/" target="_blank">Open source </a>
          </small>
        </div>

    </div>
  </body>
</html>