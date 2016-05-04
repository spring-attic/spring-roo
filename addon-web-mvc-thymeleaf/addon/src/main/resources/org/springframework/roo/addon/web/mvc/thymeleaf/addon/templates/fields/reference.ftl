<#macro input label fieldName referencedEntity identifierField referencedPath fieldOne fieldTwo>
<div class="form-group">
    <label for="${fieldName}" class="col-md-3 control-label" data-th-text="${r"#{"}${label}${r"}"}">${referencedEntity}</label>
    <div class="col-md-6">
      <!-- Select2 -->
      <select data-th-field="*{${fieldName}}" class="dropdown-select-ajax" style="width: 50%" 
        data-placeholder="Select an option" data-allow-clear="true" 
        data-id-field="${identifierField}" data-text-fields="${fieldOne},${fieldTwo}" 
        data-ajax--url="${referencedPath}" data-ajax--cache="true" data-ajax--delay="250" 
        data-th-title="${r"#{"}label_requiredfield${r"}"}"
        data-ajax--data-type="json">
          <option data-th-unless="*{${fieldName}} == null" 
            data-th-value="*{${fieldName}.${identifierField}}" 
            data-th-text="|*{${label}.${fieldOne}} *{${label}.${fieldTwo}}|" 
         selected="selected">${referencedEntity}</option>
      </select>
      <span id="name-help" class="help-block"
      data-th-if="${r"${#fields.hasErrors"}('${fieldName}')}"
      data-th-errors="*{${fieldName}}" 
      data-th-text="${r"#{"}label_help_message${r"}"}">Help message.</span>
    </div>
</div>
</#macro>