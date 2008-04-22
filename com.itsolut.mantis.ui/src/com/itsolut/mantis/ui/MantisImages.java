package com.itsolut.mantis.ui;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;

public class MantisImages {
	private static ImageRegistry imageRegistry;

	private static final URL baseURL = MantisUIPlugin.getDefault().getBundle().getEntry("/icons/");
	
	public static final String T_VIEW = "eview16";
	
	public static final ImageDescriptor OVERLAY_CRITICAL = create(T_VIEW, "overlay-critical.gif");

	public static final ImageDescriptor OVERLAY_MAJOR = create(T_VIEW, "overlay-major.gif");

	public static final ImageDescriptor OVERLAY_ENHANCEMENT = create(T_VIEW, "overlay-enhancement.gif");

	public static final ImageDescriptor OVERLAY_MINOR = create(T_VIEW, "overlay-minor.gif");

	private static ImageDescriptor create(String prefix, String name) {
		try {
			return ImageDescriptor.createFromURL(makeIconFileURL(prefix, name));
		} catch (MalformedURLException e) {
			return ImageDescriptor.getMissingImageDescriptor();
		}
	}
	
	private static URL makeIconFileURL(String prefix, String name) throws MalformedURLException {
		if (baseURL == null)
			throw new MalformedURLException();

		StringBuffer buffer = new StringBuffer(prefix);
		if (prefix != "")
			buffer.append('/');
		buffer.append(name);
		return new URL(baseURL, buffer.toString());
	}

	private static ImageRegistry getImageRegistry() {
		if (imageRegistry == null) {
			imageRegistry = new ImageRegistry();
		}

		return imageRegistry;
	}

	/**
	 * Lazily initializes image map.
	 */
	public static Image getImage(ImageDescriptor imageDescriptor) {
		ImageRegistry imageRegistry = getImageRegistry();
		Image image = imageRegistry.get("" + imageDescriptor.hashCode());
		if (image == null) {
			image = imageDescriptor.createImage();
			imageRegistry.put("" + imageDescriptor.hashCode(), image);
		}
		return image;
	}
	
}
