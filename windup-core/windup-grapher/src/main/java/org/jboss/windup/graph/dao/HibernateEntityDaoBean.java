package org.jboss.windup.graph.dao;

import javax.inject.Singleton;

import org.jboss.windup.graph.model.meta.javaclass.HibernateEntityFacet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class HibernateEntityDaoBean extends BaseDaoBean<HibernateEntityFacet> {
	private static final Logger LOG = LoggerFactory.getLogger(HibernateEntityDaoBean.class);
	public HibernateEntityDaoBean() {
		super(HibernateEntityFacet.class);
	}
	
	
}
