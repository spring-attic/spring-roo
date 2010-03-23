package org.springframework.roo.classpath;

import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.itd.ItdMetadataScanner;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.metadata.MetadataTimingStatistic;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

@Component
@Service
public class MetadataCommands implements CommandMarker {
	
	@Reference private MetadataService metadataService;
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;
	@Reference private PhysicalTypeMetadataProvider physicalTypeMetadataProvider;
	@Reference private ItdMetadataScanner itdMetadataScanner;
	
	@CliCommand(value="metadata trace", help="Traces metadata event delivery notifications")
	public void metadataTrace(@CliOption(key={"","level"}, mandatory=true, help="The verbosity of notifications (0=none, 1=some, 2=all)") int level) {
		metadataDependencyRegistry.setTrace(level);
	}
	
	@CliCommand(value="metadata status", help="Shows metadata statistics")
	public String metadataTimings() {
		StringBuilder sb = new StringBuilder();
		for (MetadataTimingStatistic stat : metadataDependencyRegistry.getTimings()) {
			sb.append(stat.toString()).append(System.getProperty("line.separator"));
		}
		sb.append(metadataService.toString());
		return sb.toString();
	}
	
	@CliCommand(value="metadata for id", help="Shows detailed information about the metadata item")
	public String metadataForId(@CliOption(key={"", "metadataId"}, mandatory=true, help="The metadata ID (should start with MID:)") String metadataId) {
		StringBuilder sb = new StringBuilder();
		sb.append("Identifier : ").append(metadataId).append(System.getProperty("line.separator"));
		
		Set<String> upstream = metadataDependencyRegistry.getUpstream(metadataId);
		if (upstream.size() == 0) {
			sb.append("Upstream   : ").append(System.getProperty("line.separator"));
		}

		for (String s : upstream) {
			sb.append("Upstream   : ").append(s).append(System.getProperty("line.separator"));
		}
		
		// Include any "class level" notifications that this instance would receive (useful for debugging)
		// Only necessary if the ID doesn't already represent a class (as such dependencies would have been listed earlier)
		if (!MetadataIdentificationUtils.isIdentifyingClass(metadataId)) {
			String asClass = MetadataIdentificationUtils.create(MetadataIdentificationUtils.getMetadataClass(metadataId));
			for (String s : metadataDependencyRegistry.getUpstream(asClass)) {
				sb.append("Upstream   : ").append(s).append(" (via class)").append(System.getProperty("line.separator"));
			}
		}

		Set<String> downstream = metadataDependencyRegistry.getDownstream(metadataId);
		if (downstream.size() == 0) {
			sb.append("Downstream : ").append(System.getProperty("line.separator"));
		}
		for (String s : downstream) {
			sb.append("Downstream : ").append(s).append(System.getProperty("line.separator"));
		}
		
		// Include any "class level" notifications that this instance would receive (useful for debugging)
		// Only necessary if the ID doesn't already represent a class (as such dependencies would have been listed earlier)
		if (!MetadataIdentificationUtils.isIdentifyingClass(metadataId)) {
			String asClass = MetadataIdentificationUtils.create(MetadataIdentificationUtils.getMetadataClass(metadataId));
			for (String s : metadataDependencyRegistry.getDownstream(asClass)) {
				sb.append("Downstream : ").append(s).append(" (via class)").append(System.getProperty("line.separator"));
			}
		}

		if (MetadataIdentificationUtils.isIdentifyingInstance(metadataId)) {
			sb.append("Metadata   : ").append(metadataService.get(metadataId));
		}
		return sb.toString();
	}
	
	@CliCommand(value="metadata for type", help="Shows detailed metadata for the indicated type")
	public String metadataForType(@CliOption(key={"", "type"}, mandatory=true, help="The Java type name to display metadata for") JavaType javaType) {
		String id = physicalTypeMetadataProvider.findIdentifier(javaType);
		if (id == null) {
			return "Cannot locate source for " + javaType.getFullyQualifiedTypeName();
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Java Type  : ").append(javaType.getFullyQualifiedTypeName()).append(System.getProperty("line.separator"));
		for (MetadataItem item : itdMetadataScanner.getMetadata(id)) {
			if (!item.getId().equals(id)) {
				sb.append("ITD scan   : ").append(item.getId()).append(System.getProperty("line.separator"));
			}
		}
		sb.append(metadataForId(id));
		return sb.toString();
	}

}
