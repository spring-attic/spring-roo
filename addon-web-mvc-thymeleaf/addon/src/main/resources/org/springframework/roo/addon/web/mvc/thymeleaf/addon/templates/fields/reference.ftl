<#macro input label fieldName fieldId z referencedEntity identifierField referecedMvcUrl_select2 required>
<div class="form-group has-error has-feedback" data-z="${z}" id="${fieldId}"
data-th-classappend="${r"${#fields.hasErrors"}('${fieldName}')}? 'has-error has-feedback'" data-th-class="form-group">
    <label for="${fieldName}" class="col-md-3 control-label" data-th-text="${r"#{"}${label}${r"}"}">${referencedEntity}</label>
    <div class="col-md-6">
      <!-- Select2 -->
      <select data-th-field="*{${fieldName}}" class="form-control dropdown-select-ajax"
        data-allow-clear="true"
        data-data-ajax--url="${r"${"}(#mvc.url('${referecedMvcUrl_select2}')).build()}" data-ajax--cache="true" data-ajax--delay="250"
        data-ajax--data-type="json"
        <#if required == true>
        required="required"
        data-allow-clear="false"
        <#else>
        data-allow-clear="true"
        </#if>
        data-data-placeholder="${r"#{"}info_select_an_option${r"}"}">
          <option data-th-unless="*{${fieldName}} == null"
            data-th-value="*{${fieldName}.${identifierField}}"
            data-th-text="*{${fieldName}}"
         selected="selected">${referencedEntity}</option>
      </select>
      <span
      data-th-classappend="${r"${#fields.hasErrors"}('${fieldName}')}? 'glyphicon glyphicon-remove form-control-feedback'"
      class="glyphicon glyphicon-remove form-control-feedback"
      data-th-if="${r"${#fields.hasErrors"}('${fieldName}')}" aria-hidden="true"></span>
      <span
      id="${fieldName}-error" class="help-block"
      data-th-if="${r"${#fields.hasErrors"}('${fieldName}')}"
      data-th-errors="*{${fieldName}}">Error message.</span>
    </div>
</div>
</#macro>
