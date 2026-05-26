package pipeline;

public enum ComponentType {
    RECEIVER("Receiver"),
    DECRYPTOR("Decryptor"),
    PROCESSOR("Processor"),
    ENCRYPTOR("Encryptor"),
    SENDER("Sender");

    private final String label;

    ComponentType(String label) {
        this.label = label;
    }

    public String getName(int id) {
        return label + "-" + id;
    }
}