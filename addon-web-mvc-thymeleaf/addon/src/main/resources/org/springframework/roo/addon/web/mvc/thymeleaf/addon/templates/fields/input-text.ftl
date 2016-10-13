<#macro input label fieldName fieldId z size>
<div class="form-group has-error has-feedback" data-z="${z}" id="${fieldId}"
  data-th-classappend="${r"${#fields.hasErrors"}('${fieldName}')}? 'has-error has-feedback'" data-th-class="form-group">
  <label for="${fieldName}" class="col-md-3 control-label"
    data-th-text="${r"#{"}${label}${r"}"}">${fieldName}</label>
  <div class="col-md-${size}">
    <input id="${fieldName}" name="${fieldName}" data-th-value="*{{${fieldName}}}" type="text" class="form-control"
      placeholder="${fieldName}"
      data-th-placeholder="${r"#{"}${label}${r"}"}"
      data-toggle="tooltip" aria-describedby="${fieldName}Status"/>
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
