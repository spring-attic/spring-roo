<!--TABLE-->
<div class="table-responsive" id="containerFields" 
data-th-fragment="${detail.entityItemId}ModalConfirmBody"
data-th-with="collectionLink=${r"${@"}linkBuilder.of('${detail.configuration.mvcDetailCollectionControllerName}')}">
  <#if entity.userManaged>
     ${entity.codeManaged}
  <#else>
    <table id="${detail.entityItemId}-table-item-to-remove"
         data-parent-table="${detail.rootEntity.entityItemId}-table"
         data-data-parent-id="${r"${"}${modelAttributeName} != null ? ${modelAttributeName}.${entity.configuration.identifierField} : ''}"
         data-data-load-url="${r"${"}collectionLink.to('datatablesByIdsIn').with('${modelAttributeName}', '_PARENTID_')}">
      <thead>
        <tr>
          <#list detail.configuration.referenceFieldFields as referencedFieldField>
          <#if referencedFieldField != detail.rootEntity.entityName>
              <th data-data="${referencedFieldField.fieldName}" data-th-text="${r"#{"}${referencedFieldField.label}}">${referencedFieldField.fieldName}</th>
          </#if>
          </#list>
        </tr>
      </thead>
      <tbody data-th-remove="all">
        <tr>
          <#list detail.configuration.referenceFieldFields as referencedFieldField>
          <#if referencedFieldField != detail.rootEntity.entityName>
              <td>${referencedFieldField.fieldName}</td>
          </#if>
          </#list>
        </tr>
      </tbody>
    </table>
  </#if>
</div>
<!-- /TABLE -->