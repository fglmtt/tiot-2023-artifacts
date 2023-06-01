package it.unimore.dipi.iot.digitaltwin.odte;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project wldt-digital-twin-mqtt
 * @created 08/05/2023 - 15:10
 */
public class OdteResultDescription {

    private double timeliness;

    private double reliability;

    private double availability;

    private double odte;

    public OdteResultDescription() {
    }

    public OdteResultDescription(double timeliness, double reliability, double availability, double odte) {
        this.timeliness = timeliness;
        this.reliability = reliability;
        this.availability = availability;
        this.odte = odte;
    }

    public double getTimeliness() {
        return timeliness;
    }

    public void setTimeliness(double timeliness) {
        this.timeliness = timeliness;
    }

    public double getReliability() {
        return reliability;
    }

    public void setReliability(double reliability) {
        this.reliability = reliability;
    }

    public double getAvailability() {
        return availability;
    }

    public void setAvailability(double availability) {
        this.availability = availability;
    }

    public double getOdte() {
        return odte;
    }

    public void setOdte(double odte) {
        this.odte = odte;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("OdteResultDescription{");
        sb.append("timeliness=").append(timeliness);
        sb.append(", reliability=").append(reliability);
        sb.append(", availability=").append(availability);
        sb.append(", odte=").append(odte);
        sb.append('}');
        return sb.toString();
    }
}
