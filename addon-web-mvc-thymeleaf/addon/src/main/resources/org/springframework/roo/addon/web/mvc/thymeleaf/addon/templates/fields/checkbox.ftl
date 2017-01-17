<#macro input label fieldName fieldId z>
<div class="form-group has-error has-feedback" id="${fieldId}" data-z="${z}"
  data-th-classappend="${r"${#fields.hasErrors"}('${fieldName}')}? 'has-error has-feedback'" data-th-class="form-group">
  <label class="col-md-3 control-label" for="${fieldName}"
    data-th-text="${r"#{"}${label}}">${fieldName}</label>
  <div class="col-md-6">
    <div class="checkbox">
      <input type="checkbox" data-th-field="*{${fieldName}}"
        data-th-title="${r"#{"}label_requiredfield}"
        data-toggle="tooltip"/>
       <span
        data-th-classappend="${r"${#fields.hasErrors"}('${fieldName}')}? 'form-control-feedback'"
        class="glyphicon glyphicon-remove form-control-feedback"
        data-th-if="${r"${#fields.hasErrors"}('${fieldName}')}" aria-hidden="true"></span>
        <span
        id="${fieldName}-error" class="help-block"
        data-th-if="${r"${#fields.hasErrors"}('${fieldName}')}"
        data-th-errors="*{${fieldName}}">Error message.</span>
    </div>
  </div>
</div>
</#macro>
