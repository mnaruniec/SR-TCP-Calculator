package server;

import java.math.BigInteger;

public class WorkerComputation {
    // limits for operands' sizes, preventing client from causing server OOM
    public static final int DECIMAL_LIMIT = 100000000; // ~100MB
    public static final int BIT_LIMIT = DECIMAL_LIMIT / 80 * 33;

    public static String ERROR_MSG = "ERROR";

    private BigInteger partialResult = new BigInteger("0");
    private NextOperation nextOperation = NextOperation.PLUS;
    private StringBuilder nextValue = null;
    private boolean error = false;

    public String getResult() {
        if (error) {
            return ERROR_MSG;
        }

        // last token was an operation char
        if (nextOperation != NextOperation.EXPECTED && nextValue == null) {
            error = true;
            return ERROR_MSG;
        }

        runOperationIfReady();
        return partialResult.toString();
    }

    public void processInput(byte[] buffer, int read) {
        if (error) {
            return;
        }
        for (int i = 0; i < read; i++) {
            byte b = buffer[i];
            switch(b) {
                case '\t':
                case ' ':
                    runOperationIfReady();
                    break;
                case '+':
                    if (nextOperation != NextOperation.EXPECTED) {
                        if (!runOperationIfReady()) {
                            error = true;
                        }
                    }
                    nextOperation = NextOperation.PLUS;
                    break;
                case '-':
                    if (nextOperation != NextOperation.EXPECTED) {
                        if (!runOperationIfReady()) {
                            error = true;
                        }
                    }
                    nextOperation = NextOperation.MINUS;
                    break;
                default:
                    if (b >= '0' && b <= '9' && nextOperation != NextOperation.EXPECTED) {
                        appendDigit((char) b);
                    } else {
                        error = true;
                    }
            }
        }
        this.checkLimits();
    }

    private void appendDigit(char c) {
        if (nextValue == null) {
            nextValue = new StringBuilder();
        }
        nextValue.append(c);
    }

    private boolean runOperationIfReady() {
        if (nextValue != null) {
            runOperation();
            return true;
        }
        return false;
    }

    private void runOperation() {
        BigInteger newValue = new BigInteger(nextValue.toString());
        partialResult = nextOperation == NextOperation.PLUS
                ? partialResult.add(newValue)
                : partialResult.subtract(newValue);
        nextOperation = NextOperation.EXPECTED;
        nextValue = null;
    }

    private void checkLimits() {
        if (nextValue != null && nextValue.length() > DECIMAL_LIMIT) {
            error = true;
        } else if (partialResult.bitCount() > BIT_LIMIT) {
            error = true;
        }
    }
}
