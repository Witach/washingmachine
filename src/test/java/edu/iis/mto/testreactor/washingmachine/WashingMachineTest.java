package edu.iis.mto.testreactor.washingmachine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WashingMachineTest {

    @Mock
    private DirtDetector dirtDetector;
    @Mock
    private Engine engine;
    @Mock
    private WaterPump waterPump;
    private WashingMachine washingMashine;

    @BeforeEach
    void setUp() throws Exception {
        washingMashine = new WashingMachine(dirtDetector, engine, waterPump);
    }

    @Nested
    class WashingMachineStateTest {

        @Test
        void shouldEndWashingWithSuccessStatus() {
            LaundryBatch laundryBatch = getNotTooHeavyBatch();
            ProgramConfiguration programConfiguration = createProgramConfiguration();

            LaundryStatus returnedLaundryStatus = washingMashine.start(laundryBatch, programConfiguration);

            assertEquals(ErrorCode.NO_ERROR, returnedLaundryStatus.getErrorCode());
        }

        @Test
        void shouldEndWashingWithNotToHeavyLightMaterial(){
            LaundryBatch laundryBatch = getNotTooHeavyBatchWithLightMaterial();
            ProgramConfiguration programConfiguration = createProgramConfiguration();

            LaundryStatus returnedLaundryStatus = washingMashine.start(laundryBatch, programConfiguration);

            assertEquals(ErrorCode.NO_ERROR, returnedLaundryStatus.getErrorCode());
        }

        @Test
        void shouldReturnAnErrorWhenTooHeavyWithHeavyMaterial() {
            LaundryBatch laundryBatch = getTooHeavyBatchWithHeavyMaterial();
            ProgramConfiguration programConfiguration = createProgramConfiguration();

            LaundryStatus returnedLaundryStatus = washingMashine.start(laundryBatch, programConfiguration);

            assertEquals(ErrorCode.TOO_HEAVY, returnedLaundryStatus.getErrorCode());
        }

        @Test
        void shouldReturnAnErrorWhenToHeavyWithLightMaterial() {
            LaundryBatch laundryBatch = getTooHeavyBatchWithLightMaterial();
            ProgramConfiguration programConfiguration = createProgramConfiguration();

            LaundryStatus returnedLaundryStatus = washingMashine.start(laundryBatch, programConfiguration);

            assertEquals(ErrorCode.TOO_HEAVY, returnedLaundryStatus.getErrorCode());
        }


        @Test
        void shouldReturnAnErrorWhenWaterPumpFails() throws WaterPumpException {
            LaundryBatch laundryBatch = getNotTooHeavyBatch();
            ProgramConfiguration programConfiguration = createProgramConfiguration();
            makeWaterPompFaulty(waterPump);

            LaundryStatus returnedLaundryStatus = washingMashine.start(laundryBatch, programConfiguration);

            assertEquals(ErrorCode.WATER_PUMP_FAILURE, returnedLaundryStatus.getErrorCode());
        }

        @Test
        void shouldReturnAnEngineErrorWhenEngineFails() throws EngineException {
            LaundryBatch laundryBatch = getNotTooHeavyBatch();
            ProgramConfiguration programConfiguration = createProgramConfiguration();
            makeEngineFaulty(engine);

            LaundryStatus returnedLaundryStatus = washingMashine.start(laundryBatch, programConfiguration);

            assertEquals(ErrorCode.ENGINE_FAILURE, returnedLaundryStatus.getErrorCode());
        }

        @Test
        void shouldReturnAnUnknownErrorWhenUnknownExceptionOccurs() throws EngineException {
            LaundryBatch laundryBatch = getNotTooHeavyBatch();
            ProgramConfiguration programConfiguration = createProgramConfiguration();
            makeEngineFaultyInUnknownWay(engine);

            LaundryStatus returnedLaundryStatus = washingMashine.start(laundryBatch, programConfiguration);

            assertEquals(ErrorCode.UNKNOWN_ERROR, returnedLaundryStatus.getErrorCode());
        }

        @Test
        void shouldDetectLongTime() {
            LaundryBatch laundryBatch = getNotTooHeavyBatch();
            ProgramConfiguration programConfiguration = createAutoProgramConfiguration();
            makeDirtDetectorReturnPercentageOfDirt(70);

            LaundryStatus returnedLaundryStatus = washingMashine.start(laundryBatch, programConfiguration);

            assertEquals(Program.LONG, returnedLaundryStatus.getRunnedProgram());
        }

        @Test
        void shouldDetectMediumTime() {
            LaundryBatch laundryBatch = getNotTooHeavyBatch();
            ProgramConfiguration programConfiguration = createAutoProgramConfiguration();
            makeDirtDetectorReturnPercentageOfDirt(30);

            LaundryStatus returnedLaundryStatus = washingMashine.start(laundryBatch, programConfiguration);

            assertEquals(Program.MEDIUM, returnedLaundryStatus.getRunnedProgram());
        }
    }

    @Nested
    class WashingMachineBehaviourTest{


        @Test
        void shouldWashInCorrectSequenceWithStaticProgram() throws WaterPumpException, EngineException {
            LaundryBatch laundryBatch = getNotTooHeavyBatch();
            ProgramConfiguration programConfiguration = createProgramConfiguration();

            washingMashine.start(laundryBatch, programConfiguration);

            InOrder inOrder = inOrder(dirtDetector, engine, waterPump);
            inOrder.verify(waterPump).pour(anyDouble());
            inOrder.verify(engine).runWashing(anyInt());
            inOrder.verify(waterPump).release();
            inOrder.verify(engine).spin();
        }

        @Test
        void shouldWashInCorrectSequenceWithAutoProgram() throws WaterPumpException, EngineException {
            LaundryBatch laundryBatch = getNotTooHeavyBatch();
            ProgramConfiguration programConfiguration = createAutoProgramConfiguration();

            makeDirtDetectorReturnPercentageOfDirt(40);

            washingMashine.start(laundryBatch, programConfiguration);

            InOrder inOrder = inOrder(dirtDetector, engine, waterPump);
            inOrder.verify(dirtDetector).detectDirtDegree(any());
            inOrder.verify(waterPump).pour(anyDouble());
            inOrder.verify(engine).runWashing(anyInt());
            inOrder.verify(waterPump).release();
            inOrder.verify(engine).spin();
        }

        @Test
        void shouldNotSpinIfProgramIsWithoutSpin() throws EngineException {
            LaundryBatch laundryBatch = getNotTooHeavyBatch();

            ProgramConfiguration programConfiguration = ProgramConfiguration.builder()
                    .withSpin(false)
                    .withProgram(Program.LONG)
                    .build();

            washingMashine.start(laundryBatch, programConfiguration);

            verify(engine,times(0)).spin();

        }

        @Test
        void shouldNotStartWashWhenTooHeavy(){
            LaundryBatch laundryBatch = getTooHeavyBatchWithHeavyMaterial();

            ProgramConfiguration programConfiguration = createProgramConfiguration();

            washingMashine.start(laundryBatch, programConfiguration);
            verifyNoInteractions(engine,dirtDetector,waterPump);
        }

    }


    void makeWaterPompFaulty(WaterPump waterPump) throws WaterPumpException {
        doThrow(new WaterPumpException())
                .when(waterPump)
                .pour(anyDouble());
    }

    void makeEngineFaulty(Engine engine) throws EngineException {
        doThrow(new EngineException()).when(engine)
                .runWashing(anyInt());
    }

    LaundryBatch getNotTooHeavyBatch() {
        return LaundryBatch.builder()
                .withMaterialType(Material.JEANS)
                .withWeightKg(3)
                .build();
    }

    LaundryBatch getNotTooHeavyBatchWithLightMaterial() {
        return LaundryBatch.builder()
                .withMaterialType(Material.DELICATE)
                .withWeightKg(3)
                .build();
    }

    LaundryBatch getTooHeavyBatchWithHeavyMaterial() {
        return LaundryBatch.builder()
                .withMaterialType(Material.JEANS)
                .withWeightKg(30)
                .build();
    }

    LaundryBatch getTooHeavyBatchWithLightMaterial() {
        return LaundryBatch.builder()
                .withMaterialType(Material.SYNTETIC)
                .withWeightKg(300)
                .build();
    }

    void makeEngineFaultyInUnknownWay(Engine engine) throws EngineException {
        doThrow(new RuntimeException()).when(engine)
                .runWashing(anyInt());
    }

    ProgramConfiguration createProgramConfiguration() {
        return ProgramConfiguration.builder()
                .withProgram(Program.LONG)
                .build();
    }

    ProgramConfiguration createAutoProgramConfiguration() {
        return ProgramConfiguration.builder()
                .withProgram(Program.AUTODETECT)
                .withSpin(true)
                .build();
    }


    void makeDirtDetectorReturnPercentageOfDirt(double percentValue) {
        when(dirtDetector.detectDirtDegree(any()))
                .thenReturn(new Percentage(percentValue));
    }

}
