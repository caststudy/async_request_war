/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.servlet.async_request_war;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This class illustrates the usage of Servlet 3.0 asynchronization APIs. It is
 * ported from Grizzly Comet sample and use Servlet 3.0 API here.
 *
 *
 * @author Shing Wai Chan
 * @author JeanFrancois Arcand
 */
@WebServlet(urlPatterns = { "/chat" }, asyncSupported = true)
public class AjaxCometServlet extends HttpServlet {
	private static final Map<String,AsyncContext> map=new HashMap<>();

	private static final Queue<AsyncContext> queue = new ConcurrentLinkedQueue<AsyncContext>();

	private static final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<String>();

	private static final String BEGIN_SCRIPT_TAG = "<script type='text/javascript'>\n";

	private static final String END_SCRIPT_TAG = "</script>\n";

	private static final long serialVersionUID = -2919167206889576860L;

	private static final String JUNK = "<!-- Comet is a programming technique that enables web "
			+ "servers to send data to the client without having any need " + "for the client to request it. -->\n";

	private Thread notifierThread = null;

	@Override
	public void init() throws ServletException {
		// TODO Auto-generated method stub
		// super.init();
		System.out.println("init().. 2..");
	}

	@Override
	public void init(ServletConfig config) throws ServletException {

		System.out.println("int(config).. 1...");
		super.init(config);
		Runnable notifierRunnable = new Runnable() {
			public void run() {
				boolean done = false;
				while (!done) {
					String cMessage = null;
					try {
						cMessage = messageQueue.take();
						for (AsyncContext ac : queue) {
							try {
//								System.out.println("run:msg=" + cMessage + ";count=" + queue.size());
					
								PrintWriter acWriter = ac.getResponse().getWriter();
								// 向浏览器输出一条js语句
								acWriter.println(cMessage);
								acWriter.flush();
							} catch (SocketException e) {
//								queue.remove(ac);
								System.out.println("SocketE:" + e.getMessage());
							} catch (IOException ex) {
								System.out.println("IOE:" + ex.getMessage());
//								queue.remove(ac);
							} catch (Exception e) {
								queue.remove(ac);
								System.out.println("E:" + e.getMessage());
							}
						}
					} catch (InterruptedException iex) {
						done = true;
						System.out.println("Interrupt:"+iex);
					}
				}
			}
		};
		notifierThread = new Thread(notifierRunnable);
		notifierThread.start();
	}

	private void addAsyncContext(HttpServletRequest req) {
		// 调用了startAsync()方法取得AsyncContext对象之后，此次请求的响应会被延后
		final AsyncContext ac = req.startAsync();
		String name=req.getParameter("name");
		map.put(name, ac);
		System.out.println("get:AsyscContext=" + ac+",name="+name);
		ac.setTimeout(10 * 60 * 1000);
		ac.addListener(new AsyncListener() {
			public void onComplete(AsyncEvent event) throws IOException {
				System.out.println("doGet onComplete.....");
//				queue.remove(ac);
			}

			public void onTimeout(AsyncEvent event) throws IOException {
				System.out.println("doGet onTimeout.....");
//				queue.remove(ac);
			}

			public void onError(AsyncEvent event) throws IOException {
				System.out.println("doGet onError.....");
//				queue.remove(ac);
			}

			public void onStartAsync(AsyncEvent event) throws IOException {
				System.out.println("doGet onStartAsync.....");

			}
		});
		queue.add(ac);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		res.setContentType("text/html");
		res.setHeader("Cache-Control", "no-cache");// 浏览器缓存
		res.setHeader("Pragma", "no-cache");// 兼容 HTTP/1.0 客户端
		
		// PrintWriter writer = res.getWriter();
		// // for Safari, Chrome, IE and Opera
		// for (int i = 0; i < 10; i++) {
		// writer.write(JUNK);
		// }
		// //
		// writer.write(BEGIN_SCRIPT_TAG+"alert('"+req.getParameter("msg")+"')"+END_SCRIPT_TAG);
		// writer.flush();
		addAsyncContext(req);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		res.setContentType("text/plain");
		res.setHeader("Cache-Control", "no-cache");
		res.setHeader("Pragma", "no-cache");
		req.setCharacterEncoding("UTF-8");		
		res.setCharacterEncoding("UTF-8");		
		String action = req.getParameter("action");
		String name = req.getParameter("name");
		
//		System.out.println("post--------------"+req.getSession().getId());

		if ("login".equals(action)) {
			res.getWriter().print("{status:'success',name:"+name+"}");
			
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					try {
						Thread.sleep(50);  //20190626 add
						System.out.println("login add msg");
						String cMessage = BEGIN_SCRIPT_TAG + toJsonp("System Message", name + " has joined.") + END_SCRIPT_TAG;
						AjaxCometServlet.this.notify(cMessage);	
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			}).start();
			
			
			
		} else if ("msg".equals(action)) {			
			String message = req.getParameter("message");
			try {
				map.get(name).getRequest();
				String cMessage = BEGIN_SCRIPT_TAG + toJsonp(name, message) + END_SCRIPT_TAG;
				notify(cMessage);
			} catch (Exception e) {
				res.getWriter().print("{status:'failure',name:"+name+"}");			
			}		

			
		} else {
			res.sendError(422, "Unprocessable Entity");
		}
	}

	@Override
	public void destroy() {
		queue.clear();
		notifierThread.interrupt();
	}

	private void notify(String cMessage) throws IOException {
		try {
			messageQueue.put(cMessage);
		} catch (Exception ex) {
			IOException t = new IOException();
			t.initCause(ex);
			throw t;
		}
	}

	private String escape(String orig) {
		StringBuffer buffer = new StringBuffer(orig.length());

		for (int i = 0; i < orig.length(); i++) {
			char c = orig.charAt(i);
			switch (c) {
			case '\b':
				buffer.append("\\b");
				break;
			case '\f':
				buffer.append("\\f");
				break;
			case '\n':
				buffer.append("<br />");
				break;
			case '\r':
				// ignore
				break;
			case '\t':
				buffer.append("\\t");
				break;
			case '\'':
				buffer.append("\\'");
				break;
			case '\"':
				buffer.append("\\\"");
				break;
			case '\\':
				buffer.append("\\\\");
				break;
			case '<':
				buffer.append("&lt;");
				break;
			case '>':
				buffer.append("&gt;");
				break;
			case '&':
				buffer.append("&amp;");
				break;
			default:
				buffer.append(c);
			}
		}

		return buffer.toString();
	}

	private String toJsonp(String name, String message) {
		return "window.parent.app.update({ name: \"" + escape(name) + "\", message: \"" + escape(message) + "\" });\n";
	}
}
