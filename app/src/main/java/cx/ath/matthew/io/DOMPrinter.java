/*
 * Java DOM Printing Library
 *
 * Copyright (c) Matthew Johnson 2005
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * 
 * To Contact the author, please email src@matthew.ath.cx
 *
 */

package cx.ath.matthew.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Print a DOM tree to the given OutputStream
 */
public class DOMPrinter
{
   /**
    * Print the given node and all its children.
    * @param n The Node to print.
    * @param os The Stream to print to.
    */
   public static void printNode(Node n, OutputStream os)
   {
      PrintStream p = new PrintStream(os);
      printNode(n, p);
   }
   /**
    * Print the given node and all its children.
    * @param n The Node to print.
    * @param p The Stream to print to.
    */
   public static void printNode(Node n, PrintStream p)
   {
      if (null != n.getNodeValue()) p.print(n.getNodeValue());
      else {
         p.print("<"+n.getNodeName());      
         if (n.hasAttributes()) {
            NamedNodeMap nnm = n.getAttributes();
            for (int i = 0; i < nnm.getLength(); i++) {
               Node attr = nnm.item(i);
               p.print(" "+attr.getNodeName()+"='"+attr.getNodeValue()+"'");
            }
         }
         if (n.hasChildNodes()) {
            p.print(">");
            NodeList nl = n.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++)
               printNode(nl.item(i), p);
            p.print("</"+n.getNodeName()+">");
         } else {
            p.print("/>");
         }
      }
   }
   /**
    * Print the given document and all its children.
    * @param d The Document to print.
    * @param p The Stream to print to.
    */
   public static void printDOM(Document d, PrintStream p)
   {
      DocumentType dt = d.getDoctype();
      if (null != dt) {
         p.print("<!DOCTYPE "+dt.getName());
         String pub = dt.getPublicId();
         String sys = dt.getSystemId();
         if (null != pub) p.print(" PUBLIC \""+pub+"\" \""+sys+"\"");
         else if (null != sys) p.print(" SYSTEM \""+sys+"\"");
         p.println(">");
      }
      Element e = d.getDocumentElement();
      printNode(e, p);
   }
   /**
    * Print the given document and all its children.
    * @param d The Document to print.
    * @param os The Stream to print to.
    */
   public static void printDOM(Document d, OutputStream os)
   {
      PrintStream p = new PrintStream(os);
      printDOM(d, p);
   }
}

