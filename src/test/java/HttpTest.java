import dev.inventex.octa.http.Request;

public class HttpTest {
    public static void main(String[] args) {
        Request request = Request.Builder.get("https://google.com")
            .build();

    }
}
