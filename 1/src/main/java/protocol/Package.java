package protocol;

import java.util.Objects;

public class Package {
    private byte bSrc;
    private long bPktId;

    public Package() {}

    public Package(byte bSrc, long bPktId, Message message) {
        this.bSrc = bSrc;
        this.bPktId = bPktId;
        this.message = message;
    }

    private Message message;

    public byte getbSrc() {
        return bSrc;
    }

    public void setbSrc(byte bSrc) {
        this.bSrc = bSrc;
    }

    public long getbPktId() {
        return bPktId;
    }

    public void setbPktId(long bPktId) {
        this.bPktId = bPktId;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Package aPackage = (Package) o;
        return bSrc == aPackage.bSrc &&
                bPktId == aPackage.bPktId &&
                Objects.equals(message, aPackage.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bSrc, bPktId, message);
    }
}
