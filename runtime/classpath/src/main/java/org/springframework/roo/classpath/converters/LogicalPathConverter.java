package org.springframework.roo.classpath.converters;

import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.PhysicalPath;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

@Component
@Service
public class LogicalPathConverter implements Converter<LogicalPath> {

    @Reference ProjectOperations projectOperations;

    public LogicalPath convertFromText(final String value,
            final Class<?> targetType, final String optionContext) {
        LogicalPath logicalPath = LogicalPath.getInstance(value);
        if (logicalPath.getModule().equals("FOCUSED")) {
            logicalPath = LogicalPath.getInstance(logicalPath.getPath(),
                    projectOperations.getFocusedModuleName());
        }
        return logicalPath;
    }

    public boolean getAllPossibleValues(final List<Completion> completions,
            final Class<?> targetType, final String existingData,
            final String optionContext, final MethodTarget target) {
        for (final Pom pom : projectOperations.getPoms()) {
            for (final PhysicalPath physicalPath : pom.getPhysicalPaths()) {
                completions.add(new Completion(physicalPath.getLogicalPath()
                        .getName()));
            }
        }
        return false;
    }

    public boolean supports(final Class<?> requiredType,
            final String optionContext) {
        return LogicalPath.class.isAssignableFrom(requiredType);
    }
}
