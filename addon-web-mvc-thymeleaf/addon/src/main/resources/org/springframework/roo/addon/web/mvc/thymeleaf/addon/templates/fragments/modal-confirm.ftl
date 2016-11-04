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
  <div data-layout-fragment="modalConfirm(id, title, message, onclickCallback)">
    <div class="modal fade" data-th-id="${r"${id}"} +'Modal'" tabindex="-1"
      role="dialog"
      aria-labelledby="modalLabel" data-th-attr="aria-labelledby=${r"${id}"} + 'ModalLabel'">
      <div class="modal-dialog" role="document">
        <div class="modal-content">
          <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal"
              aria-label="Close">
              <span aria-hidden="true">&times;</span>
            </button>
            <h2 class="modal-title" data-th-id="${r"${id}"} + 'ModalBody'"
              data-th-text="${r"${title}"}">Delete Entity</h2>
          </div>
          <div class="modal-body" data-th-id="${r"${id}"} + 'ModalBody'">

            <p data-th-text="${r"${message}"}">The element is going to be deleted</p>

            <form class="form-horizontal">
              <div class="form-group">
                <div class="col-md-12">
                  <div class="pull-left">
                    <button type="reset" class="btn btn-default"
                      data-th-onclick="'jQuery(\'#' + ${r"${id}"} + 'Modal\').modal(\'hide\');'"
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
      </div>
    </div>
  </div>
  </body>
  </#if>
</html>