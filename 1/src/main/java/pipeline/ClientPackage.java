package pipeline;

import protocol.Package;

public class ClientPackage {
    private final Package pkg;
    private final Object clientId;
    public ClientPackage(Package pkg, Object clientId) {
        this.pkg = pkg;
        this.clientId = clientId;
    }
    public Package getPackage() { return pkg; }
    public Object getClientId() { return clientId; }
}