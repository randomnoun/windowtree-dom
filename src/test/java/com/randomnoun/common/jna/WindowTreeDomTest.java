package com.randomnoun.common.jna;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import com.randomnoun.common.XmlUtil;
import com.randomnoun.common.log4j2.Log4j2CliConfiguration;

/** Unit test for WindowTreeDom 
 *
 * @blog http://www.randomnoun.com/wp/2012/12/26/automating-windows-from-java-and-windowtreedom/
 **/
public class WindowTreeDomTest extends TestCase {
	
	Logger logger = Logger.getLogger(WindowTreeDomTest.class);
	
	public void testWindowTreeDom() throws ParserConfigurationException, TransformerException, IOException {
		if (System.getProperty("os.name").startsWith("Windows")) {
			WindowTreeDom wtd = new WindowTreeDom();
			Document d = wtd.getDom();
			XmlUtil.getXmlString(d, true);
			// logger.info(XmlUtil.getXmlString(d, true));
		} else {
			logger.info("Not running tests on operating sytem '" + System.getProperty("os.name") + "'");
		}
	}
	
	public static void main(String args[]) throws ParserConfigurationException, IOException, TransformerException {
		Log4j2CliConfiguration lcc = new Log4j2CliConfiguration();
		lcc.init("", null);
		WindowTreeDomTest wtdt = new WindowTreeDomTest();
		wtdt.testWindowTreeDom();
	}

}
