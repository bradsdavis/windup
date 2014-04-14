package org.jboss.windup.engine.visitor;

import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.jboss.windup.engine.visitor.base.EmptyGraphVisitor;
import org.jboss.windup.graph.dao.JavaClassDaoBean;
import org.jboss.windup.graph.model.resource.JavaClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For all classes, apply regular expressions, and color the nodes. 
 * 
 * @author bradsdavis@gmail.com
 *
 */
public class BlacklistMethodCandidateVisitor extends EmptyGraphVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(BlacklistMethodCandidateVisitor.class);

	@Inject
	private JavaClassDaoBean javaClassDao;
	
	@Override
	public void run() {
		Collection<Pattern> patterns = new LinkedList<Pattern>();
		patterns.add(Pattern.compile("org.kie.api.task.model.U.*"));
		
		//for all rules....
		
		for(Pattern p : patterns) {
			for(final org.jboss.windup.graph.model.resource.JavaClass entry : javaClassDao.findByJavaClassPattern(p.pattern())) {
				visitJavaClass(entry);
			}
		}
		javaClassDao.commit();
		
		for(JavaClass clz : javaClassDao.findCandidateBlacklistClasses()) {
			LOG.info("Leverages Blacklist: "+clz.getQualifiedName());
			for(JavaClass p : clz.providesForJavaClass()) {
				LOG.info(" -- Provides for: "+p.getQualifiedName());
			}
		}
		
		for(JavaClass clz : javaClassDao.findClassesLeveragingCandidateBlacklists()) {
			LOG.info("With Candidate: "+clz.getQualifiedName());
		}
	}
	

	@Override
	public void visitJavaClass(org.jboss.windup.graph.model.resource.JavaClass entry) {
		LOG.info("Blacklisting: "+entry.getQualifiedName());
		javaClassDao.markAsBlacklistCandidate(entry);
	}
	
}
