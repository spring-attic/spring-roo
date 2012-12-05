package org.springframework.roo.addon.layers.service;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;

public interface TemplateService
{
	public void buildServiceXmlConfiguration(ClassOrInterfaceTypeDetails serviceInterface, ClassOrInterfaceTypeDetails serviceClass);
}
