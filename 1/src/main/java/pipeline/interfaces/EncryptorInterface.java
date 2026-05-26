package pipeline.interfaces;

import protocol.Package;

public interface EncryptorInterface {
    byte[] encrypt(Package message);
}