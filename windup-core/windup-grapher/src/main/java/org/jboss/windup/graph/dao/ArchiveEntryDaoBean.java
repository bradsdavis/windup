package org.jboss.windup.graph.dao;

import java.util.Iterator;

import org.jboss.windup.graph.model.resource.ArchiveEntryResource;
import org.jboss.windup.graph.model.resource.ArchiveResource;
import org.jboss.windup.graph.model.resource.JavaClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thinkaurelius.titan.core.attribute.Text;
import com.thinkaurelius.titan.util.datastructures.IterablesUtil;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;

public class ArchiveEntryDaoBean extends BaseDaoBean<ArchiveEntryResource> {

	private static Logger LOG = LoggerFactory.getLogger(ArchiveEntryDaoBean.class);
	
	public ArchiveEntryDaoBean() {
		super(ArchiveEntryResource.class);
	}
	
	

	public Iterable<ArchiveEntryResource> findByArchive(final ArchiveResource resource) {
		Iterable<Vertex> pipeline = new GremlinPipeline<Vertex, Vertex>(context
				.getGraph().getVertices("type", typeValue))
				
				//check to see whether there is an edge coming in that links to the resource providing the java class model.
				.filter(new PipeFunction<Vertex, Boolean>() {
					public Boolean compute(Vertex argument) {
						Iterable<Vertex> v = argument.getVertices(Direction.IN, "child");
						return v.iterator().next().getId().equals(resource.asVertex().getId());
					}
				});
		return context.getFramed().frameVertices(pipeline, ArchiveEntryResource.class);
	}

	
	public Iterable<ArchiveEntryResource> findArchiveEntry(String value) {
		return super.getByProperty("archiveEntry", value);
	}


	public long findArchiveEntryWithExtensionCount(String ... values) {
		return count(findArchiveEntryWithExtension(values));
	}
	
	//builds a regular expression query in lucene to search for archives matching extensions.
	public Iterable<ArchiveEntryResource> findArchiveEntryWithExtension(String ... values) {
		//build regex
		if(values.length == 0) {
			return IterablesUtil.emptyIterable();
		}
		
		final String regex;
		if(values.length == 1) {
			regex = ".+\\."+values[0]+"$";
		}
		else {
			StringBuilder builder = new StringBuilder();
			builder.append("\\b(");
			int i=0;
			for(String value : values) {
				if(i>0) {
					builder.append("|");
				}
				builder.append(value);
				i++;
			}
			builder.append(")\\b");
			regex = ".+\\."+builder.toString()+"$";
		}

		LOG.debug("Regex: "+regex);
		return context.getFramed().query().has("type", typeValue).has("archiveEntry", Text.REGEX, regex).vertices(type);
	}
}
