<#import "listDetails.ftl" as details>
<!DOCTYPE html>
<html lang="en" data-layout-decorate="~{layouts/default-list-layout}">

  <#if userManagedComponents?has_content && userManagedComponents["head"]??>
  ${userManagedComponents["head"]}
  <#else>
  <head id="head">
    <title data-th-text="${r"#{"}label_list_entity(${r"#{"}${entityLabelPlural}})}">
    List ${entityName} - ${projectName} - SpringRoo Application</title>

  </head>
  </#if>

  <#if userManagedComponents?has_content && userManagedComponents["body"]??>
    ${userManagedComponents["body"]}
  <#else>
  <body id="body">

    <header role="banner">
      <!-- Content replaced by layout of the page displayed -->
    </header>

    <!--CONTAINER-->
    <div class="container bg-container">
      <!-- CONTENT -->
      <!--
        Only the inner content of the following tag "section" is included
        within the template, in the section "content"
      -->
      <section data-layout-fragment="content">
        <div class="container-fluid content">

          <h1 data-th-text="${r"#{"}${entityLabelPlural}}">${entityName}s</h1>

          <!--TABLE-->
          <div class="table-responsive" id="containerFields" data-th-with="collectionLink=${r"${@"}linkBuilder.of('${mvcCollectionControllerName}')},itemLink=${r"${@"}linkBuilder.of('${mvcItemControllerName}')}">
            <#if entity.userManaged>
               ${entity.codeManaged}
            <#else>
              <table id="${entity.entityItemId}-table" style="width: 99.7%"
                   class="table table-striped table-hover table-bordered"
                   data-datatables="true"
                   data-row-id="${entity.configuration.identifierField}"
                   data-select="single"
                   data-z="${entity.z}"
                   <#if entity.readOnly == false>
                   data-order="[[ 1, &quot;asc&quot; ]]"
                   <#else>
                   data-order="[[ 0, &quot;asc&quot; ]]"
                   </#if>
                   data-data-load-url="${r"${"}collectionLink.to('datatables')}"
                   data-data-show-url="${r"${"}itemLink.to('show').with('${modelAttributeName}', '_ID_')}"
                   data-data-export-csv-url="${r"${"}collectionLink.to('exportCsv')}" 
                   data-data-export-xls-url="${r"${"}collectionLink.to('exportXls')}" 
                   data-data-export-pdf-url="${r"${"}collectionLink.to('exportPdf')}"
                   <#if entity.readOnly == false>
                   data-data-create-url="${r"${"}collectionLink.to('createForm')}"
                   data-data-edit-url="${r"${"}itemLink.to('editForm').with('${modelAttributeName}', '_ID_')}"
                   data-data-delete-url="${r"${"}itemLink.to('delete').with('${modelAttributeName}', '_ID_')}"
                   data-data-delete-batch-url="${r"${"}collectionLink.to('deleteBatch').with('ids', '_ID_')}"
                   </#if>
                   >
                <caption class="sr-only" data-th-text="${r"#{"}label_list_entity(${r"#{"}${entityLabelPlural}})}">${entityName} List</caption>
                <thead>
                  <tr>
                    <th data-data="id" data-checkboxes=true></th>
                    <#list fields as field>
                    <#if field.type != "LIST">
                    <th data-data="${field.fieldName}" data-th-text="${r"#{"}${field.label}}">${field.fieldName}</th>
                    </#if>
                    </#list>
                    <th data-data="${entity.configuration.identifierField}" data-orderable="false" data-searchable="false"
                         class="dttools" data-th-text="${r"#{"}label_tools}">Tools</th>
                  </tr>
                </thead>
                <tbody data-th-remove="all">
                  <tr>
                    <td></td>
                    <#list fields as field>
                    <#if field.type != "LIST">
                    <td>${field.fieldName}</td>
                    </#if>
                    </#list>
                    <td data-th-text="${r"#{"}label_tools}">Tools</td>
                  </tr>
                </tbody>
              </table>
              <#if entity.readOnly == false>
                <!-- content replaced by modal-confirm fragment of modal-confirm.html -->
                <div data-th-replace="~{fragments/modal-confirm-delete :: modalConfirmDelete(tableId='${entity.entityItemId}-table',
                    title=${r"#{"}label_delete_entity(${r"#{"}${entityLabelPlural}})}, message=${r"#{"}info_delete_item_confirm})}">
                </div>
                <div data-th-replace="~{fragments/modal-confirm-delete-batch :: modalConfirmDeleteBatch(tableId='${entity.entityItemId}-table',
                    title=${r"#{"}label_delete_entity(${r"#{"}${entityLabelPlural}})}, message=${r"#{"}info_delete_batch_confirm})}">
                </div>
              </#if>
              <div data-th-replace="~{fragments/modal-export-empty-error :: modalExportEmptyError(tableId='${entity.entityItemId}-table',
                  title=${r"#{"}label_export_empty_error(${r"#{"}${entityLabelPlural}})}, message=${r"#{"}info_export_empty_error})}">
              </div>
            </#if>
          </div>
          <!-- /TABLE -->

          <#if detailsLevels?size != 0>
          <!-- details -->
            <#list detailsLevels as detailsLevel>
            <@details.section detailsLevel=detailsLevel/>
            </#list>
          </#if>

          <div class="clearfix">
            <div class="pull-left">
              <a href="../index.html" class="btn btn-default" data-th-href="@{/}">
                 <span class="glyphicon glyphicon-chevron-left" aria-hidden="true"></span>
                 <span data-th-text="${r"#{"}label_back}">Back</span>
              </a>
            </div>
          </div>

        </div>

      </section>
      <!-- /CONTENT -->
    </div>
    <!-- /CONTAINER -->

    <footer class="container">
      <!-- Content replaced by layout of the page displayed -->
    </footer>

    <!-- JavaScript
    ================================================== -->
    <!-- Placed at the end of the document so that the pages load faster -->
    <!-- JavaScript loaded by layout of the page displayed -->
    <!--
         Only the inner content of the following tag "javascript" is included
         within the template, in the div "javascript"
        -->
    <div data-layout-fragment="javascript">
    </div>

  </body>
  </#if>

</html>
