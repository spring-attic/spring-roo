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
  <div data-th-fragment="modalConfirmDeleteBatch(tableId, title, message)">
      <div data-th-replace="~{fragments/modal :: modal(id = ${r"$"}{tableId} + 'DeleteBatchConfirm',
           title = ${r"$"}{title}, body = ~{::modalConfirmBody}, footer = ~{::modalConfirmFooter})}">
        <div data-row-id="" data-th-id="${r"$"}{tableId} + DeleteBatchRowId" data-th-fragment="modalConfirmBody">
          <p data-th-text="${r"$"}{message}">Going to remove the selected elements</p>
        </div>
        <div data-th-fragment="modalConfirmFooter">
            <button type="reset" class="btn btn-default pull-left" data-dismiss="modal" aria-label="Close" data-th-text="${r"#"}{label_reset}">Cancel</button>
            <button type="button" class="btn btn-primary pull-right" data-th-id="${r"$"}{tableId} + DeleteBatchButton" data-dismiss="modal" aria-label="Confirm" data-th-text="${r"#"}{label_submit}">Accept</button>
        </div>
    </div>

    <div data-th-replace="~{fragments/modal :: modal(id = ${r"$"}{tableId} + 'DeleteBatchSuccess', title = ${r"$"}{title}, body = ~{::modalSuccessBody}, footer = ~{})}">
       <p data-th-text="|${r"#"}{info_deleted_items_batch}|" data-th-fragment="modalSuccessBody">Removed selected items</p>
    </div>

    <div data-th-replace="~{fragments/modal :: modal(id = ${r"$"}{tableId} + 'DeleteBatchError', title = ${r"$"}{title}, body = ~{::modalErrorBody}, footer = ~{})}">
       <p data-th-text="|${r"#"}{error_deleting_item}|" data-th-fragment="modalErrorBody">Error deleting selected items.</p>
    </div>

  </div>
  </body>
</#if>
</html>