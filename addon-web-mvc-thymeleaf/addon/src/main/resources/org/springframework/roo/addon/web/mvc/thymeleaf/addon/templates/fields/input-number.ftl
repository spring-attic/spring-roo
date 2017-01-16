<#macro input label fieldName fieldId z width required min max digitsFraction digitsInteger>
<div class="form-group has-error has-feedback" data-z="${z}" id="${fieldId}"
  data-th-classappend="${r"${#fields.hasErrors"}('${fieldName}')}? 'has-error has-feedback'" data-th-class="form-group">
  <label for="${fieldName}" class="col-md-3 control-label"
    data-th-text="${r"#{"}${label}}">${fieldName}</label>
  <div class="col-md-${width}">
    <input id="${fieldName}" name="${fieldName}" data-th-value="*{{${fieldName}}}" type="text" class="form-control inputmask"
      placeholder="${fieldName}"
      data-th-placeholder="${r"#{"}${label}}"
      data-toggle="tooltip" aria-describedby="${fieldName}Status" 
      data-inputmask-alias="numeric" 
      <#if digitsInteger != "NULL">data-inputmask-integerDigits="${digitsInteger}"</#if> 
      <#if digitsFraction != "NULL">data-inputmask-digits="${digitsFraction}"</#if>
      <#if required == true>required="required"</#if> 
      <#if min != "NULL">min="${min}"</#if> 
      <#if max != "NULL">max="${max}"</#if> />
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
