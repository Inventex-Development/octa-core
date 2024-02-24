import dev.inventex.octa.console.LogLevel;
import dev.inventex.octa.console.Logger;

public class LoggerTest {
    public static void main(String[] args) {
        Logger.setLevel(LogLevel.WARN);

        Logger.info("This is an info message");
        Logger.warn("This is a warning message");
        Logger.error("This is an error message");
    }
}
