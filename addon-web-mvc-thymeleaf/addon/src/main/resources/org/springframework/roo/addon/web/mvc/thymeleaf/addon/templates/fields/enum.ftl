<#macro input label fieldName items>
<div class="form-group">
    <label for="${fieldName}" class="col-md-3 control-label" data-th-text="${label}">${fieldName}</label>
    <div class="col-md-6">
      <!-- Select2 -->
      <select id="${fieldName}" class="dropdown-select-simple" style="width: 50%" 
        data-placeholder="Select an option" data-allow-clear="true" data-th-field="*{${fieldName}}">
         <option data-th-each="item : ${r"${"}${items}${r"}"}" 
              data-th-value="${r"${item}"}"
              data-th-text="${r"${item}"}">Value</option>
      </select>
    </div>
</div>
</#macro>