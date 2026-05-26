package pipeline.enums;

public enum CommandType {
    GET_QUANTITY(1),
    REMOVE_FROM_STOCK(2),
    DEPOSIT(3),
    ADD_GROUP(4),
    ADD_PRODUCT(5),
    SET_PRICE(6);

    private final int code;

    CommandType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static CommandType fromCode(int code) {
        for (CommandType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown command: " + code);
    }
}