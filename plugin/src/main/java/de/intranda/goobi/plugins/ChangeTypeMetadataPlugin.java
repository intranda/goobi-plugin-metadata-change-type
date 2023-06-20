package de.intranda.goobi.plugins;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.goobi.production.plugin.interfaces.IMetadataEditorExtension;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.metadaten.Metadaten;
import de.sub.goobi.persistence.managers.ProcessManager;
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
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;
import ugh.exceptions.UGHException;
import ugh.exceptions.WriteException;
import ugh.fileformats.mets.MetsMods;

@PluginImplementation
@Log4j2
public class ChangeTypeMetadataPlugin implements IMetadataEditorExtension {

    private static final long serialVersionUID = 3644294549629729916L;

    @Getter
    private String pagePath = "/uii/plugin_metadata_changeType.xhtml"; //NOSONAR

    @Getter
    private String title = "intranda_metadata_changeType";

    @Getter
    private String modalId = "changeTypeModal";

    private List<String> metadataToCopy;

    private String propertyName = "";

    @Getter
    private List<SelectItem> metadataTemplates;

    @Getter
    @Setter
    private String selectedTemplate;

    private Metadaten bean;

    @Override
    public void initializePlugin(Metadaten bean) {
        this.bean = bean;

        metadataTemplates = new ArrayList<>();
        XMLConfiguration xml = ConfigPlugins.getPluginConfig(title);
        xml.setExpressionEngine(new XPathExpressionEngine());

        String projectName = bean.getMyProzess().getProjekt().getTitel();
        SubnodeConfiguration config = null;
        try {
            config = xml.configurationAt("/section[project = '" + projectName + "']");
        } catch (IllegalArgumentException e) {
            log.info("Error during plugin initialization", e);
            throw e;
        }

        propertyName = config.getString("/titleProperty", "title");
        List<String> templateProjectNames = Arrays.asList(config.getStringArray("/templateProject"));

        populateProcessList(templateProjectNames);

        // get metadata whitelist to import some existing metadata to the new document (uuid, identifier, ...)
        metadataToCopy = Arrays.asList(config.getStringArray("/metadata"));
    }

    private void populateProcessList(List<String> templateProjectNames) {
        String metadataFolder = ConfigurationHelper.getInstance().getMetadataFolder();
        StringBuilder sb = new StringBuilder();
        sb.append("select prozesseid, WERT from prozesseeigenschaften where titel = '");
        sb.append(propertyName);
        sb.append("' and prozesseid in ( ");
        sb.append("select prozesseid from prozesse where prozesse.ProjekteID in (select projekteid from projekte where titel in (");
        StringBuilder sublist = new StringBuilder();
        for (String template : templateProjectNames) {
            if (sublist.length() > 0) {
                sublist.append(", ");
            }
            sublist.append("'");
            sublist.append(template);
            sublist.append("'");
        }
        sb.append(sublist.toString());
        sb.append("))) order by WERT;");

        List<?> rows = ProcessManager.runSQL(sb.toString());
        for (Object obj : rows) {
            Object[] objArr = (Object[]) obj;
            String id = (String) objArr[0];
            String label = (String) objArr[1];
            String file = metadataFolder + id + "/meta.xml";
            SelectItem si = new SelectItem(file, label);
            metadataTemplates.add(si);
        }
    }

    public void changeTemplate() {
        // create backup of old fileformat

        if (!createBackup()) {
            return;
        }

        // create new fileformat based on selected template

        Path path = Paths.get(selectedTemplate);
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
            copyMetadata(logical, oldLogical, metadataName);
            copyPerson(logical, oldLogical, metadataName);
            copyGroup(logical, oldLogical, metadataName);

        }

