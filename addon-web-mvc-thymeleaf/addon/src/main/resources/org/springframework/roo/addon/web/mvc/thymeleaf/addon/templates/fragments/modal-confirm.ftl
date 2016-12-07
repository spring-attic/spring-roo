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
  <div
    data-th-fragment="modalConfirm(id, title, message, onclickCallback)"
    data-th-replace="~{${r"fragments/modal :: modal(id = ${id}, title = ${title}, message = ${message}, body = ~{::modalBody})"}}">
    <div data-th-fragment="modalBody">
        <div data-th-id="${r"${id}"} + 'ModalBody'">

          <p data-th-text="${r"${"}message}">Going to remove the selected
            element</p>

          <div class="progress hide">
            <div class="progress-bar progress-bar-striped active"
              role="progressbar" aria-valuenow="45" aria-valuemin="0"
              aria-valuemax="100" style="width: 100%">
              <span class="sr-only">100% Complete</span>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button type="reset" class="btn btn-default pull-left"
              data-dismiss="modal" aria-label="Close"
              data-th-text="${r"#{"}label_reset}">Cancel</button>
          <button type="button" class="btn btn-primary pull-right"
              data-th-text="${r"#{"}label_submit}" data-th-onclick="${r"${"}onclickCallback}"
              >Accept</button>
        </div>
    </div>
  </div>
</body>
</#if>
</html>