package com.randomnoun.common.jna;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.randomnoun.common.XmlUtil;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

/** A class to convert the Win32 windows tree into a DOM object
 * 
 * @see <a href="http://www.randomnoun.com/wp/2012/12/26/automating-windows-from-java-and-windowtreedom/">http://www.randomnoun.com/wp/2012/12/26/automating-windows-from-java-and-windowtreedom/</a>
 * @author knoxg
 */
public class WindowTreeDom {

	// the User32 functions we invoke from this class
	public interface User32 extends StdCallLibrary {
      User32 INSTANCE = (User32) Native.loadLibrary("user32", User32.class, 
        W32APIOptions.DEFAULT_OPTIONS);
      
      public static final DWORD GW_OWNER = new DWORD(4);
      boolean EnumWindows(WinUser.WNDENUMPROC lpEnumFunc, Pointer arg);
      boolean EnumChildWindows(HWND hWnd, WNDENUMPROC lpEnumFunc, Pointer data);
      int GetWindowText(HWND hWnd, char[] lpString, int nMaxCount);
      int GetClassName(HWND hWnd, char[] lpClassName, int nMaxCount);
      public HWND GetWindow(HWND hWnd, DWORD cmd);
      HWND GetParent(HWND hWnd);
    }
	
	/** JNA interface to USER32.DLL */
	final static User32 lib = User32.INSTANCE;

	/** Logger instance for this class */
	static Logger logger = Logger.getLogger(WindowTreeDom.class);
	
	/** WindowTreeDom constructor.
	 * 
	 * @see #getDom()
	 */
	public WindowTreeDom() {
		
	}
	
	/** This callback is invoked for each window found. It generates XML 
	 * {#link org.w3c.Element}s for each window, and attaches them to the supplied 
	 * {#link org.w3c.Document}.
	 * 
	 */
	private static class WindowCallback implements WinUser.WNDENUMPROC {
		Document doc;
		Element documentElement;
		Element topLevelWindow; 
		Map<String, Element> hwndMap = new HashMap<String, Element>();
		
		/** Creates a new window callback
		 * 
		 * @param doc The XML document populated by this callback.
		 * @param topLevelHWND If non-null, the windows being returned should all be
		 *   child windows of this HWND (via EnumChildWindows), otherwise it is
		 *   assumed toplevel windows are returned (via EnumWindows)
		 * @param topLevelWindow If non-null, the document Element within <tt>doc</tt>
		 *   which will contain new child elements.
		 */
		public WindowCallback(Document doc, HWND topLevelHWND, Element topLevelWindow) {
			this.doc = doc;
			this.topLevelWindow = topLevelWindow;
			if (topLevelWindow != null) {
				hwndMap.put(topLevelHWND.getPointer().toString(), topLevelWindow);
			}
			documentElement = doc.getDocumentElement();
		}
		
		public boolean callback(HWND hWnd, Pointer data) {
			
			char[] buffer = new char[512];
			User32.INSTANCE.GetWindowText(hWnd, buffer, 512);
			
			char[] buffer2 = new char[1026];
			int classLen = User32.INSTANCE.GetClassName(hWnd, buffer2, 1026);
		    
			String windowTitle = Native.toString(buffer);
			String className = Native.toString(buffer2);

			HWND parent = User32.INSTANCE.GetParent(hWnd);
			HWND owner = User32.INSTANCE.GetWindow(hWnd, User32.GW_OWNER);
			
		    // check if this has already been created in the DOM
			Element el = hwndMap.get(hWnd.getPointer().toString());
			if (el==null) {
				el = doc.createElement("window");
			} else {
				el.removeAttribute("pwindow");
			}
			el.setAttribute("hwnd", hWnd.getPointer().toString());
			if (owner!=null) {
				el.setAttribute("owner", owner.getPointer().toString());
			}
			el.setAttribute("title", windowTitle);
			el.setAttribute("class", className);
			
			hwndMap.put(hWnd.getPointer().toString(), el);
			if (topLevelWindow==null) { 
				// this is a real top level element, so enumerate its children
				WindowCallback childDommer = new WindowCallback(doc, hWnd, el);
				// this code relies on being able to enum child windows whilst enumming toplevel windows
				lib.EnumChildWindows (hWnd, childDommer, new Pointer(0));
				try {
					childDommer.checkForOrphanedWindows();
				} catch (TransformerException e) {
					logger.error("Problem serialising orphaned windows to XML", e);
				}
			}
			
			if (parent==null) {
				documentElement.appendChild(el);
				if (topLevelWindow!=null) {
					// have seen VMDragDetectWndClass'es here, presumably a vmware thing
					// (note that this window won't be in the parent callback's hwndMap)
					try {
						logger.warn("Toplevel child window found: " + XmlUtil.getXmlString(el, true));
					} catch (TransformerException e) {
						logger.error("Toplevel child window found, problem serialising toplevel windows to XML", e);
					}
				}
				
			} else {
				Element parentEl = hwndMap.get(parent.getPointer().toString());
				if (parentEl==null) {
					// throw new IllegalStateException("Unknown parent window '" + parent.getPointer().toString() + "'");
					// it appears that we can get IME child windows being returned 
					// by EnumWindows, even though they're not top-level
					parentEl = doc.createElement("window");
					parentEl.setAttribute("pwindow", "true");
					parentEl.setAttribute("hwnd", parent.getPointer().toString());
					hwndMap.put(parent.getPointer().toString(), parentEl); 
				}
				parentEl.appendChild(el);
			}
			
			return true;
		}
		
		/** Lists any window nodes that were generated via enumeration, whose 
		 * parent nodes were not generated.
		 * 
		 * @throws TransformerException
		 */
		public void checkForOrphanedWindows() throws TransformerException {
			for (Element e : hwndMap.values()) {
				if (!e.getAttribute("pwindow").equals("")) {
					// the desktop window isn't in the enumeration
					logger.warn("Parent window found that was not in enumeration: " + XmlUtil.getXmlString(e, true));
					// throw new IllegalStateException("Window found without parent window");
				}
			}
		}

	}
	
	/** Generate an XML document from the Win32 window tree */
	public Document getDom() throws ParserConfigurationException, TransformerException {
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		Document doc = docBuilder.newDocument();
		Element topElement = doc.createElement("windows");
		doc.appendChild(topElement);
		
		WindowCallback dommer = new WindowCallback(doc, null, null);
		lib.EnumWindows (dommer, new Pointer(0));
		dommer.checkForOrphanedWindows();
		
		return doc;
	}
	
	/** Return the hwnd of an element, as a pointer represented as a long 
	 * 
	 * @param windowEl a window element returned from getDom()
	 * 
	 * @return the hwnd of the element.
	 */
	public HWND getHwnd(Element windowEl) {
        String hwndString = windowEl.getAttribute("hwnd");
        if (hwndString.startsWith("native@0x")) {
        	return new HWND(new Pointer(Long.parseLong(hwndString.substring(9), 16)));
        } else {
        	throw new IllegalStateException("Could not determine HWND of window element: found '" + hwndString + "'");
        }
	}
	
}
