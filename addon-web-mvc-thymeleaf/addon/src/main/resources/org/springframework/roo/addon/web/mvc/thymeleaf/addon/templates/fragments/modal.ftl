<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8" />
    <title>Modal</title>
  </head>
<#if userManagedComponents?has_content && userManagedComponents["body"]??>
  ${userManagedComponents["body"]}
<#else>
  <body>
    <!--
    Only the inner content of the following div is included within the
    template, in the section modal
    -->
    <div data-th-fragment="modal(id, title, body, footer)"
         class="modal fade" role="dialog"
         id="Modal" data-th-id="${r"$"}{id}"
         tabindex="-1"
         aria-labelledby="ModalLabel" data-th-attr="aria-labelledby=${r"$"}{id} + 'Label'">
      <div class="modal-dialog" role="document">
        <div class="modal-content">
          <div class="modal-header" id="ModalHeader" data-th-id="${r"$"}{id} + 'Header'">
            <button type="button" class="close" data-dismiss="modal" aria-label="Close">
              <span aria-hidden="true">&times;</span>
            </button>
            <h2 class="modal-title"
                id="ModalLabel" data-th-id="${r"$"}{id} + 'Label'"
                data-th-text="${r"$"}{title}">Title</h2>
          </div>
          <div class="modal-body" data-th-insert="${r"$"}{body}"
               id="ModalBody" data-th-id="${r"$"}{id} + 'Body'"></div>
          <div class="modal-footer" data-th-insert="${r"$"}{footer}"
               id="ModalFooter" data-th-id="${r"$"}{id} + 'Footer'"></div>
        </div>
      </div>
    </div>
  </body>
</#if>
</html>