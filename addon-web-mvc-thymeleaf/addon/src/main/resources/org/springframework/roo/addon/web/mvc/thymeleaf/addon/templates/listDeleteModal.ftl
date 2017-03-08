<!--TABLE-->
<div class="table-responsive" id="containerFields" 
data-th-fragment="${entity.entityItemId}ModalConfirmBody"
data-th-with="collectionLink=${r"${@"}linkBuilder.of('${mvcCollectionControllerName}')},itemLink=${r"${@"}linkBuilder.of('${mvcItemControllerName}')}">
  <#if entity.userManaged>
     ${entity.codeManaged}
  <#else>
    <table id="${entity.entityItemId}-table-item-to-remove"
         data-data-load-url="${r"${"}collectionLink.to('datatablesByIdsIn')}">
      <thead>
        <tr>
          <#list fields as field>
          <#if field.type != "LIST">
          <th data-data="${field.fieldName}" data-th-text="${r"#{"}${field.label}}">${field.fieldName}</th>
          </#if>
          </#list>
        </tr>
      </thead>
      <tbody data-th-remove="all">
        <tr>
          <#list fields as field>
          <#if field.type != "LIST">
          <td>${field.fieldName}</td>
          </#if>
          </#list>
        </tr>
      </tbody>
    </table>
  </#if>
</div>
<!-- /TABLE -->