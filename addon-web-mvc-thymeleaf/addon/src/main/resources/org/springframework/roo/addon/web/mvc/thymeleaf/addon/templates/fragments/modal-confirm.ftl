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
    data-layout-fragment="modalConfirm(id, title, message, onclickCallback)"
    data-th-replace="${r"fragments/modal :: modal(id = ${id}, title = ${title}, message = ${message}, body = ~{::modalBody})"}">

    <div class="modal-body" data-th-id="${r"${id}"} + 'ModalBody'">

      <p data-th-text="${r"${message}"}">Going to remove the selected
        element</p>

      <div class="progress hide">
        <div class="progress-bar progress-bar-striped active"
          role="progressbar" aria-valuenow="45" aria-valuemin="0"
          aria-valuemax="100" style="width: 100%">
          <span class="sr-only">100% Complete</span>
        </div>
      </div>

      <form class="form-horizontal">
        <div class="form-group">
          <div class="col-md-12">
            <div class="pull-left">
              <button type="button" class="btn btn-default"
                data-dismiss="modal" aria-label="Close"
                data-th-text="${r"#{label_reset}"}">Cancel</button>
            </div>
            <div class="pull-right">
              <button type="button" class="btn btn-primary"
                data-th-text="${r"#{label_submit}"}" data-th-onclick="${r"${onclickCallback}"}"
                >Submit</button>
            </div>
          </div>
        </div>
      </form>
    </div>
  </div>
</body>
</#if>
</html>