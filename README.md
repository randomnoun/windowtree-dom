[![Maven Central](https://img.shields.io/maven-central/v/com.randomnoun.microsoft/windowtree-dom.svg)](https://search.maven.org/artifact/com.randomnoun.microsoft/windowtree-dom)

# windowtree-dom

Expose the window hierarchy in Windows™ as an XML document

There's only one class of note in this project: [WindowTreeDom](https://github.com/randomnoun/windowtree-dom/blob/main/src/main/java/com/randomnoun/common/jna/WindowTreeDom.java), which has a blog writeup [here](http://www.randomnoun.com/wp/2012/12/26/automating-windows-from-java-and-windowtreedom/).

The idea behind it was that it was easier to query and interrogate an XML DOM rather than navigating the windows™ windows hierarchy directly as XML has things like XPath. The XML DOM was a read-only view of that hierarchy.

It used to live in the [common-public](https://github.com/randomnoun/common-public) project, but it was then causing all projects that use that project to add [JNA](https://github.com/java-native-access/jna) as a dependency, which was a bit of overkill for a toy class that I only used once to automate some email clients.

So I've split it out here.

I might document this a bit better later.

# LICENSE

windowtree-dom is licensed under the BSD 2-clause license.




