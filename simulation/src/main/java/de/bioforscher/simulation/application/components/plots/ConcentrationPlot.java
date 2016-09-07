package de.bioforscher.simulation.application.components.plots;

import de.bioforscher.chemistry.descriptive.ChemicalEntity;
import de.bioforscher.core.events.UpdateEventListener;
import de.bioforscher.simulation.application.components.species.ColorableChemicalEntity;
import de.bioforscher.simulation.application.renderer.SpeciesColorManager;
import de.bioforscher.simulation.model.BioNode;
import de.bioforscher.simulation.model.NodeUpdatedEvent;
import de.bioforscher.simulation.modules.model.PotentialUpdate;
import de.bioforscher.simulation.modules.model.Simulation;
import de.bioforscher.simulation.util.BioGraphUtilities;
import de.bioforscher.simulation.util.EnvironmentalVariables;
import de.bioforscher.simulation.util.SingaPreferences;
import de.bioforscher.units.UnitDictionary;
import de.bioforscher.units.quantities.MolarConcentration;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.util.StringConverter;

import javax.measure.Quantity;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The chart is used for visualization of BioNode concentrations changes over
 * the course of a simulation.
 *
 * @author Christoph Leberecht
 */
public class ConcentrationPlot extends LineChart<Number, Number> implements UpdateEventListener<NodeUpdatedEvent> {

    private ObservableList<ChemicalEntity> observedEntities = FXCollections.observableArrayList();
    private Simulation simulation;
    // mirrors the data received from events
    private Map<Integer, Set<PotentialUpdate>> mirroredData;
    private BioNode referencedNode;

    private int maximalDataPoints;
    private int tickSpacing;
    private boolean scaleXAxis = false;

    public ConcentrationPlot(Set<ChemicalEntity> observedEntities, BioNode referencedNode, Simulation simulation) {
        super(new NumberAxis(), new NumberAxis());
        this.simulation = simulation;
        this.referencedNode = referencedNode;
        this.mirroredData = new HashMap<>();
        setObservedSpecies(observedEntities);
        initializeData();
        initializePreferences();
        configureChart();
        configureXAxis();
        configureYAxis();
    }

    private void initializePreferences() {
        SingaPreferences preferences = new SingaPreferences();
        this.maximalDataPoints = preferences.preferences
                .getInt(SingaPreferences.Plot.MAXIMAL_DATA_POINTS, SingaPreferences.Plot.MAXIMAL_DATA_POINTS_VALUE);
        this.tickSpacing = preferences.preferences
                .getInt(SingaPreferences.Plot.TICK_SPACING, SingaPreferences.Plot.TICK_SPACING_VALUE);
    }

    private void initializeData() {
        for (ChemicalEntity entity : this.observedEntities) {
            XYChart.Series<Number, Number> series = new Series<>();
            series.setName(entity.getName());
            this.getData().add(series);
            SpeciesColorManager.getInstance().initializeEntity(entity, ColorableChemicalEntity.generateRandomColor());
            series.getNode().setStyle("-fx-stroke: " +
                    ColorableChemicalEntity.getHexColor(SpeciesColorManager.getInstance().getColor(entity)) + " ");
        }
    }

    public void updateColor(ChemicalEntity entity) {
        Series<Number, Number> series = this.getData().stream()
                .filter(s -> s.getName().equals(entity.getName()))
                .findFirst().get();
        series.getNode().setStyle("-fx-stroke: " +
                ColorableChemicalEntity.getHexColor(SpeciesColorManager.getInstance().getColor(entity)) + " ");
    }

    private void configureChart() {
        this.setAnimated(false);
        // this.setMinHeight(250);
        // this.setMinWidth(300);
    }

