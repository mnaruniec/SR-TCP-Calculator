package server;

import java.math.BigInteger;

public class WorkerComputation {
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
            System.out.println(1);
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
            System.out.println("char i: " + b);
            switch(b) {
                case '\t':
                case ' ':
                    runOperationIfReady();
                    break;
                case '+':
                    if (nextOperation != NextOperation.EXPECTED) {
                        if (!runOperationIfReady()) {
                            System.out.println(2);
                            error = true;
                        }
                    }
                    nextOperation = NextOperation.PLUS;
                    break;
                case '-':
                    if (nextOperation != NextOperation.EXPECTED) {
                        if (!runOperationIfReady()) {
                            System.out.println(2);
                            error = true;
                        }
                    }
                    nextOperation = NextOperation.MINUS;
                    break;
                default:
                    if (b >= '0' && b <= '9' && nextOperation != NextOperation.EXPECTED) {
                        appendDigit((char) b);
                    } else {
                        System.out.println("4: i = " + i);

                        error = true;
                    }
            }
        }
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
}
