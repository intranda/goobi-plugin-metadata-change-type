//package de.intranda.goobi.plugins;
//
//import org.goobi.production.plugin.interfaces.IMetadataEditorExtension;
//
//import de.sub.goobi.metadaten.Metadaten;
//import lombok.Getter;
//import lombok.extern.log4j.Log4j2;
//import net.xeoh.plugins.base.annotations.PluginImplementation;
//
//@PluginImplementation
//@Log4j2
//public class OtherSampleMetadataPlugin implements IMetadataEditorExtension {
//
//    // TODO uncomment this to test multiple plugins
//
//    private static final long serialVersionUID = 3644294549629729916L;
//
//    @Getter
//    private String pagePath = "/uii/plugin_metadata_sample.xhtml";
//
//    @Getter
//    private String title = "a_intranda_metadata_sample";
//
//    @Getter
//    private String modalId = "sampleModal";
//
//    @Override
//    public void initializePlugin(Metadaten bean) {
//        log.info("init sample plugin");
//    }
//}