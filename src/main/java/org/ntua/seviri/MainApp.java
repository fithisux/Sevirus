/*
 	Copyright (c) ${date} Dr. Vasileios I. Anagnostopoulos.
	This file is part of Sevirus.

    Sevirus is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Sevirus is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Foobar.  If not, see <http://www.gnu.org/licenses/>.

	Contributors:
   Vasileios I. Anagnostopoulos - initial API and implementation under subcontract for Aberystwyth University
 */
package org.ntua.seviri;

import br.atualy.devolucaodevenda.util.FxDialogs;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.locationtech.jts.geom.MultiPolygon;
import org.ntua.generic.ProcessorUtilities;
import org.ntua.generic.DataStructures.Place;
import org.ntua.seviri.model.GeosPixels;
import org.ntua.seviri.model.GeosProcessor;
import org.ntua.seviri.model.GeosProcessor.GeosException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class MainApp {

    public Stage primaryStage;

    @FXML
    TextField seviri_folder_textfield;
    @FXML
    TextField output_folder_textfield;
    @FXML
    TextField static_folder_textfield;
    @FXML
    TextField shape_folder_textfield;
    @FXML
    TextField points_file_textfield;
    @FXML
    ComboBox country_combo_box;
    @FXML
    TextField proximityThreshold;

    int selected_index = -1;
    Map<String, MultiPolygon> countries = null;
    GeosPixels my_country_pixels = null;

    @FXML
    public void initialize() {
    }

    /**
     * Opens a dialog to edit details for the specified person. If the user
     * clicks OK, the changes are saved into the provided person object and true
     * is returned.
     *
     * @param person the person object to be edited
     * @return true if the user clicked OK, false otherwise.
     */
    @FXML
    public void seviri_folder_fcn() {
        File selectedDirectory = RetentionFileChooser.showDirOpenDialog(this.primaryStage);
        if (selectedDirectory == null) {
        } else {
            this.seviri_folder_textfield.setText(selectedDirectory
                    .getAbsolutePath());
        }

    }

    @FXML
    public void about_fcn() {

        FxDialogs.showInformation("Developer team.",
                "Dr. Vasileios Anagnostopoulos (NTUA DKMS GROUP).\nDr George Petropoulos (ABERYSTWYTH UNIVERSITY).");
    }

    @FXML
    public void output_folder_fcn() {
        File selectedDirectory = RetentionFileChooser.showDirOpenDialog(this.primaryStage);
        if (selectedDirectory == null) {
        } else {
            this.output_folder_textfield.setText(selectedDirectory
                    .getAbsolutePath());
        }
    }

    @FXML
    public void static_folder_fcn() {
        File selectedDirectory = RetentionFileChooser.showDirOpenDialog(this.primaryStage);
        if (selectedDirectory == null) {
        } else {
            this.static_folder_textfield.setText(selectedDirectory
                    .getAbsolutePath());
        }
    }

    @FXML
    public void shape_file_fcn() {
        File selectedFile = RetentionFileChooser.showOpenDialog(this.primaryStage, RetentionFileChooser.FilterMode.SHAPE_FILES);
        if (selectedFile == null) {
        } else {

            try {
                String shapefile = selectedFile.getAbsolutePath();
                this.countries = ProcessorUtilities.loadCountries(shapefile);

                if (this.countries.size() == 0) {
                    FxDialogs.showWarning("Empty shapefile",
                            "Could not load countries");
                    return;
                }
                ObservableList<String> options = FXCollections
                        .observableArrayList();
                for (Map.Entry<String, MultiPolygon> countryinfo : countries
                        .entrySet()) {
                    options.add(countryinfo.getKey());
                }
                options.sort(null);
                this.country_combo_box.getItems().clear();
                this.country_combo_box.getItems().addAll(options);
                this.country_combo_box.getSelectionModel().select(0);
                this.selected_index = 0;
                this.shape_folder_textfield.setText(shapefile);
            } catch (IOException e) {
                FxDialogs.showWarning("Problematic shapefile",
                        "Could not read shapefile.");
            }
        }

    }

    @FXML
    public void load_points_fcn() {
        File selectedFile = RetentionFileChooser.showOpenDialog(this.primaryStage, RetentionFileChooser.FilterMode.CSV_FILES);
        if (selectedFile == null) {
        } else {
            this.points_file_textfield.setText(selectedFile.getAbsolutePath());
        }

    }

    @FXML
    public void export_fcn() {
        String static_folder = this.static_folder_textfield.getText();
        try {
            final GeosProcessor processor = new GeosProcessor(static_folder);
            this.nuovo(processor, "TOTAL");
        } catch (IOException ex) {
            FxDialogs.showWarning("Problematic run", "Could not run tool");
        }
    }

    @FXML
    public void extract_pixels_fcn() {
        String thresholdText = this.proximityThreshold.getText();

        double thresholdValue = 5;

        try {
            thresholdValue = Double.parseDouble(thresholdText);
        } catch(NullPointerException | NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initStyle(StageStyle.UTILITY);
            alert.setTitle("Error");
            alert.setHeaderText("threshold is not a floating point");
            alert.setContentText("Use a floating point number");
            alert.showAndWait();
            this.proximityThreshold.setText("5");
            return;
        }
        String static_folder = this.static_folder_textfield.getText();
        String input_folder = this.seviri_folder_textfield.getText();
        String output_folder = this.output_folder_textfield.getText();
        String places_file = this.points_file_textfield.getText();
        final String[] seviri_files = (new File(input_folder)).list();
        final double threshold = thresholdValue;

        try {
            final GeosProcessor processor = new GeosProcessor(static_folder);
            List<Place> places = ProcessorUtilities.readPlaces(places_file);

            Service<String> service = new Service<String>() {
                @Override
                protected Task<String> createTask() {
                    return new Task<String>() {
                        @Override
                        protected String call() throws InterruptedException {
                            updateMessage("Extracting pixels . . .");
                            updateProgress(0, seviri_files.length);
                            for (int i = 0; i < seviri_files.length; i++) {
                                try {
                                    String fileName = input_folder + "/"
                                            + seviri_files[i];
                                    String name = (new File(fileName)).getName();
                                    String product_type = name.split("_")[3];
                                    String geographical_area = name.split("_")[4];
                                    String temptiming = name.split("_")[5];
                                    String csvFile = output_folder+"/pixelextract_" + product_type + "_"
                                            + geographical_area + "_" + temptiming + ".csv";
                                    processor
                                            .process(fileName, places, csvFile, threshold);
                                } catch (IOException ex) {
                                    return ex.getMessage();
                                }
                                updateProgress(i + 1, seviri_files.length);
                                updateMessage("Found " + (i + 1) + " of "
                                        + seviri_files.length + " runs!");
                            }
                            updateMessage("Extracted all.");
                            return null;
                        }
                    };
                }
            };

            service.setOnSucceeded(new javafx.event.EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent t) {
                    String val = service.getValue();

                    if (val != null) {
                        FxDialogs.showWarning("Problematic run",
                                "Could not run tool");
                    }
                }
            });

            service.setOnFailed(evt -> {
                System.err.println("The task failed with the following exception:");
                service.getException().printStackTrace(System.err);
            });

            FxDialogs.showProgress(this.primaryStage, "Progress Dialog", "Searching for runs",
                    service);
            service.start();

        } catch (IOException ex) {
            FxDialogs.showWarning("Problematic run", "Could not run tool");
        }
    }

    public GeosPixels getCountryPixels() {
        return this.my_country_pixels;
    }

    public void setCountryPixels(GeosPixels my_country_pixels) {
        this.my_country_pixels = my_country_pixels;
    }

    @FXML
    public void execute_seviri_fcn() {
        String static_folder = this.static_folder_textfield.getText();
        GeosProcessor processor = null;
        try {
            processor = new GeosProcessor(static_folder);

        } catch (IOException ex) {
            FxDialogs.showWarning("Problematic run", "Could not run tool");
            return;
        }

        String country_name = (String) this.country_combo_box
                .getSelectionModel().getSelectedItem();
        this.nuovo(processor, country_name);

    }

    public void nuovo(GeosProcessor processor, String country_name) {
        String input_folder = this.seviri_folder_textfield.getText();
        String output_folder = this.output_folder_textfield.getText();
        final String[] seviri_files = (new File(input_folder)).list();
        final Map<String, MultiPolygon> countries = this.countries;
        if (seviri_files.length == 0) {
            FxDialogs.showError("Problematic conversion", "No Seviri files found");
            return;
        }

        Service<String> service = new Service<String>() {
            @Override
            protected Task<String> createTask() {
                return new Task<String>() {
                    @Override
                    protected String call() throws InterruptedException {
                        updateMessage("Converting . . .");
                        int step = 0;
                        int steps = seviri_files.length;
                        if (!country_name.equals("TOTAL")) {
                            steps++;
                        }

                        updateProgress(step, steps);
                        MultiPolygon country_polygon = null;
                        if (!country_name.equals("TOTAL")) {
                            country_polygon = countries.get(country_name);
                        }

                        System.out.println("Seviri num of files" + seviri_files.length);
                        for (int i = 0; i < seviri_files.length; i++) {
                            try {
                                processor.doConversion(input_folder + "/"
                                                + seviri_files[i], output_folder,
                                        country_name, country_polygon);
                            } catch (GeosException ex) {
                                return ex.getMessage();
                            }
                            updateProgress(++step, steps);
                            updateMessage("Found file " + (i + 1) + " of "
                                    + seviri_files.length + " seviri files!");
                        }
                        updateMessage("Converted all.");
                        return null;
                    }
                };
            }
        };

        service.setOnSucceeded(new javafx.event.EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                String val = service.getValue();

                if (val != null) {
                    FxDialogs.showWarning("Problematic run",
                            "Could not run tool");
                }
            }
        });

        service.setOnFailed(evt -> {
            System.err.println("The task failed with the following exception:");
            service.getException().printStackTrace(System.err);
        });
        FxDialogs.showProgress(this.primaryStage, "Progress Dialog", "Searching for runs",
                service);
        service.start();

    }

    @FXML
    public boolean closeMe() {
        return false;
    }

    /**
     * Returns the main stage.
     *
     * @return
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

}