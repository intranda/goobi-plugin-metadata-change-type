package de.intranda.goobi.plugins;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.goobi.production.plugin.interfaces.IMetadataEditorExtension;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.StorageProvider;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.metadaten.Metadaten;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.MetadataGroup;
import ugh.dl.Person;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;
import ugh.exceptions.TypeNotAllowedForParentException;
import ugh.exceptions.UGHException;
import ugh.exceptions.WriteException;
import ugh.fileformats.mets.MetsMods;

@PluginImplementation
@Log4j2
public class ChangeTypeMetadataPlugin implements IMetadataEditorExtension {

    private static final long serialVersionUID = 3644294549629729916L;

    @Getter
    private String pagePath = "/uii/plugin_metadata_changeType.xhtml";

    @Getter
    private String title = "intranda_metadata_changeType";

    @Getter
    private String modalId = "changeTypeModal";

    private String templateFolder;

    private List<String> metadataToCopy;

    @Getter
    private List<SelectItem> metadataTemplates;

    @Getter
    @Setter
    private String selectedTemplate;

    private Metadaten bean;

    public void initializePlugin(Metadaten bean) {
        this.bean = bean;

        metadataTemplates = new ArrayList<>();
        XMLConfiguration config = ConfigPlugins.getPluginConfig(title);
        config.setExpressionEngine(new XPathExpressionEngine());

        // get metadata whitelist to import some existing metadata to the new document (uuid, identifier, ...) 
        metadataToCopy = Arrays.asList(config.getStringArray("/metadata"));

        // get folder for template files from configuration
        templateFolder = config.getString("/templateFolder");

        // list all file names

        List<String> filenames = StorageProvider.getInstance().list(templateFolder);

        for (String file : filenames) {
            // get label for filenames? (remove extension, replace _ and - with spaces)
            String label = file.replace(".xml", "").replace("-", " ").replace("_", " ");
            // build SelectItem list
            SelectItem si = new SelectItem(file, label);
            metadataTemplates.add(si);
        }
    }

    public void changeTemplate() {
        // create backup of old fileformat
     
// TODO
        
        // create new fileformat based on selected template

        Path path = Paths.get(templateFolder, selectedTemplate);
        Fileformat newFileformat = null;
        DigitalDocument digDoc = null;
        try {
            newFileformat = new MetsMods(bean.getMyPrefs());
            newFileformat.read(path.toString());
            digDoc = newFileformat.getDigitalDocument();
        } catch (UGHException e) {
            log.error(e);
            return;
        }

        // copy metadata/person/groups from metadataToCopy list from old fileformat to new fileformat
        DocStruct logical = digDoc.getLogicalDocStruct();
        DocStruct physical = digDoc.getPhysicalDocStruct();

        DocStruct oldLogical = bean.getDocument().getLogicalDocStruct();
        for (String metadataName : metadataToCopy) {
            for (Metadata md : oldLogical.getAllMetadata()) {
                if (md.getType().getName().equals(metadataName)) {
                    // TODO copy metadata
                }
            }

            for (Person p : oldLogical.getAllPersons()) {
                if (p.getType().getName().equals(metadataName)) {
                    // TODO copy person
                }
            }

            for (MetadataGroup grp : oldLogical.getAllMetadataGroups()) {
                if (grp.getType().getName().equals(metadataName)) {
                    // TODO copy group
                }
            }

        }

        // copy assigned pages to new file format
        DocStruct oldPhysical = bean.getDocument().getPhysicalDocStruct();
        for (DocStruct oldPage : oldPhysical.getAllChildren()) {
            try {
                DocStruct newPage = digDoc.createDocStruct(oldPage.getType());
                physical.addChild(newPage);

                for (Metadata md : oldPage.getAllMetadata()) {
                    Metadata newMd = new Metadata(md.getType());
                    newMd.setValue(md.getValue());
                    newPage.addMetadata(newMd);
                }

            } catch (UGHException e) {
                log.error(e);
            }

        }

        // save new ff as process metadata

        try {
            bean.getMyProzess().writeMetadataFile(newFileformat);
        } catch (WriteException | PreferencesException | IOException | SwapException e) {
            log.error(e);
        }

        // reload fileformat in mets editor
        try {
            bean.XMLlesenStart();
        } catch (ReadException | PreferencesException | IOException | SwapException | DAOException e) {
            log.error(e);
        }

    }

}
