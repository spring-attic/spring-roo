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

    <!-- CONTAINER -->
    <div class="container bg-container">
      <!-- CONTENT -->
      <!--
        Only the inner content of the following tag "section" is included
        within the template, in the section "content"
      -->
      <section data-layout-fragment="content" data-th-with="searchLink=${r"${@"}linkBuilder.of('${mvcSearchControllerName}')},collectionLink=${r"${@"}linkBuilder.of('${mvcCollectionControllerName}')},itemLink=${r"${@"}linkBuilder.of('${mvcItemControllerName}')}">
        <div class="container-fluid content">

          <h1 data-th-text="${r"#{"}${entityLabelPlural}}">${entityName}s</h1>

          <!-- FILTER -->
          <#assign conditionalEmpty="">
          <#list formbeanfields as field>
            <#assign conditionalEmpty="${conditionalEmpty} formBean.${field.fieldName} == null &&">
          </#list>
          <#if conditionalEmpty != "">
            <#assign conditionalEmpty="${conditionalEmpty?substring(0, conditionalEmpty?length - 2)}">
          </#if>

          <#assign allFieldsList="">
          <#list formbeanfields as field>
            <#assign allFieldsList="${allFieldsList}${field.fieldName}=*{${field.fieldName}},">
          </#list>
          <#if allFieldsList != "">
            <#assign allFieldsList="${allFieldsList?substring(0, allFieldsList?length - 1)}">
          </#if>

          <div class="panel panel-default" data-th-object="${r"${"}formBean}">

            <div class="panel-heading"  data-th-if="${r"${"}${conditionalEmpty}}">
              <a class="btn btn-default btn-xs"
                 data-th-with="url=${r"${"}searchLink.to('${finderName}Form')}"
                 data-th-href="@{${r"${url}("}${allFieldsList})}">
                <span class="glyphicon glyphicon-pencil" aria-hidden="true"></span>
              </a>
              <span class="panel-title" data-th-text="${r"#{"}label_not_filtered}">Not filtered</span>
            </div>

            <div class="panel-heading" data-th-unless="${r"${"}${conditionalEmpty}}">
              <span class="btn-group" role="group">
                <a class="btn btn-default btn-xs"
                   data-th-with="url=${r"${"}searchLink.to('${finderName}Form')}"
                   data-th-href="@{${r"${url}("}${allFieldsList})}">
                  <span class="glyphicon glyphicon-pencil" aria-hidden="true"></span>
                </a>
                <a class="btn btn-default btn-xs" role="button" href="#filterBody" data-toggle="collapse">
                  <span class="glyphicon glyphicon-sort" aria-hidden="true"></span>
                </a>
              </span>
              <span class="panel-title">
                <span data-th-text="${r"#{"}label_filtered_by}">Filtered by</span>:
              </span>
              <#list formbeanfields as field>
              <span class="label label-default" data-th-unless="*{${field.fieldName} == null}">${field.fieldName}</span>
              </#list>
            </div>

            <div class="panel-body collapse in" id="filterBody" data-th-unless="${r"${"}${conditionalEmpty}}">
              <#list formbeanfields as field>

                <#assign allFieldsListWithoutCurrent="">
                <#list formbeanfields as formbeanfield>
                  <#if formbeanfield.fieldName != field.fieldName>
                  <#assign allFieldsListWithoutCurrent="${allFieldsListWithoutCurrent}${formbeanfield.fieldName}=*{${formbeanfield.fieldName}},">
                  </#if>
                </#list>
                <#if allFieldsListWithoutCurrent != "">
                  <#assign allFieldsListWithoutCurrent="(${allFieldsListWithoutCurrent?substring(0, allFieldsListWithoutCurrent?length - 1)})">
                </#if>

                 <a class="btn btn-primary btn-xs" data-th-unless="*{${field.fieldName} == null}"
                             data-th-with="url=${r"${"}searchLink.to('${finderName}')}"
                             data-th-href="@{${r"${url}"}${allFieldsListWithoutCurrent}}">
                            <span class="glyphicon glyphicon-remove-circle"></span>
                            <span data-th-text="|${field.fieldName}: *{${field.fieldName}}|">${field.fieldName}: a ${field.fieldName}</span>
                </a>
              </#list>
            </div>

          </div>
          <!-- /FILTER -->

          <!-- TABLE -->
          <div class="table-responsive" id="containerFields">
            <#if entity.userManaged>
               ${entity.codeManaged}
            <#else>
              <table id="${entity.entityItemId}-table" style="width: 99.7%"
                   class="table table-striped table-hover table-bordered"
                   data-datatables="true"
                   data-row-id="${entity.configuration.identifierField}"
                   data-select="single"
                   data-z="${entity.z}"
                   data-order="[[ 0, &quot;asc&quot; ]]"
                   <#list formbeanfields as field>
                   data-data-load-url-param-${field.fieldWithoutCamelCase}="${r"${"}formBean.${field.fieldName}}"
                   </#list>
                   data-data-load-url="${r"${"}searchLink.to('${finderName}Dt')}"
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
                <caption class="sr-only"
                  data-th-text="${r"#{"}label_list_entity(${r"#{"}${entityLabelPlural}})}">${entityName} List</caption>
                <thead>
                  <tr>
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
                    <#list fields as field>
                    <#if field.type != "LIST">
                    <td>${field.fieldName}</td>
                    </#if>
                    </#list>
                    <td data-th-text="${r"#{"}label_tools}">Tools</td>
                  </tr>
                </tbody>
              </table>
              <!-- content replaced by modal-confirm fragment of modal-confirm.html -->
              <div data-th-replace="~{fragments/modal-confirm-delete :: modalConfirmDelete(tableId='${entity.entityItemId}-table',
                  title=${r"#{"}label_delete_entity(${r"#{"}${entityLabelPlural}})}, message=${r"#{"}info_delete_item_confirm}, baseUrl = @{${controllerPath}/})}">
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
      <!-- /CONTENT-->
    </div>
    <!-- /CONTAINER-->

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