        // copy assigned pages to new file format
        DocStruct oldPhysical = bean.getDocument().getPhysicalDocStruct();
        if (oldPhysical != null && oldPhysical.getAllChildren() != null) {
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

    private void copyGroup(DocStruct logical, DocStruct oldLogical, String metadataName) {
        // collect metadata to keep
        List<MetadataGroup> newGroups = new ArrayList<>();

        if (oldLogical.getAllMetadataGroups() != null) {
            for (MetadataGroup grp : oldLogical.getAllMetadataGroups()) {
                if (grp.getType().getName().equals(metadataName)) {
                    try {
                        MetadataGroup newGroup = duplicateGroup(grp);
                        newGroups.add(newGroup);
                    } catch (UGHException e) {
                        log.error(e);
                    }

                }
            }
        }

        if (!newGroups.isEmpty()) {
            // cleanup existing metadata
            List<MetadataGroup> removeList = new ArrayList<>();
            if (logical.getAllMetadataGroups() != null) {
                for (MetadataGroup grp : logical.getAllMetadataGroups()) {
                    if (grp.getType().getName().equals(metadataName)) {
                        removeList.add(grp);
                    }
                }
            }
            for (MetadataGroup md : removeList) {
                logical.removeMetadataGroup(md, true);
            }
            // add new metadata
            for (MetadataGroup md : newGroups) {
                try {
                    logical.addMetadataGroup(md);
                } catch (UGHException e) {
                    log.error(e);
                }
            }
        }

    }

    private MetadataGroup duplicateGroup(MetadataGroup grp) throws MetadataTypeNotAllowedException {
        MetadataGroup newGroup = new MetadataGroup(grp.getType());
        if (grp.getMetadataList() != null) {
            for (Metadata md : grp.getMetadataList()) {
                Metadata newMd = new Metadata(md.getType());
                newMd.setValue(md.getValue());
                newMd.setAutorityFile(md.getAuthorityID(), md.getAuthorityURI(), md.getAuthorityValue());
                newGroup.addMetadata(newMd);
            }
        }
        if (grp.getPersonList() != null) {
            for (Person p : grp.getPersonList()) {
                Person person = new Person(p.getType());
                person.setFirstname(p.getFirstname());
                person.setLastname(p.getLastname());
                person.setAutorityFile(p.getAuthorityID(), p.getAuthorityURI(), p.getAuthorityValue());
                newGroup.addPerson(person);
            }
        }
        return newGroup;
    }

    private void copyPerson(DocStruct logical, DocStruct oldLogical, String metadataName) {

        // collect metadata to keep
        List<Person> newMetadatatList = new ArrayList<>();
        if (oldLogical.getAllPersons() != null) {
            for (Person p : oldLogical.getAllPersons()) {
                if (p.getType().getName().equals(metadataName)) {
                    try {
                        Person person = new Person(p.getType());
                        person.setFirstname(p.getFirstname());
                        person.setLastname(p.getLastname());
                        person.setAutorityFile(p.getAuthorityID(), p.getAuthorityURI(), p.getAuthorityValue());
                        newMetadatatList.add(person);
                    } catch (UGHException e) {
                        log.error(e);
                    }

                }
            }
        }
        if (!newMetadatatList.isEmpty()) {
            // cleanup existing metadata
            List<Person> removeList = new ArrayList<>();
            if (logical.getAllPersons() != null) {
                for (Person p : logical.getAllPersons()) {
                    if (p.getType().getName().equals(metadataName)) {
                        removeList.add(p);
                    }
                }
            }
            for (Person md : removeList) {
                logical.removePerson(md, true);
            }
            // add new metadata
            for (Person md : newMetadatatList) {
                try {
                    logical.addPerson(md);
                } catch (UGHException e) {
                    log.error(e);
                }
            }
        }
    }

    private void copyMetadata(DocStruct logical, DocStruct oldLogical, String metadataName) {
        // collect metadata to keep
        List<Metadata> newMetadatatList = new ArrayList<>();
        if (oldLogical.getAllMetadata() != null) {
            for (Metadata md : oldLogical.getAllMetadata()) {
                if (md.getType().getName().equals(metadataName)) {
                    try {
                        Metadata newMd = new Metadata(md.getType());
                        newMd.setValue(md.getValue());
                        newMd.setAutorityFile(md.getAuthorityID(), md.getAuthorityURI(), md.getAuthorityValue());
                        newMetadatatList.add(newMd);
                    } catch (UGHException e) {
                        log.error(e);
                    }

                }
            }
        }
        if (!newMetadatatList.isEmpty()) {
            // cleanup existing metadata
            List<Metadata> removeList = new ArrayList<>();
            if (logical.getAllMetadata() != null) {
                for (Metadata md : logical.getAllMetadata()) {
                    if (md.getType().getName().equals(metadataName)) {
                        removeList.add(md);
                    }
                }
            }
            for (Metadata md : removeList) {
                logical.removeMetadata(md, true);
            }
            // add new metadata
            for (Metadata md : newMetadatatList) {
                try {
                    logical.addMetadata(md);
                } catch (UGHException e) {
                    log.error(e);
                }

            }
        }
    }

    private boolean createBackup() {
        try {
            Fileformat backup = new MetsMods(bean.getMyPrefs());
            backup.setDigitalDocument(bean.getDocument());
            return backup.write(bean.getMyProzess().getMetadataFilePath().replace("meta.xml", "backup.xml"));
        } catch (UGHException | IOException | SwapException e) {
            log.error(e);
            return false;
        }
    }

}
