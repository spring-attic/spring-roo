<#macro input label fieldName format>
<div class="form-group"
  data-th-classappend="${r"${#fields.hasErrors"}('${fieldName}')}? 'has-error'">
  <label for="${fieldName}" class="col-md-3 control-label" 
    data-th-text="${r"#{"}${label}${r"}"}">${fieldName}</label>
  <div class="col-md-3">
    <input id="${fieldName}" data-th-field="*{${fieldName}}" type="text" class="form-control"
      placeholder="${fieldName}"
      data-th-placeholder="${r"#{"}${label}${r"}"}"
      data-th-title="${r"#{"}label_requiredfield${r"}"}"
      data-toggle="tooltip"/> <span
      id="name-help" class="help-block"
      data-th-if="${r"${#fields.hasErrors"}('${fieldName}')}"
      data-th-errors="*{${fieldName}}"
      data-th-text="${r"#{"}label_help_message${r"}"}" >Help message.</span>
  </div>
  <script type="text/javascript">
    jQuery(function() {
        jQuery("#${fieldName}").datetimepicker({
            format : "${format}"
        });
    });
  </script>
</div>
</#macro>