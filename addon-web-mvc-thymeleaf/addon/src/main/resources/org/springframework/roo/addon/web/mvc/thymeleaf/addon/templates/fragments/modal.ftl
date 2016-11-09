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
    <div data-th-fragment="modal(tableId, id, title, body)"
    class="modal fade" data-th-id="${r"${id}"}" tabindex="-1" role="dialog"
    aria-labelledby="modalLabel"
    data-th-attr="aria-labelledby=${r"${id} + 'Label'"}">
      <div class="modal-dialog" role="document">
        <div class="modal-content">
          <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal"
            aria-label="Close">
            <span aria-hidden="true">&times;</span></button>
            <h2 class="modal-title" data-th-id="${r"${id}"} + 'ModalLabel'" data-th-text="${r"${title}"}" >Title</h2>
          </div>
          <div class="modal-body" data-th-id="${r"${id}"} + 'Body'">
            <!-- Content added -->
             <p data-th-replace="${r"${body}"}">Message</p>
          </div>
        </div>
      </div>
    </div>
  </body>
</#if>
</html>