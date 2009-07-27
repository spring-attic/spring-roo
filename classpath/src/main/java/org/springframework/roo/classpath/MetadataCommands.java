package org.springframework.roo.classpath;

import java.util.Set;

import org.springframework.roo.classpath.itd.ItdMetadataScanner;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.support.lifecycle.ScopeDevelopmentShell;
import org.springframework.roo.support.util.Assert;

@ScopeDevelopmentShell
public class MetadataCommands implements CommandMarker {
	
	private MetadataService metadataService;
	private MetadataDependencyRegistry metadataDependencyRegistry;
	private PhysicalTypeMetadataProvider physicalTypeMetadataProvider;
	private ItdMetadataScanner itdMetadataScanner;
	
	public MetadataCommands(MetadataService metadataService, MetadataDependencyRegistry metadataDependencyRegistry, PhysicalTypeMetadataProvider physicalTypeMetadataProvider, ItdMetadataScanner itdMetadataScanner) {
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(metadataDependencyRegistry, "Metadata dependency registry required");
		Assert.notNull(physicalTypeMetadataProvider, "Physical type metadata provider required");
		Assert.notNull(itdMetadataScanner, "ITD metadata scanner required");
		this.metadataService = metadataService;
		this.metadataDependencyRegistry = metadataDependencyRegistry;
		this.physicalTypeMetadataProvider = physicalTypeMetadataProvider;
		this.itdMetadataScanner = itdMetadataScanner;
	}

	@CliCommand(value="metadata trace", help="Traces metadata event delivery notifications (0=none, 1=some, 2=all)")
	public void metadataTrace(@CliOption(key={"","level"}, mandatory=true) int level) {
		metadataDependencyRegistry.setTrace(level);
	}
	
	@CliCommand(value="metadata summary", help="Shows statistics on the metadata system")
	public String metadataSummary() {
		return metadataService.toString();
	}
	
	@CliCommand(value="metadata for id", help="Shows detailed information about the metadata item")
	public String metadataForId(@CliOption(key={"", "metadataId"}, mandatory=true) String metadataId) {
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
	public String metadataForType(@CliOption(key={"", "type"}, mandatory=true) JavaType javaType) {
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
