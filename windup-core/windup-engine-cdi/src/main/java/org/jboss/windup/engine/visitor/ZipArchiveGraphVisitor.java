package org.jboss.windup.engine.visitor;

import java.io.File;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jboss.windup.engine.util.ZipUtil;
import org.jboss.windup.engine.visitor.base.EmptyGraphVisitor;
import org.jboss.windup.graph.dao.ApplicationReferenceDaoBean;
import org.jboss.windup.graph.dao.ArchiveDaoBean;
import org.jboss.windup.graph.dao.FileResourceDaoBean;
import org.jboss.windup.graph.model.meta.ApplicationReference;
import org.jboss.windup.graph.model.resource.ArchiveResource;
import org.jboss.windup.graph.model.resource.FileResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Takes untyped archives, and casts them to the appropriate type within the graph.
 * For nested archives, this also extracts the nested archive for profiling.
 * 
 * @author bradsdavis@gmail.com
 *
 */
public class ZipArchiveGraphVisitor extends EmptyGraphVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(ZipArchiveGraphVisitor.class);
	
	@Inject
	private FileResourceDaoBean fileDao;
	
	@Inject
	private ArchiveDaoBean archiveDao;
	
	@Inject
	private ApplicationReferenceDaoBean applicationReferenceDao;
	
	private Set<String> getZipExtensions() {
		Set<String> extensions = new HashSet<String>();
		extensions.add(".war");
		extensions.add(".ear");
		extensions.add(".jar");
		extensions.add(".sar");
		extensions.add(".rar");
		
		return extensions;
	}
	
	
	private boolean endsWithExtension(String path) {
		for(String extension : getZipExtensions()) {
			if(StringUtils.endsWith(path, extension)) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void run() {
		//feed all file listeners...
		for(FileResource file : fileDao.findArchiveEntryWithExtension("war", "ear", "jar", "sar", "rar")) {
			visitFile(file);
		}
		fileDao.commit();
	}
	
	@Override
	public void visitFile(FileResource file) {
		//now, check to see whether it is a JAR, and republish the typed value.
		String filePath = file.getFilePath();
		
		if(endsWithExtension(filePath)) {
			ZipFile zipFile = null;			
			try {
				java.io.File reference = new File(file.getFilePath());
				zipFile = new ZipFile(reference);
				
				//go ahead and make it into an archive.
				
				ArchiveResource archive = archiveDao.create(null);
				
				//mark the archive as a top level archive.
				ApplicationReference applicationReference = applicationReferenceDao.create(null);
				applicationReference.setArchive(archive);
				
				archive.setArchiveName(reference.getName());
				archive.setFileResource(file);
				
				//first, make the file reference.
				Enumeration<?> entries = zipFile.entries();
				while(entries.hasMoreElements()) {
					ZipEntry entry = (ZipEntry)entries.nextElement();
					//skip.
					if(entry.isDirectory()) {
						continue;
					}
					if(endsWithExtension(entry.getName())) {
						//unzip.
						String subArchiveName = StringUtils.substringAfterLast(entry.getName(), "/");
						java.io.File subArchiveTempFile = ZipUtil.unzipToTemp(zipFile, entry);
						org.jboss.windup.graph.model.resource.FileResource subArchiveTempFileReference = fileDao.getByFilePath(subArchiveTempFile.getAbsolutePath());

						ArchiveResource subArchive = archiveDao.create(null);
						subArchive.setArchiveName(subArchiveName);
						subArchive.setFileResource(subArchiveTempFileReference);
						
						//add the element as a child..
						archive.addChildArchive(subArchive);
					}
				}
			} catch (Exception e) {
				LOG.error("Exception creating zip from: "+filePath, e);
			}
			finally {
				IOUtils.closeQuietly(zipFile);
			}
		}
	}
}