    private void configureXAxis() {
        this.getXAxis().setAutoRanging(false);
        // TODO false for swiping style
        ((NumberAxis) this.getXAxis()).setForceZeroInRange(true);
        ((NumberAxis) this.getXAxis()).setLowerBound(0);
        ((NumberAxis) this.getXAxis()).setUpperBound(this.maximalDataPoints);
        ((NumberAxis) this.getXAxis()).setTickUnit(this.tickSpacing);
        this.getXAxis().setLabel("Time in " + EnvironmentalVariables.getInstance().getTimeStep().getUnit().toString());
        ((NumberAxis) this.getXAxis()).setTickLabelFormatter(new StringConverter<Number>() {

            private NumberFormat formatter = new DecimalFormat("0.000E0");

            @Override
            public String toString(Number object) {
                return this.formatter.format(object.doubleValue() * EnvironmentalVariables.getInstance().getTimeStep()
                        .getValue().doubleValue());
            }

            @Override
            public Number fromString(String string) {
                return 0;
            }
        });
    }

    private void configureYAxis() {
        this.getYAxis().setLabel("Molar concentration in " + UnitDictionary.GRAM_PER_MOLE.toString());
    }

    public void setObservedSpecies(Set<ChemicalEntity> observedSpecies) {
        observedSpecies.forEach(this.observedEntities::add);
    }

    public void addSpecies(ChemicalEntity entity) {
        this.observedEntities.add(entity);
        Series<Number, Number> series = new Series<>();
        series.setName(entity.getName());
        this.getData().add(series);
    }

    public void removeSpecies(ChemicalEntity entity) {
        this.observedEntities.remove(entity);
        this.getData().stream()
                .filter(series -> series.getName().equals(entity.getName()))
                .forEach(this.getData()::remove);
    }

    public void hideSeries(ChemicalEntity entity) {
        this.getData().stream()
                .filter(series -> series.getName().equals(entity.getName()))
                .forEach(series -> series.getNode().setVisible(false));
    }

    public void showSeries(ChemicalEntity entity) {
        this.getData().stream()
                .filter(series -> series.getName().equals(entity.getName()))
                .forEach(series -> series.getNode().setVisible(true));
    }

    @Override
    protected void dataItemAdded(Series<Number, Number> series, int itemIndex, Data<Number, Number> item) {
        // suppress printing of points
    }

    @Override
    public void onEventReceived(NodeUpdatedEvent event) {

        if (event.getNode().equals(this.referencedNode)) {
            Map<ChemicalEntity, Quantity<MolarConcentration>> concentrations = event.getNode().getConcentrations();
            // TODO iterate over species instead of series
            for (ChemicalEntity entity : this.observedEntities) {
                // get associated value
                Series<Number, Number> series = this.getData().stream()
                        .filter(s -> s.getName().equals(entity.getName()))
                        .findFirst().get();
                // add to mirrored values
                this.mirroredData.put(event.getEpoch(), BioGraphUtilities.collectAsPotentialUpdates(concentrations));
                // get concentration of entity
                double concentration = concentrations.get(entity).getValue().doubleValue();
                // add to plot
                Platform.runLater(() -> {
                    series.getData().add(new XYChart.Data<>(event.getEpoch(), concentration));
                    if (this.scaleXAxis) {
                        if (series.getData().size() > this.maximalDataPoints) {
                            series.getData().remove(series.getData().size() - this.maximalDataPoints);
                        }
                    }
                });

            }
            if (this.scaleXAxis) {
                ((NumberAxis) this.getXAxis()).setLowerBound(event.getEpoch() - this.maximalDataPoints);
                ((NumberAxis) this.getXAxis()).setUpperBound(event.getEpoch() - 1);
            } else {
                ((NumberAxis) this.getXAxis()).setUpperBound(event.getEpoch() - 1);
                if (event.getEpoch() % 6 == 0) {
                    ((NumberAxis) this.getXAxis()).setTickUnit(event.getEpoch() / 6);
                }
            }
        }

    }

    public ObservableList<ChemicalEntity> getObservedEntities() {
        return this.observedEntities;
    }

    public BioNode getReferencedNode() {
        return this.referencedNode;
    }


}