package org.springframework.roo.addon.web.mvc.controller;

import org.w3c.dom.Document;

public interface XmlFileManager {

	void writeToDiskIfNecessary(String filename, Document proposed);

}
