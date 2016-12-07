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
  <div data-th-fragment="modalConfirmDelete(tableId, title, message)">
      <div data-th-replace="~{fragments/modal :: modal(id = ${r"$"}{tableId} + 'DeleteConfirm',
           title = ${r"$"}{title}, body = ~{::modalConfirmBody}, footer = ~{::modalConfirmFooter})}">
        <div data-row-id=""
             data-th-id="${r"$"}{tableId} + DeleteRowId"
             data-th-fragment="modalConfirmBody">
          <p data-th-text="${r"$"}{message}">Going to remove the selected
              element</p>
        </div>
        <div data-th-fragment="modalConfirmFooter">
            <button type="reset" class="btn btn-default pull-left"
              data-dismiss="modal" aria-label="Close"
              data-th-text="${r"#{"}label_reset}">Cancel</button>
            <button type="button" class="btn btn-primary pull-right"
              data-th-id="${r"${tableId}"} + DeleteButton"
              data-dismiss="modal" aria-label="Confirm"
              data-th-text="${r"#{"}label_submit}">Accept</button>
        </div>
    </div>

    <div data-th-replace="~{fragments/modal :: modal(id = ${r"${tableId}"} + 'DeleteSuccess', title = ${r"${title}"}, body = ~{::modalSuccessBody}, footer = ~{})}">
       <p data-th-text="${r"|#{info_deleted_items_number(1)}|"}" data-th-fragment="modalSuccessBody">1 Removed item</p>
    </div>

    <div data-th-replace="~{fragments/modal :: modal(id = ${r"${tableId}"} + 'DeleteError', title = ${r"${title}"}, body = ~{::modalErrorBody}, footer = ~{})}">
       <p data-th-text="${r"|#{error_deleting_item}|"}" data-th-fragment="modalErrorBody">Error deleting selected item.</p>
    </div>

  </div>
  </body>
  </#if>
</html>