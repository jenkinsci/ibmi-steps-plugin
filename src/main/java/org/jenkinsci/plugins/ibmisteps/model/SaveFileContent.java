package org.jenkinsci.plugins.ibmisteps.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.as400.access.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class SaveFileContent implements Serializable {
    private static final long serialVersionUID = -4924933386903899243L;

    private final String name;
    private final long size;
    private final String creationLPAR;
    private final String description;
    private final List<SAVFEntry> entries = new LinkedList<>();
    private final String targetRelease;

    private String savedLibrary;

    public SaveFileContent(final SaveFile saveFile) throws AS400SecurityException,
            ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException {
        name = saveFile.getName();
        description = saveFile.getDescription();
        size = saveFile.getLength();
        final AS400 ibmi = saveFile.getSystem();
        if (saveFile.getTargetRelease().equals(SaveFile.CURRENT_RELEASE)) {
            targetRelease = String.format("V%sR%sM%s",
                    ibmi.getVersion(),
                    ibmi.getRelease(),
                    ibmi.getModification());
        } else if (saveFile.getTargetRelease().equals(SaveFile.PREVIOUS_RELEASE)) {
            int version = ibmi.getVersion();
            int release = ibmi.getRelease();
            if (version == 7 && release == 1) {
                version = 6;
            } else if (version == 6) {
                version = 5;
                release = 4;
            } else {
                release--;
            }
            targetRelease = String.format("V%sR%sM%s",
                    version,
                    release,
                    ibmi.getModification());
        } else {
            targetRelease = saveFile.getTargetRelease();
        }
        creationLPAR = saveFile.getSystem().getSystemName();

        final List<SaveFileEntry> savfEntries = Optional.ofNullable(saveFile.listEntries())
                .map(Arrays::asList)
                .orElseGet(Collections::emptyList);

        for (final SaveFileEntry entry : savfEntries) {
            if (entry.getType().equalsIgnoreCase("*lib")) {
                savedLibrary = entry.getLibrary();
            } else {
                entries.add(new SAVFEntry(entry));
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getCreationLPAR() {
        return creationLPAR;
    }

    public String getSavedLibrary() {
        return savedLibrary;
    }

    public String getDescription() {
        return description;
    }

    public List<SAVFEntry> getEntries() {
        return entries;
    }

    public String getTargetRelease() {
        return targetRelease;
    }

    public long getSize() {
        return size;
    }

    public String toJSON() throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }

    public static class SAVFEntry implements Serializable {
        private static final long serialVersionUID = -1097647869173691842L;

        private final String name;
        private final String description;
        private final String library;
        private final String extendedObjectAttribute;
        private final boolean dataSaved;
        private final String owner;
        private final long size;
        private final String type;

        public SAVFEntry(final SaveFileEntry entry) {
            name = entry.getName();
            description = entry.getDescription();
            library = entry.getLibrary();
            extendedObjectAttribute = entry.getExtendedObjectAttribute();
            dataSaved = entry.isDataSaved();
            owner = entry.getOwner();
            size = entry.getSize();
            type = entry.getType();
        }

        public String getDescription() {
            return description;
        }

        public String getExtendedObjectAttribute() {
            return extendedObjectAttribute;
        }

        public String getLibrary() {
            return library;
        }

        public String getName() {
            return name;
        }

        public String getOwner() {
            return owner;
        }

        public long getSize() {
            return size;
        }

        public String getType() {
            return type;
        }

        public boolean isDataSaved() {
            return dataSaved;
        }
    }
}
