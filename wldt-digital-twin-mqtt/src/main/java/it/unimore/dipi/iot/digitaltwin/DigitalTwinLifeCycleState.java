package it.unimore.dipi.iot.digitaltwin;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project wldt-digital-twin-mqtt
 * @created 18/04/2023 - 09:55
 */
public enum DigitalTwinLifeCycleState {
    STARTED(1),
    UN_BOUND(2),
    BOUND(3),
    UN_SYNC(4),
    SHADOWED(5);

    private int value;

    private DigitalTwinLifeCycleState(int value){
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
