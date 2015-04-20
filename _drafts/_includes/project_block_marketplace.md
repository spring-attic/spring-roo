<div class="project--container parent-project project--links--container-marketplace span4">

   <div class="project-logo--container">
     <div class="project--logo">
      <!-- logo url -->
      <img src="{{include.logo_url}}" />
     </div>
   </div>

	 <div class="project--title">{{include.project_title}}</div>
	 <p class="project--description">{{include.project_description}}</p>

	<div class="project-links--container-marketplace">

	 <!-- link to repository index.xml file -->
	 <a class="project-link left" title="{{include.repo_url}}" href="{{include.repo_url}}" target="_blank"><i class="icon-folder-open icon-2x"></i></a>

	 <!-- link to roo addon suite .esa file (optional) -->
	 <a class="project-link right" title="{{include.esa_url}}" href="{{include.esa_url}}" target="_blank"><i class="icon-download-alt icon-2x"></i></a>


	</div>

</div>

