package org.springframework.roo.classpath;

import static org.springframework.roo.support.util.StringUtils.LINE_SEPARATOR;

import java.util.Set;

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
import org.springframework.roo.project.Path;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.support.util.Assert;

@Component
@Service
public class MetadataCommands implements CommandMarker {
	
	// Fields
	@Reference private MetadataService metadataService;
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;
	@Reference private MemberDetailsScanner memberDetailsScanner;
	@Reference private MetadataLogger metadataLogger;
	@Reference private TypeLocationService typeLocationService;
	
	@CliCommand(value="metadata trace", help="Traces metadata event delivery notifications")
	public void metadataTrace(@CliOption(key={"","level"}, mandatory=true, help="The verbosity of notifications (0=none, 1=some, 2=all)") int level) {
		metadataLogger.setTraceLevel(level);
	}
	
	@CliCommand(value="metadata status", help="Shows metadata statistics")
	public String metadataTimings() {
		StringBuilder sb = new StringBuilder();
		for (MetadataTimingStatistic stat : metadataLogger.getTimings()) {
			sb.append(stat.toString()).append(LINE_SEPARATOR);
		}
		sb.append(metadataService.toString());
		return sb.toString();
	}
	
	@CliCommand(value = "metadata for id", help = "Shows detailed information about the metadata item")
	public String metadataForId(@CliOption(key = { "", "metadataId" }, mandatory = true, help = "The metadata ID (should start with MID:)") String metadataId) {
		StringBuilder sb = new StringBuilder();
		sb.append("Identifier : ").append(metadataId).append(LINE_SEPARATOR);
		
		Set<String> upstream = metadataDependencyRegistry.getUpstream(metadataId);
		if (upstream.isEmpty()) {
			sb.append("Upstream   : ").append(LINE_SEPARATOR);
		}

		for (String s : upstream) {
			sb.append("Upstream   : ").append(s).append(LINE_SEPARATOR);
		}
		
		// Include any "class level" notifications that this instance would receive (useful for debugging)
		// Only necessary if the ID doesn't already represent a class (as such dependencies would have been listed earlier)
		if (!MetadataIdentificationUtils.isIdentifyingClass(metadataId)) {
			String asClass = MetadataIdentificationUtils.create(MetadataIdentificationUtils.getMetadataClass(metadataId));
			for (String s : metadataDependencyRegistry.getUpstream(asClass)) {
				sb.append("Upstream   : ").append(s).append(" (via class)").append(LINE_SEPARATOR);
			}
		}

		Set<String> downstream = metadataDependencyRegistry.getDownstream(metadataId);
		if (downstream.isEmpty()) {
			sb.append("Downstream : ").append(LINE_SEPARATOR);
		}
		for (String s : downstream) {
			sb.append("Downstream : ").append(s).append(LINE_SEPARATOR);
		}
		
		// Include any "class level" notifications that this instance would receive (useful for debugging)
		// Only necessary if the ID doesn't already represent a class (as such dependencies would have been listed earlier)
		if (!MetadataIdentificationUtils.isIdentifyingClass(metadataId)) {
			String asClass = MetadataIdentificationUtils.create(MetadataIdentificationUtils.getMetadataClass(metadataId));
			for (String s : metadataDependencyRegistry.getDownstream(asClass)) {
				sb.append("Downstream : ").append(s).append(" (via class)").append(LINE_SEPARATOR);
			}
		}

		if (MetadataIdentificationUtils.isIdentifyingInstance(metadataId)) {
			sb.append("Metadata   : ").append(metadataService.get(metadataId));
		}
		return sb.toString();
	}
	
	@CliCommand(value="metadata for type", help="Shows detailed metadata for the indicated type")
	public String metadataForType(@CliOption(key={"", "type"}, mandatory=true, help="The Java type name to display metadata for") JavaType javaType) {
		String id = typeLocationService.findIdentifier(javaType);
		if (id == null) {
			return "Cannot locate source for " + javaType.getFullyQualifiedTypeName();
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Java Type  : ").append(javaType.getFullyQualifiedTypeName()).append(LINE_SEPARATOR);
		PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA));
		if (ptm == null || ptm.getMemberHoldingTypeDetails() == null || !(ptm.getMemberHoldingTypeDetails() instanceof ClassOrInterfaceTypeDetails)) {
			sb.append("Java type details unavailable").append(LINE_SEPARATOR);
		} else {
			ClassOrInterfaceTypeDetails cid = (ClassOrInterfaceTypeDetails) ptm.getMemberHoldingTypeDetails();
			for (MemberHoldingTypeDetails holder : memberDetailsScanner.getMemberDetails(getClass().getName(), cid).getDetails()) {
				sb.append("Member scan: ").append(holder.getDeclaredByMetadataId()).append(LINE_SEPARATOR);
			}
		}
		sb.append(metadataForId(id));
		return sb.toString();
	}

	@CliCommand(value="metadata cache", help="Shows detailed metadata for the indicated type")
	public String metadataCacheMaximum(@CliOption(key={"maximumCapacity"}, mandatory=true, help="The maximum number of metadata items to cache") int maxCapacity) {
		Assert.isTrue(maxCapacity >= 100, "Maximum capacity must be 100 or greater");
		metadataService.setMaxCapacity(maxCapacity);
		// Show them that the change has taken place
		return metadataTimings();
	}
}
