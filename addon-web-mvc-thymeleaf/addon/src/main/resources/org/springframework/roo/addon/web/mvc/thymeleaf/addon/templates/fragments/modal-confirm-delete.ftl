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
  <div data-th-fragment="modalConfirmDelete(tableId, title, message, baseUrl)">
    <div
      data-th-replace="fragments/modal :: modal(tableId = ${r"${tableId}"}, id = ${r"${tableId}"} + 'DeleteConfirm', title = ${r"${title}"}, body = ~{::modalConfirmBody})">

          <div class="modal-body" data-th-fragment="modalConfirmBody" data-row-id="" data-th-id="${r"${tableId}"} + DeleteRowId">

            <p data-th-text="${r"${message}"}">Going to remove the selected
              element</p>

            <form class="form-horizontal">
              <div class="form-group">
                <div class="col-md-12">
                  <div class="pull-left">
                    <button type="reset" class="btn btn-default"
                      data-dismiss="modal" aria-label="Close"
                      data-th-text="${r"#{label_reset}"}">Cancel</button>
                  </div>
                  <div class="pull-right">
                    <button type="button" class="btn btn-primary" data-th-id="${r"${tableId}"} + DeleteButton"
                      data-dismiss="modal" aria-label="Confirm"
                      data-th-text="${r"#{label_submit}"}">Accept</button>
                  </div>
                </div>
              </div>
            </form>
          </div>
        </div>

        <div
          data-th-replace="fragments/modal :: modal(tableId = ${r"${tableId}"}, id = ${r"${tableId}"} + 'DeleteSuccess', title = ${r"${title}"}, body = ~{::modalSuccessBody})">
          <p data-th-text="${r"|#{info_deleted_items_number(1)}|"}" data-th-fragment="modalSuccessBody">1 Removed item</p>
        </div>

        <div
          data-th-replace="fragments/modal :: modal(tableId = ${r"${tableId}"}, id = ${r"${tableId}"} + 'DeleteError', title = ${r"${title}"}, body = ~{::modalErrorBody})">
          <p data-th-text="${r"|#{error_deleting_item}|"}" data-th-fragment="modalErrorBody">Error deleting selected item.</p>
        </div>

    </div>
  </body>
  </#if>
</html>