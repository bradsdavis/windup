package org.jboss.windup.rules.apps.java.model;

import org.jboss.windup.graph.model.resource.FileModel;
import org.jboss.windup.graph.model.resource.SourceFileModel;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

/**
 * Represents a source ".java" file on disk.
 * 
 * @author jsightler <jesse.sightler@gmail.com>
 */
@TypeValue(JavaSourceFileModel.TYPE)
public interface JavaSourceFileModel extends FileModel, SourceFileModel
{
    public static final String UNPARSEABLE_JAVA_CLASSIFICATION = "Unparseable Java File";
    public static final String UNPARSEABLE_JAVA_DESCRIPTION = "This Java file could not be parsed";

    public static final String TYPE = "JavaSourceFileModel";
    public static final String PACKAGE_NAME = "packageName";
    public static final String JAVA_CLASS_MODEL = "javaClass";

    /**
     * Contains the Java package name
     */
    @Property(PACKAGE_NAME)
    public String getPackageName();

    /**
     * Contains the Java package name
     */
    @Property(PACKAGE_NAME)
    public void setPackageName(String packageName);

    /**
     * Lists the {@link JavaClassModel}s contained within this source file
     */
    @Adjacency(label = JAVA_CLASS_MODEL, direction = Direction.OUT)
    public Iterable<JavaClassModel> getJavaClasses();

    /**
     * Lists the {@link JavaClassModel}s contained within this source file
     */
    @Adjacency(label = JAVA_CLASS_MODEL, direction = Direction.OUT)
    public void addJavaClass(JavaClassModel javaClassModel);
}