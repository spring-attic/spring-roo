package org.springframework.roo.addon.jpa;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.springframework.roo.addon.jpa.identifier.Identifier;
import org.springframework.roo.addon.jpa.identifier.IdentifierService;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.model.JavaType;

/**
 * Abstract class to make {@link IdentifierService} collection available to
 * subclasses.
 * 
 * @author Alan Stewart
 * @author Ben Alex
 * @since 1.1
 */
@Component(componentAbstract = true)
@Reference(name = "identifierService", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = IdentifierService.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
public abstract class AbstractIdentifierServiceAwareMetadataProvider extends
        AbstractItdMetadataProvider {

    private final Set<IdentifierService> identifierServices = new HashSet<IdentifierService>();

    protected void bindIdentifierService(
            final IdentifierService identifierService) {
        synchronized (identifierServices) {
            identifierServices.add(identifierService);
        }
    }

    /**
     * Locates any {@link Identifier} that is applicable to this
     * {@link JavaType}.
     * <p>
     * See {@link IdentifierService#getIdentifiers(JavaType)} for the full
     * contract of what this method returns. Note this method simply returns the
     * first non-null result of invoking
     * {@link IdentifierService#getIdentifiers(JavaType)}. It returns null if no
     * provider is authoritative.
     * 
     * @param javaType the entity or PK identifier class for which column
     *            information is desired (required)
     * @return the applicable identifiers, or null if no registered
     *         {@link IdentifierService} was authoritative for this type TODO
     *         made obsolete by {@link PersistenceMemberLocator}?
     */
    protected List<Identifier> getIdentifiersForType(final JavaType javaType) {
        List<Identifier> identifierServiceResult = null;
        synchronized (identifierServices) {
            for (final IdentifierService service : identifierServices) {
                identifierServiceResult = service.getIdentifiers(javaType);
                if (identifierServiceResult != null) {
                    // Someone has authoritatively indicated the fields for this
                    // PK, so we don't need to continue looping
                    break;
                }
            }
        }
        return identifierServiceResult;
    }

    protected void unbindIdentifierService(
            final IdentifierService identifierService) {
        synchronized (identifierServices) {
            identifierServices.remove(identifierService);
        }
    }
}