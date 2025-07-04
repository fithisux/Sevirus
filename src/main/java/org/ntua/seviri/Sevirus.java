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

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;

public class Sevirus extends Application {

    public Stage primaryStage;
    @FXML
    MainApp mainApp;
    private AnchorPane rootLayout;

    public static void main(String[] args) {
        launch(args);
    }

    public void initRootLayout() {
        try {
            // Load root layout from fxml file.
            FXMLLoader loader = new FXMLLoader();
            System.out.println(org.ntua.seviri.SeviriLauncher.class.getResource("/view/Seviri.fxml"));
            loader.setLocation(org.ntua.seviri.SeviriLauncher.class.getResource("/view/Seviri.fxml"));
            rootLayout = (AnchorPane) loader.load();
            this.mainApp = loader.getController();
            this.mainApp.setPrimaryStage(this.primaryStage);
            // Show the scene containing the root layout.
            Scene scene = new Scene(rootLayout);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Seviri Tool");
            primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {

                @Override
                public void handle(WindowEvent event) {
                    System.exit(0);
                    ;
                }
            });
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Seviri Pixel Extraction");
        initRootLayout();
    }

}
