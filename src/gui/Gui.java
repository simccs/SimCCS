package gui;

import java.io.File;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

/**
 *
 * @author yaw
 */
public class Gui extends Application {

    private NetworkDisplay displayPane;
    private ChoiceBox scenarioChoice;
    private RadioButton dispDelaunayEdges;
    private RadioButton dispCandidateNetwork;
    private RadioButton sourceLabeled;
    private RadioButton sourceVisible;
    private RadioButton sinkLabeled;
    private RadioButton sinkVisible;
    private RadioButton dispCostSurface;
    private ChoiceBox runChoice;
    private AnchorPane solutionPane;
    private TextArea messenger;

    @Override
    public void start(Stage stage) {
        Scene scene = buildGUI(stage);
        stage.setScene(scene);
        stage.setTitle("SimCCS");
        stage.show();
    }

    public Scene buildGUI(Stage stage) {
        Group group = new Group();

        // Build display pane.
        displayPane = new NetworkDisplay();
        // Offset Network Display to account for controlPane.
        displayPane.setTranslateX(220);
        // Associate scroll/navigation actions.
        SceneGestures sceneGestures = new SceneGestures(displayPane);
        displayPane.addEventFilter(MouseEvent.MOUSE_PRESSED, sceneGestures.getOnMousePressedEventHandler());
        displayPane.addEventFilter(MouseEvent.MOUSE_DRAGGED, sceneGestures.getOnMouseDraggedEventHandler());
        displayPane.addEventFilter(ScrollEvent.ANY, sceneGestures.getOnScrollEventHandler());

        // Make background.
        Rectangle background = new Rectangle();
        background.setStroke(Color.WHITE);
        background.setFill(Color.WHITE);
        displayPane.getChildren().add(background);

        // Add base cost surface display.
        PixelatedImageView map = new PixelatedImageView();
        map.setPreserveRatio(true);
        map.setFitWidth(830);
        map.setFitHeight(660);
        map.setSmooth(false);
        displayPane.getChildren().add(map);

        // Action handler.
        ControlActions controlActions = new ControlActions(map, this);
        displayPane.setControlActions(controlActions);

        // Build tab background with messenger.
        AnchorPane messengerPane = new AnchorPane();
        messengerPane.setStyle("-fx-background-color: white; -fx-border-color: lightgrey;");
        messengerPane.setPrefSize(220, 80);
        messengerPane.setLayoutX(0);
        messengerPane.setLayoutY(580);
        messenger = new TextArea();
        messenger.setEditable(false);
        messenger.setWrapText(true);
        messenger.setPrefSize(192, 70);
        messenger.setLayoutX(14);
        messenger.setLayoutY(5);
        messengerPane.getChildren().add(messenger);
        controlActions.addMessenger(messenger);

        // Build tab pane and tabs.
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Tab dataTab = new Tab();
        dataTab.setText("Data");
        tabPane.getTabs().add(dataTab);
        Tab modelTab = new Tab();
        modelTab.setText("Model");
        tabPane.getTabs().add(modelTab);
        Tab resultsTab = new Tab();
        resultsTab.setText("Results");
        tabPane.getTabs().add(resultsTab);

        // Clear messenger when clicking in control area.
        tabPane.addEventFilter(MouseEvent.MOUSE_CLICKED,
                new EventHandler<MouseEvent>() {
            public void handle(MouseEvent event) {
                //messenger.clear();
            }
        });

        // Build data pane.
        AnchorPane dataPane = new AnchorPane();
        dataPane.setStyle("-fx-background-color: white; -fx-border-color: lightgrey;");
        dataPane.setPrefSize(220, 580);
        dataTab.setContent(dataPane);

        // Build model pane.
        AnchorPane modelPane = new AnchorPane();
        modelPane.setStyle("-fx-background-color: white; -fx-border-color: lightgrey;");
        modelPane.setPrefSize(220, 580);
        modelTab.setContent(modelPane);

        // Build results pane.
        AnchorPane resultsPane = new AnchorPane();
        resultsPane.setStyle("-fx-background-color: white; -fx-border-color: lightgrey;");
        resultsPane.setPrefSize(220, 580);
        resultsTab.setContent(resultsPane);

        // Populate data pane.
        // Build scenario selection control and add to control pane.
        scenarioChoice = new ChoiceBox();
        scenarioChoice.setPrefSize(150, 27);
        TitledPane scenarioContainer = new TitledPane("Scenario", scenarioChoice);
        scenarioContainer.setCollapsible(false);
        scenarioContainer.setPrefSize(192, 63);
        scenarioContainer.setLayoutX(14);
        scenarioContainer.setLayoutY(73);
        dataPane.getChildren().add(scenarioContainer);
        runChoice = new ChoiceBox();
        scenarioChoice.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> selected, String oldScenario, String newScenario) {
                controlActions.selectScenario(newScenario, background, runChoice);
            }
        });

        // Build dataset selection control and add to control pane.
        Button openDataset = new Button("[Open Dataset]");
        openDataset.setMnemonicParsing(false);
        openDataset.setPrefSize(150, 27);
        openDataset.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setTitle("Open Dataset");
                File selectedDataset = directoryChooser.showDialog(stage);
                if (selectedDataset != null) {
                    openDataset.setText(selectedDataset.getName());
                    controlActions.selectDataset(selectedDataset, scenarioChoice);
                }
            }
        });
        TitledPane datasetContainer = new TitledPane("Dataset", openDataset);
        datasetContainer.setCollapsible(false);
        datasetContainer.setPrefSize(192, 63);
        datasetContainer.setLayoutX(14);
        datasetContainer.setLayoutY(5);
        dataPane.getChildren().add(datasetContainer);

        //Build network buttons and add to control pane.
        Button candidateNetwork = new Button("Candidate Network");
        candidateNetwork.setLayoutX(27);
        candidateNetwork.setLayoutY(4);
        candidateNetwork.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                controlActions.generateCandidateGraph();
            }
        });

        AnchorPane buttonPane = new AnchorPane();
        buttonPane.setPrefSize(190, 30);
        buttonPane.setMinSize(0, 0);
        buttonPane.getChildren().addAll(candidateNetwork);
        TitledPane networkContainer = new TitledPane("Network Generation", buttonPane);
        networkContainer.setCollapsible(false);
        networkContainer.setPrefSize(192, 63);
        networkContainer.setLayoutX(14);
        networkContainer.setLayoutY(141);
        dataPane.getChildren().add(networkContainer);

        //Build display selection legend and add to control pane.
        AnchorPane selectionPane = new AnchorPane();
        selectionPane.setPrefSize(206, 237);
        selectionPane.setMinSize(0, 0);

        dispDelaunayEdges = new RadioButton("Raw Delaunay Edges");
        dispDelaunayEdges.setLayoutX(4);
        dispDelaunayEdges.setLayoutY(83);
        selectionPane.getChildren().add(dispDelaunayEdges);
        Pane rawDelaunayLayer = new Pane();
        sceneGestures.addEntityToResize(rawDelaunayLayer);
        displayPane.getChildren().add(rawDelaunayLayer);
        controlActions.addRawDelaunayLayer(rawDelaunayLayer);
        dispDelaunayEdges.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> selected, Boolean oldVal, Boolean show) {
                controlActions.toggleRawDelaunayDisplay(show);
            }
        });

        dispCandidateNetwork = new RadioButton("Candidate Network");
        dispCandidateNetwork.setLayoutX(4);
        dispCandidateNetwork.setLayoutY(106);
        selectionPane.getChildren().add(dispCandidateNetwork);
        Pane candidateNetworkLayer = new Pane();
        sceneGestures.addEntityToResize(candidateNetworkLayer);
        displayPane.getChildren().add(candidateNetworkLayer);
        controlActions.addCandidateNetworkLayer(candidateNetworkLayer);
        dispCandidateNetwork.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> selected, Boolean oldVal, Boolean show) {
                controlActions.toggleCandidateNetworkDisplay(show);
            }
        });

        Label sourceLabel = new Label("Sources:");
        sourceLabel.setLayoutX(2);
        sourceLabel.setLayoutY(5);
        selectionPane.getChildren().add(sourceLabel);

        // Toggle source locations display button.
        sourceLabeled = new RadioButton("Label");  // Need reference before definition.
        sourceVisible = new RadioButton("Visible");
        sourceVisible.setLayoutX(62);
        sourceVisible.setLayoutY(4);
        selectionPane.getChildren().add(sourceVisible);
        Pane sourcesLayer = new Pane();
        displayPane.getChildren().add(sourcesLayer);
        controlActions.addSourceLocationsLayer(sourcesLayer);
        sceneGestures.addEntityToResize(sourcesLayer);
        sourceVisible.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> selected, Boolean oldVal, Boolean show) {
                if (!show) {
                    sourceLabeled.setSelected(false);
                }
                controlActions.toggleSourceDisplay(show);
            }
        });

        // Toggle source labels display button.
        sourceLabeled.setLayoutX(131);
        sourceLabeled.setLayoutY(4);
        selectionPane.getChildren().add(sourceLabeled);
        Pane sourceLabelsLayer = new Pane();
        displayPane.getChildren().add(sourceLabelsLayer);
        controlActions.addSourceLabelsLayer(sourceLabelsLayer);
        sceneGestures.addEntityToResize(sourceLabelsLayer);
        sourceLabeled.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> selected, Boolean oldVal, Boolean show) {
                if (!sourceVisible.isSelected()) {
                    show = false;
                    sourceLabeled.setSelected(false);
                }
                controlActions.toggleSourceLabels(show);
            }
        });

        Label sinkLabel = new Label("Sinks:");
        sinkLabel.setLayoutX(19);
        sinkLabel.setLayoutY(30);
        selectionPane.getChildren().add(sinkLabel);

        // Toggle sink locations display button.
        sinkLabeled = new RadioButton("Label");  // Need reference before definition.
        sinkVisible = new RadioButton("Visible");
        sinkVisible.setLayoutX(62);
        sinkVisible.setLayoutY(29);
        selectionPane.getChildren().add(sinkVisible);
        Pane sinksLayer = new Pane();
        displayPane.getChildren().add(sinksLayer);
        controlActions.addSinkLocationsLayer(sinksLayer);
        sceneGestures.addEntityToResize(sinksLayer);
        sinkVisible.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> selected, Boolean oldVal, Boolean show) {
                if (!show) {
                    sinkLabeled.setSelected(false);
                }
                controlActions.toggleSinkDisplay(show);
            }
        });

        // Toggle sink labels.
        sinkLabeled.setLayoutX(131);
        sinkLabeled.setLayoutY(29);
        selectionPane.getChildren().add(sinkLabeled);
        Pane sinkLabelsLayer = new Pane();
        displayPane.getChildren().add(sinkLabelsLayer);
        controlActions.addSinkLabelsLayer(sinkLabelsLayer);
        sceneGestures.addEntityToResize(sinkLabelsLayer);
        sinkLabeled.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> selected, Boolean oldVal, Boolean show) {
                if (!sinkVisible.isSelected()) {
                    show = false;
                    sinkLabeled.setSelected(false);
                }
                controlActions.toggleSinkLabels(show);
            }
        });

        // Toggle cost surface button.
        dispCostSurface = new RadioButton("Cost Surface");
        dispCostSurface.setLayoutX(4);
        dispCostSurface.setLayoutY(60);
        selectionPane.getChildren().add(dispCostSurface);
        dispCostSurface.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> selected, Boolean oldVal, Boolean show) {
                controlActions.toggleCostSurface(show, background);
            }
        });

        TitledPane selectionContainer = new TitledPane("Legend", selectionPane);
        selectionContainer.setCollapsible(false);
        selectionContainer.setPrefSize(192, 156);
        selectionContainer.setLayoutX(14);
        selectionContainer.setLayoutY(210);
        dataPane.getChildren().add(selectionContainer);

        // Solution area
        AnchorPane formulationPane = new AnchorPane();
        formulationPane.setPrefSize(206, 237);
        formulationPane.setMinSize(0, 0);

        Label crfLabel = new Label("Capital Recovery Rate");
        crfLabel.setLayoutX(4);
        crfLabel.setLayoutY(8);
        formulationPane.getChildren().add(crfLabel);
        TextField crfValue = new TextField(".1");
        crfValue.setEditable(true);
        crfValue.setPrefColumnCount(2);
        crfValue.setLayoutX(143);
        crfValue.setLayoutY(4);
        formulationPane.getChildren().add(crfValue);

        Label yearLabel = new Label("Number of Years");
        yearLabel.setLayoutX(4);
        yearLabel.setLayoutY(38);
        formulationPane.getChildren().add(yearLabel);
        TextField yearValue = new TextField("30");
        yearValue.setEditable(true);
        yearValue.setPrefColumnCount(2);
        yearValue.setLayoutX(143);
        yearValue.setLayoutY(34);
        formulationPane.getChildren().add(yearValue);

        Label paramLabel = new Label("Capture Target (MT/y)");
        paramLabel.setLayoutX(4);
        paramLabel.setLayoutY(68);
        formulationPane.getChildren().add(paramLabel);
        TextField paramValue = new TextField("15");
        paramValue.setEditable(true);
        paramValue.setPrefColumnCount(2);
        paramValue.setLayoutX(143);
        paramValue.setLayoutY(64);
        formulationPane.getChildren().add(paramValue);

        RadioButton capVersion = new RadioButton("Cap");
        RadioButton priceVersion = new RadioButton("Price");
        ToggleGroup capVsPrice = new ToggleGroup();
        capVersion.setToggleGroup(capVsPrice);
        priceVersion.setToggleGroup(capVsPrice);

        capVersion.setLayoutX(5);
        capVersion.setLayoutY(94);
        formulationPane.getChildren().add(capVersion);
        capVersion.setSelected(true);
        capVersion.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> selected, Boolean oldVal, Boolean show) {
                if (!oldVal) {
                    priceVersion.setSelected(false);
                    paramLabel.setText("Capture Target (MT/y)");
                    paramValue.setText("15");
                }
            }
        });

        priceVersion.setLayoutX(60);
        priceVersion.setLayoutY(94);
        formulationPane.getChildren().add(priceVersion);
        priceVersion.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> selected, Boolean oldVal, Boolean show) {
                if (!oldVal) {
                    capVersion.setSelected(false);
                    paramLabel.setText("Tax/Credit ($/t)");
                    paramValue.setText("0");
                }
            }
        });

        // Populate model pane.
        TitledPane modelContainer = new TitledPane("Problem Formulation", formulationPane);
        modelContainer.setCollapsible(false);
        modelContainer.setPrefSize(192, 147);
        modelContainer.setLayoutX(14);
        modelContainer.setLayoutY(5);
        modelPane.getChildren().add(modelContainer);

        // Solution pane.
        AnchorPane mipSolutionPane = new AnchorPane();
        mipSolutionPane.setPrefSize(192, 100);
        mipSolutionPane.setMinSize(0, 0);

        Button generateSolutionFile = new Button("Generate MPS File");
        generateSolutionFile.setLayoutX(33);
        generateSolutionFile.setLayoutY(5);
        mipSolutionPane.getChildren().add(generateSolutionFile);
        generateSolutionFile.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                String modelVersion = "";
                if (capVersion.isSelected()) {
                    modelVersion = "c";
                } else if (priceVersion.isSelected()) {
                    modelVersion = "p";
                }

                controlActions.generateMPSFile(crfValue.getText(), yearValue.getText(), paramValue.getText(), modelVersion);
            }
        });

        Label solverLabel = new Label("Solver:");
        solverLabel.setLayoutX(4);
        solverLabel.setLayoutY(44);
        mipSolutionPane.getChildren().add(solverLabel);

        Button cplexSolve = new Button("CPLEX");
        cplexSolve.setLayoutX(51);
        cplexSolve.setLayoutY(38);
        mipSolutionPane.getChildren().add(cplexSolve);
        cplexSolve.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                controlActions.runCPLEX();
            }
        });

        // Populate MIP solution method pane.
        TitledPane mipSolutionContainer = new TitledPane("MIP Solver", mipSolutionPane);
        mipSolutionContainer.setCollapsible(false);
        mipSolutionContainer.setPrefSize(192, 95);
        mipSolutionContainer.setLayoutX(14);
        mipSolutionContainer.setLayoutY(158);
        modelPane.getChildren().add(mipSolutionContainer);

        // Populate results pane.
        // Build solution selection control.
        runChoice.setPrefSize(150, 27);
        runChoice.setLayoutX(20);
        runChoice.setLayoutY(4);

        solutionPane = new AnchorPane();
        solutionPane.setPrefSize(190, 30);
        solutionPane.setMinSize(0, 0);
        solutionPane.getChildren().add(runChoice);

        TitledPane resultsContainer = new TitledPane("Load Solution", solutionPane);
        resultsContainer.setCollapsible(false);
        resultsContainer.setPrefSize(192, 94);
        resultsContainer.setLayoutX(14);
        resultsContainer.setLayoutY(5);
        resultsPane.getChildren().add(resultsContainer);

        // Build solution display area.
        AnchorPane solutionDisplayPane = new AnchorPane();
        solutionDisplayPane.setPrefSize(190, 30);
        solutionDisplayPane.setMinSize(0, 0);
        solutionDisplayPane.setLayoutX(0);
        solutionDisplayPane.setLayoutY(110);
        resultsPane.getChildren().add(solutionDisplayPane);

        // Solution labels.
        Label sources = new Label("Sources:");
        sources.setLayoutX(69);
        sources.setLayoutY(0);
        Label sourcesValue = new Label("-");
        sourcesValue.setLayoutX(135);
        sourcesValue.setLayoutY(0);
        solutionDisplayPane.getChildren().addAll(sources, sourcesValue);

        Label sinks = new Label("Sinks:");
        sinks.setLayoutX(86);
        sinks.setLayoutY(20);
        Label sinksValue = new Label("-");
        sinksValue.setLayoutX(135);
        sinksValue.setLayoutY(20);
        solutionDisplayPane.getChildren().addAll(sinks, sinksValue);

        Label stored = new Label("Annual CO2 Stored:");
        stored.setLayoutX(2);
        stored.setLayoutY(40);
        Label storedValue = new Label("-");
        storedValue.setLayoutX(135);
        storedValue.setLayoutY(40);
        solutionDisplayPane.getChildren().addAll(stored, storedValue);

        Label edges = new Label("Edges:");
        edges.setLayoutX(81);
        edges.setLayoutY(60);
        Label edgesValue = new Label("-");
        edgesValue.setLayoutX(135);
        edgesValue.setLayoutY(60);
        solutionDisplayPane.getChildren().addAll(edges, edgesValue);

        Label length = new Label("Project Length:");
        length.setLayoutX(30);
        length.setLayoutY(80);
        Label lengthValue = new Label("-");
        lengthValue.setLayoutX(135);
        lengthValue.setLayoutY(80);
        solutionDisplayPane.getChildren().addAll(length, lengthValue);

        Label total = new Label("Total Cost\n   ($m/yr)");
        total.setLayoutX(65);
        total.setLayoutY(120);
        Label unit = new Label("Unit Cost\n ($/tCO2)");
        unit.setLayoutX(150);
        unit.setLayoutY(120);
        solutionDisplayPane.getChildren().addAll(total, unit);

        Label cap = new Label("Capture:");
        cap.setLayoutX(4);
        cap.setLayoutY(160);
        Label capT = new Label("-");
        capT.setLayoutX(75);
        capT.setLayoutY(160);
        Label capU = new Label("-");
        capU.setLayoutX(160);
        capU.setLayoutY(160);
        solutionDisplayPane.getChildren().addAll(cap, capT, capU);

        Label trans = new Label("Transport:");
        trans.setLayoutX(4);
        trans.setLayoutY(180);
        Label transT = new Label("-");
        transT.setLayoutX(75);
        transT.setLayoutY(180);
        Label transU = new Label("-");
        transU.setLayoutX(160);
        transU.setLayoutY(180);
        solutionDisplayPane.getChildren().addAll(trans, transT, transU);

        Label stor = new Label("Storage:");
        stor.setLayoutX(4);
        stor.setLayoutY(200);
        Label storT = new Label("-");
        storT.setLayoutX(75);
        storT.setLayoutY(200);
        Label storU = new Label("-");
        storU.setLayoutX(160);
        storU.setLayoutY(200);
        solutionDisplayPane.getChildren().addAll(stor, storT, storU);

        Label tot = new Label("Total:");
        tot.setLayoutX(4);
        tot.setLayoutY(220);
        Label totT = new Label("-");
        totT.setLayoutX(75);
        totT.setLayoutY(220);
        Label totU = new Label("-");
        totU.setLayoutX(160);
        totU.setLayoutY(220);
        solutionDisplayPane.getChildren().addAll(tot, totT, totU);

        Label[] solutionValues = new Label[]{sourcesValue, sinksValue, storedValue, edgesValue, lengthValue, capT, capU, transT, transU, storT, storU, totT, totU};

        // Run selection action.
        runChoice.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> selected, String oldSolution, String newSolution) {
                controlActions.selectSolution(newSolution, solutionValues);
            }
        });
        runChoice.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (isShowing) {
                controlActions.initializeSolutionSelection(runChoice);
            }
        });
        runChoice.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override
            public void handle(ScrollEvent s) {
                double direction = s.getDeltaY();
                String currentChoice = (String) runChoice.getValue();
                ObservableList<String> choices = runChoice.getItems();
                int index = choices.indexOf(currentChoice);
                if (direction < 0 && index < choices.size() - 1) {
                    runChoice.setValue(choices.get(index + 1));
                } else if (direction > 0 && index > 0) {
                    runChoice.setValue(choices.get(index - 1));
                }
            }
        });

        Pane solutionLayer = new Pane();
        displayPane.getChildren().add(solutionLayer);
        controlActions.addSolutionLayer(solutionLayer);
        sceneGestures.addEntityToResize(solutionLayer);

        // Add everything to group and display.
        group.getChildren().addAll(displayPane, tabPane, messengerPane);
        return new Scene(group, 1050, 660);
    }

    public void displayCostSurface() {
        dispCostSurface.setSelected(true);
    }

    public void fullReset() {
        //scenarioChoice;
        dispDelaunayEdges.setSelected(false);
        dispCandidateNetwork.setSelected(false);
        sourceLabeled.setSelected(false);
        sourceVisible.setSelected(false);
        sinkLabeled.setSelected(false);
        sinkVisible.setSelected(false);
        dispCostSurface.setSelected(false);
        messenger.setText("");
    }

    public void softReset() {
        dispDelaunayEdges.setSelected(false);
        dispCandidateNetwork.setSelected(false);
        sourceLabeled.setSelected(false);
        sourceVisible.setSelected(false);
        sinkLabeled.setSelected(false);
        sinkVisible.setSelected(false);
        dispCostSurface.setSelected(false);
        messenger.setText("");
    }

    public double getScale() {
        return displayPane.getScale();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
