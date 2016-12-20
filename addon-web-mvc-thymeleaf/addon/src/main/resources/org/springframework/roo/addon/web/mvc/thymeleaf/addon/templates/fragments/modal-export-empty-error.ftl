<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8" />
<title>Modal</title>
</head>
<#if userManagedComponents?has_content && userManagedComponents["body"]??>
  ${userManagedComponents["body"]}
<#else>
  <body id="body">
    <!--
    Only the inner content of the following div is included within the
    template, in the section modal
    -->
    <div data-th-fragment="modalExportEmptyError(tableId, title, message)">
    
      <div data-th-replace="~{fragments/modal :: modal(id = ${r"${"}tableId} + 'ExportEmptyError', title = ${r"${"}title}, body = ~{::modalErrorBody}, footer = ~{})}">
        <p data-th-text="|${r"#{"}error_export_empty}|" data-th-fragment="modalErrorBody">No records found to generate a report.</p>
      </div>

    </div>
  </body>
</#if>
</html>