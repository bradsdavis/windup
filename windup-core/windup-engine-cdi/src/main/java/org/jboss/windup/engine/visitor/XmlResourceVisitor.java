package org.jboss.windup.engine.visitor;

import static org.joox.JOOX.$;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.jboss.windup.engine.util.xml.LocationAwareContentHandler;
import org.jboss.windup.engine.util.xml.LocationAwareContentHandler.Doctype;
import org.jboss.windup.engine.util.xml.LocationAwareXmlReader;
import org.jboss.windup.engine.util.xml.XmlUtil;
import org.jboss.windup.engine.visitor.base.EmptyGraphVisitor;
import org.jboss.windup.graph.dao.ArchiveEntryDaoBean;
import org.jboss.windup.graph.dao.DoctypeDaoBean;
import org.jboss.windup.graph.dao.NamespaceDaoBean;
import org.jboss.windup.graph.dao.XmlResourceDaoBean;
import org.jboss.windup.graph.model.meta.xml.DoctypeMeta;
import org.jboss.windup.graph.model.meta.xml.NamespaceMeta;
import org.jboss.windup.graph.model.resource.ArchiveEntryResource;
import org.jboss.windup.graph.model.resource.XmlResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
/**
 * Adds the XMLResource Facet to the resource.
 *  Extracts Doctype and Namespace information in the XML files.
 *  Extracts Root Element Tag name.
 * 
 * @author bradsdavis@gmail.com
 *
 */
public class XmlResourceVisitor extends EmptyGraphVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(XmlResourceVisitor.class);
	
	@Inject
	private DoctypeDaoBean doctypeDao;
	
	@Inject
	private NamespaceDaoBean namespaceDao;
	
	@Inject
	private XmlResourceDaoBean xmlResourceDao;
	
	@Inject
	private ArchiveEntryDaoBean archiveEntryDao;
	
	@Override
	public void run() {
		int i = 0;
		for(final ArchiveEntryResource entry : archiveEntryDao.findArchiveEntryWithExtension("xml")) {
			visitArchiveEntry(entry);
			
			if(i % 10 == 0 && i > 0) {
				archiveEntryDao.commit();
			}
		}
		archiveEntryDao.commit();
	}
	
	@Override
	public void visitArchiveEntry(ArchiveEntryResource entry) {

		//try and read the XML...
		InputStream is = null;
		try {
			is = entry.asInputStream();
			
			//read it to a Document object.
			Document parsedDocument = LocationAwareXmlReader.readXML(is);
			
			//pull out doctype data.
			Doctype docType = (Doctype) parsedDocument.getUserData(LocationAwareContentHandler.DOCTYPE_KEY_NAME);
			
			//if this is successful, then we know it is a proper XML file.
			//set it to the graph as an XML file.
			XmlResource resource = xmlResourceDao.create(null);
			resource.setResource(entry);

			//get and index by the root tag.
			String tagName = $(parsedDocument).tag();
			resource.setRootTagName(tagName);
			
			if(docType != null) {
				//create the doctype from
				Iterator<DoctypeMeta> metas = doctypeDao.findByProperties(docType.getPublicId(), docType.getSystemId());
				if(metas.hasNext()) {
					DoctypeMeta meta = metas.next();
					meta.addXmlResource(resource);
					resource.addMeta(meta);
				}
				else {
					LOG.debug("Adding doctype: "+docType);
					DoctypeMeta meta = doctypeDao.create(null);
					meta.addXmlResource(resource);
					meta.setBaseURI(docType.getBaseURI());
					meta.setName(docType.getName());
					meta.setPublicId(docType.getPublicId());
					meta.setSystemId(docType.getSystemId());
				}
			}

			Map<String, String> namespaceSchemaLocations = XmlUtil.getSchemaLocations(parsedDocument); 
			if(namespaceSchemaLocations != null && namespaceSchemaLocations.size() > 0) {
				for(String namespace : namespaceSchemaLocations.keySet()) {
					NamespaceMeta meta = namespaceDao.createNamespaceSchemaLocation(namespace, namespaceSchemaLocations.get(namespace));
					meta.addXmlResource(resource);
				}
			}
			
		}
		catch(Exception e) {
			LOG.error("Encountered Exception: "+e.getMessage(), e);
		} finally {
			IOUtils.closeQuietly(is);
		}
		
	}
}
