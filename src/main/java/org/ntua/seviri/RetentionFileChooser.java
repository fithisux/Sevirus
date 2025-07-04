package org.ntua.seviri;

import javafx.beans.property.SimpleObjectProperty;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

// From here
// https://stackoverflow.com/questions/36920131/can-a-javafx-filechooser-remember-the-last-directory-it-opened
//

public class RetentionFileChooser {
    public enum FilterMode {
        //Setup supported filters
        CSV_FILES("csv files (*.csv)", "*.csv"),
        SHAPE_FILES("shp files (*.shp)", "*.shp");

        private FileChooser.ExtensionFilter extensionFilter;

        FilterMode(String extensionDisplayName, String... extensions){
            extensionFilter = new FileChooser.ExtensionFilter(extensionDisplayName, extensions);
        }

        public FileChooser.ExtensionFilter getExtensionFilter(){
            return extensionFilter;
        }
    }

    public static FileChooser instance = null;
    public static DirectoryChooser dirInstance = null;
    public static SimpleObjectProperty<File> lastKnownDirectoryProperty = new SimpleObjectProperty<>();

    private RetentionFileChooser(){ }

    private static FileChooser getInstance(FilterMode... filterModes){
        if(instance == null) {
            instance = new FileChooser();
            instance.initialDirectoryProperty().bindBidirectional(lastKnownDirectoryProperty);
        }
        //Set the filters to those provided
        //You could add check's to ensure that a default filter is included, adding it if need be
        instance.getExtensionFilters().setAll(
                Arrays.stream(filterModes)
                        .map(FilterMode::getExtensionFilter)
                        .collect(Collectors.toList()));
        return instance;
    }

    private static DirectoryChooser getDirInstance(){
        if(dirInstance == null) {
            dirInstance = new DirectoryChooser();
            dirInstance.initialDirectoryProperty().bindBidirectional(lastKnownDirectoryProperty);
        }
        return dirInstance;
    }

    public static File showOpenDialog(FilterMode... filterModes){
        return showOpenDialog(null, filterModes);
    }

    public static File showOpenDialog(Window ownerWindow, FilterMode...filterModes){
        File chosenFile = getInstance(filterModes).showOpenDialog(ownerWindow);
        if(chosenFile != null){
            lastKnownDirectoryProperty.setValue(chosenFile.getParentFile());
        }
        return chosenFile;
    }

    public static File showDirOpenDialog(Window ownerWindow, FilterMode...filterModes){
        File selectedDirectory = getDirInstance().showDialog(ownerWindow);
        if (selectedDirectory != null) {
            lastKnownDirectoryProperty.setValue(selectedDirectory.getParentFile());
        }
        return selectedDirectory;
    }

    public static File showSaveDialog(FilterMode... filterModes){
        return showSaveDialog(null, filterModes);
    }

    public static File showSaveDialog(Window ownerWindow, FilterMode... filterModes){
        File chosenFile = getInstance(filterModes).showSaveDialog(ownerWindow);
        if(chosenFile != null){
            lastKnownDirectoryProperty.setValue(chosenFile.getParentFile());
        }
        return chosenFile;
    }
}
