<#macro input label fieldName>
<div class="form-group"
  data-th-classappend="${r"${#fields.hasErrors"}('${fieldName}')}? 'has-error'">
  <label for="${fieldName}" class="col-md-3 control-label">${fieldName}</label>
  <div class="col-md-3">
    <input data-th-field="*{${fieldName}}" type="text" class="form-control"
      placeholder="${fieldName}"
      data-th-placeholder="${fieldName}"
      data-toggle="tooltip" title="Mandatory Field"/> <span
      id="name-help" class="help-block"
      data-th-if="${r"${#fields.hasErrors"}('${fieldName}')}"
      data-th-errors="*{${fieldName}}">Help message.</span>
  </div>
</div>
</#macro>