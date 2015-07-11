package org.jboss.windup.analytics;

import io.keen.client.java.JavaKeenClientBuilder;
import io.keen.client.java.KeenClient;
import io.keen.client.java.KeenLogging;
import io.keen.client.java.KeenProject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.jboss.windup.config.AbstractRuleProvider;
import org.jboss.windup.config.GraphRewrite;
import org.jboss.windup.config.metadata.MetadataBuilder;
import org.jboss.windup.config.operation.GraphOperation;
import org.jboss.windup.config.phase.ReportGenerationPhase;
import org.jboss.windup.config.query.Query;
import org.jboss.windup.graph.GraphContext;
import org.jboss.windup.graph.model.ArchiveModel;
import org.jboss.windup.graph.model.ProjectModel;
import org.jboss.windup.graph.model.WindupConfigurationModel;
import org.jboss.windup.graph.service.ArchiveService;
import org.jboss.windup.graph.service.GraphService;
import org.jboss.windup.graph.service.WindupConfigurationService;
import org.jboss.windup.rules.apps.java.model.JavaClassModel;
import org.jboss.windup.rules.apps.java.service.JavaClassService;
import org.jboss.windup.rules.apps.xml.model.DoctypeMetaModel;
import org.jboss.windup.rules.apps.xml.model.NamespaceMetaModel;
import org.jboss.windup.rules.apps.xml.model.XmlFileModel;
import org.ocpsoft.rewrite.config.Configuration;
import org.ocpsoft.rewrite.config.ConfigurationBuilder;
import org.ocpsoft.rewrite.context.EvaluationContext;

/**
 * Pushes analytics to the cloud.
 */
public class CollectAnalyticsRuleProvider extends AbstractRuleProvider
{
    private static final Logger LOG = Logger.getLogger(CollectAnalyticsRuleProvider.class.getSimpleName());

    public CollectAnalyticsRuleProvider()
    {
        super(MetadataBuilder.forProvider(CollectAnalyticsRuleProvider.class, "Report anonymous analytics")
                    .setPhase(ReportGenerationPhase.class));
    }

    @Override
    public Configuration getConfiguration(GraphContext context)
    {
        return ConfigurationBuilder.begin()
                    .addRule()
                    .when(Query.fromType(WindupConfigurationModel.class))
                    .perform(new GraphOperation()
                    {
                        @Override
                        public void perform(GraphRewrite event, EvaluationContext context)
                        {
                            // configuration of current execution
                            WindupConfigurationModel configurationModel = WindupConfigurationService.getConfigurationModel(event.getGraphContext());

                            // reference to input project model
                            ProjectModel projectModel = configurationModel.getInputPath().getProjectModel();
                            createAnalytics(event.getGraphContext(), projectModel);
                        }

                        @Override
                        public String toString()
                        {
                            return this.getClass().getName();
                        }
                    });

    }

    @SuppressWarnings("unchecked")
    private void createAnalytics(GraphContext context, ProjectModel projectModel)
    {
       if(!isAnalyticsEnabled(context.getOptionMap())) {
           return;
       }
       
        //write results to google analytics.
        LOG.info("Analytics should be run.");
        
        KeenClient client = new JavaKeenClientBuilder().build();
        KeenClient.initialize(client);
        
        KeenProject project = new KeenProject("55a146d3d2eaaa01ffb4b96d", "25c0960fae6a249963cde428b523b1f71cf89b6a8ce3502dec856ddcd8b146a8e23267e769cd4ff685b01cae05c8adae3082a2a4981b50dd298a3cec890643eb04f48cc0f1949bf467eb99b66193d07d83eb5b1d8aff27de39846b0f50f0a0c7d4bea847ec9e02cf51b534a2e07963c9", null);
        client.setDefaultProject(project);
        
        if(LOG.isLoggable(Level.INFO)) {
            KeenLogging.enableLogging();
            KeenClient.client().setDebugMode(true);
        }

        processXmlDoctype(context, client);
        processXmlNamespaces(context, client);
        processJavaClasses(context, client);

        client.sendQueuedEvents();
        LOG.info("Analytics submitted.");
    }
    
    
    private void processJavaClasses(GraphContext context, KeenClient client) {
        JavaClassService jcs = new JavaClassService(context);

        Map<String, Object> event = new HashMap<String, Object>();
        List<String> javaclz = new ArrayList<>();
        
        for(JavaClassModel jcm : jcs.findAll()) {
            String qualified = jcm.getQualifiedName();
            javaclz.add(qualified);
            
            LOG.info(" - event class: "+qualified);
        }

        event.put("java", javaclz);
        client.queueEvent("class", event);
    }
    
    private void processXmlNamespaces(GraphContext context, KeenClient client) {
        GraphService<NamespaceMetaModel> namespaceService = new GraphService<NamespaceMetaModel>(context, NamespaceMetaModel.class);
        
        Map<String, Object> event = new HashMap<String, Object>();
        List<String> namespaces = new ArrayList<>();

        for(NamespaceMetaModel ns : namespaceService.findAll()) {
            Map<String, Object> n = new HashMap<>();
            addValue(n, "uri", ns.getURI());
            addValue(n, "location", ns.getSchemaLocation());
            
            //determine count of files leveraging URI
            List<String> fileNames = new ArrayList<>();
            for(XmlFileModel x : ns.getXmlResources()) {
                fileNames.add(x.getFileName());
            }
            n.put("file", fileNames);
        }
        
        event.put("namespace", namespaces);
        client.queueEvent("xml", event);
    }
    
    private void processXmlDoctype(GraphContext context, KeenClient client) {
        GraphService<DoctypeMetaModel> docTypeService = new GraphService<DoctypeMetaModel>(context, DoctypeMetaModel.class);
        
        Map<String, Object> event = new HashMap<String, Object>();
        List<Map<String, Object>> doctypes = new ArrayList<>();

        for(DoctypeMetaModel dt : docTypeService.findAll()) {
            Map<String, Object> doctype = new HashMap<>();
            addValue(doctype, "baseURI", dt.getBaseURI());
            addValue(doctype, "name", dt.getName());
            addValue(doctype, "publicId", dt.getPublicId());
            addValue(doctype, "systemId", dt.getSystemId());
            doctypes.add(doctype);
        }
        
        event.put("doctype", doctypes);
        client.queueEvent("xml", event);
    }
    

    private void processArchives(GraphContext context, KeenClient client) {
        ArchiveService archiveService = new ArchiveService(context);
        
        Map<String, Object> event = new HashMap<String, Object>();
        List<Map<String, Object>> archives = new ArrayList<>();

        for(ArchiveModel ar : archiveService.findAll()) {
            Map<String, Object> archive = new HashMap<>();
            addValue(archive, "sha1", ar.getSHA1Hash());
            addValue(archive, "md5", ar.getMD5Hash());
            
            archives.add(archive);
        }
        
        event.put("archive", archives);
        client.queueEvent("xml", event);
    }
    
    
    
    
    
    private boolean isAnalyticsEnabled(Map<String, Object> options) {
        if(options.containsKey("offline")) {
            Boolean offlineValue = (Boolean)options.get("offline");
            
            if(offlineValue != null && offlineValue) {
                LOG.info("Offline mode. Skipping.");
                return false;
            }
        }
        
        if(!options.containsKey("analytics")) {
            return false;
        }
        
        return true;
    }
    
    private void addValue(final Map<String, Object> map, final String key, final String val) {
        if(StringUtils.isNotBlank(val)) {
            map.put(key, val);
        }
    }
}
