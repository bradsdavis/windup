package org.jboss.windup.engine.visitor.reporter.html.renderer;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.jboss.windup.engine.WindupContext;
import org.jboss.windup.engine.visitor.base.EmptyGraphVisitor;
import org.jboss.windup.engine.visitor.reporter.html.model.ServerResourceReport;
import org.jboss.windup.engine.visitor.reporter.html.model.ServerResourceReport.DatabaseRow;
import org.jboss.windup.engine.visitor.reporter.html.model.ServerResourceReport.JMSQueueRow;
import org.jboss.windup.engine.visitor.reporter.html.model.ServerResourceReport.JMXRow;
import org.jboss.windup.graph.dao.JarArchiveDaoBean;
import org.jboss.windup.graph.model.resource.ArchiveResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import freemarker.template.Template;

public class ServerResourceReportRenderer extends EmptyGraphVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(ServerResourceReportRenderer.class);
	
	@Inject
	private WindupContext context;
	

	private final Configuration cfg;
	
	public ServerResourceReportRenderer() {
		cfg = new Configuration();
        cfg.setTemplateUpdateDelay(500);
        cfg.setClassForTemplateLoading(this.getClass(), "/");
	}
	
	@Override
	public void run() {
		try {
			Template template = cfg.getTemplate("/reports/templates/server-resources.ftl");
			
			Map<String, Object> objects = new HashMap<String, Object>();
			objects.put("server", generageReports());
			
			File runDirectory = context.getRunDirectory();
			File archiveReportDirectory = new File(runDirectory, "applications");
			File archiveDirectory = new File(archiveReportDirectory, "application");
			FileUtils.forceMkdir(archiveDirectory);
			File archiveReport = new File(archiveDirectory, "server-resources.html");
			
			template.process(objects, new FileWriter(archiveReport));
			
			LOG.info("Wrote report: "+archiveReport.getAbsolutePath());
			
		} catch (Exception e) {
			throw new RuntimeException("Exception writing report.", e);
		}
	}
	
	
	protected ServerResourceReport generageReports() {
		ServerResourceReport report = new ServerResourceReport();
		
		for(int i=0; i<10; i++) {
			report.getDatabases().add(new DatabaseRow("Oracle"+i, "java:/Whatever"));
		}
		
		for(int i=0; i<10; i++) {
			report.getJmxBeans().add(new JMXRow("someObject"+i, "com.example.jmx.JMXBeanName"));
		}
		
		for(int i=0; i<10; i++) {
			report.getQueues().add(new JMSQueueRow("someQueue"+i, "java:/exampleQueue", "Topic"));
		}
		
		return report;
	}
}
