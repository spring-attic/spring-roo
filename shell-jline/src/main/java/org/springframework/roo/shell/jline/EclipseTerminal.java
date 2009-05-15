package org.springframework.roo.shell.jline;

import jline.UnsupportedTerminal;

public class EclipseTerminal extends UnsupportedTerminal {

	@Override
	public boolean isANSISupported() {
		return false;
	}

}
