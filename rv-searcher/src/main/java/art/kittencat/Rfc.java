package art.kittencat;

public record Rfc(String id,
                  String title,
                  String abstractText,
                  byte[] htmlCompressed,
                  byte[] textCompressed,
                  String words) {
}
