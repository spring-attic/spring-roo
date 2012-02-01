package org.springframework.roo.addon.jpa.activerecord;

import static org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord.CLEAR_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord.COUNT_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord.FIND_ALL_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord.FIND_ENTRIES_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord.FIND_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord.FLUSH_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord.MERGE_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord.PERSIST_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord.REMOVE_METHOD_DEFAULT;
import static org.springframework.roo.model.RooJavaType.ROO_JPA_ACTIVE_RECORD;

import org.apache.commons.lang3.StringUtils;
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
        return StringUtils.defaultIfEmpty(clearMethod, CLEAR_METHOD_DEFAULT);
    }

    public String getCountMethod() {
        return StringUtils.defaultIfEmpty(countMethod, COUNT_METHOD_DEFAULT);
    }

    public String getFindAllMethod() {
        return StringUtils.defaultIfEmpty(findAllMethod,
                FIND_ALL_METHOD_DEFAULT);
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

    /**
     * Returns the custom finder names specified by the annotation
     * 
     * @return
     */
    public String[] getFinders() {
        return finders;
    }

    public String getFindMethod() {
        return StringUtils.defaultIfEmpty(findMethod, FIND_METHOD_DEFAULT);
    }

    public String getFlushMethod() {
        return StringUtils.defaultIfEmpty(flushMethod, FLUSH_METHOD_DEFAULT);
    }

    public String getMergeMethod() {
        return StringUtils.defaultIfEmpty(mergeMethod, MERGE_METHOD_DEFAULT);
    }

    public String getPersistenceUnit() {
        return persistenceUnit;
    }

    public String getPersistMethod() {
        return StringUtils
                .defaultIfEmpty(persistMethod, PERSIST_METHOD_DEFAULT);
    }

    public String getRemoveMethod() {
        return StringUtils.defaultIfEmpty(removeMethod, REMOVE_METHOD_DEFAULT);
    }

    public String getTransactionManager() {
        return transactionManager;
    }
}
