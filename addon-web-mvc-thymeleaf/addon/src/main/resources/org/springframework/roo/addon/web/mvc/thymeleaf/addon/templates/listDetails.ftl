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
                <div class="table-responsive">
                  <#assign dconfig=detail.configuration>
                  <#if detail.userManaged>
                    ${detail.codeManaged}
                  <#else>
                    <table id="${detail.entityItemId}-table"
                      class="table table-striped table-hover table-bordered"
                      data-z="${detail.z}"
                      data-row-id="${dconfig.identifierField}"
                      data-datatables="true"
                      data-parent-table="${parentEntity.entityItemId}-table"
                      data-order="[[ 0, &quot;asc&quot; ]]"
                      data-data-load-url="${r"${"}(#mvc.url('${dconfig.mvcUrl_datatablesDetails}')).buildAndExpand('_PARENTID_')}"
                      data-data-show-url="${r"${"}(#mvc.url('${dconfig.mvcUrl_show}')).buildAndExpand(${dconfig.mvcUrl_itemExpandBuilderExp})}"
                      <#if entity.readOnly == false>
                      data-data-edit-url="${r"${"}(#mvc.url('${dconfig.mvcUrl_editForm}')).buildAndExpand(${dconfig.mvcUrl_itemExpandBuilderExp})}"
                      data-data-delete-url="${r"${"}(#mvc.url('${dconfig.mvcUrl_delete}')).${dconfig.mvcUrl_delete_dt_ext}}"
                      data-data-create-url="${r"${"}(#mvc.url('${dconfig.mvcUrl_createForm}')).buildAndExpand('_PARENTID_')}"
                      </#if>
                      >
                      <caption data-th-text="${r"#{"}label_list_of_entity(${r"#"}{${dconfig.referencedFieldLabel}})}">${detail.fieldNameCapitalized} List</caption>
                      <thead>
                        <tr>
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
                    <div data-th-replace="fragments/modal-confirm-delete :: modalConfirmDelete(tableId='${detail.entityItemId}-table',
                        title=${r"#{"}label_delete_entity(${r"#"}{${dconfig.referencedFieldLabel}})}, message=${r"#{"}info_delete_item_confirm})">
                    </div>
                </div> <!--/table-responsive">
                <!--END TABLE-->
              </div> <!--/tab-pane -->
              </#list>
            </div> <!--/tab-content-->
        </div>
</#macro>
