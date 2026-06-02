package pipeline;

public class ClientBytes {
    private final byte[] data;
    private final Object clientId;
    public ClientBytes(byte[] data, Object clientId) {
        this.data = data.clone();
        this.clientId = clientId;
    }
    public byte[] getData() { return data.clone(); }
    public Object getClientId() { return clientId; }
}