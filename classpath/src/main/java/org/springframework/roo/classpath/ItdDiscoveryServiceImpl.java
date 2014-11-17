package org.springframework.roo.classpath;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
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
@Component
@Service
public class ItdDiscoveryServiceImpl implements ItdDiscoveryService {

    private final Map<String, Set<String>> changeMap = new HashMap<String, Set<String>>();
    private final Map<String, String> itdIdToTypeMap = new HashMap<String, String>();
    private final Map<String, Map<String, MemberHoldingTypeDetails>> typeMap = new HashMap<String, Map<String, MemberHoldingTypeDetails>>();

    public void addItdTypeDetails(final ItdTypeDetails itdTypeDetails) {
        if (itdTypeDetails == null || itdTypeDetails.getGovernor() == null) {
            return;
        }
        if (typeMap.get(itdTypeDetails.getGovernor().getName()
                .getFullyQualifiedTypeName()) == null) {
            typeMap.put(itdTypeDetails.getGovernor().getName()
                    .getFullyQualifiedTypeName(),
                    new HashMap<String, MemberHoldingTypeDetails>());
        }
        itdIdToTypeMap.put(itdTypeDetails.getDeclaredByMetadataId(),
                itdTypeDetails.getGovernor().getName()
                        .getFullyQualifiedTypeName());
        typeMap.get(
                itdTypeDetails.getGovernor().getName()
                        .getFullyQualifiedTypeName()).put(
                itdTypeDetails.getDeclaredByMetadataId(), itdTypeDetails);
        updateChanges(itdTypeDetails.getGovernor().getName(), false);
    }

    public boolean haveItdsChanged(final String requestingClass,
            final JavaType javaType) {
        Set<String> changesSinceLastRequest = changeMap.get(requestingClass);
        if (changesSinceLastRequest == null) {
            changesSinceLastRequest = new LinkedHashSet<String>(
                    typeMap.keySet());
            changeMap.put(requestingClass, changesSinceLastRequest);
        }
        for (final String changedId : changesSinceLastRequest) {
            if (changedId.equals(javaType.getFullyQualifiedTypeName())) {
                changesSinceLastRequest.remove(changedId);
                return true;
            }
        }
        return false;
    }

    public void removeItdTypeDetails(final String itdTypeDetailsId) {
        if (StringUtils.isBlank(itdTypeDetailsId)) {
            return;
        }
        final String type = itdIdToTypeMap.get(itdTypeDetailsId);
        if (type != null) {
            final Map<String, MemberHoldingTypeDetails> typeDetailsHashMap = typeMap
                    .get(type);
            if (typeDetailsHashMap != null) {
                typeDetailsHashMap.remove(itdTypeDetailsId);
            }
            updateChanges(new JavaType(type), true);
        }
    }

    private void updateChanges(final JavaType javaType, final boolean remove) {
        for (final String requestingClass : changeMap.keySet()) {
            if (remove) {
                changeMap.get(requestingClass).remove(
                        javaType.getFullyQualifiedTypeName());
            }
            else {
                changeMap.get(requestingClass).add(
                        javaType.getFullyQualifiedTypeName());
            }
        }
    }
}
