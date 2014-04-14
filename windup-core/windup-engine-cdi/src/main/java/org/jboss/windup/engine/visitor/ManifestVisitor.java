package org.jboss.windup.engine.visitor;

import java.io.InputStream;
import java.util.jar.Manifest;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jboss.windup.engine.WindupContext;
import org.jboss.windup.engine.visitor.base.EmptyGraphVisitor;
import org.jboss.windup.graph.dao.ArchiveEntryDaoBean;
import org.jboss.windup.graph.dao.JarManifestDaoBean;
import org.jboss.windup.graph.model.meta.JarManifest;
import org.jboss.windup.graph.model.resource.ArchiveEntryResource;
import org.jboss.windup.graph.model.resource.JarArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extracts manifest information to graph. 
 * 
 * @author bradsdavis@gmail.com
 *
 */
public class ManifestVisitor extends EmptyGraphVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(ManifestVisitor.class);

	@Inject
	private WindupContext context;
	
	@Inject
	private ArchiveEntryDaoBean archiveEntryDao;
	
	@Inject
	private JarManifestDaoBean jarManifestDao;
	
	@Override
	public void run() {
		for(ArchiveEntryResource resource : archiveEntryDao.findArchiveEntry("META-INF/MANIFEST.MF")) {
			visitArchiveEntry(resource);
		}
		archiveEntryDao.commit();
	}
	
	@Override
	public void visitArchiveEntry(ArchiveEntryResource entry) {
		JarManifest jarManifest = jarManifestDao.create();
		JarArchive archive = context.getGraphContext().getFramed().frame(entry.getArchive().asVertex(), JarArchive.class);
		jarManifest.setResource(entry);
		jarManifest.setJarArchive(archive);
		
		InputStream is = null; 
		try {
			is = entry.asInputStream();
			Manifest manifest = new Manifest(entry.asInputStream());
			if(manifest == null || manifest.getMainAttributes().size() == 0) {
				return;
			}
			
			for(Object key : manifest.getMainAttributes().keySet()) {
				String property = StringUtils.trim(key.toString());
				String propertyValue = StringUtils.trim(manifest.getMainAttributes().get(key).toString());
				jarManifest.setProperty(property, propertyValue);
			}
		}
		catch(Exception e) {
			LOG.warn("Exception reading manifest.", e);
		}
		finally {
			IOUtils.closeQuietly(is);
		}
		
		
	}
}
