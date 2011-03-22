package com.trifork.stamdata.replication.gui.controllers;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.inject.Inject;
import freemarker.template.*;


public class PageRenderer {

	protected final Configuration templates;

	@Inject
	PageRenderer(Configuration templates) {

		this.templates = templates;
	}

	protected void render(String templatePath, Map<String, Object> vars, HttpServletRequest request, HttpServletResponse response) throws IOException {

		response.setContentType("text/html; charset=utf-8");

		if (vars == null) {
			vars = new HashMap<String, Object>();
		}

		vars.put("contextRoot", request.getContextPath());

		try {
			Template page = templates.getTemplate(templatePath, "UTF-8");

			StringWriter bodyWriter = new StringWriter();
			page.process(vars, bodyWriter);
			vars.put("body", bodyWriter.toString());
			bodyWriter.close();

			Template html = templates.getTemplate("application.ftl");

			PrintWriter w = response.getWriter();
			html.process(vars, w);
		}
		catch (TemplateException e) {
			throw new IOException(e);
		}
	}
}