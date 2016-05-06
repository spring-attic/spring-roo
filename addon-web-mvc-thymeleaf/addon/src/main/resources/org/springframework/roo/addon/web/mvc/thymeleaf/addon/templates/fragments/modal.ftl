<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8" />
    <title>Modal</title>
  </head>
  <body>
    <div data-layout-fragment="modal(id, title)">
          <div class="modal fade" data-th-id="${r"${id}"} + 'Modal'" tabindex="-1" role="dialog" aria-labelledby="modalLabel">
            <div class="modal-dialog" role="document">
              <div class="modal-content">
                <div class="modal-header">
                  <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                  <h2 class="modal-title" data-th-id="${r"${id}"} + 'ModalLabel'" data-th-text="${r"${title}"}" >Delete Client</h2>
                </div>
                <div class="modal-body" data-th-id="${r"${id}"} + 'ModalBody'">
                  <!-- Content -->
                </div>
              </div>
            </div>
          </div>
    </div>
  </body>
</html>