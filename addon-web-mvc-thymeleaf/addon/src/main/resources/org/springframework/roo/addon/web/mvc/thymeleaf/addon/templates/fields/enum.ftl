<#macro input label fieldName z items>
<div class="form-group" data-z="${z}" id="${fieldName}" data-th-classappend="${r"${#fields.hasErrors"}('${fieldName}')}? 'has-error has-feedback'">
    <label for="${fieldName}" class="col-md-3 control-label" data-th-text="${r"#{"}${label}${r"}"}">${fieldName}</label>
    <div class="col-md-6">
      <!-- Select2 -->
      <select data-th-field="*{${fieldName}}" class="dropdown-select-simple" style="width: 50%" 
        data-placeholder="Select an option" data-allow-clear="true">
         <option data-th-each="item : ${r"${"}${items}${r"}"}" 
              data-th-value="${r"${item}"}"
              data-th-text="${r"${item}"}">Value</option>
      </select>
      <span id="name-help" class="help-block alert"
      data-th-if="${r"${#fields.hasErrors"}('${fieldName}')}"
      data-th-errors="*{${fieldName}}">Help message.</span>
    </div>
</div>
</#macro>