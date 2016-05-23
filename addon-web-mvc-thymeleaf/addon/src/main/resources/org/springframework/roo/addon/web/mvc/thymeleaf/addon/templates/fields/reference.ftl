<#macro input label fieldName z referencedEntity identifierField referencedPath fieldOne fieldTwo>
<div class="form-group" data-z="${z}" id="${fieldName}" data-th-classappend="${r"${#fields.hasErrors"}('${fieldName}')}? 'has-error has-feedback'">
    <label for="${fieldName}" class="col-md-3 control-label" data-th-text="${r"#{"}${label}${r"}"}">${referencedEntity}</label>
    <div class="col-md-6">
      <!-- Select2 -->
      <select data-th-field="*{${fieldName}}" class="dropdown-select-ajax" style="width: 50%" 
        data-placeholder="Select an option" data-allow-clear="true"
        data-id-field="${identifierField}" data-text-fields="${fieldOne},${fieldTwo}" 
        data-ajax--url="${referencedPath}" data-ajax--cache="true" data-ajax--delay="250" 
        data-ajax--data-type="json">
          <option data-th-unless="*{${fieldName}} == null" 
            data-th-value="*{${fieldName}.${identifierField}}" 
            data-th-text="|*{${fieldName}.${fieldOne}} *{${fieldName}.${fieldTwo}}|" 
         selected="selected">${referencedEntity}</option>
      </select>
      <span id="name-help" class="help-block alert"
      data-th-if="${r"${#fields.hasErrors"}('${fieldName}')}"
      data-th-errors="*{${fieldName}}">Help message.</span>
    </div>
</div>
</#macro>