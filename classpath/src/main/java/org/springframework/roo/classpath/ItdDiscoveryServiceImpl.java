package org.springframework.roo.classpath;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.StringUtils;

/**
 * Implementation of {@link ItdDiscoveryService}.
 *
 * @author James Tyrrell
 * @since 1.2.0
 */
@Component(immediate = true)
@Service
public class ItdDiscoveryServiceImpl implements ItdDiscoveryService {

	// Fields
	private final Map<String, HashMap<String, MemberHoldingTypeDetails>> typeMap = new HashMap<String, HashMap<String, MemberHoldingTypeDetails>>();
	private final Map<String, String> itdIdToTypeMap = new HashMap<String, String>();
	private final Map<String, Set<String>> changeMap = new HashMap<String, Set<String>>();


	public void addItdTypeDetails(ItdTypeDetails itdTypeDetails) {
		if (itdTypeDetails == null || itdTypeDetails.getGovernor() == null) {
			return;
		}
		if (typeMap.get(itdTypeDetails.getGovernor().getName().getFullyQualifiedTypeName()) == null) {
			typeMap.put(itdTypeDetails.getGovernor().getName().getFullyQualifiedTypeName(), new HashMap<String, MemberHoldingTypeDetails>());
		}
		itdIdToTypeMap.put(itdTypeDetails.getDeclaredByMetadataId(), itdTypeDetails.getGovernor().getName().getFullyQualifiedTypeName());
		typeMap.get(itdTypeDetails.getGovernor().getName().getFullyQualifiedTypeName()).put(itdTypeDetails.getDeclaredByMetadataId(), itdTypeDetails);
		updateChanges(itdTypeDetails.getGovernor().getName(), false);
	}

	public void removeItdTypeDetails(String itdTypeDetailsId) {
		if (!StringUtils.hasText(itdTypeDetailsId)) {
			return;
		}
		String type = itdIdToTypeMap.get(itdTypeDetailsId);
		if (type != null) {
			Map<String, MemberHoldingTypeDetails> typeDetailsHashMap = typeMap.get(type);
			if (typeDetailsHashMap != null) {
				typeDetailsHashMap.remove(itdTypeDetailsId);
			}
			updateChanges(new JavaType(type), true);
		}
	}

	public boolean haveItdsChanged(String requestingClass, JavaType javaType) {
		Set<String> changesSinceLastRequest = changeMap.get(requestingClass);
		if (changesSinceLastRequest == null) {
			changesSinceLastRequest = new LinkedHashSet<String>(typeMap.keySet());
			changeMap.put(requestingClass, changesSinceLastRequest);
		}
		for (String changedId : changesSinceLastRequest) {
			if (changedId.equals(javaType.getFullyQualifiedTypeName())) {
				changesSinceLastRequest.remove(changedId);
				return true;
			}
		}
		return false;
	}

	private void updateChanges(JavaType javaType, boolean remove) {
		for (String requestingClass : changeMap.keySet()) {
			if (remove) {
				changeMap.get(requestingClass).remove(javaType.getFullyQualifiedTypeName());
			} else {
				changeMap.get(requestingClass).add(javaType.getFullyQualifiedTypeName());
			}
		}
	}
}
