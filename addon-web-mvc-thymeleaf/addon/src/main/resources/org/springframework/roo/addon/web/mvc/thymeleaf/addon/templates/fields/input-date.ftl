<#macro input label fieldName fieldId z format required>
<div class="form-group has-error has-feedback" id="${fieldId}" data-z="${z}"
  data-th-classappend="${r"${#fields.hasErrors"}('${fieldName}')}? 'has-error has-feedback'" data-th-class="form-group">
  <label for="${fieldName}" class="col-md-3 control-label"
    data-th-text="${r"#{"}${label}}">${fieldName}</label>
  <div class="col-md-3">
    <input id="${fieldName}" name="${fieldName}" data-th-value="*{{${fieldName}}}" type="text" class="form-control datetimepicker"
      placeholder="${fieldName}"
      data-th-placeholder="${r"#{"}${label}}"
      data-th-attr="data-dateformat=${r"${"}${fieldName}${r"_date_format}"}"
      data-toggle="tooltip" <#if required == true>required="required"</#if> /> <span
      data-th-classappend="${r"${#fields.hasErrors"}('${fieldName}')}? 'glyphicon glyphicon-remove form-control-feedback'"
      data-th-if="${r"${#fields.hasErrors"}('${fieldName}')}"
      aria-hidden="true"></span>
     <span id="${fieldName}-error" class="help-block"
      data-th-if="${r"${#fields.hasErrors"}('${fieldName}')}"
      data-th-errors="*{${fieldName}}">Error message.</span>
  </div>
</div>
</#macro>
