package org.springframework.roo.project.providers;

import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.project.ProjectCommands;
import org.springframework.roo.project.ProjectService;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * Roo Shell converter for {@link ProjectManagerProvider} of
 * {@link ProjectCommands}
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class ProjectManagerProviderIdConverter implements Converter<ProjectManagerProviderId> {

	@Reference
    private ProjectService projectService;

    protected void bindOperations(ProjectService operations) {
        this.projectService = operations;
    }

    protected void unbindOperations(ProjectService operations) {
        this.projectService = null;
    }

    /* (non-Javadoc)
     * @see org.springframework.roo.shell.Converter#convertFromText(java.lang.String, java.lang.Class, java.lang.String)
     */
    @Override
    public ProjectManagerProviderId convertFromText(String value,
            Class<?> targetType, String optionContext) {
        return projectService.getProviderIdByName(value);
    }

    /* (non-Javadoc)
     * @see org.springframework.roo.shell.Converter#getAllPossibleValues(java.util.List, java.lang.Class, java.lang.String, java.lang.String, org.springframework.roo.shell.MethodTarget)
     */
    @Override
    public boolean getAllPossibleValues(List<Completion> completions,
            Class<?> targetType, String existingData, String optionContext,
            MethodTarget target) {
        for (final ProjectManagerProviderId id : projectService.getProvidersId()) {
            if (existingData.isEmpty() || id.getId().equals(existingData)
                    || id.getId().startsWith(existingData)) {
                completions.add(new Completion(id.getId()));
            }
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.springframework.roo.shell.Converter#supports(java.lang.Class, java.lang.String)
     */
    @Override
    public boolean supports(Class<?> type, String optionContext) {
        return ProjectManagerProviderId.class.isAssignableFrom(type);
    }


}
