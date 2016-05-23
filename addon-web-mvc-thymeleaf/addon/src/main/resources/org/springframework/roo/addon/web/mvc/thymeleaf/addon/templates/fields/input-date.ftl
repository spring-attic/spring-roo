<#macro input label fieldName z format>
<div class="form-group" id="${fieldName}" data-z="${z}"
  data-th-classappend="${r"${#fields.hasErrors"}('${fieldName}')}? 'has-error has-feedback'">
  <label for="${fieldName}" class="col-md-3 control-label" 
    data-th-text="${r"#{"}${label}${r"}"}">${fieldName}</label>
  <div class="col-md-3">
    <input name="${fieldName}" data-th-value="*{{${fieldName}}}" type="text" class="form-control datetimepicker"
      placeholder="${fieldName}"
      data-th-placeholder="${r"#{"}${label}${r"}"}"
      data-th-attr="data-dateformat=${r"${"}${fieldName}${r"_date_format}"}"
      data-toggle="tooltip"/> <span
      data-th-classappend="${r"${#fields.hasErrors"}('${fieldName}')}? 'glyphicon glyphicon-remove form-control-feedback'"
      data-th-if="${r"${#fields.hasErrors"}('${fieldName}')}"
      aria-hidden="true"></span>
     <span id="name-help" class="help-block alert"
      data-th-if="${r"${#fields.hasErrors"}('${fieldName}')}"
      data-th-errors="*{${fieldName}}">Help message.</span>
  </div>
</div>
</#macro>