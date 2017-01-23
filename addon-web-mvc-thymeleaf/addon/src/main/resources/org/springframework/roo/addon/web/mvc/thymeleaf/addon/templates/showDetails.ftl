<#macro section detailsLevel>
        <hr>
        <div id="details-level-${detailsLevel[0].level}">
            <ul class="nav nav-tabs" id="nav-tabs">
            <#assign firstDetail=true>
            <#list detailsLevel as detail>
              <#if firstDetail == true>
                <li class="active">
              <#else>
                <li>
              </#if>
              <#if detail.tabLinkCode??>
                  ${detail.tabLinkCode}
              <#else>
                  <a id="${detail.entityItemId}-table-tab" data-toggle="tab" href="#detail-${detail.entityItemId}" data-z="${detail.z}">${detail.fieldNameCapitalized}</a>
              </#if>
                </li>
            </#list>
            </ul>

            <div class="tab-content" id="tab-content">
              <#assign firstDetail=true>
              <#list detailsLevel as detail>
                <#if firstDetail == true>
              <div id="detail-${detail.entityItemId}" class="tab-pane active">
                <#assign firstDetail=false>
                <#else>
              <div id="detail-${detail.entityItemId}" class="tab-pane">
                </#if>
                <!--START DETAIL TABLE-->
                <#if detail.parentEntity??>
                  <#assign parentEntity=detail.parentEntity>
                <#else>
                  <#assign parentEntity=detail.rootEntity>
                </#if>
                <#assign dconfig=detail.configuration>
                <div class="table-responsive" data-th-with="detailCollectionLink=${r"${@"}linkBuilder.of('${dconfig.mvcDetailCollectionControllerName}')},detailItemLink=${r"${@"}linkBuilder.of('${dconfig.mvcDetailItemControllerName}')},id=${r"*{{"}${identifierField}}}">
                  <#if detail.userManaged>
                    ${detail.codeManaged}
                  <#else>
                    <table id="${detail.entityItemId}-table" style="width: 99.7%"
                      class="table table-striped table-hover table-bordered"
                      data-datatables="true"
                      data-row-id="${dconfig.identifierField}"
                      data-z="${detail.z}"
                      <#if entity.readOnly == false>
                      data-order="[[ 1, &quot;asc&quot; ]]"
                      <#else>
                      data-order="[[ 0, &quot;asc&quot; ]]"
                      </#if>
                      data-data-load-url="${r"${"}detailCollectionLink.to('datatables').with('${modelAttributeName}', id)}"
                      data-data-show-url="${r"${"}detailItemLink.to('show').with('${detail.modelAttribute}', '_ID_')}"
                      <#if entity.readOnly == false>
                       data-data-edit-url="${r"${"}detailItemLink.to('editForm').with('${detail.modelAttribute}', '_ID_')}"
                       data-data-create-url="${r"${"}detailCollectionLink.to('createForm').with('${modelAttributeName}', id)}"
                       data-data-delete-url="${r"${"}detailCollectionLink.to('removeFrom${detail.fieldNameCapitalized}').with('${modelAttributeName}', id).with('${detail.fieldName}ToRemove', '_ID_')}"
                       data-data-delete-batch-url="${r"${"}detailCollectionLink.to('removeFrom${detail.fieldNameCapitalized}Batch').with('${modelAttributeName}', id).with('${detail.fieldName}ToRemove', '_ID_')}"
                      </#if>
                      >
                      <caption class="sr-only" data-th-text="${r"#{"}label_list_of_entity(${r"#"}{${dconfig.referencedFieldLabel}})}">${detail.fieldNameCapitalized} List</caption>
                      <thead>
                        <tr>
                          <th data-data="id" data-checkboxes=true></th>
                          <#list detail.configuration.referenceFieldFields as referencedFieldField>
                          <#if referencedFieldField != entityName>
                              <th data-data="${referencedFieldField.fieldName}" data-th-text="${r"#{"}${referencedFieldField.label}}">${referencedFieldField.fieldName}</th>
                          </#if>
                          </#list>
                              <th data-data="${dconfig.identifierField}" data-orderable="false" data-searchable="false"
                                 class="dttools" data-th-text="${r"#{"}label_tools}">Tools</th>
                        </tr>
                      </thead>
                      <tbody data-th-remove="all">
                        <tr>
                          <#list detail.configuration.referenceFieldFields as referencedFieldField>
                          <#if referencedFieldField != entityName>
                              <td>${referencedFieldField.fieldName}</td>
                          </#if>
                          </#list>
                          <td data-th-text="${r"#{"}label_tools}">Tools</td>
                        </tr>
                      </tbody>
                    </table>
                  </#if>
                    <!-- content replaced by modal-confirm fragment of modal-confirm.html -->
                    <div data-th-replace="~{fragments/modal-confirm-delete :: modalConfirmDelete(tableId='${detail.entityItemId}-table',
                        title=${r"#{"}label_delete_entity(${r"#"}{${dconfig.referencedFieldLabel}})}, message=${r"#{"}info_delete_item_confirm})}">
                    </div>
                   <div data-th-replace="~{fragments/modal-confirm-delete-batch :: modalConfirmDeleteBatch(tableId='${detail.entityItemId}-table',
                        title=${r"#{"}label_delete_entity(${r"#{"}${dconfig.referencedFieldLabel}})}, message=${r"#{"}info_delete_batch_confirm})}">
                    </div>
                    <div data-th-replace="~{fragments/modal-export-empty-error :: modalExportEmptyError(tableId='${detail.entityItemId}-table',
                        title=${r"#{"}label_export_empty_error(${r"#{"}${dconfig.referencedFieldLabel}})}, message=${r"#{"}info_export_empty_error})}">
                    </div>
                </div> <!--/table-responsive">
                <!--END TABLE-->
              </div> <!--/tab-pane -->
              </#list>
            </div> <!--/tab-content-->
        </div>
</#macro>
