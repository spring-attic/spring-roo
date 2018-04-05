<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8" />
    <title>Page menu</title>
  </head>
<#if userManagedComponents?has_content && userManagedComponents["body"]??>
  ${userManagedComponents["body"]}
<#else>
  <body id="body">
    <!--
    Only the internal content of the following div is included within
    the template, in concurrency fragment
    -->
    <div data-th-fragment="concurrency-control(editFormUrl)" id="concurrency-control-form" class="alert alert-warning"
         data-th-if="${r"${"}concurrency}"
         data-th-data-edit-form-url="${r"${"}editFormUrl}"
         data-th-data-new-version="${r"${"}newVersion}" >
      <h2 data-th-text="${r"#{"}label_concurrency_title}">Warning! This record has been updated by an other user.</h2>
      <div class="radio">
        <label>
          <input type="radio" name="concurrency" value="apply">
            <span data-th-text="${r"#{"}label_concurrency_apply}">Apply my changes anyway</span> <i>
            <span data-th-text="${r"#{"}label_concurrency_apply_info}">(discard all the changes applied by the other users).</span></i>
          </input>
        </label>
      </div>
      <div class="radio">
        <label>
          <input type="radio" name="concurrency" value="discard" checked="">
            <span data-th-text="${r"#{"}label_concurrency_discard}">Discard all my changes and reload this record.</span>
          </input>
        </label>
      </div>
      <br>
      <button type="submit" class="btn btn-primary">Accept</button>
    </br></div>

  </body>
</#if>
</html>