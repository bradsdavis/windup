package org.jboss.windup.graph.model.meta;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Incidence;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

@TypeValue("DatasourceMeta")
public interface DatasourceMeta extends Meta {

	@Property("name")
	public String getName();
	
	@Property("name")
	public void setName(String name);

	@Property("type")
	public String getType();
	
	@Property("type")
	public void setType(String type);

	@Property("version")
	public String getVersion();
	
	@Property("version")
	public void setVersion(String version);

	@Incidence(label="meta", direction=Direction.IN)
	public Iterable<Edge> getSource();
	
	abstract class Impl implements DatasourceMeta {
		@Override
		public String toString() {
			return ReflectionToStringBuilder.toString(this);
		}
	}
}
