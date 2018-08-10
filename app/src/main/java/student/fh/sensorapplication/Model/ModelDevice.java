package student.fh.sensorapplication.Model;

public class ModelDevice {

    private String endpointName;
    private String endpointKey;
    private boolean isConnected;


    public ModelDevice(String endpointKey, String endpointName, boolean isConnected) {
        this.endpointName = endpointName;
        this.endpointKey = endpointKey;
        this.isConnected = isConnected;
    }

    public String getEndpointName() {
        return endpointName;
    }

    public String getEndpointKey() {
        return endpointKey;
    }

    public boolean isConnected() {
        return isConnected;
    }
}
