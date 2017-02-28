<!DOCTYPE html>
<html lang="en" data-layout-decorate="~{layouts/default-list-layout}">
  <#if userManagedComponents?has_content && userManagedComponents["head"]??>
  ${userManagedComponents["head"]}
  <#else>
  <head id="head">

    <title data-th-text="|${r"#{"}label_show_entity(${r"#{"}${entityLabel}${r"}"})${r"}"}|">Show ${entityName} - Spring Roo application</title>

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
      <section data-th-fragment="inline-content" data-layout-fragment="content" data-th-object="${modelAttribute}">
        <div>

          <!-- CONTENT -->
            <div id="mainEntityFieldSet">
              <ul class="list-unstyled" id="containerFields">
              <#list fields as field>
                <#if field.userManaged>
                  ${field.codeManaged}
                <#else>
                  <li id="${field.fieldId}" data-z="${field.z}">
            	      <strong data-th-text="${r"#{"}${field.label}}">${field.fieldName}</strong>
                    <span data-th-text="*{{${field.fieldName}}}">${field.fieldName}Value</span>
         	        </li>
                </#if>
              </#list>
              </ul>
            </div>

            <#if compositeRelationFields?has_content>
              <#list compositeRelationFields?keys as referencedField>
                <div id="${referencedField}FieldSet">
                  <#list compositeRelationFields[referencedField] as field>
                  <#if field?index == 0>
                  <h2 data-th-text="${r"#{"}label_data_entity(${r"#{"}${field.legendLabel}})}">${field.entityName} data</h2>
                  <ul class="list-unstyled" id="containerFields">
                  </#if>
                    <#if field.userManaged>
                      ${field.codeManaged}
                    <#else>
                      <li id="${field.fieldId}" data-z="${field.z}">
                        <strong data-th-text="${r"#{"}${field.label}}">${field.fieldName}</strong>
                        <span data-th-text="*{{${field.fieldName}}}">${field.fieldName}Value</span>
                      </li>
                    </#if>
                 </#list>
                 </ul>
                </div>
              </#list>
            </#if>
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
