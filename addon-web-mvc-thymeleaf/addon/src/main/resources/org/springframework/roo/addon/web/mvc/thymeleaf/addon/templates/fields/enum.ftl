<#macro input label fieldName fieldId z items required>
<div class="form-group has-error has-feedback" data-z="${z}" id="${fieldId}"
data-th-classappend="${r"${#fields.hasErrors"}('${fieldName}')}? 'has-error has-feedback'" data-th-class="form-group">
    <label for="${fieldName}" class="col-md-3 control-label" data-th-text="${r"#{"}${label}}">${fieldName}</label>
    <div class="col-md-6">
      <!-- Select2 -->
      <select data-th-field="*{${fieldName}}" class="form-control dropdown-select-simple"
         data-allow-clear="true" data-th-attr="data-placeholder=${r"#{"}info_select_an_option}" <#if required == true>required="required"</#if>>
         <option data-th-each="item : ${r"${"}${items}}"
              data-th-value="${r"${item}"}"
              data-th-text="${r"${{item}}"}">Value</option>
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
