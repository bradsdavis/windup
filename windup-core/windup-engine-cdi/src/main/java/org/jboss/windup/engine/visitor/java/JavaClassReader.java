package org.jboss.windup.engine.visitor.java;

import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.EmptyVisitor;
import org.apache.bcel.classfile.JavaClass;
import org.apache.commons.lang.StringUtils;
import org.jboss.windup.graph.dao.JavaClassDaoBean;
import org.jboss.windup.graph.model.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an Apache BCEL Class Visitor, in order to read Bytecode details of the Java Class.
 *  
 * @author bradsdavis
 *
 */
public class JavaClassReader extends EmptyVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(JavaClassReader.class);

	private final JavaClass javaClass;
	
	private final JavaClassDaoBean javaClassDao;
	private org.jboss.windup.graph.model.resource.JavaClass current;
	private final Resource resource;
	
	public JavaClassReader(JavaClass clz, JavaClassDaoBean javaClassDao, Resource resource) {
		this.javaClass = clz;
		this.javaClassDao = javaClassDao;
		this.resource = resource;
	}
	
	public void process() {
		this.javaClass.accept(this);
	}
	
	@Override
	public void visitJavaClass(JavaClass obj) {
		current = javaClassDao.getJavaClass(obj.getClassName());
		int major = obj.getMajor();
		int minor = obj.getMinor();
		current.setMajorVersion(major);
		current.setMinorVersion(minor);
		current.addResource(resource);
		current.setPackageName(obj.getPackageName());
		
		for(String interfaceName : obj.getInterfaceNames()) {
			org.jboss.windup.graph.model.resource.JavaClass interfaceClass = javaClassDao.getJavaClass(interfaceName);
			//then we make the connection.
			current.addImplements(interfaceClass);
		}
		
		//process the pool.
		Constant[] pool = obj.getConstantPool().getConstantPool();
		for(Constant c : pool) {
			if(c == null) continue;
			c.accept(this);
		} 
		
		String superClz = obj.getSuperclassName();
		org.jboss.windup.graph.model.resource.JavaClass superJavaClass = javaClassDao.getJavaClass(superClz);
		current.setExtends(superJavaClass);
	}
	
	@Override
	public void visitConstantClass(ConstantClass obj) {
		ConstantPool pool = javaClass.getConstantPool();
		String classVal = obj.getConstantValue(pool).toString();
		classVal = StringUtils.replace(classVal, "/", ".");

		if(StringUtils.equals(classVal, this.javaClass.getClassName())) {
			//skip adding class name.
			return;
		}
		
		org.jboss.windup.graph.model.resource.JavaClass clz = javaClassDao.getJavaClass(classVal);
		current.addImport(clz);
	}
	
}
