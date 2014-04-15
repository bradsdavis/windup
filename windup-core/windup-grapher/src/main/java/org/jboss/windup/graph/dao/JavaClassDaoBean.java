package org.jboss.windup.graph.dao;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jboss.windup.graph.model.resource.JavaClass;
import org.jboss.windup.graph.model.resource.Resource;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;

public class JavaClassDaoBean extends BaseDaoBean<JavaClass> {

	public JavaClassDaoBean() {
		super(JavaClass.class);
	}

	public synchronized JavaClass getJavaClass(String qualifiedName) {
		JavaClass clz = getByUniqueProperty("qualifiedName", qualifiedName);

		if (clz == null) {
			clz = (JavaClass) this.create(null);
			clz.setQualifiedName(qualifiedName);
		}

		return clz;
	}
	
	public Iterable<JavaClass> findByJavaClassPattern(String regex) {
		return super.findValueMatchingRegex("qualifiedName", regex);
	}
	
	public Iterable<JavaClass> findByJavaPackage(String packageName) {
		return getContext().getFramed().query().has("type", typeValue).has("packageName", packageName).vertices(type);
	}
	
	public Iterable<JavaClass> findByJavaVersion(JavaVersion version) {
		return getContext().getFramed().query().has("type", typeValue).has("majorVersion", version.major).has("minorVersion", version.minor).vertices(type);
	}

	public Iterable<JavaClass> getAllClassNotFound() {
		
		//iterate through all vertices
		Iterable<Vertex> pipeline = new GremlinPipeline<Vertex, Vertex>(context
				.getGraph().getVertices("type", typeValue))
				
				//check to see whether there is an edge coming in that links to the resource providing the java class model.
				.filter(new PipeFunction<Vertex, Boolean>() {
					public Boolean compute(Vertex argument) {
						if (argument.getEdges(Direction.IN, "javaClassFacet").iterator().hasNext()) {
							return false;
						}
						//allow it through if there are no edges coming in that provide the java class model.
						return true;
					}
				});
		return context.getFramed().frameVertices(pipeline, JavaClass.class);
	}
	
	public Iterable<JavaClass> getAllDuplicateClasses() {
		//iterate through all vertices
		Iterable<Vertex> pipeline = new GremlinPipeline<Vertex, Vertex>(context
				.getGraph().getVertices("type", typeValue))
				
				//check to see whether there is an edge coming in that links to the resource providing the java class model.
				.filter(new PipeFunction<Vertex, Boolean>() {
					public Boolean compute(Vertex argument) {
						Iterator<Edge> edges = argument.getEdges(Direction.IN, "javaClassFacet").iterator();
						if(edges.hasNext()) {
							edges.next();
							if(edges.hasNext()) {
								return true;
							}
						}
						//if there aren't two edges, return false.
						return false;
					}
				});
		return context.getFramed().frameVertices(pipeline, JavaClass.class);
	}
	

	
	public boolean isJavaClass(Resource resource) {
		return (new GremlinPipeline<Vertex, Vertex>(resource.asVertex())).out("javaClassFacet").iterator().hasNext();
	}
	
	public JavaClass getJavaClassFromResource(Resource resource) {
		Iterator<Vertex> v = (new GremlinPipeline<Vertex, Vertex>(resource.asVertex())).out("javaClassFacet").iterator();
		if(v.hasNext()) {
			return context.getFramed().frame(v.next(), JavaClass.class);
		}
		
		return null;
	}
	
	public void markAsBlacklistCandidate(JavaClass clz) {
		clz.asVertex().setProperty("blacklistCandidate", true);
	}
	
	public void markAsCustomerPackage(JavaClass clz) {
		clz.asVertex().setProperty("customerPackage", true);
	}
	
	public Iterable<JavaClass> findClassesWithSource() {
		//iterate through all vertices
		Iterable<Vertex> pipeline = new GremlinPipeline<Vertex, Vertex>(context
				.getGraph().getVertices("type", typeValue))
				
				//check to see whether there is an edge coming in that links to the resource providing the java class model.
				.filter(new PipeFunction<Vertex, Boolean>() {
					public Boolean compute(Vertex argument) {
						return argument.getEdges(Direction.OUT, "source").iterator().hasNext();
					}
				});
		return context.getFramed().frameVertices(pipeline, JavaClass.class);
	}
	
	
	public Iterable<JavaClass> findCandidateBlacklistClasses() {
		//for all java classes
		Iterable<Vertex> pipeline = new GremlinPipeline<Vertex, Vertex>(context
				.getGraph().getVertices("type", this.typeValue))
				.as("clz").has("blacklistCandidate").back("clz").cast(Vertex.class);
		return context.getFramed().frameVertices(pipeline, JavaClass.class);
	}
	
	public Iterable<JavaClass> findClassesLeveragingCandidateBlacklists() {
		Iterable<Vertex> pipeline = new GremlinPipeline<Vertex, Vertex>(context
				.getGraph().getVertices("type", this.typeValue))
				.has("blacklistCandidate")
				.in("extends", "imports", "implements")
				.dedup();
		return context.getFramed().frameVertices(pipeline, JavaClass.class);
	}
	
	public Iterable<JavaClass> findLeveragedCandidateBlacklists(JavaClass clz) {
		Set<JavaClass> results = new HashSet<JavaClass>();
		for(JavaClass javaClz : clz.dependsOnJavaClass()) {
			if(javaClz.isBlacklistCandidate()) {
				results.add(javaClz);
			}
		}
		
		return results;
	}
	
	
	public Iterable<JavaClass> findCustomerPackageClasses() {
		//for all java classes
		Iterable<Vertex> pipeline = new GremlinPipeline<Vertex, Vertex>(context
				.getGraph().getVertices("type", this.typeValue))
				.has("customerPackage").V();
		return context.getFramed().frameVertices(pipeline, JavaClass.class);
	}
	
	
	public enum JavaVersion {
		JAVA_7(7, 0),
		JAVA_6(6, 0),
		JAVA_5(5, 0),
		JAVA_1_4(1, 4),
		JAVA_1_3(1, 3),
		JAVA_1_2(1, 2),
		JAVA_1_1(1, 1);
		
		final int major;
		final int minor;
		
		JavaVersion(int major, int minor) {
			this.major = major;
			this.minor = minor;
		}
		
		public int getMajor() {
			return major;
		}
		
		public int getMinor() {
			return minor;
		}
	}


}
