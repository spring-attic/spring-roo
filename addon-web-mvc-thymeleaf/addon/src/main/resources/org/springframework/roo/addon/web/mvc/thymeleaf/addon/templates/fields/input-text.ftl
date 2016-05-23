<#macro input label fieldName z>
<div class="form-group" data-z="${z}" id="${fieldName}"
  data-th-classappend="${r"${#fields.hasErrors"}('${fieldName}')}? 'has-error has-feedback'">
  <label for="${fieldName}" class="col-md-3 control-label"
    data-th-text="${r"#{"}${label}${r"}"}">${fieldName}</label>
  <div class="col-md-3">
    <input name="${fieldName}" data-th-value="*{{${fieldName}}}" type="text" class="form-control"
      placeholder="${fieldName}"
      data-th-placeholder="${r"#{"}${label}${r"}"}"
      data-toggle="tooltip"/> <span
      id="name-help" class="help-block alert"
      data-th-if="${r"${#fields.hasErrors"}('${fieldName}')}"
      data-th-errors="*{${fieldName}}">Help message.</span>
  </div>
</div>
</#macro>