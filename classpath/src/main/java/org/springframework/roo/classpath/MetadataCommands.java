package org.springframework.roo.classpath;

import static org.apache.commons.io.IOUtils.LINE_SEPARATOR;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataLogger;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.metadata.MetadataTimingStatistic;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.converter.PomConverter;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

@Component
@Service
public class MetadataCommands implements CommandMarker {

    private static final String METADATA_FOR_MODULE_COMMAND = "metadata for module";

    @Reference private MemberDetailsScanner memberDetailsScanner;
    @Reference private MetadataDependencyRegistry metadataDependencyRegistry;
    @Reference private MetadataLogger metadataLogger;
    @Reference private MetadataService metadataService;
    @Reference private ProjectOperations projectOperations;
    @Reference private TypeLocationService typeLocationService;

    @CliAvailabilityIndicator(METADATA_FOR_MODULE_COMMAND)
    public boolean isModuleMetadataAvailable() {
        return projectOperations.getFocusedModule() != null;
    }

    @CliCommand(value = "metadata cache", help = "Shows detailed metadata for the indicated type")
    public String metadataCacheMaximum(
            @CliOption(key = { "maximumCapacity" }, mandatory = true, help = "The maximum number of metadata items to cache") final int maxCapacity) {
        Validate.isTrue(maxCapacity >= 100,
                "Maximum capacity must be 100 or greater");
        metadataService.setMaxCapacity(maxCapacity);
        // Show them that the change has taken place
        return metadataTimings();
    }

    @CliCommand(value = "metadata for id", help = "Shows detailed information about the metadata item")
    public String metadataForId(
            @CliOption(key = { "", "metadataId" }, mandatory = true, help = "The metadata ID (should start with MID:)") final String metadataId) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Identifier : ").append(metadataId)
                .append(IOUtils.LINE_SEPARATOR);

        for (final String upstreamId : metadataDependencyRegistry
                .getUpstream(metadataId)) {
            sb.append("Upstream   : ").append(upstreamId)
                    .append(LINE_SEPARATOR);
        }

        // Include any "class level" notifications that this instance would
        // receive (useful for debugging)
        // Only necessary if the ID doesn't already represent a class (as such
        // dependencies would have been listed earlier)
        if (!MetadataIdentificationUtils.isIdentifyingClass(metadataId)) {
            final String mdClassId = MetadataIdentificationUtils
                    .getMetadataClassId(metadataId);
            for (final String upstreamId : metadataDependencyRegistry
                    .getUpstream(mdClassId)) {
                sb.append("Upstream   : ").append(upstreamId)
                        .append(" (via MD class)").append(LINE_SEPARATOR);
            }
        }

        for (final String downstreamId : metadataDependencyRegistry
                .getDownstream(metadataId)) {
            sb.append("Downstream : ").append(downstreamId)
                    .append(LINE_SEPARATOR);
        }

        // Include any notifications that this class of metadata would trigger
        // (useful for debugging)
        // Only necessary if the ID doesn't already represent a class (as such
        // dependencies would have been listed earlier)
        if (!MetadataIdentificationUtils.isIdentifyingClass(metadataId)) {
            final String mdClassId = MetadataIdentificationUtils
                    .getMetadataClassId(metadataId);
            for (final String downstreamId : metadataDependencyRegistry
                    .getDownstream(mdClassId)) {
                sb.append("Downstream : ").append(downstreamId)
                        .append(" (via MD class)").append(LINE_SEPARATOR);
            }
        }

        if (MetadataIdentificationUtils.isIdentifyingInstance(metadataId)) {
            sb.append("Metadata   : ").append(metadataService.get(metadataId));
        }
        return sb.toString();
    }

    @CliCommand(value = METADATA_FOR_MODULE_COMMAND, help = "Shows the ProjectMetadata for the indicated project module")
    public String metadataForModule(
            @CliOption(key = { "", "module" }, mandatory = false, optionContext = PomConverter.INCLUDE_CURRENT_MODULE, help = "The module for which to retrieve the metadata (defaults to the focused module)") final Pom pom) {
        final Pom targetPom = ObjectUtils.defaultIfNull(pom,
                projectOperations.getFocusedModule());
        if (targetPom == null) {
            return "This project has no modules";
        }
        final String projectMID = ProjectMetadata
                .getProjectIdentifier(targetPom.getModuleName());
        return metadataService.get(projectMID).toString();
    }

    @CliCommand(value = "metadata for type", help = "Shows detailed metadata for the indicated type")
    public String metadataForType(
            @CliOption(key = { "", "type" }, mandatory = true, help = "The Java type for which to display metadata") final JavaType javaType) {
        final String id = typeLocationService
                .getPhysicalTypeIdentifier(javaType);
        if (id == null) {
            return "Cannot locate source for "
                    + javaType.getFullyQualifiedTypeName();
        }
        final StringBuilder sb = new StringBuilder();
        sb.append("Java Type  : ").append(javaType.getFullyQualifiedTypeName())
                .append(System.getProperty("line.separator"));
        final ClassOrInterfaceTypeDetails javaTypeDetails = typeLocationService
                .getTypeDetails(javaType);
        if (javaTypeDetails == null) {
            sb.append("Java type details unavailable").append(
                    System.getProperty("line.separator"));
        }
        else {
            for (final MemberHoldingTypeDetails holder : memberDetailsScanner
                    .getMemberDetails(getClass().getName(), javaTypeDetails)
                    .getDetails()) {
                sb.append("Member scan: ")
                        .append(holder.getDeclaredByMetadataId())
                        .append(System.getProperty("line.separator"));
            }
        }
        sb.append(metadataForId(id));
        return sb.toString();
    }

    @CliCommand(value = "metadata status", help = "Shows metadata statistics")
    public String metadataTimings() {
        final StringBuilder sb = new StringBuilder();
        for (final MetadataTimingStatistic stat : metadataLogger.getTimings()) {
            sb.append(stat.toString()).append(LINE_SEPARATOR);
        }
        sb.append(metadataService.toString());
        return sb.toString();
    }

    @CliCommand(value = "metadata trace", help = "Traces metadata event delivery notifications")
    public void metadataTrace(
            @CliOption(key = { "", "level" }, mandatory = true, help = "The verbosity of notifications (0=none, 1=some, 2=all)") final int level) {
        metadataLogger.setTraceLevel(level);
    }
}
