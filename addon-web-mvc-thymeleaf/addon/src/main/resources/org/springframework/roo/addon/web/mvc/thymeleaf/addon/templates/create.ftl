<#import "fields/input-text.ftl" as text>
<#import "fields/input-number.ftl" as number>
<#import "fields/input-date.ftl" as date>
<#import "fields/reference.ftl" as reference>
<#import "fields/checkbox.ftl" as checkbox>
<#import "fields/enum.ftl" as enum>
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
      data-th-href="@{/webjars/datetimepicker/build/jquery.datetimepicker.min.css}" />

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
      <section data-layout-fragment="content">
        <div class="container-fluid content" data-th-with="collectionLink=${r"${@"}linkBuilder.of('${mvcCollectionControllerName}')}">

          <h1 data-th-text="${r"#{"}label_create_entity(${r"#{"}${entityLabel}})}">Create ${entityName}</h1>

          <!-- FORM -->
          <form class="form-horizontal validate" method="POST" data-th-object="${modelAttribute}"
            data-th-action="${r"${"}collectionLink.to('create').with('${modelAttributeName}', ${modelAttributeName}.${identifierField})}">

            <fieldset id="containerFields">
              <legend class="sr-only" data-th-text="${r"#{"}label_data_entity(${r"#{"}${entityLabel}})}">${entityName} data </legend>

              <#list fields as field>
                  <#if field.userManaged>
                      ${field.codeManaged}
                  <#elseif field.type == "TEXT">
                      <@text.input label=field.label fieldName=field.fieldName fieldId=field.fieldId z=field.z width=6 required=field.configuration.required maxLength=field.configuration.maxLength />
                  <#elseif field.type == "NUMBER">
                      <@number.input label=field.label fieldName=field.fieldName fieldId=field.fieldId
                      		z=field.z width=3 required=field.configuration.required
                      		min=field.configuration.min max=field.configuration.max
                      		digitsFraction=field.configuration.digitsFraction
                      		digitsInteger=field.configuration.digitsInteger />
                  <#elseif field.type == "DATE">
                      <@date.input label=field.label
                          fieldName=field.fieldName
                          fieldId=field.fieldId
                          z=field.z
                          format=field.configuration.format required=field.configuration.required />
                  <#elseif field.type == "REFERENCE">
                      <@reference.input label=field.label
                          fieldName=field.fieldName
                          fieldId=field.fieldId
                          z=field.z
                          referencedEntity=field.configuration.referencedEntity
                          identifierField=field.configuration.identifierField
                          select2MethodName=field.configuration.select2MethodName
                          select2ControllerName=field.configuration.select2ControllerName
                          required=field.configuration.required />
                  <#elseif field.type == "ENUM">
                      <@enum.input label=field.label
                          fieldName=field.fieldName
                          fieldId=field.fieldId
                          z=field.z
                          items=field.configuration.items required=field.configuration.required />
                  <#elseif field.type == "BOOLEAN">
                      <@checkbox.input label=field.label fieldName=field.fieldName fieldId=field.fieldId z=field.z />
                  </#if>
              </#list>
            </fieldset>

              <#if compositeRelationFields?has_content>
                <#list compositeRelationFields?keys as referencedField>
                  <fieldset id="${referencedField}FieldSet">
                    <#list compositeRelationFields[referencedField] as field>
                      <#if field?index == 0>
                        <legend data-th-text="${r"#{"}label_data_entity(${r"#{"}${field.legendLabel}})}">${field.entityName} data </legend>
                      </#if>

                      <#if field.userManaged>
                        ${field.codeManaged}
                      <#elseif field.type == "TEXT">
                        <@text.input label=field.label fieldName=field.fieldName fieldId=field.fieldId z=field.z width=6 required=field.configuration.required maxLength=field.configuration.maxLength />
                      <#elseif field.type == "NUMBER">
                        <@number.input label=field.label fieldName=field.fieldName fieldId=field.fieldId z=field.z width=3 required=field.configuration.required min=field.configuration.min max=field.configuration.max digitsFraction=field.configuration.digitsFraction digitsInteger=field.configuration.digitsInteger />
                      <#elseif field.type == "DATE">
                        <@date.input label=field.label
                          fieldName=field.fieldName
                          fieldId=field.fieldId
                          z=field.z
                          format=field.configuration.format required=field.configuration.required />
                      <#elseif field.type == "REFERENCE">
                        <@reference.input label=field.label
                          fieldName=field.fieldName
                          fieldId=field.fieldId
                          z=field.z
                          referencedEntity=field.configuration.referencedEntity
                          identifierField=field.configuration.identifierField
                          select2MethodName=field.configuration.select2MethodName
                          select2ControllerName=field.configuration.select2ControllerName
                          required=field.configuration.required />
                      <#elseif field.type == "ENUM">
                        <@enum.input label=field.label
                          fieldName=field.fieldName
                          fieldId=field.fieldId
                          z=field.z
                          items=field.configuration.items required=field.configuration.required />
                      <#elseif field.type == "BOOLEAN">
                        <@checkbox.input label=field.label fieldName=field.fieldName fieldId=field.fieldId z=field.z />
                      </#if>
                    </#list>
                  </fieldset>
                </#list>
              </#if>

              <!-- buttons form -->
              <div class="form-group">
                <div class="col-md-9 col-md-offset-3">
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
  <!-- JavaScript loaded by layout of the page displayed -->
  <!--
       Only the inner content of the following tag "javascript" is included
       within the template, in the div "javascript"
      -->
  <div data-layout-fragment="javascript">

    <!-- DateTimePicker -->
    <script
      src="https://cdnjs.cloudflare.com/ajax/libs/jquery-datetimepicker/2.5.4/build/jquery.datetimepicker.full.min.js"
      data-th-src="@{/webjars/datetimepicker/build/jquery.datetimepicker.full.min.js}"></script>
    <script src="../../static/public/js/datetimepicker-defaults.js"
      data-th-src="@{/public/js/datetimepicker-defaults.js}"></script>

    <!-- jquery.inputmask -->
    <script
       src="https://cdnjs.cloudflare.com/ajax/libs/jquery.inputmask/3.3.1/jquery.inputmask.bundle.min.js"
       data-th-src="@{/webjars/jquery.inputmask/min/jquery.inputmask.bundle.min.js}"></script>
    <script type="text/javascript" data-th-inline="javascript">
					(function(jQuery) {
						jQuery(document)
								.ready(
										function() {
											Inputmask
													.extendAliases({
														'numeric' : {
															'groupSeparator' : /*[[${r"#{"}label_inputmask_groupSeparator}]]*/'.',
															'radixPoint' : /*[[${r"#{"}label_inputmask_radixPoint}]]*/','
														},
														'currency' : {
															'prefix' : /*[[${r"#{"}label_inputmask_prefix}]]*/'',
															'suffix' : /*[[${r"#{"}label_inputmask_suffix}]]*/' €'
														}
													});
										});
					})(jQuery);
  		</script>
    <script src="../../static/public/js/inputmask-defaults.js"
      data-th-src="@{/public/js/inputmask-defaults.js}"></script>

    <!-- JQuery Validation -->
    <script
      src="http://ajax.aspnetcdn.com/ajax/jquery.validate/1.15.0/jquery.validate.min.js"
      data-th-src="@{/webjars/jquery-validation/dist/jquery.validate.min.js}">
		  </script>
    <script
      src="http://ajax.aspnetcdn.com/ajax/jquery.validate/1.15.0/additional-methods.min.js"
      data-th-src="@{/webjars/jquery-validation/dist/additional-methods.min.js}">
			</script>
    <script
      src="http://ajax.aspnetcdn.com/ajax/jquery.validate/1.15.0/localization/messages_es.js"
      data-th-src="@{/webjars/jquery-validation/src/localization/messages_}+${r"${#"}locale.language}+'.js'"
      data-th-if="${r"${#"}locale.language} != 'en'">
			</script>
    <script src="../../static/public/js/validation-defaults.js"
      data-th-src="@{/public/js/validation-defaults.js}">
			</script>

    <script type="text/javascript" data-th-inline="javascript">
					(function(jQuery) {
						jQuery(document)
								.ready(
										function() {
											jQuery
													.extend(
															jQuery.validator.messages,
															{
																'dateformat' : /*[[${r"#{"}error_invalid_date}]]*/'Please enter a correct date/time',
																'inputmask' : /*[[${r"#{"}error_invalid_maskValue}]]*/'Please enter a valid value',
															});
										});
					})(jQuery);
		  </script>

    </div>

  </body>
  </#if>

</html>