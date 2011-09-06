package org.springframework.roo.classpath;

import java.util.HashMap;
import java.util.LinkedHashSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.model.JavaType;

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
	private HashMap<String, HashMap<String, MemberHoldingTypeDetails>> typeMap = new HashMap<String, HashMap<String, MemberHoldingTypeDetails>>();

	public void addItdTypeDetails(ItdTypeDetails itdTypeDetails) {
		if (itdTypeDetails == null || itdTypeDetails.getGovernor() == null) {
			return;
		}
		if (typeMap.get(itdTypeDetails.getGovernor().getName().getFullyQualifiedTypeName()) == null) {
			typeMap.put(itdTypeDetails.getGovernor().getName().getFullyQualifiedTypeName(), new HashMap<String, MemberHoldingTypeDetails>());
		}

		typeMap.get(itdTypeDetails.getGovernor().getName().getFullyQualifiedTypeName()).put(itdTypeDetails.getDeclaredByMetadataId(), itdTypeDetails);
		updateChanges(itdTypeDetails.getGovernor().getName(), false);
	}

	public void removeItdTypeDetails(ItdTypeDetails itdTypeDetails) {
		if (itdTypeDetails == null || itdTypeDetails.getGovernor() == null) {
			return;
		}

		typeMap.remove(itdTypeDetails.getGovernor().getName().getFullyQualifiedTypeName());
		updateChanges(itdTypeDetails.getGovernor().getName(), true);
	}

	private final HashMap<String, LinkedHashSet<String>> changeMap = new HashMap<String, LinkedHashSet<String>>();

	public boolean haveItdsChanged(String requestingClass, JavaType javaType) {
		LinkedHashSet<String> changesSinceLastRequest = changeMap.get(requestingClass);
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
