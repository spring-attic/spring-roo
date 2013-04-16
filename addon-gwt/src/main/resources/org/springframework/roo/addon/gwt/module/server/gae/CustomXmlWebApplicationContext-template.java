package __TOP_LEVEL_PACKAGE__.__SEGMENT_PACKAGE__;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.web.context.support.XmlWebApplicationContext;

import com.google.appengine.api.utils.SystemProperty;

public class CustomXmlWebApplicationContext extends XmlWebApplicationContext
{
	protected void initBeanDefinitionReader(XmlBeanDefinitionReader beanDefinitionReader)
	{
		super.initBeanDefinitionReader(beanDefinitionReader);
		if (SystemProperty.environment.value() == SystemProperty.Environment.Value.Production)
		{
			beanDefinitionReader.setValidating(false);
		}
		beanDefinitionReader.setValidating(false);
	}
}