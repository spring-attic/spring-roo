package org.springframework.roo.uaa;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.uaa.client.UaaService;
import org.springframework.uaa.client.internal.UaaServiceImpl;

/**
 * Makes {@link UaaService} available as an OSGi component.
 * 
 * @author Ben Alex
 * @since 1.1.1
 *
 */
@Service
@Component
public class UaaServiceComponent extends UaaServiceImpl {}
