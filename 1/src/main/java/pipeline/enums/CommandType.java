package pipeline.enums;

public enum CommandType {
    GET_QUANTITY(1),
    REMOVE_FROM_STOCK(2),
    DEPOSIT(3),
    ADD_GROUP(4),
    ADD_PRODUCT(5),
    SET_PRICE(6),
    DELETE_PRODUCT(7),
    UPDATE_PRODUCT(8),
    SEARCH_PRODUCTS(9),
    GET_PRODUCT(10),
    CREATE_PRODUCT(11);

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