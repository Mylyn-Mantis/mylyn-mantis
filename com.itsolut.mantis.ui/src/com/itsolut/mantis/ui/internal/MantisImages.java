package com.itsolut.mantis.ui.internal;


import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;

import com.itsolut.mantis.ui.MantisUIPlugin;

public class MantisImages {

	private static final URL baseURL = MantisUIPlugin.getDefault().getBundle().getEntry("/icons/");
	
	public static final String T_VIEW = "eview16";
	
	public static final ImageDescriptor OVERLAY_CRITICAL = create(T_VIEW, "overlay-critical.gif");

	public static final ImageDescriptor OVERLAY_MAJOR = create(T_VIEW, "overlay-major.gif");

	public static final ImageDescriptor OVERLAY_ENHANCEMENT = create(T_VIEW, "overlay-enhancement.gif");

	public static final ImageDescriptor OVERLAY_MINOR = create(T_VIEW, "overlay-minor.gif");
	
	public static final ImageDescriptor WIZARD = create(T_VIEW, "wizban/mantis_logo_button.gif");
	
	public static final ImageDescriptor PRE = create("editor", "pre.png");

	private static ImageDescriptor create(String prefix, String name) {
		try {
			return ImageDescriptor.createFromURL(makeIconFileURL(prefix, name));
		} catch (MalformedURLException e) {
			return ImageDescriptor.getMissingImageDescriptor();
		}
	}
	
	public static URL makeIconFileURL(String prefix, String name) throws MalformedURLException {

		StringBuffer buffer = new StringBuffer(prefix);
		if (!"".equals(prefix))
			buffer.append('/');
		buffer.append(name);
		return new URL(baseURL, buffer.toString());
	}
	
}
