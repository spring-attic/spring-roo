package org.springframework.roo.addon.jpa.activerecord;

import static org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord.CLEAR_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord.COUNT_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord.FIND_ALL_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord.FIND_ENTRIES_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord.FIND_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord.FIND_ENTRIES_SORTED_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord.FIND_ALL_SORTED_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord.FLUSH_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord.MERGE_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord.PERSIST_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord.REMOVE_METHOD_DEFAULT;
import static org.springframework.roo.model.RooJavaType.ROO_JPA_ACTIVE_RECORD;

import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.classpath.itd.MemberHoldingTypeDetailsMetadataItem;

/**
 * The purely CRUD-related values of a parsed {@link RooJpaActiveRecord}
 * annotation.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class JpaCrudAnnotationValues extends AbstractAnnotationValues {

    @AutoPopulate private String clearMethod = CLEAR_METHOD_DEFAULT;
    @AutoPopulate private String countMethod = COUNT_METHOD_DEFAULT;
    @AutoPopulate private String findAllMethod = FIND_ALL_METHOD_DEFAULT;
    @AutoPopulate private String findEntriesMethod = FIND_ENTRIES_METHOD_DEFAULT;
    @AutoPopulate private String findAllSortedMethod = FIND_ALL_SORTED_METHOD_DEFAULT;
    @AutoPopulate private String findEntriesSortedMethod = FIND_ENTRIES_SORTED_METHOD_DEFAULT;
    @AutoPopulate private String[] finders;
    @AutoPopulate private String findMethod = FIND_METHOD_DEFAULT;
    @AutoPopulate private String flushMethod = FLUSH_METHOD_DEFAULT;
    @AutoPopulate private String mergeMethod = MERGE_METHOD_DEFAULT;
    @AutoPopulate private String persistenceUnit = "";
    @AutoPopulate private String persistMethod = PERSIST_METHOD_DEFAULT;
    @AutoPopulate private String removeMethod = REMOVE_METHOD_DEFAULT;
    @AutoPopulate private String transactionManager = "";

    /**
     * Constructor
     * 
     * @param annotatedType
     */
    public JpaCrudAnnotationValues(
            final MemberHoldingTypeDetailsMetadataItem<?> annotatedType) {
        super(annotatedType, ROO_JPA_ACTIVE_RECORD);
        AutoPopulationUtils.populate(this, annotationMetadata);
    }

    public String getClearMethod() {
        return clearMethod;
    }

    public String getCountMethod() {
        return countMethod;
    }
    
    public String getFindAllMethod() {
        return findAllMethod;
    } 
    
    public String getFindAllSortedMethod() {
        return findAllSortedMethod;
    }

    /**
     * Returns the prefix for the "find entries" method, e.g. the "find" part of
     * "findFooEntries"
     * 
     * @return
     */
    public String getFindEntriesMethod() {
        return findEntriesMethod;
    }
    
    public String getFindEntriesSortedMethod() {
        return findEntriesSortedMethod;
    }

    /**
     * Returns the custom finder names specified by the annotation
     * 
     * @return
     */
    public String[] getFinders() {
        return finders;
    }

    public String getFindMethod() {
        return findMethod;
    }

    public String getFlushMethod() {
        return flushMethod;
    }

    public String getMergeMethod() {
        return mergeMethod;
    }

    public String getPersistenceUnit() {
        return persistenceUnit;
    }

    public String getPersistMethod() {
        return persistMethod;
    }

    public String getRemoveMethod() {
        return removeMethod;
    }

    public String getTransactionManager() {
        return transactionManager;
    }
}
