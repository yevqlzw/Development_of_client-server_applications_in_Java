package pipeline.interfaces;

import protocol.Message;

public interface ProcessorInterface {
    void process(Message message);
}