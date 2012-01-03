package org.springframework.roo.addon.javabean;

import org.springframework.roo.classpath.details.annotations.populator.AnnotationValuesTestCase;

/**
 * Unit test of {@link JavaBeanAnnotationValues}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class JavaBeanAnnotationValuesTest extends
        AnnotationValuesTestCase<RooJavaBean, JavaBeanAnnotationValues> {

    @Override
    protected Class<RooJavaBean> getAnnotationClass() {
        return RooJavaBean.class;
    }

    @Override
    protected Class<JavaBeanAnnotationValues> getValuesClass() {
        return JavaBeanAnnotationValues.class;
    }
}
