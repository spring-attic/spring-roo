<!DOCTYPE html>
<html lang="en" data-layout-decorate="~{layouts/default-layout}">
  <#if userManagedComponents?has_content && userManagedComponents["head"]??>
  ${userManagedComponents["head"]}
  <#else>
  <head id="head">
    <title data-th-text="${r"#{"}label_create_entity(${r"#{"}${entityLabel}})}">
    Create ${entityName} - ${projectName} - SpringRoo Application</title>
    <!-- DateTimePicker -->
    <link rel="stylesheet" type="text/css"
      href="https://cdnjs.cloudflare.com/ajax/libs/jquery-datetimepicker/2.5.4/build/jquery.datetimepicker.min.css"
      data-th-href="@{/webjars/datetimepicker/build/jquery.datetimepicker.min.css}"/>
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
      <section data-layout-fragment="content">
        <div class="container-fluid content" data-th-with="collectionLink=${r"${@"}linkBuilder.of('${mvcCollectionControllerName}')},detailCollectionLink=${r"${@"}linkBuilder.of('${detail.configuration.mvcDetailCollectionControllerName}')}">
  	<!--
  	  Only the inner content of the following tag "section" is included
  	  within the template, in the section "content"
  	-->

          <h1 data-th-text="${r"#{"}label_edit_entity(${r"#{"}${entityLabel}})}">Edit ${entityName}</h1>

          <#assign dconfig=detail.configuration>
          <!-- FORM -->
          <form class="form-horizontal validate" method="POST" data-th-object="${modelAttribute}"
            data-th-action="${r"${"}detailCollectionLink.to('create').with('${modelAttributeName}', ${modelAttributeName}.${identifierField})}">
            <input type="hidden" name="parentVersion" data-th-value="*{version}" />
            
            <!-- CONCURRENCY CONTROL -->
            <div class="alert alert-warning" data-th-if="${r"${"}concurrency}">
              <h2 data-th-text="${r"#{"}label_concurrency_title}">Warning! This record has been updated by an other user.</h2>
              <div class="radio">
                <label>
                  <input type="radio" name="concurrency" value="apply"> <span data-th-text="${r"#{"}label_concurrency_apply}">Apply my changes anyway</span> <i><span data-th-text="${r"#{"}label_concurrency_apply_info}">(discard all the changes applied by the other users).</span></i>
                </input></label>
              </div>
              <div class="radio">
                <label>
                  <input type="radio" name="concurrency" value="discard" checked=""> <span data-th-text="${r"#{"}label_concurrency_discard}">Discard all my changes and reload this record.</span>
                </input></label>
              </div>
              <br>
              <button type="submit" class="btn btn-primary">Accept</button>
            </br></div>
            <!-- /CONCURRENCY CONTROL -->
            
            <fieldset id="containerFields">
              <legend data-th-text="${r"#{"}label_data_entity(${r"#{"}${detail.configuration.entityLabel}})}">${detail.entityName} data </legend>

              <#if detail.userManaged>
                ${detail.codeManaged}
              <#else>
                <div class="form-group has-error has-feedback" data-z="${detail.z}" id="${detail.entityItemId}"
                    data-th-class="form-group" data-th-with="select2ControllerLink=${r"${@"}linkBuilder.of('${dconfig.select2ControllerName}')}">
                    <div class="col-md-6 col-md-offset-3">
                      <select id="${detail.fieldName}" name="${detail.fieldName}Ids"
                        class="form-control dropdown-select-ajax"
                        data-allow-clear="true"
                        data-ajax--cache="true"
                        data-ajax--data-type="json"
                        data-ajax--delay="250"
                        multiple="multiple"
                        data-data-ajax--url="${r"${"}select2ControllerLink.to('${dconfig.select2MethodName}')}"
                        data-data-placeholder="${r"#{"}${select2_placeholder}}">
                          <option data-th-each="item: *{${detail.pathStringFieldNames}}"
                             selected="true"
                             data-th-text="${r"${{"}item}}" data-th-value="${r"${"}item.${dconfig.identifierField}}">Another product to select</option>
                      </select>
                    </div>
                </div>
              </#if>
              </fieldset>

              <!-- buttons form -->
              <div class="form-group">
                <div class="col-md-6 col-md-offset-3">
                    <button type="submit" class="btn btn-primary" data-th-text="${r"#{"}label_save}">Save</button>
                    <button type="reset" class="btn btn-default"
                      onclick="location.href='list.html'"
                      data-th-onclick="'location.href=\'' + @{${"${"}collectionLink.to('list')}} + '\''"
                      data-th-text="${r"#{"}label_reset}">Cancel</button>
                </div>
              </div>

          </form>
          <!-- /FORM -->

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
  <div data-layout-fragment="javascript">
  </div>

    <!-- Application -->
    <script type="text/javascript" charset="utf8"
      src="../../static/public/js/main.js"></script>

</body>
</#if>
</html>
