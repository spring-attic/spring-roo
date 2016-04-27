<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8" />
    <title>Footer</title>
  </head>
  <body>
    <div data-layout-fragment="footer">
        <p class="text-right" data-th-text="|&copy; ${r"${#calendars.format(#dates.createNow(),'yyyy')}"} © Powered By Spring Roo|">
          © Powered By Spring Roo
        </p>
    </div>
  </body>
</html>