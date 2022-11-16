package de.intranda.goobi.plugins;

import org.goobi.production.plugin.interfaces.IMetadataEditorExtension;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
@Log4j2
public class ChangeTypeMetadataPlugin implements IMetadataEditorExtension {

    private static final long serialVersionUID = 3644294549629729916L;

    @Getter
    private String pagePath = "/uii/plugin_metadata_changeType.xhtml";

    @Getter
    private String title = "intranda_metadata_changeType";

    @Getter
    private String modalId = "#changeTypeModal";

}
