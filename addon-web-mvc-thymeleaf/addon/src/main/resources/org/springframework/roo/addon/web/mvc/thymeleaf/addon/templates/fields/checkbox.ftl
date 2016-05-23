<#macro input label fieldName z>
<div class="form-group" id="${fieldName}" data-z="${z}"
  data-th-classappend="${r"${#fields.hasErrors"}('${fieldName}')}? 'has-error has-feedback'">
  <label for="${fieldName}" class="col-md-3 control-label" 
    data-th-text="${r"#{"}${label}${r"}"}">${fieldName}</label>
  <div class="col-md-3">
    <input type="checkbox" data-th-field="*{${fieldName}}" class="form-control"
      data-th-title="${r"#{"}label_requiredfield${r"}"}"
      data-toggle="tooltip"/> 
     <span id="name-help" class="help-block alert"
      data-th-if="${r"${#fields.hasErrors"}('${fieldName}')}"
      data-th-errors="*{${fieldName}}">Help message.</span>
  </div>
</div>
</#macro>